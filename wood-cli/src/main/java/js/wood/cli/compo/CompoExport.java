package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.InputStreamEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "export", description = "Export component to repository.")
public class CompoExport extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about exported files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component name, path relative to project root. Ex: res/page/home", converter = CompoNameConverter.class)
	private CompoName name;

	private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();

	@Override
	protected ExitCode exec() throws IOException {
		if (!name.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name.value()));
		}

		Path workingDir = files.getProjectDir();
		Path compoDir = workingDir.resolve(name.path());
		if (!files.exists(compoDir)) {
			console.print("Missing component directory %s.", compoDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		// WOOD project naming convention: descriptor file basename is the same as component directory
		Path descriptorFile = compoDir.resolve(files.getFileName(compoDir) + ".xml");
		if (!files.exists(descriptorFile)) {
			console.print("Missing component descriptor %s.", descriptorFile);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		CompoCoordinates compoCoordinates = compoCoordinates(descriptorFile);
		if (!compoCoordinates.isValid()) {
			console.print("Invalid component descriptor %s. Missing component coordinates.", descriptorFile);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		console.print("Exporting component %s...", name);
		if (verbose) {
			console.print("Cleanup repository component %s.", compoCoordinates);
		}
		cleanupRepositoryComponent(compoCoordinates);

		for (Path compoFile : files.listFiles(compoDir)) {
			if (verbose) {
				console.print("Upload file %s.", compoFile);
			}
			uploadComponentFile(compoFile, compoCoordinates);
		}
		return ExitCode.SUCCESS;
	}

	private void cleanupRepositoryComponent(CompoCoordinates coordinates) throws IOException {
		String url = String.format("%s/%s/", config.get("repository.url"), coordinates.toPath());
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			HttpDelete httpDelete = new HttpDelete(url);
			try (CloseableHttpResponse response = client.execute(httpDelete)) {
				int statusCode = response.getStatusLine().getStatusCode();
				if(statusCode == 404) {
					if(verbose) {
						console.print("Component %s not existing on repository.", coordinates.toPath());
					}
					return;
				}
				if (statusCode != 200) {
					throw new IOException(format("Fail to cleanup component %s", coordinates));
				}
			}
		}
	}

	private void uploadComponentFile(Path compoFile, CompoCoordinates coordinates) throws IOException {
		String url = String.format("%s/%s/%s", config.get("repository.url"), coordinates.toPath(), files.getFileName(compoFile));
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setHeader("Content-Type", "application/octet-stream");
			httpPost.setEntity(new InputStreamEntity(files.getInputStream(compoFile)));

			try (CloseableHttpResponse response = client.execute(httpPost)) {
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new IOException(format("Fail to upload file %s", compoFile));
				}
			}
		}
	}

	private CompoCoordinates compoCoordinates(Path descriptorFile) throws IOException {
		DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
		Document descriptorDoc = documentBuilder.loadXML(files.getReader(descriptorFile));
		String groupId = text(descriptorDoc, "groupId");
		String artifactId = text(descriptorDoc, "artifactId");
		String version = text(descriptorDoc, "version");
		return new CompoCoordinates(groupId, artifactId, version);
	}

	private static String text(Document doc, String tagName) {
		Element element = doc.getByTag(tagName);
		return element != null ? element.getText() : null;
	}

	// --------------------------------------------------------------------------------------------
	// Tests support

	void setCommandSpec(CommandSpec commandSpec) {
		this.commandSpec = commandSpec;
	}

	void setName(CompoName name) {
		this.name = name;
	}

	void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	void setHttpClientBuilder(HttpClientBuilder httpClientBuilder) {
		this.httpClientBuilder = httpClientBuilder;
	}
}
