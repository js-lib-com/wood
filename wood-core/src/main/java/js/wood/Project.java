package js.wood;

import java.io.File;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import js.util.Params;
import js.util.Strings;
import js.wood.impl.AttOperatorsHandler;
import js.wood.impl.DataAttrOperatorsHandler;
import js.wood.impl.FilesHandler;
import js.wood.impl.NamingStrategy;
import js.wood.impl.ProjectDescriptor;
import js.wood.impl.Reference;
import js.wood.impl.XmlnsOperatorsHandler;

/**
 * Project global singleton. A project has a root directory; all {@link Path} instances are relative to this project root.
 * Regarding build process, project reads component files under project builder control. For this, project scans source
 * directories hierarchy, collects and caches meta data that is available for entire lifetime of the build process. Note that
 * Project does not consider all sub-directories from project root; only source directories. This allows Project to be part of a
 * larger project layout, like an Eclipse project.
 * <p>
 * Current Project file system is depicted below. There are four source directories and one build target. There is also a
 * project configuration XML file, see {@link ProjectDescriptor}. It is acceptable to share directories with master project, if any.
 * For example <code>lib</code> can hold Java archives. Anyway, master project files must not use extensions expected by this
 * tool library and file name should obey syntax described by {@link FilePath}.
 * 
 * <pre>
 *  /                     ; project root
 *  /build/site           ; site build target directory
 *  /gen/                 ; optional generated scripts, mostly HTTP-RMI stubs
 *  /lib/                 ; third-party user interface components and script libraries
 *  /res/                 ; application user interface resources
 *  |   /asset            ; project assets directory stores global variables and media files
 *  |   /theme            ; site styles for UI primitive elements and theme variables
 *  ~                     ; application defined components
 *  /script/              ; application specific scripts structured in packages
 *  +-project.xml         ; project configuration file
 * </pre>
 * 
 * Note that default build target directory is a sub-directory of master project build. This is to allow storing all build files
 * in the same place. Anyway, site build directory is Project configurable, see <code>site-dir</code> from
 * {@link ProjectDescriptor}.
 * <p>
 * Project instance is used in two different modes: build and preview. When working for build, project scans and cache files
 * meta data; cache is valid for entire build process. On preview mode project does not use cache in order to display most
 * updated content.
 * 
 * @author Iulian Rotaru
 * @version draft
 */
public class Project {
	// ------------------------------------------------------
	// Project instance

	/**
	 * Project name is used for internal representation. This value may be supplied by {@link ProjectDescriptor#getName(String)}
	 * with default to project directory name.
	 */
	private final String name;

	/**
	 * Project display is for user interface. If this value is not provided by {@link ProjectDescriptor#getDisplay(String)} uses
	 * project name converted to title case.
	 */
	private final String display;

	/**
	 * Project description. Uses project display if this value is not provided by {@link ProjectDescriptor#getDescription(String)}.
	 */
	private final String description;

	/** Project directory. All project file are included here, no external references allowed. */
	protected final File projectDir;

	private final DirPath resourcesDir;

	/**
	 * Assets are variables and media files used in common by all components. Do not abuse it since it breaks component
	 * encapsulation. This directory is optional.
	 */
	protected final DirPath assetsDir;

	/**
	 * Contains site styles for UI primitive elements and theme variables. This directory content describe overall site design -
	 * for example flat design, and is usually imported. Theme directory is optional.
	 */
	protected final DirPath themeDir;

	/**
	 * Project configuration loaded from <code>project.xml</code> file. By convention, configuration file should be stored
	 * project directory root.
	 */
	protected final ProjectDescriptor descriptor;

	/** List of paths excluded from build process. Configurable per project, see {@link ProjectDescriptor#getExcludes()}. */
	private final List<Path> excludes;

	/**
	 * Project operator handler based on project selected naming strategy. Default naming strategy is
	 * {@link NamingStrategy#XMLNS}.
	 */
	private final IOperatorsHandler operatorsHandler;

