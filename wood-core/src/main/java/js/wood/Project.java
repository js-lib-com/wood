package js.wood;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import js.util.Files;
import js.util.Params;
import js.util.Strings;
import js.wood.impl.AttrOperatorsHandler;
import js.wood.impl.CustomElementsRegistry;
import js.wood.impl.DataAttrOperatorsHandler;
import js.wood.impl.IOperatorsHandler;
import js.wood.impl.MediaQueryDefinition;
import js.wood.impl.OperatorsNaming;
import js.wood.impl.ProjectDescriptor;
import js.wood.impl.ScriptDescriptor;
import js.wood.impl.XmlnsOperatorsHandler;

/**
 * A WOOD project is a collection of loosely coupled components. It has a root directory and all paths are relative to this
 * project root. Project instance is initialized from {@link ProjectDescriptor}, but all descriptor properties have sensible
 * default values, therefore descriptor is optional. There is also a mandatory project properties file that contains settings
 * for WOOD tools. This properties file is private to local development context and usually is not saved to source code
 * repository.
 * <p>
 * Project is a not constrained tree of directories and files, source files - layouts, styles, scripts, medias and variables,
 * and descriptors. Directories hierarchy is indeed not constrained but there are naming conventions for component files - see
 * {@link Component} class description.
 * <p>
 * It is recommended as good practice to have separated modules for user interface and back-end logic. Anyway, there are no
 * constraints that impose this separation of concerns. It is, for example, allowed to embed WOOD project directories in a
 * master Java project, perhaps Eclipse, e.g. a <code>lib</code> directory for both WOOD components and Java archives. Anyway,
 * master project files must not use file extensions expected by this tool library and files name should obey syntax described
 * by {@link FilePath}. Finally, there is the option to exclude certain directories from WOOD builder processing.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class Project {
	/**
	 * Create project instance for a given project root directory. Project root should point to a valid WOOD project; current
	 * implementation requires only project properties file - see {@link ProjectProperties#PROPERTIES_FILE}.
	 * <p>
	 * Implementation note: project creation occurs in two steps: instance creation and post create initialization. On instance
	 * creation constructor takes care to registers file system visitor; the actual file system traversal and visitors
	 * invocation occur on post create.
	 * 
	 * @param projectRoot project root directory.
	 * @return project instance.
	 */
	public static Project create(File projectRoot, IReferenceHandler referenceHandler) {
		Project project = new Project(projectRoot);
		project.create();
		return project;
	}

	private final Set<File> excludeDirs;

	/**
	 * Registry for components based on W3C custom elements. It contains information about custom element tag name, component
	 * path and related WOOD operator.
	 */
	private final CustomElementsRegistry customElements = new CustomElementsRegistry();

	/** Map script path to its dependencies. Only scripts with declared dependencies are included in this map. */
	private final Map<String, List<IScriptDescriptor>> scriptDependencies = new HashMap<>();

	/**
	 * File path visitors for project file system scanning. This visitors are registered by
	 * {@link #registerVisitor(IFilePathVisitor)} on constructors, both this class and subclasses, and invoked on file system
	 * scanning performed on {@link #configure(ProjectDescriptor)}.
	 */
	private final List<IFilePathVisitor> filePathVisitors = new ArrayList<>();

	/** Project root directory. All project file are included here, no external references allowed. */
	private File projectRoot;

	/**
	 * Project configuration loaded from <code>project.xml</code> file. It is allowed for project descriptor to be missing in
	 * which case there are sensible default values.
	 */
	private ProjectDescriptor descriptor;

	/** Build directory where site files are created. Configured on project properties, <code>build.dir</code> property. */
	private FilePath buildDir;

	/**
	 * Assets are variables and media files used in common by all components. Do not abuse it since it breaks component
	 * encapsulation. This directory is optional.
	 */
	private FilePath assetDir;

	/**
	 * Contains site styles for UI primitive elements and theme variables. This directory content describe overall site design -
	 * for example flat design, and is usually imported. Theme directory is optional.
	 */
	private FilePath themeDir;

	/**
	 * Project operator handler based on project selected naming strategy. Default naming strategy is
	 * {@link OperatorsNaming#XMLNS}.
	 */
	private IOperatorsHandler operatorsHandler;

	/**
	 * Construct and initialize project instance. Created instance is immutable.
	 * 
	 * @param projectRoot path to existing project root directory.
	 */
	protected Project(File projectRoot) {
		this.projectRoot = projectRoot;

		FilePath descriptorFile = createFilePath(CT.PROJECT_CONFIG);
		this.descriptor = new ProjectDescriptor(descriptorFile);

		this.buildDir = createFilePath(descriptor.getBuildDir());
		this.assetDir = createFilePath(descriptor.getAssetDir());
		this.themeDir = createFilePath(descriptor.getThemeDir());

		this.excludeDirs = this.descriptor.getExcludeDirs().stream().map(exclude -> file(exclude)).collect(Collectors.toSet());
		this.excludeDirs.add(file(descriptor.getBuildDir()));

		registerVisitor(new FilePathVisitor(customElements, scriptDependencies));
	}

	private File file(String path) {
		return new File(projectRoot, path);
	}

	/**
	 * Test constructor.
	 * 
	 * @param projectRoot path to existing project root directory,
	 * @param descriptor project descriptor, possible empty if project descriptor file is missing.
	 */
	Project(File projectRoot, ProjectDescriptor descriptor) {
		this.projectRoot = projectRoot;
		this.descriptor = descriptor;

		this.buildDir = createFilePath(descriptor.getBuildDir());
		this.assetDir = createFilePath(descriptor.getAssetDir());
		this.themeDir = createFilePath(descriptor.getThemeDir());

		this.excludeDirs = this.descriptor.getExcludeDirs().stream().map(exclude -> file(exclude)).collect(Collectors.toSet());
		this.excludeDirs.add(file(descriptor.getBuildDir()));
	}

	/**
	 * Register file system visitor. This method is called from project - and its subclasses, constructor. All registered
	 * visitors are invoked on {@link #configure(ProjectDescriptor)} when file system scanning occurs.
	 * 
	 * @param visitor file system visitor.
	 * @see #filePathVisitors
	 */
	protected void registerVisitor(IFilePathVisitor visitor) {
		this.getFilePathVisitors().add(visitor);
	}

	public final void create() {
		walkFileTree(this, projectRoot, filePathVisitors);

		switch (descriptor.getOperatorsNaming()) {
		case XMLNS:
			this.operatorsHandler = new XmlnsOperatorsHandler();
			break;

		case DATA_ATTR:
			this.operatorsHandler = new DataAttrOperatorsHandler();
			break;

		case ATTR:
			this.operatorsHandler = new AttrOperatorsHandler();
			break;

		default:
			this.operatorsHandler = null;
		}
	}

	/**
	 * Get Java absolute path to this project root. Project root directory contains all WOOD project files.
	 * 
	 * @return project root Java directory.
	 * @see #projectRoot
	 */
	public File getProjectRoot() {
		return projectRoot;
	}

	/**
	 * Get project build directory where site files are created.
	 * 
	 * @return build directory.
	 * @see #buildDir
	 */
	public FilePath getBuildDir() {
		return buildDir;
	}

	/**
	 * Get project assets directory that contains variables and media files used in common by all components. Returned directory
	 * is optional and is caller responsibility to ensure it exists before using it.
	 * 
	 * @return project assets directory.
	 * @see #assetDir
	 */
	public FilePath getAssetDir() {
		return assetDir;
	}

	/**
	 * Get site theme directory that contains styles for UI primitive elements and theme variables. Returned directory is
	 * optional and is caller responsibility to ensure it exists before using it.
	 * 
	 * @return site theme directory.
	 * @see #themeDir
	 */
	public FilePath getThemeDir() {
		return themeDir;
	}

	/**
	 * Return the list of authors which may be empty if none declared on project descriptor.
	 * 
	 * @return list of authors, possible empty but never null.
	 */
	public List<String> getAuthors() {
		return descriptor.getAuthors();
	}

	/**
	 * Return project name usable on user interfaces.
	 * 
	 * @return project display.
	 * @see #display
	 */
	public String getDisplay() {
		return descriptor.getDisplay(Strings.toTitleCase(this.projectRoot.getName()));
	}

	/**
	 * Get favicon file path as defined on project descriptor.
	 * 
	 * @return favicon file path.
	 * @see ProjectDescriptor#getFavicon()
	 */
	public FilePath getFavicon() {
		return createFilePath(descriptor.getFavicon());
	}

	/**
	 * Get file path for PWA manifest, as defined on project descriptor.
	 * 
	 * @return PWA manifest file path.
	 * @see ProjectDescriptor#getManifest()
	 */
	public FilePath getManifest() {
		return createFilePath(CT.MANIFEST_FILE);
	}

	/**
	 * Get file path for PWA service worker script, as defined on project descriptor.
	 * 
	 * @return file path for PWA service worker script.
	 * @see ProjectDescriptor#getServiceWorker()
	 */
	public FilePath getServiceWorker() {
		return createFilePath(descriptor.getServiceWorker());
	}

	/**
	 * Get project supported locale settings as configured by project descriptor. Returned list is immutable.
	 * 
	 * @return project locale.
	 * @see ProjectDescriptor#getLocales()
	 */
	public List<Locale> getLocales() {
		return Collections.unmodifiableList(descriptor.getLocales());
	}

	/**
	 * Get project configured default locale. By convention default path locale is the first from descriptor locale list.
	 * 
	 * @return default locale.
	 */
	public Locale getDefaultLocale() {
		return descriptor.getLocales().get(0);
	}

	/**
	 * Get media query definition for given alias.
	 * 
	 * @param alias alias for media query definition.
	 * @return media query definition or null if alias not defined.
	 * @see ProjectDescriptor#getMediaQueryDefinitions()
	 */
	public MediaQueryDefinition getMediaQueryDefinition(String alias) {
		return descriptor.getMediaQueryDefinitions().stream().filter(query -> query.getAlias().equals(alias)).findAny().orElse(null);
	}

	/**
	 * Get descriptors for page meta elements, declared on project descriptor. Meta descriptors order is preserved.
	 * <p>
	 * Return empty list if there are no meta descriptors declared on project descriptor. Returned list is not modifiable.
	 * 
	 * @return Immutable list of meta descriptors, possible empty.
	 * @see ProjectDescriptor#getMetaDescriptors()
	 */
	public List<IMetaDescriptor> getMetaDescriptors() {
		return Collections.unmodifiableList(descriptor.getMetaDescriptors());
	}

	/**
	 * Get descriptors for page link elements, declared on project descriptor. Link descriptors order is preserved.
	 * <p>
	 * Return empty list if there are no link descriptors declared on project descriptor. Returned list is not modifiable.
	 * 
	 * @return Immutable list of link descriptors, possible empty.
	 * @see {@link ProjectDescriptor#getLinkDescriptors()}
	 */
	public List<ILinkDescriptor> getLinkDescriptors() {
		return Collections.unmodifiableList(descriptor.getLinkDescriptors());
	}

	/**
	 * Get descriptors for page script elements, declared on project descriptor. Script descriptors order is preserved.
	 * <p>
	 * Return empty list if there are no script descriptors declared on project descriptor. Returned list is not modifiable.
	 * 
	 * @return Immutable list of script descriptors, possible empty.
	 * @see ProjectDescriptor#getScriptDescriptors()
	 */
	public List<IScriptDescriptor> getScriptDescriptors() {
		return Collections.unmodifiableList(descriptor.getScriptDescriptors());
	}

	/**
	 * Get dependencies for requested script file or empty list if none declared. Script dependencies are declared on component
	 * descriptors and initialized on file system scanning, performed on {@link #configure(ProjectDescriptor)}.
	 * 
	 * @param source script file path.
	 * @return script dependencies list, possible empty.
	 * @see FilePathVisitor
	 */
	public List<IScriptDescriptor> getScriptDependencies(String source) {
		List<IScriptDescriptor> dependencies = scriptDependencies.get(source);
		return Collections.unmodifiableList(dependencies != null ? dependencies : Collections.emptyList());
	}

	/**
	 * Get style files declared into project theme directory. Returned instance is immutable.
	 * 
	 * @return theme styles.
	 */
	public ThemeStyles getThemeStyles() {
		return new ThemeStyles(getThemeDir());
	}

	public OperatorsNaming getOperatorsNaming() {
		return descriptor.getOperatorsNaming();
	}

	/**
	 * Get operator handler as configured by selected naming strategy.
	 * 
	 * @return operator handler for project naming convention.
	 */
	public IOperatorsHandler getOperatorsHandler() {
		return operatorsHandler;
	}

	/**
	 * Determine if project uses a naming convention that requires XML name space.
	 * 
	 * @return true if project naming convention requires XML name space.
	 */
	boolean hasNamespace() {
		return operatorsHandler instanceof XmlnsOperatorsHandler;
	}

	ICustomElementsRegistry getCustomElementsRegistry() {
		return customElements;
	}

	// --------------------------------------------------------------------------------------------
	// media files retrieving

	/**
	 * Get the file path for project media file referenced from given source file. This method tries to locate media file into
	 * the parent of given source file and assets directories, in this order. If reference has path - see
	 * {@link Reference#hasPath()}, attempt to find media file on source parent subdirectory. If source file is in project root,
	 * e.g. manifest.json, source parent directory is null, in which case searches only asset directory.
	 * <p>
	 * When search for media file only base name and locale variant is considered, that is, no extension. Anyway, if locale
	 * variant parameter is null searches only media files without variants at all.
	 * <p>
	 * Returns null if media file is not found.
	 * 
	 * @param locale locale variant, possible null.
	 * @param reference media resource reference,
	 * @param sourceFile source file using media resource.
	 * @return media file or null.
	 */
	public FilePath getMediaFile(Locale locale, Reference reference, FilePath sourceFile) {
		Params.notNull(reference, "Reference");
		Params.notNull(sourceFile, "Source file");

		// search media files on source and asset directories, in this order
		// if source file is in project root, e.g. manifest.json, source directory is null
		// if this is the case search for media files only on asset directory

		FilePath sourceDir = sourceFile.getParentDir();
		FilePath mediaFile = null;
		if (sourceDir != null) {
			mediaFile = findMediaFile(sourceDir, reference, locale);
		}
		return mediaFile != null ? mediaFile : findMediaFile(assetDir, reference, locale);
	}

	/**
	 * Scan source directory for media files matching base name and locale variant. This helper method tries to locate file
	 * matching both base name and locale; extension is not considered. If not found try to return base variant, that is, file
	 * that match only base name and has no locale. If still not found returns null.
	 * 
	 * @param sourceDir directory to scan for media files,
	 * @param reference media file reference,
	 * @param locale locale variant, null for project default locale.
	 * @return media file or null.
	 */
	static FilePath findMediaFile(FilePath sourceDir, Reference reference, Locale locale) {
		if (reference.hasPath()) {
			sourceDir = sourceDir.getSubdirPath(reference.getPath());
		}
		FilePath mediaFile = null;
		if (locale != null) {
			// scan directory for first media file with basename and locale variant
			mediaFile = sourceDir.findFirst(file -> file.isMedia() && file.hasBaseName(reference.getName()) && file.getVariants().hasLocale(locale));
		}
		if (mediaFile != null) {
			return mediaFile;
		}
		// scan directory for first media file with basename and no variants at all
		return sourceDir.findFirst(file -> file.isMedia() && file.hasBaseName(reference.getName()) && file.getVariants().isEmpty());
	}

	// --------------------------------------------------------------------------------------------
	// factory methods

	public FilePath createFilePath(String path) {
		return new FilePath(this, path);
	}

	public FilePath createFilePath(File file) {
		return new FilePath(this, Files.getRelativePath(projectRoot, file, true));
	}

	public CompoPath createCompoPath(String path) {
		return new CompoPath(this, path);
	}

	public Component createComponent(FilePath layoutPath, IReferenceHandler referenceHandler) {
		return new Component(layoutPath, referenceHandler);
	}

	public Variables createVariables(FilePath dir) {
		return new Variables(dir);
	}

	// --------------------------------------------------------------------------------------------
	// scanner for project file system

	/**
	 * Recursively traverse project file system and invoke visitors for every file found. Visitors are invoked in provided
	 * order.
	 * <p>
	 * This method is executed on {@link #configure(ProjectDescriptor)}. Walking process is started with project root and
	 * recursively invoked it self till all project files are visited.
	 * 
	 * @param project master project,
	 * @param dir current visited directory,
	 * @param visitors visitors list.
	 * @throws WoodException if fail to list directory or there is an error on visitor execution.
	 */
	static void walkFileTree(Project project, File dir, List<IFilePathVisitor> visitors) throws WoodException {
		Params.isDirectory(dir, "Directory");

		File[] files = dir.listFiles();
		if (files == null) {
			throw new WoodException("Fail to list directory |%s|.", dir);
		}

		for (File file : files) {
			if (file.isDirectory()) {
				if (file.getName().startsWith(".") || project.excludeDirs.contains(file)) {
					continue;
				}
				walkFileTree(project, file, visitors);
			}

			try {
				for (IFilePathVisitor visitor : visitors) {
					visitor.visitFile(project, project.createFilePath(file));
				}
			} catch (Throwable t) {
				throw new WoodException("Scan processing fail on file %s: %s: %s", file, t.getClass(), t.getMessage());
			}
		}
	}

	/**
	 * File path visitor for project file system scanning.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	public interface IFilePathVisitor {
		void visitFile(Project project, FilePath file) throws Exception;
	}

	/**
	 * Implementation for file path visitor. Current version scan component descriptors for script dependencies and register
	 * components based on W3C custom elements.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	static class FilePathVisitor implements IFilePathVisitor {
		private static final DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);

		private final ICustomElementsRegistry customElements;
		private final Map<String, List<IScriptDescriptor>> scriptDependencies;

		public FilePathVisitor(ICustomElementsRegistry customElements, Map<String, List<IScriptDescriptor>> scriptDependencies) {
			this.customElements = customElements;
			this.scriptDependencies = scriptDependencies;
		}

		@Override
		public void visitFile(Project project, FilePath file) throws Exception {
			if (!file.isComponentDescriptor()) {
				return;
			}

			Document document = documentBuilder.loadXML(file.getReader());

			for (Element scriptElement : document.findByXPath("//script")) {
				for (Element dependencyElement : scriptElement.getChildren()) {
					String scriptSource = scriptElement.getAttr("src");
					List<IScriptDescriptor> dependencies = scriptDependencies.get(scriptSource);
					if (dependencies == null) {
						dependencies = new ArrayList<>();
						scriptDependencies.put(scriptSource, dependencies);
					}
					dependencies.add(ScriptDescriptor.create(dependencyElement));
				}
			}

			FilePath parentDir = file.getParentDir();
			if (parentDir == null) {
				return;
			}

			String tagName = layoutRootTagName(file.getParentDir());
			if (tagName != null) {
				// by convention descriptor document root is WOOD operator simple name
				String operator = document.getRoot().getTag();
				customElements.register(tagName, project.createCompoPath(parentDir.value()), operator);
			}
		}
	}

	/**
	 * Get root tag name for components based on W3C custom elements. Returns null if component is not based on W3C custom
	 * element. This method opens layout file - by WOOD component convention layout file should have the same name as directory
	 * base name, and read tag name for its root element.
	 * 
	 * If layout file is missing return null. Also returns null if root tag name does not obey W3C custom elements naming
	 * convention: it should have at least one hyphen.
	 * 
	 * @param compoDir component directory.
	 * @return root tag name or null if component is not based on custom element.
	 * @throws IOException if layout file reading fails.
	 */
	private static String layoutRootTagName(FilePath compoDir) throws IOException {
		if (compoDir == null) {
			return null;
		}
		FilePath layoutFile = compoDir.getFilePath(compoDir.getBasename() + CT.DOT_LAYOUT_EXT);
		if (!layoutFile.exists()) {
			return null;
		}

		StringBuilder builder = new StringBuilder();
		int hyphensCount = 0;
		try (Reader reader = layoutFile.getReader()) {
			for (;;) {
				int i = reader.read();
				if (i == -1) {
					return null;
				}
				char c = (char) i;
				if (Character.isWhitespace(c) || c == '>') {
					break;
				}
				if (c == '-') {
					++hyphensCount;
				}
				builder.append(c);
			}

			if (hyphensCount == 0) {
				return null;
			}
			if (builder.charAt(0) != '<') {
				return null;
			}
			return builder.substring(1);
		}
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	void setAssetDir(FilePath assetDir) {
		this.assetDir = assetDir;
	}

	ProjectDescriptor getDescriptor() {
		return descriptor;
	}

	List<IFilePathVisitor> getFilePathVisitors() {
		return filePathVisitors;
	}

	Set<File> getExcludes() {
		return excludeDirs;
	}

	Map<String, List<IScriptDescriptor>> getScriptDependencies() {
		return scriptDependencies;
	}
}
