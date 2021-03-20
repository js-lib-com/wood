package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.FileEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
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

	@Parameters(index = "0", description = "Component path relative to project root.")
	private String path;

	@Override
	protected int exec() throws Exception {
		print("Exporting component %s...", path);

		File workingDir = workingDir();
		File compoDir = new File(workingDir, path);
		if (!compoDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", path));
		}

		// WOOD project naming convention: descriptor file basename is the same as component directory
		File descriptorFile = new File(compoDir, compoDir.getName() + ".xml");
		if (!descriptorFile.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Invalid component %s. Missing descriptor.", path));
		}
		CompoCoordinates compoCoordinates = compoCoordinates(descriptorFile);
		if (!compoCoordinates.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Invalid descriptor for component %s. Missing component coordinates.", path));
		}

		File[] compoFiles = compoDir.listFiles();
		if (compoFiles == null) {
			throw new IOException(format("Cannot list files for component %s.", compoDir));
		}

		if (verbose) {
			print("Cleanup repository component %s.", compoCoordinates);
		}
		cleanupRepositoryComponent(compoCoordinates);
		for (File compoFile : compoFiles) {
			if (verbose) {
				print("Upload file %s.", compoFile);
			}
			uploadComponentFile(compoFile, compoCoordinates);
		}
		return 0;
	}

	private static void cleanupRepositoryComponent(CompoCoordinates compoName) throws IOException {
		String url = String.format("http://maven.js-lib.com/%s/", compoName.toPath());
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpDelete httpDelete = new HttpDelete(url);
			CloseableHttpResponse response = client.execute(httpDelete);
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new IOException(format("Fail to cleanup component %s", compoName));
			}
		}
	}

	private static void uploadComponentFile(File compoFile, CompoCoordinates compoName) throws IOException {
		String url = String.format("http://maven.js-lib.com/%s/%s", compoName.toPath(), compoFile.getName());
		try (CloseableHttpClient client = HttpClients.createDefault()) {
			HttpPost httpPost = new HttpPost(url);
			httpPost.setHeader("Content-Type", "application/octet-stream");
			httpPost.setEntity(new FileEntity(compoFile));

			CloseableHttpResponse response = client.execute(httpPost);
			if (response.getStatusLine().getStatusCode() != 200) {
				throw new IOException(format("Fail to upload file %s", compoFile));
			}
		}
	}

	private static CompoCoordinates compoCoordinates(File descriptorFile) throws FileNotFoundException {
		DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
		Document descriptorDoc = documentBuilder.loadXML(descriptorFile);
		String groupId = text(descriptorDoc, "groupId");
		String artifactId = text(descriptorDoc, "artifactId");
		String version = text(descriptorDoc, "version");
		return new CompoCoordinates(groupId, artifactId, version);
	}

	private static String text(Document doc, String tagName) {
		Element element = doc.getByTag(tagName);
		return element != null ? element.getText() : null;
	}
}
