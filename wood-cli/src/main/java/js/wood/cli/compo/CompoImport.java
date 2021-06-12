package js.wood.cli.compo;

import static java.lang.String.format;
import static js.util.Strings.concat;

import java.io.IOException;
import java.net.URI;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.FilesUtil;
import com.jslib.commons.cli.IFile;
import com.jslib.commons.cli.Task;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;
import js.wood.WOOD;
import js.wood.WoodException;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "import", description = "Import component from repository.")
public final class CompoImport extends Task {
	private static final Pattern ARTIFACT_DIR_PATTERN = Pattern.compile("^[a-z0-9-]+/$", Pattern.CASE_INSENSITIVE);
	private static final Pattern VERSION_DIR_PATTERN = Pattern.compile("^\\d+\\.\\d+(?:\\.\\d+)?/$", Pattern.CASE_INSENSITIVE);
	/** Pattern for files listed on index page. */
	private static final Pattern FILE_PATTERN = Pattern.compile("^[a-z0-9_.\\-]+\\.[a-z0-9]+$", Pattern.CASE_INSENSITIVE);

	@Option(names = { "-r", "--reload" }, description = "Force reload from repository. Local component files are deleted.")
	private boolean reload;
	@Option(names = { "-y", "--yes" }, description = "Auto-confirm import.")
	private boolean yes;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component repository coordinates, e.g. com.js-lib.web:captcha:1.1", converter = CompoCoordinatesConverter.class, arity = "0..1")
	private CompoCoordinates coordinates;
	@Parameters(index = "1", description = "Target directory path, relative to project root.", arity = "0..1")
	private String path;

	private DocumentBuilder documentBuilder;
	private CompoRepository repository;

	@Override
	protected ExitCode exec() throws Exception {
		console.print("Import component.");
		documentBuilder = Classes.loadService(DocumentBuilder.class);
		repository = new CompoRepository();

		if (coordinates == null) {
			String groupId = console.input("component group");
			URI groupURI = URI(config.getex("repository.url"), groupId);

			for (IFile artifact : httpRequest.getApacheDirectoryIndex(groupURI, ARTIFACT_DIR_PATTERN)) {
				console.print("- %s", artifact.getName());
			}
			String artifactId = console.input("component artifact");
			URI artifactURI = URI(groupURI.toString(), artifactId);

			for (IFile version : httpRequest.getApacheDirectoryIndex(artifactURI, VERSION_DIR_PATTERN)) {
				console.print("- %s", version.getName());
			}
			String version = console.input("component version");
			coordinates = new CompoCoordinates(groupId, artifactId, version);
		}

		return importComponent(coordinates);
	}

	private ExitCode importComponent(CompoCoordinates compoCoordinates) throws Exception {
		console.print("Importing %s", compoCoordinates);

		String path = console.input("local path");
		if (!yes) {
			console.warning("All component '%s' files will be permanently removed and replaced.", path);
			if (!console.confirm("Please confirm: yes | [no]", "yes")) {
				console.print("User cancel.");
				return ExitCode.CANCEL;
			}
		}

		// recursive import of the component's dependencies
		for (CompoCoordinates dependency : repository.getCompoDependencies(compoCoordinates)) {
			ExitCode code = importComponent(dependency);
			if (code != ExitCode.SUCCESS) {
				return code;
			}
		}

		Path projectDir = files.getProjectDir();
		Path compoDir = projectDir.resolve(path);
		if (!files.exists(compoDir)) {
			files.createDirectory(compoDir);
		}

		Path descriptorFile = compoDir.resolve(files.getFileName(compoDir) + ".xml");
		if (!files.exists(descriptorFile)) {
			throw new WoodException("Invalid component |%s|. Missing component descriptor.", compoDir);
		}
		CompoDescriptor compoDescriptor = new CompoDescriptor(files, descriptorFile);

		compoDescriptor.createScripts();
		for (CompoCoordinates coordinates : compoDescriptor.getDependencies()) {
			Path projectCompoPath = projectCompoDir(coordinates);
			Path scriptFile = projectCompoPath != null ? compoFile(projectCompoPath, "js") : null;
			compoDescriptor.addScriptDependency(scriptFile);
		}

		compoDescriptor.removeDependencies();
		compoDescriptor.save();

		return ExitCode.SUCCESS;
	}

