package js.wood.cli.compo;

import java.io.File;
import java.io.IOException;
import java.net.URL;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.lang.BugError;
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

	@Parameters(index = "0", description = "Component versioned name, e.g. com.js-lib.web:captcha:1.1", converter = CompoNameConverter.class)
	private CompoName name;
	@Parameters(index = "1", description = "Target directory path, relative to project root.")
	private String path;

	@Override
	protected int exec() throws IOException {
		print("Import component %s into %s", name, path);

		String mavenHome = System.getProperty("MAVEN_HOME");
		if (mavenHome == null) {
			throw new BugError("Missing -DMAVEN_HOME from Java arguments.");
		}
		File mavenHomeDir = new File(mavenHome);
		File mavenRepositoryDir = new File(mavenHomeDir, "repository");
		if (!mavenRepositoryDir.exists()) {
			throw new BugError("Missing repository.");
		}

		File groupDir = new File(mavenRepositoryDir, name.getGroupId().replace('.', File.separatorChar));
		File artifactDir = new File(groupDir, name.getArtifactId());
		// version is the last directory from path that hosts component files
		File compoDir = new File(artifactDir, name.getVersion());

		if (reload) {
			if (!compoDir.exists() && !compoDir.mkdirs()) {
				throw new IOException("Cannot create component directory.");
			}
			downloadCompoment(name, compoDir);
		} else {
			if (!compoDir.exists()) {
				if (!compoDir.mkdirs()) {
					throw new IOException("Cannot create component directory.");
				}
				downloadCompoment(name, compoDir);
			}
		}

		File workingDir = workingDir();
		File targetDir = new File(workingDir, path);
		targetDir = new File(targetDir, name.getArtifactId());
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
	private void downloadCompoment(CompoName name, File targetDir) throws IOException {
		URL indexPageURL = new URL(String.format("http://maven.js-lib.com/%s/", name.toPath()));
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
