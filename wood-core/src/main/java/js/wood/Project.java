package js.wood;

import java.io.File;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.stream.Collectors;

import js.lang.BugError;
import js.util.Files;
import js.util.Params;
import js.util.Strings;
import js.wood.impl.AttOperatorsHandler;
import js.wood.impl.DataAttrOperatorsHandler;
import js.wood.impl.EditablePath;
import js.wood.impl.IOperatorsHandler;
import js.wood.impl.MediaQueryDefinition;
import js.wood.impl.NamingStrategy;
import js.wood.impl.ProjectDescriptor;
import js.wood.impl.XmlnsOperatorsHandler;

/**
 * Project global context. A project has a root directory and all {@link Path} instances are relative to this project root. This
 * class provides basic functionality but there are specialized project classes for build and preview. Project is initialized
 * from {@link ProjectDescriptor}, that is optional. If project descriptor file is missing there are sensible defaults.
 * <p>
 * Project is a not constrained tree of directories and files, source files - layouts, styles, scripts, medias and variables,
 * and descriptors. Directories hierarchy is indeed not constrained but there are naming conventions for component files - see
 * {@link Component} class description.
 * <p>
 * Is recommended as good practice to have separated modules for user interface and back-end logic. Anyway, there are no
 * constraints that impose this separation of concerns. It is allowed to embed WOOD project directories in a master Java
 * project, perhaps Eclipse. For example, is possible to have a <code>lib</code> directory for both WOOD components and Java
 * archives. Anyway, master project files must not use extensions expected by this tool library and file name should obey syntax
 * described by {@link Path} classes hierarchy.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class Project {
	/** Project root directory. All project file are included here, no external references allowed. */
	private final File projectRoot;

	/** Directory path instance used as root. */
	private final DirPath projectDir;

	/**
	 * Project configuration loaded from <code>project.xml</code> file. It is allowed for project descriptor to be missing in
	 * which case there are sensible default values.
	 */
	private final ProjectDescriptor descriptor;

	/**
	 * Project name for user interface. If this value is not provided by {@link ProjectDescriptor#getDisplay(String)} uses
	 * project name converted to title case.
	 */
	private final String display;

	/**
	 * Project description. Uses project display if this value is not provided by
	 * {@link ProjectDescriptor#getDescription(String)}.
	 */
	private final String description;

	/**
	 * Assets are variables and media files used in common by all components. Do not abuse it since it breaks component
	 * encapsulation. This directory is optional.
	 */
	private final DirPath assetsDir;

	/**
	 * Contains site styles for UI primitive elements and theme variables. This directory content describe overall site design -
	 * for example flat design, and is usually imported. Theme directory is optional.
	 */
	private final DirPath themeDir;

	/**
	 * Project operator handler based on project selected naming strategy. Default naming strategy is
	 * {@link NamingStrategy#XMLNS}.
	 */
	private final IOperatorsHandler operatorsHandler;

	/** Directories excluded from build processing. */
	protected final List<DirPath> excludes;

	/**
	 * Construct and initialize project instance. Created instance is immutable.
	 * 
	 * @param projectRoot path to existing project root directory.
	 * @throws IllegalArgumentException if project root does not designate an existing directory.
	 */
	public Project(File projectRoot) throws IllegalArgumentException {
		this(projectRoot, new ProjectDescriptor(new File(projectRoot, CT.PROJECT_CONFIG)));
	}

	/**
	 * Test constructor.
	 * 
	 * @param projectRoot path to existing project root directory,
	 * @param descriptor project descriptor, possible empty if project descriptor file is missing.
	 * @throws IllegalArgumentException if project root does not designate an existing directory.
	 */
	Project(File projectRoot, ProjectDescriptor descriptor) {
		Params.isDirectory(projectRoot, "Project directory");
		this.projectRoot = projectRoot;
		this.projectDir = new DirPath(this);
		this.descriptor = descriptor;
		this.excludes = descriptor.getExcludes().stream().map(exclude -> new DirPath(this, exclude)).collect(Collectors.toList());

		this.assetsDir = createDirPath(CT.ASSETS_DIR);
		this.themeDir = createDirPath(CT.THEME_DIR);

		this.display = descriptor.getDisplay(Strings.toTitleCase(this.projectRoot.getName()));
		this.description = descriptor.getDescription(this.display);

		switch (this.descriptor.getNamingStrategy()) {
		case XMLNS:
			operatorsHandler = new XmlnsOperatorsHandler();
			break;

		case DATA_ATTR:
			operatorsHandler = new DataAttrOperatorsHandler();
			break;

		case ATTR:
			operatorsHandler = new AttOperatorsHandler();
			break;

		default:
			operatorsHandler = null;
		}
	}

	/**
	 * Return project name usable on user interfaces.
	 * 
	 * @return project display.
	 * @see #display
	 */
	public String getDisplay() {
		return display;
	}

	/**
	 * Get project description.
	 * 
	 * @return project description.
	 * @see #description
	 */
	public String getDescription() {
		return description;
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
	 * Get directory path instance used as project root.
	 * 
	 * @return project root directory path.
	 * @see #projectDir
	 */
	public DirPath getProjectDir() {
		return projectDir;
	}

	/**
	 * Get project assets directory that contains variables and media files used in common by all components. Returned directory
	 * is optional and is caller responsibility to ensure it exists before using it.
	 * 
	 * @return project assets directory.
	 * @see #assetsDir
	 */
	public DirPath getAssetsDir() {
		return assetsDir;
	}

	/**
	 * Get site theme directory that contains styles for UI primitive elements and theme variables. Returned directory is
	 * optional and is caller responsibility to ensure it exists before using it.
	 * 
	 * @return site theme directory.
	 * @see #themeDir
	 */
	public DirPath getThemeDir() {
		return themeDir;
	}

	/**
	 * Get project favicon.
	 * 
	 * @return project favicon.
	 */
	public FilePath getFavicon() {
		return assetsDir.getFilePath(CT.FAVICON);
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
	 */
	public List<IScriptDescriptor> getScriptDescriptors() {
		return Collections.unmodifiableList(descriptor.getScriptDescriptors());
	}

	/**
	 * Get style files declared into project theme directory. Returned instance is immutable.
	 * <p>
	 * This method should be overridden by builder and preview project subclasses.
	 * 
	 * @return theme styles.
	 */
	public ThemeStyles getThemeStyles() {
		throw new BugError("Abstract method invoked.");
	}

	/**
	 * Get project media file referenced from given source file. This method tries to locate media file into source parent and
	 * assets directories, in this order. When search for media file only base name and language variant is considered, that is,
	 * no extension.
	 * <p>
	 * Returns null if media file is not found.
	 * 
	 * @param locale locale variant,
	 * @param reference media resource reference,
	 * @param sourceFile source file using media resource.
	 * @return media file or null.
	 * @throws IllegalArgumentException if any parameters is null.
	 */
	public FilePath getMediaFile(Locale locale, Reference reference, FilePath sourceFile) {
		Params.notNull(locale, "Locale");
		Params.notNull(reference, "Reference");
		Params.notNull(sourceFile, "Source file");

		if (getDefaultLocale().equals(locale)) {
			locale = null;
		}

		DirPath sourceDir = sourceFile.getParentDirPath();
		boolean isComponentSource = sourceDir.isComponent();
		// search media in source directory only if directory is a component
		FilePath mediaFile = isComponentSource ? findMediaFile(sourceDir, reference, locale) : null;
		if (mediaFile != null) {
			return mediaFile;
		}

		return findMediaFile(assetsDir, reference, locale);
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
	static FilePath findMediaFile(DirPath sourceDir, Reference reference, Locale locale) {
		if (reference.hasPath()) {
			sourceDir = sourceDir.getSubdirPath(reference.getPath());
		}
		if (locale == null) {
			return sourceDir.findFirst(file -> file.isMedia() && file.hasBaseName(reference.getName()) && file.getVariants().isEmpty());
		}
		FilePath mediaFile = sourceDir.findFirst(file -> file.isMedia() && file.hasBaseName(reference.getName()) && file.getVariants().hasLocale(locale));
		if (mediaFile != null) {
			return mediaFile;
		}
		return sourceDir.findFirst(file -> file.isMedia() && file.hasBaseName(reference.getName()) && file.getVariants().isEmpty());
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
	public boolean hasNamespace() {
		return operatorsHandler instanceof XmlnsOperatorsHandler;
	}

	public FilePath createFilePath(File file) {
		return new FilePath(this, Files.getRelativePath(projectRoot, file, true));
	}

	public DirPath createDirPath(File file) {
		return createDirPath(Files.getRelativePath(projectRoot, file, true));
	}

	public CompoPath createCompoPath(String path) {
		return new CompoPath(this, path);
	}

	public EditablePath createEditablePath(String path) {
		return new EditablePath(this, path);
	}

	public DirPath createDirPath(String path) {
		if (!path.endsWith(Path.SEPARATOR)) {
			path += Path.SEPARATOR;
		}
		return new DirPath(this, path);
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	String getAuthor() {
		return descriptor.getAuthor();
	}

	ProjectDescriptor getDescriptor() {
		return descriptor;
	}

	List<DirPath> getExcludes() {
		return excludes;
	}
}