	/**
	 * Copy repository component identified by its coordinates to project component directory. While importing into project, it
	 * is acceptable that source repository component to be renamed. If this is the case, this method takes care to rename
	 * layout, style, script and descriptor files.
	 * <p>
	 * After all files are copied into project component, this method takes care to update WOOD operators into layout file(s) -
	 * see {@link #updateLayoutOperators(Path)}.
	 * <p>
	 * Warning: this method remove all target component directory files.
	 * 
	 * @param compoCoordinates coordinates for repository component,
	 * @param compoDir project component directory.
	 * @throws IOException if copy operation fails.
	 */
	void compoCopy(CompoCoordinates compoCoordinates, Path compoDir) throws IOException {
		files.cleanDirectory(compoDir, verbose);

		files.walkFileTree(repository.getCompoDir(compoCoordinates), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				String fileName = files.getFileName(file);
				// ensure that original repository component files are renamed using project component directory name
				// this applies to layout, style, script and descriptor files
				if (Files.basename(fileName).equals(compoCoordinates.getArtifactId())) {
					fileName = concat(files.getFileName(compoDir), '.', Files.getExtension(fileName));
				}
				if (verbose) {
					console.print("Copy file %s", file);
				}
				files.copy(file, compoDir.resolve(fileName));
				return FileVisitResult.CONTINUE;
			}
		});

		files.walkFileTree(compoDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (files.getFileName(file).endsWith(".htm")) {
					updateLayoutOperators(file, config.get("project.operators"));
				}
				return FileVisitResult.CONTINUE;
			}
		});
	}

	/**
	 * Post process layout file operators after component files copied to project. This method scan layout document and rename
	 * WOOD operators accordingly project naming strategy, as defined by <code>project.operators</code> configuration property.
	 * Keep in mind that on repository component operators always use <code>XMLNS</code> naming convention.
	 * <p>
	 * Also, if operator value is a reference to a dependency component, find it on project file system and resolve operator
	 * path value. On repository, component references are always represented as component coordinates.
	 * 
	 * @param layoutFile layout file path.
	 * @throws IOException if layout file reading or document parsing fail.
	 */
	void updateLayoutOperators(Path layoutFile, String operatorsNaming) throws IOException {
		boolean hasNamespace = "XMLNS".equals(operatorsNaming);
		String prefix = "DATA_ATTR".equals(operatorsNaming) ? "data-" : "";
		Path projectDir = files.getProjectDir();

		try {
			Document document = documentBuilder.loadXMLNS(files.getReader(layoutFile));

			for (String operator : new String[] { "template", "editable", "content", "compo", "param" }) {
				for (Element element : document.findByAttrNS(WOOD.NS, operator)) {
					String value = element.getAttrNS(WOOD.NS, operator);

					// if value is a component coordinates for a dependency, resolve it to project component path
					CompoCoordinates coordinates = CompoCoordinates.parse(value);
					if (coordinates != null) {
						// convert project component directory into component path
						// WOOD component path always uses slash ('/') as separator and is always relative to project root
						value = projectDir.relativize(projectCompoDir(coordinates)).toString().replace('\\', '/');
					}

					if (!hasNamespace) {
						element.removeAttrNS(WOOD.NS, operator);
						element.setAttr(prefix + operator, value);
					} else {
						element.setAttrNS(WOOD.NS, operator, value);
					}
				}
			}

			if (!hasNamespace) {
				document.removeNamespaceDeclaration(WOOD.NS);
			}

			// close writer and do not serialize XML declaration
			document.serialize(files.getWriter(layoutFile), true, false);
		} catch (SAXException e) {
			throw new IOException(format("Fail to update operators on layout file |%s|: %s: %s", layoutFile, e.getClass(), e.getMessage()));
		}
	}

	Path compoFile(Path compoPath, String extension) {
		String compoName = files.getFileName(compoPath);
		return compoPath.resolve(concat(compoName, '.', extension));
	}

	Path projectCompoDir(CompoCoordinates coordinates) throws IOException {
		class Component {
			Path path = null;
		}
		final Component component = new Component();

		files.walkFileTree(files.getProjectDir(), new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
				Path descriptorFile = dir.resolve(files.getFileName(dir) + ".xml");
				if (!files.exists(descriptorFile)) {
					return FileVisitResult.CONTINUE;
				}
				CompoDescriptor descriptor = new CompoDescriptor(files, descriptorFile);
				if (!coordinates.equals(descriptor.getCoordinates())) {
					return FileVisitResult.CONTINUE;
				}
				component.path = dir;
				return FileVisitResult.TERMINATE;
			}
		});

		return component.path;
	}

	static URI URI(String server, String... paths) {
		StringBuilder uri = new StringBuilder(server);
		if (!uri.toString().endsWith("/")) {
			uri.append('/');
		}
		for (String path : paths) {
			uri.append(path.replace('.', '/'));
			if (!path.endsWith("/")) {
				uri.append('/');
			}
		}
		return URI.create(uri.toString());
	}

	// --------------------------------------------------------------------------------------------

	class CompoRepository {
		private final Path repositoryDir;

		private boolean reload;
		private boolean verbose;

		public CompoRepository() throws IOException {
			this.repositoryDir = files.getPath(config.getex("repository.dir"));
		}

		void setReload(boolean reload) {
			this.reload = reload;
		}

		/**
		 * Get the directory path from local components repository (cache) that contains component identified by given
		 * coordinates.
		 * 
		 * @param coordinates component coordinates.
		 * @return component repository directory.
		 */
		public Path getCompoDir(CompoCoordinates coordinates) {
			return repositoryDir.resolve(coordinates.toPath());
		}

		public List<CompoCoordinates> getCompoDependencies(CompoCoordinates coordinates) throws XPathExpressionException, IOException, SAXException {
			Path repositoryCompoDir = repositoryDir.resolve(coordinates.toPath());
			if (!files.exists(repositoryCompoDir)) {
				reload = true;
			}
			if (reload) {
				files.createDirectories(repositoryCompoDir);
				files.cleanDirectory(repositoryCompoDir, verbose);
				downloadCompoment(coordinates, repositoryCompoDir);
			}

			Path compoDir = repositoryDir.resolve(coordinates.toPath());
			Path descriptorFile = compoDir.resolve(coordinates.getArtifactId() + ".xml");
			CompoDescriptor descriptor = new CompoDescriptor(files, descriptorFile);
			return descriptor.getDependencies();
		}

		/**
		 * Download component files from repository into target directory. This method assume repository server is configured
		 * with page indexing. If first loads remote directory index, then scan and download all links matching
		 * {@link #FILE_PATTERN}.
		 * 
		 * @param targetDir target directory.
		 * @throws IOException if download fails for whatever reason.
		 * @throws SAXException
		 * @throws XPathExpressionException
		 */
		private void downloadCompoment(CompoCoordinates coordinates, Path targetDir) throws IOException, SAXException, XPathExpressionException {
			URI indexPage = URI.create((format("%s/%s/", config.getex("repository.url"), coordinates.toPath())));
			Document indexPageDoc = httpRequest.loadHTML(indexPage);

			for (Element linkElement : indexPageDoc.findByXPath("//*[@href]")) {
				String link = linkElement.getAttr("href");
				Matcher matcher = FILE_PATTERN.matcher(link);
				if (matcher.find()) {
					URI linkURI = indexPage.resolve(link);
					Path file = targetDir.resolve(Strings.last(link, '/'));
					if (verbose) {
						console.print("Download file %s.", linkURI);
					}
					httpRequest.download(linkURI, file);
					if (verbose) {
						console.print("Copy file %s.", file);
					}
				}
			}
		}
	}

	static class CompoDescriptor {
		private static final DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);

		private final FilesUtil files;
		private final Path descriptorFile;
		private final Document document;
		private final boolean hasDpendencies;

		private Element scriptElement;

		public CompoDescriptor(FilesUtil files, Path descriptorFile) throws IOException {
			this.files = files;
			this.descriptorFile = descriptorFile;
			try {
				this.document = documentBuilder.loadXML(files.getReader(descriptorFile));
			} catch (SAXException e) {
				throw new IOException(format("Fail to parse component descriptor |%s|.", descriptorFile));
			}
			this.hasDpendencies = document.getByTag("dependencies") != null;
		}

		public List<CompoCoordinates> getDependencies() {
			List<CompoCoordinates> dependencies = new ArrayList<>();
			for (Element dependency : document.findByTag("dependency")) {
				dependencies.add(new CompoCoordinates(getText(dependency, "groupId"), getText(dependency, "artifactId"), getText(dependency, "version")));
			}
			return dependencies;
		}

		public void createScripts() {
			if (!hasDpendencies) {
				return;
			}
			Element scriptsElement = document.getByTag("scripts");
			if (scriptsElement == null) {
				scriptsElement = document.createElement("scripts");
				document.getRoot().addChild(scriptsElement);
			}
			Path scriptFile = files.changeExtension(descriptorFile, "js");
			scriptElement = document.createElement("script", "src", src(scriptFile));
			scriptsElement.addChild(scriptElement);
		}

		public void addScriptDependency(Path scriptFile) {
			if (scriptElement != null) {
				scriptElement.addChild(document.createElement("dependency", "src", src(scriptFile)));
			}
		}

		private String src(Path scriptFile) {
			return files.getProjectDir().relativize(scriptFile).toString().replace('\\', '/');
		}

		public CompoCoordinates getCoordinates() {
			String groupId = getText("groupId");
			String artifactId = getText("artifactId");
			String version = getText("version");
			return new CompoCoordinates(groupId, artifactId, version);
		}

		private String getText(String tagName) {
			Element element = document.getByTag(tagName);
			return element != null ? element.getText() : null;
		}

		private static String getText(Element element, String tagName) {
			Element childElement = element.getByTag(tagName);
			return childElement != null ? childElement.getText() : null;
		}

		public void removeDependencies() {
			if (!hasDpendencies) {
				return;
			}
			Element dependencies = document.getByTag("dependencies");
			if (dependencies != null) {
				dependencies.remove();
			}
		}

		public void save() throws IOException {
			document.serialize(files.getWriter(descriptorFile), true);
		}

		Document getDocument() {
			return document;
		}

		void setScriptElement(Element scriptElement) {
			this.scriptElement = scriptElement;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tests support

	void setCoordinates(CompoCoordinates coordinates) {
		this.coordinates = coordinates;
	}

	CompoCoordinates getCoordinates() {
		return coordinates;
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

	void setRepository(CompoRepository repository) {
		this.repository = repository;
	}
}
