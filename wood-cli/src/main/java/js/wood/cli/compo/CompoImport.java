package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.xml.sax.SAXException;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "import", description = "Import component from repository.")
public final class CompoImport extends Task {
	@Option(names = { "-r", "--reload" }, description = "Force reload from repository. Local component files are deleted.")
	private boolean reload;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component repository coordinates, e.g. com.js-lib.web:captcha:1.1", converter = CompoCoordinatesConverter.class)
	private CompoCoordinates coordinates;
	@Parameters(index = "1", description = "Target directory path, relative to project root.")
	private String path;

	@Override
	protected ExitCode exec() throws IOException, XPathExpressionException, SAXException {
		console.print("Import component %s into %s", coordinates, path);

		Path repositoryDir = files.getPath(config.get("repository.dir"));
		Path repositoryCompoDir = repositoryDir.resolve(coordinates.toFile());
		if (reload) {
			if (!files.exists(repositoryCompoDir)) {
				files.createDirectory(repositoryCompoDir);
			}
			files.cleanDirectory(repositoryCompoDir, verbose);
			downloadCompoment(repositoryCompoDir);
		} else {
			if (!files.exists(repositoryCompoDir)) {
				files.createDirectory(repositoryCompoDir);
				downloadCompoment(repositoryCompoDir);
			}
		}

		Path projectDir = files.getProjectDir();
		Path projectCompoDir = projectDir.resolve(path);
		projectCompoDir = projectCompoDir.resolve(coordinates.getArtifactId());
		if (!files.exists(projectCompoDir)) {
			files.createDirectory(projectCompoDir);
		}

		console.warning("All component '%s' files will be permanently removed and replaced.", path);
		if (!console.confirm("Please confirm: yes | [no]", "yes")) {
			console.print("User cancel.");
			return ExitCode.CANCEL;
		}

		files.cleanDirectory(projectCompoDir, verbose);
		files.copyFiles(repositoryCompoDir, projectCompoDir, verbose);
		return ExitCode.SUCCESS;
	}

	/** Pattern for files listed on index page. */
	private static final Pattern FILE_PATTERN = Pattern.compile("^[a-z0-9_.\\-]+\\.[a-z0-9]+$", Pattern.CASE_INSENSITIVE);

	private DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
	private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

	/**
	 * Download component files from repository into target directory. This method assume repository server is configured with
	 * page indexing. If first loads remote directory index, then scan and download all links matching {@link #FILE_PATTERN}.
	 * 
	 * @param targetDir target directory.
	 * @throws IOException if download fails for whatever reason.
	 * @throws SAXException 
	 * @throws XPathExpressionException 
	 */
	private void downloadCompoment(Path targetDir) throws IOException, SAXException, XPathExpressionException {
		URL indexPageURL = new URL(format("%s/%s/", config.get("repository.url"), coordinates.toPath()));
		Document indexPageDoc = documentBuilder.loadHTML(indexPageURL);

		for (Element linkElement : indexPageDoc.findByXPath("//*[@href]")) {
			String link = linkElement.getAttr("href");
			Matcher matcher = FILE_PATTERN.matcher(link);
			if (matcher.find()) {
				URL linkURL = new URL(indexPageURL, link);
				if (verbose) {
					console.print("Download file %s.", linkURL);
				}
				
				try (CloseableHttpClient client = httpClientBuilder.build()) {
					HttpGet httpGet = new HttpGet(linkURL.toExternalForm());
					try (CloseableHttpResponse response = client.execute(httpGet)) {
						if (response.getStatusLine().getStatusCode() != 200) {
							throw new IOException(format("Fail to cleanup component %s", coordinates));
						}
					}
				}
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tests support

	void setCoordinates(CompoCoordinates coordinates) {
		this.coordinates = coordinates;
	}

	void setPath(String path) {
		this.path = path;
	}

	void setReload(boolean reload) {
		this.reload = reload;
	}

	void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	void setDocumentBuilder(DocumentBuilder documentBuilder) {
		this.documentBuilder = documentBuilder;
	}

	void setHttpClientBuilder(HttpClientBuilder httpClientBuilder) {
		this.httpClientBuilder = httpClientBuilder;
	}
}