	/**
	 * Construct not initialized project instance. Initialize project instance state. Load project configuration and create
	 * directories, if missing.
	 * 
	 * @param projectPath path to project root directory.
	 * @throws IllegalArgumentException if project root does not designate an existing directory.
	 */
	public Project(String projectPath) throws IllegalArgumentException {
		Params.notNullOrEmpty(projectPath, "Project directory");
		this.projectDir = new File(projectPath);
		Params.isDirectory(this.projectDir, "Project directory");
		this.descriptor = new ProjectDescriptor(new File(this.projectDir, CT.PROJECT_CONFIG));

		this.resourcesDir = new DirPath(this, CT.RESOURCE_DIR);
		this.assetsDir = new DirPath(this, CT.ASSETS_DIR);
		this.themeDir = new DirPath(this, CT.THEME_DIR);

		this.name = descriptor.getName(this.projectDir.getName());
		this.display = descriptor.getDisplay(Strings.toTitleCase(this.name));
		this.description = descriptor.getDescription(this.display);

		this.excludes = new ArrayList<>();
		for (String exclude : descriptor.getExcludes()) {
			excludes.add(Path.create(this, exclude.trim()));
		}


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
	 * Get project name for internal use.
	 * 
	 * @return project name.
	 * @see #name
	 */
	public String getName() {
		return name;
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
	 * Get this project file system root. Project root directory contains all project files including Java code base and project
	 * user interface resources.
	 * 
	 * @return project root directory.
	 * @see #projectDir
	 */
	public File getProjectDir() {
		return projectDir;
	}

	public DirPath getResourcesDir() {
		return resourcesDir;
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
		return descriptor.getLocales();
	}

	public Locale getDefaultLocale() {
		return descriptor.getDefaultLocale();
	}

	/**
	 * Test if path is excluded from building process. If <code>path</code> is a file test also if its parent is excluded.
	 * 
	 * @param path path, file or directory.
	 * @return true if path is excluded from building process.
	 * @see #excludes
	 */
	public boolean isExcluded(Path path) {
		if (path instanceof FilePath) {
			FilePath filePath = (FilePath) path;
			return excludes.contains(filePath) || excludes.contains(filePath.getDirPath());
		}
		return excludes.contains(path);
	}

	/**
	 * Create file path instance for a project file.
	 * 
	 * @param path path value.
	 * @return file path instance.
	 */
	public FilePath getFile(String path) {
		return new FilePath(this, path);
	}

	// ------------------------------------------------------
	// Build files scanner and cache

	/**
	 * Style files stored on project theme directory. These files contains global styles regarding UI primitive elements.
	 */
	protected List<FilePath> themeStyles = new ArrayList<>();

	/**
	 * Scanner for style files stored into theme directory.
	 * 
	 * @author Iulian Rotaru
	 */
	protected class ThemeStylesScanner extends FilesHandler {
		/**
		 * Add style files residing in site styles directory to {@link Project#themeStyles} cache.
		 */
		@Override
		public void onFile(FilePath file) throws Exception {
			if (!file.getDirPath().isTheme()) {
				return;
			}
			// do not include style variants since are included by preview servlet within media query sections
			if (!file.hasVariants() && file.isStyle()) {
				themeStyles.add(file);
			}
		}
	}

	public String getAuthor() {
		return descriptor.getAuthor();
	}

	public List<IMetaReference> getMetaReferences() {
		return descriptor.getMetas();
	}

	/**
	 * Get link elements declared at project scope, therefore included in all pages head.
	 * 
	 * @return ordered set of link references.
	 */
	public List<ILinkReference> getLinkReferences() {
		return descriptor.getLinks();
	}

	public List<IScriptReference> getScriptReferences() {
		return descriptor.getScripts();
	}

	/**
	 * Get style files declared into project theme directory. Returned set is not changeable.
	 * 
	 * @return site styles.
	 * @see #themeStyles
	 */
	public List<FilePath> getThemeStyles() {
		return themeStyles;
	}

	// ------------------------------------------------------
	// Media files

	/**
	 * Get project media file referenced from given source file. This method tries to locate media file into source path parent
	 * and assets directories, in this order. When search for media file only base name and language variant is considered, that
	 * is, no extension.
	 * <p>
	 * There is no attempt to test reference type against actual file content or matching against file extension. It is
	 * developer responsibility to ensure reference points to proper type.
	 * <p>
	 * Returns null if media file is not found.
	 * 
	 * @param language language variant, possible null,
	 * @param reference media resource reference,
	 * @param source source file using media resource.
	 * @return media file or null.
	 */
	public FilePath getMediaFile(Locale locale, IReference reference, FilePath source) {
		DirPath dir = source.getDirPath();
		if (reference.hasPath()) {
			dir = dir.getSubdirPath(reference.getPath());
		}
		FilePath file = mediaFile(dir, reference.getName(), locale);

		if (file == null) {
			dir = assetsDir;
			if (reference.hasPath()) {
				dir = dir.getSubdirPath(reference.getPath());
			}
			file = mediaFile(dir, reference.getName(), locale);
		}

		return file;
	}

	/**
	 * Scan directory for media files matching base name and language variant. This helper method is used by
	 * {@link #getMediaFile(String, Reference, FilePath)}. Try to locate file matching both base name and language; extension is
	 * not considered. If not found try to return base variant, that is, file that match only base name and has no language. If
	 * still not found returns null.
	 * 
	 * @param dir directory to scan for media files,
	 * @param basename media file base name,
	 * @param language language variant, possible null.
	 * @return media file or null.
	 */
	public static FilePath mediaFile(DirPath dir, String basename, Locale locale) {
		for (FilePath file : dir.files()) {
			if (file.isMedia() && file.isBaseName(basename) && file.getVariants().hasLocale(locale)) {
				return file;
			}
		}
		for (FilePath file : dir.files()) {
			if (file.isMedia() && file.isBaseName(basename)) {
				return file;
			}
		}
		return null;
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

	private Map<String, Object> attributes = new HashMap<>();

	public void setAttribute(String name, Object value) {
		attributes.put(name, value);
	}

	public Object getAttribute(String name) {
		return attributes.get(name);
	}
}
