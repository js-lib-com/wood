package js.wood;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import js.util.Strings;
import js.wood.impl.FileType;
import js.wood.impl.FilesHandler;
import js.wood.impl.ResourceType;
import js.wood.util.Files;

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
		this.excludes.add(new DirPath(this, Files.getRelativePath(projectDir, buildDir, true) + Path.SEPARATOR));
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
		return Strings.load(new FilePath(this, path).toFile());
	}

	private void scan(DirPath dir) {
		if (dir.isExcluded()) {
			return;
		}

		dir.files(FileType.XML, new FilesHandler() {
			@Override
			public void onDirectory(DirPath dir) throws Exception {
				scan(dir);
			}

			@Override
			public void onFile(FilePath file) throws Exception {
				DirPath parentDir = file.getParentDirPath();
				if(parentDir == null) {
					return;
				}
				
				if (Files.isXML(file.toFile(), ResourceType.variables())) {
					Variables parentDirVariables = variables.get(parentDir);
					if (parentDirVariables == null) {
						parentDirVariables = new Variables(BuilderProject.this);
						variables.put(dir, parentDirVariables);
					}
					parentDirVariables.load(file);
					return;
				}

				// component descriptor for pages has root 'page'
				if (parentDir.getName().equals(file.getBaseName()) && Files.isXML(file.toFile(), "page")) {
					CompoPath compoPath = new CompoPath(BuilderProject.this, Files.getRelativePath(projectRoot, parentDir.toFile(), true));
					if (!compoPath.isExcluded()) {
						pages.add(compoPath);
					}
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
