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
import js.util.Strings;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.ITypeConverter;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "import", description = "Import component.")
public final class CompoImport extends Task {
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component versioned name, e.g. com.js-lib:captcha:1.1", converter = NameConverter.class)
	private Name name;
	@Parameters(index = "1", description = "Project library path.")
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

		File groupDir = new File(mavenRepositoryDir, name.groupId.replace('.', File.separatorChar));
		File artifactDir = new File(groupDir, name.artifactId);
		// version is the last directory from path that hosts component files
		File compoDir = new File(artifactDir, name.version);

		if (!compoDir.exists()) {
			if (!compoDir.mkdirs()) {
				throw new IOException("Cannot create component directory.");
			}
			downloadCompoment(name, compoDir);
		}

		File workingDir = workingDir();
		File targetDir = new File(workingDir, path);
		targetDir = new File(targetDir, name.artifactId);
		if (!targetDir.exists() && !targetDir.mkdirs()) {
			throw new IOException("Cannot create component directory.");
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
	private void downloadCompoment(Name name, File targetDir) throws IOException {
		URL indexPageURL = new URL(String.format("http://maven.js-lib.com/%s/%s/%s/", name.groupId.replace('.', '/'), name.artifactId, name.version));
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

	private static class Name {
		final String groupId;
		final String artifactId;
		final String version;

		public Name(String groupId, String artifactId, String version) {
			this.groupId = groupId;
			this.artifactId = artifactId;
			this.version = version;
		}

		@Override
		public String toString() {
			return Strings.concat(groupId, ':', artifactId, ':', version);
		}
	}

	private static class NameConverter implements ITypeConverter<Name> {
		private static final Pattern MAVEN_COORDINATES = Pattern.compile("^([^:]+):([^:]+):([^:]+)$", Pattern.CASE_INSENSITIVE);

		@Override
		public Name convert(String value) throws Exception {
			Matcher matcher = MAVEN_COORDINATES.matcher(value);
			if (matcher.find()) {
				return new Name(matcher.group(1), matcher.group(2), matcher.group(3));
			}
			return null;
		}
	}
}
