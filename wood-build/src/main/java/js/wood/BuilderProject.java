package js.wood;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import js.wood.impl.FilesHandler;
import js.wood.impl.ResourceType;

/**
 * WOOD {@link Project} extension for build process.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class BuilderProject extends Project {
	/**
	 * Style files from theme source directory. Theme styles are global per project and are loaded for all pages. Rules declared
	 * on theme styles are related to general aspect rather than size and position.
	 */
	private final ThemeStyles themeStyles;

	/**
	 * Variables declared on assets directory. Asset variables are used when source directory variables miss a certain value.
	 */
	private final Variables assetVariables;

	/**
	 * Cache for resource variables. Parse variables declared on a source file directory and store in a dictionary with
	 * directory as key.
	 */
	private final Map<DirPath, Variables> variables;

	/** Project page components. */
	private final List<CompoPath> pages;

	/**
	 * Construct builder project and scan project file system for theme styles, variables and page components.
	 * 
	 * @param projectDir project root directory,
	 * @param buildDir build directory.
	 */
	public BuilderProject(File projectDir, File buildDir) {
		super(projectDir);

		this.excludes.add(createDirPath(buildDir));
		this.themeStyles = new ThemeStyles(getThemeDir());
		this.assetVariables = new Variables(getAssetsDir());
		this.variables = new HashMap<>();
		this.pages = new ArrayList<>();

		scan(getProjectDir());
	}

	/**
	 * Get project theme styles.
	 * 
	 * @return project theme styles.
	 * @see #themeStyles
	 */
	@Override
	public ThemeStyles getThemeStyles() {
		return themeStyles;
	}

	/**
	 * Get global variables declared on project assets.
	 * 
	 * @return assets variables.
	 * @see #assetVariables
	 */
	public Variables getAssetVariables() {
		return assetVariables;
	}

	/**
	 * Get project variables mapped to parent directories. Returned map is not modifiable.
	 * 
	 * @return project variables.
	 * @see #variables
	 */
	public Map<DirPath, Variables> getVariables() {
		return Collections.unmodifiableMap(variables);
	}

	/**
	 * Get project pages.
	 * 
	 * @return project pages.
	 * @see #pages
	 */
	public List<CompoPath> getPages() {
		return Collections.unmodifiableList(pages);
	}

	/**
	 * Return true if this project has support for multiple locale settings.
	 * 
	 * @return true if project is multi-locale.
	 */
	public boolean isMultiLocale() {
		return getLocales().size() > 1;
	}

	/**
	 * Load file content and return it as a string. This method is applicable only to text files; using non binary files does
	 * not render predictable results.
	 * 
	 * @param path file path relative to project root.
	 * @return requested file text content.
	 * @throws IOException if file reading fails.
	 */
	public String loadFile(String path) throws IOException {
		return new FilePath(this, path).load();
	}

	/**
	 * Scan project directories to discover page components and variable definition files. This method is executed recursively
	 * in depth-first order.
	 * 
	 * @param dir current directory.
	 */
	void scan(DirPath dir) {
		dir.files(new FilesHandler() {
			@Override
			public void onDirectory(DirPath dir) {
				if (!dir.isExcluded()) {
					scan(dir);
				}
			}

			@Override
			public void onFile(FilePath file) {
				final DirPath parentDir = file.getParentDirPath();
				assert parentDir != null;

				// variables definition files are XML files with root element one of defined resource type variables
				if (file.isXML(ResourceType.variables())) {
					Variables parentDirVariables = variables.get(parentDir);
					if (parentDirVariables == null) {
						parentDirVariables = new Variables(BuilderProject.this);
						variables.put(dir, parentDirVariables);
					}
					parentDirVariables.load(file);
					return;
				}

				// XML component descriptor for pages has root 'page'
				if (file.hasBaseName(parentDir.getName()) && file.isXML("page")) {
					pages.add(createCompoPath(parentDir.value()));
					return;
				}
			}
		});
	}
}
