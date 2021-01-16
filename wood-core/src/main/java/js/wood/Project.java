package js.wood;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import js.util.Classes;
import js.util.Files;
import js.util.Params;
import js.util.Strings;
import js.wood.impl.AttOperatorsHandler;
import js.wood.impl.DataAttrOperatorsHandler;
import js.wood.impl.DescriptorScriptDependencyHandler;
import js.wood.impl.FilesHandler;
import js.wood.impl.NamingStrategy;
import js.wood.impl.ProjectConfig;
import js.wood.impl.Reference;
import js.wood.impl.ScriptDependencyStrategy;
import js.wood.impl.ScriptFile;
import js.wood.impl.XmlnsOperatorsHandler;

/**
 * Project global singleton. A project has a root directory; all {@link Path} instances are relative to this project root.
 * Regarding build process, project reads component files under project builder control. For this, project scans source
 * directories hierarchy, collects and caches meta data that is available for entire lifetime of the build process. Note that
 * Project does not consider all sub-directories from project root; only source directories. This allows Project to be part of a
 * larger project layout, like an Eclipse project.
 * <p>
 * Current Project file system is depicted below. There are four source directories and one build target. There is also a
 * project configuration XML file, see {@link ProjectConfig}. It is acceptable to share directories with master project, if any.
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
 * {@link ProjectConfig}.
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
	 * Project name is used for internal representation. This value may be supplied by {@link ProjectConfig#getName(String)}
	 * with default to project directory name.
	 */
	private final String name;

	/**
	 * Project display is for user interface. If this value is not provided by {@link ProjectConfig#getDisplay(String)} uses
	 * project name converted to title case.
	 */
	private final String display;

	/**
	 * Project description. Uses project display if this value is not provided by {@link ProjectConfig#getDescription(String)}.
	 */
	private final String description;

	/** Project directory. All project file are included here, no external references allowed. */
	private final File projectDir;

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
	private final ProjectConfig config;

	/** List of paths excluded from build process. Configurable per project, see {@link ProjectConfig#getExcludes()}. */
	private final List<Path> excludes;

	/**
	 * Project operator handler based on project selected naming strategy. Default naming strategy is
	 * {@link NamingStrategy#XMLNS}.
	 */
	private final IOperatorsHandler operatorsHandler;

	/** Script dependency handler used to collect scripts that should be included into the pages generated by build and preview. */
	private final IScriptDependecyHandler scriptDependecyHandler;

	/** Site build directory, usually part of master project build. */
	private File siteDir;

	/**
	 * Construct not initialized project instance. Initialize project instance state. Load project configuration and create
	 * directories, if missing.
	 * 
	 * @param projectPath path to project root directory.
	 * @throws IllegalArgumentException if project root does not designate an existing directory.
	 */
	public Project(String projectPath) throws IllegalArgumentException {
		Params.notNull(projectPath, "Project directory");
		this.projectDir = new File(projectPath);
		Params.isDirectory(this.projectDir, "Project directory");
		this.config = new ProjectConfig(this);

		this.resourcesDir = new DirPath(this, CT.RESOURCE_DIR);
		this.assetsDir = new DirPath(this, CT.ASSETS_DIR);
		this.themeDir = new DirPath(this, CT.THEME_DIR);
		this.siteDir = new File(projectPath, config.getSiteDir(CT.DEF_SITE_DIR));
		if (!this.siteDir.exists()) {
			this.siteDir.mkdir();
		}

		this.name = config.getName(this.projectDir.getName());
		this.display = config.getDisplay(Strings.toTitleCase(this.name));
		this.description = config.getDescription(this.display);
		this.excludes = config.getExcludes();

		switch (this.config.getNamingStrategy()) {
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

		if (this.config.getScriptDependencyStrategy() == ScriptDependencyStrategy.DISCOVERY) {
			this.scriptDependecyHandler = Classes.loadService(IScriptDependecyHandler.class);
		} else {
			this.scriptDependecyHandler = new DescriptorScriptDependencyHandler();
		}
	}

	/**
	 * Set site build directory. This method is designed for builder instance customization.
	 * 
	 * @param siteDir site build directory, relative to project root.
	 * @see siteDir
	 */
	public void setSiteDir(File siteDir) {
		this.siteDir = siteDir;
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
	 * Get site build directory.
	 * 
	 * @return site build directory.
	 * @see #siteDir
	 */
	public File getSiteDir() {
		return siteDir;
	}

	/**
	 * Get the path, relative to project root, of the site build directory. Returned value is guaranteed to have trailing file
	 * separator.
	 * 
	 * @return site build path.
	 */
	public String getSitePath() {
		return Files.getRelativePath(projectDir, siteDir, true) + Path.SEPARATOR;
	}

	public CompoPath getCompoPath(String path) {
		return new CompoPath(this, path);
	}

	/**
	 * Get project configuration loaded from configuration file <code>project.xml</code>.
	 * 
	 * @return project configuration.
	 * @see #config
	 */
	public IProjectConfig getConfig() {
		return config;
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
	 * @see ProjectConfig#getLocales()
	 */
	public List<Locale> getLocales() {
		return config.getLocales();
	}

	/**
	 * Return true if this project has support for multiple locale settings.
	 * 
	 * @return true if project is multi-locale.
	 */
	public boolean isMultiLocale() {
		return config.getLocales().size() > 1;
	}

	public Locale getDefaultLocale() {
		return config.getDefaultLocale();
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
	 * Discovered project script files identified by related file paths. Includes all scripts from scripts source directory,
	 * library and generated scripts. Note that it does not include scripts from UI resources source directory since current
	 * version of WOOD does not support.
	 */
	private Map<FilePath, ScriptFile> scripts = new HashMap<>();

	/** Script file where certain script class is defined. */
	private Map<String, ScriptFile> classScripts = new HashMap<>();

	/**
	 * Look for script files into all source directories less UI resources.
	 * 
	 * @author Iulian Rotaru
	 */
	protected class ScriptsScanner extends FilesHandler {
		/**
		 * Add meta data related to script file to project scripts meta cache. All script files are accepted less those from UI
		 * resources source directory.
		 */
		@Override
		public void onFile(FilePath file) throws Exception {
			if (file.isScript() && !file.getDirPath().isResources() && !file.isExcluded()) {
				ScriptFile scriptFile = new ScriptFile(Project.this, file);
				scripts.put(file, scriptFile);
				for (String scriptClass : scriptFile.getDefinedClasses()) {
					classScripts.put(scriptClass, scriptFile);
				}
			}
		}
	}

	/**
	 * Scanner for style files stored into theme directory. This scanner simply add site styles to {@link #themeStyles} list but
	 * takes care to include {@link CT#RESET_CSS} and {@link CT#FX_CSS} first, in mentioned order. After scanning completion
	 * {@link Project#themeStyles} contains style files in proper order for inclusion in page document.
	 * 
	 * @author Iulian Rotaru
	 */
	protected class ThemeStylesScanner extends FilesHandler {
		/**
		 * True if {@link CT#RESET_CSS} file was processed. Help in deciding where {@link CT#FX_CSS} has to be inserted into
		 * {@link Project#themeStyles} list.
		 */
		private boolean resetProcessed;

		/**
		 * Add style files residing in site styles directory to {@link Project#themeStyles} cache.
		 */
		@Override
		public void onFile(FilePath file) throws Exception {
			if (!file.getDirPath().isTheme()) {
				return;
			}
			if (CT.RESET_CSS.equals(file.getName())) {
				resetProcessed = true;
				themeStyles.add(0, file);
				return;
			}
			if (CT.FX_CSS.equals(file.getName())) {
				themeStyles.add(resetProcessed ? 1 : 0, file);
				return;
			}

			// do not include style variants since are included by preview servlet within media query sections
			if (!file.hasVariants() && file.isStyle()) {
				themeStyles.add(file);
			}
		}
	}

	/**
	 * Get style files declared into project theme directory. Returned set is not changeable.
	 * 
	 * @return site styles.
	 * @see #themeStyles
	 */
	public List<FilePath> getThemeStyles() {
		return Collections.unmodifiableList(themeStyles);
	}

	/**
	 * Create script file for for given preview file path and scan its dependencies. Returns created script file or null if
	 * given preview file does not exist.
	 * 
	 * @param previewFile preview file path.
	 * @return preview script file or null.
	 */
	public IScriptFile getPreviewScript(FilePath previewFile) {
		if (!previewFile.exists()) {
			return null;
		}
		ScriptFile previewScript = new ScriptFile(this, previewFile);
		/*
		// scan script dependencies only if discovery is enabled
		if (scriptDependencyStrategy == ScriptDependencyStrategy.DISCOVERY) {
			previewScript.scanDependencies(classScripts);
		}
		*/
		return previewScript;
	}

	/**
	 * Get script files defining certain script classes. It is a bug condition if is not possible to found script file for a
	 * particular script class.
	 * 
	 * @param layoutFile layout file using script classes,
	 * @param scriptClasses script classes.
	 * @return script files, in no particular order.
	 * @throws WoodException if script file not found.
	 */
	public Collection<IScriptFile> getScriptFiles(Collection<String> scriptClasses) {
		Set<IScriptFile> scriptFiles = new HashSet<>();
		for (String scriptClass : scriptClasses) {
			ScriptFile scriptFile = classScripts.get(scriptClass);
			if (scriptFile == null) {
				throw new WoodException("Broken script reference. No script file found for class |%s|.", scriptClass);
			}
			scriptFiles.add(scriptFile);
		}
		return scriptFiles;
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

	// ------------------------------------------------------
	// Preview support

	/**
	 * Scan project for script files and return the discovered ones. This method is designed for preview process and does not
	 * use cache. It scans script, library and generated source directories. Delegates {@link #scanScriptFiles(DirPath)} for
	 * recursive directories scanning.
	 * 
	 * @return project script files.
	 */
	public Collection<ScriptFile> previewScriptFiles() {
		scripts.clear();
		classScripts.clear();

		// script files initialization is a two stages process:
		// 1. scan for script files and initialize file and class scripts maps
		// 2. determine each script file dependencies

		scanScriptFiles(new DirPath(this, CT.SCRIPT_DIR));
		scanScriptFiles(new DirPath(this, CT.LIBRARY_DIR));
		scanScriptFiles(new DirPath(this, CT.GENERATED_DIR));

		/*
		// scan script files dependencies only if discovery is enabled
		if (scriptDependencyStrategy == ScriptDependencyStrategy.DISCOVERY) {
			for (ScriptFile scriptFile : scripts.values()) {
				scriptFile.scanDependencies(classScripts);
			}
		}
		*/
		
		return Collections.unmodifiableCollection(scripts.values());
	}

	/**
	 * Scan directory and all its descendants for script files and caches the results. This method is invoked recursively on
	 * directories tree; after completion {@link #scripts} and {@link #classScripts} fields are updated accordingly.
	 * 
	 * @param dir base directory to scan for script files.
	 */
	private void scanScriptFiles(DirPath dir) {
		// in order to reuse script files scanning logic create an instance of ScriptsScanner class
		// and delegate it from FilesHandler#onFile()
		final ScriptsScanner scriptsScanner = new ScriptsScanner();

		dir.files(new FilesHandler() {
			@Override
			public void onDirectory(DirPath dir) throws Exception {
				scanScriptFiles(dir);
			}

			@Override
			public void onFile(FilePath file) throws Exception {
				scriptsScanner.onFile(file);
			}
		});
	}

	/**
	 * Get operator handler as configured by selected naming strategy.
	 * 
	 * @return operator handler for project naming convention.
	 */
	public IOperatorsHandler getOperatorsHandler() {
		return operatorsHandler;
	}

	public IScriptDependecyHandler getScriptDependecyHandler() {
		return scriptDependecyHandler;
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
