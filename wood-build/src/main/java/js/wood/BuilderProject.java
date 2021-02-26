package js.wood;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
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
	private final File buildDir;

	private final Variables assetVariables;

	private final ThemeStyles themeStyles;

	/** Cache for resource variables. */
	private final Map<DirPath, Variables> variables;

	private final Collection<CompoPath> pages;

	/**
	 * Test constructor.
	 * 
	 * @param projectDir project root directory.
	 * @param file
	 * @throws IOException
	 */
	public BuilderProject(File projectDir, File buildDir) throws IOException {
		super(projectDir);
		this.buildDir = buildDir;
		this.excludes.add(createDirPath(buildDir));
		this.themeStyles = new ThemeStyles(getThemeDir());
		this.assetVariables = new Variables(getAssetsDir());
		this.variables = new HashMap<>();
		this.pages = new ArrayList<>();
		scan(getProjectDir());
	}

	/**
	 * Return true if this project has support for multiple locale settings.
	 * 
	 * @return true if project is multi-locale.
	 */
	public boolean isMultiLocale() {
		return getLocales().size() > 1;
	}

	@Override
	public ThemeStyles getThemeStyles() {
		return themeStyles;
	}

	public Variables getAssetVariables() {
		return assetVariables;
	}

	/**
	 * Get project variables mapped to parent directories. Returned map is not modifiable. Every variables instance from map has
	 * a reference to project asset variables, used when variables miss a reference value.
	 * 
	 * @return project variables.
	 * @see #variables
	 */
	public Map<DirPath, Variables> getVariables() {
		return Collections.unmodifiableMap(variables);
	}

	public Collection<CompoPath> getPages() {
		return Collections.unmodifiableCollection(pages);
	}

	public String loadFile(String path) throws IOException {
		return new FilePath(this, path).load();
	}

	private void scan(DirPath dir) {
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

	// --------------------------------------------------------------------------------------------
	// Test support

	File getBuildDir() {
		return buildDir;
	}

	File getBuildFile(String path) {
		return new File(buildDir, path);
	}
}
