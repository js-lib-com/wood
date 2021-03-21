package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import js.util.Files;
import js.wood.cli.Task;
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
	protected int exec() throws IOException {
		print("Import component %s into %s", coordinates, path);

		File repositoryDir = config.get("repository.dir", File.class);
		File compoDir = new File(repositoryDir, coordinates.toFile());
		if (reload) {
			if (!compoDir.exists() && !compoDir.mkdirs()) {
				throw new IOException("Cannot create component directory.");
			}
			downloadCompoment(coordinates, compoDir);
		} else {
			if (!compoDir.exists()) {
				if (!compoDir.mkdirs()) {
					throw new IOException(format("Cannot create component directory %s.", compoDir));
				}
				downloadCompoment(coordinates, compoDir);
			}
		}

		File workingDir = workingDir();
		File targetDir = new File(workingDir, path);
		targetDir = new File(targetDir, coordinates.getArtifactId());
		if (!targetDir.exists() && !targetDir.mkdirs()) {
			throw new IOException("Cannot create component directory.");
		}
		if (reload) {
			Files.removeFilesHierarchy(targetDir);
		}

		File[] compoFiles = compoDir.listFiles();
		if (compoFiles == null) {
			throw new IOException("Cannot list component files.");
		}
		for (File compoFile : compoFiles) {
			if (verbose) {
				print("Import file %s.", compoFile);
			}
			Files.copy(compoFile, new File(targetDir, compoFile.getName()));
		}

		return 0;
	}

	/** Pattern for files listed on index page. */
	private static final Pattern FILE_PATTERN = Pattern.compile("^[a-z0-9_.\\-]+\\.[a-z0-9]+$", Pattern.CASE_INSENSITIVE);

	/**
	 * Download component files from repository into target directory. This method assume repository server is configured with
	 * page indexing. If first loads remote directory index, then scan and download all links matching {@link #FILE_PATTERN}.
	 * 
	 * @param name component name,
	 * @param targetDir target directory.
	 * @throws IOException if download fails for whatever reason.
	 */
	private void downloadCompoment(CompoCoordinates name, File targetDir) throws IOException {
		URL indexPageURL = new URL(String.format("%s/%s/", config.get("repository.url"), name.toPath()));
		DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
		Document indexPageDoc = documentBuilder.loadHTML(indexPageURL);

		for (Element linkElement : indexPageDoc.findByXPath("//*[@href]")) {
			String link = linkElement.getAttr("href");
			Matcher matcher = FILE_PATTERN.matcher(link);
			if (matcher.find()) {
				URL linkURL = new URL(indexPageURL, link);
				if (verbose) {
					print("Download file %s.", linkURL);
				}
				Files.copy(linkURL, new File(targetDir, link));
			}
		}
	}
}
