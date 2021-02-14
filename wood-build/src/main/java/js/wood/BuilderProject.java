package js.wood;

import java.io.File;
import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import js.util.Strings;
import js.wood.impl.FilesHandler;
import js.wood.impl.Variables;

/**
 * WOOD {@link Project} extension for build process.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class BuilderProject extends Project {
	/** Site build directory, usually part of master project build. */
	private final File buildDir;

	/**
	 * Project layout files are HTM files describing user interface elements. By convention layout base name is the same as
	 * component directory name.
	 */
	private final Set<LayoutFile> layouts;

	/** Cache for resource variables. */
	private final Map<DirPath, Variables> variables;

	/**
	 * Test constructor.
	 * 
	 * @param projectDir project root directory.
	 */
	public BuilderProject(File projectDir) {
		this(projectDir, null);
	}

	public BuilderProject(File projectDir, File buildDir) {
		super(projectDir);
		this.buildDir = buildDir != null ? buildDir : new File(projectDir, this.descriptor.getBuildDir(CT.DEF_BUILD_DIR));
		if (!this.buildDir.exists()) {
			this.buildDir.mkdir();
		}
		this.layouts = new HashSet<>();
		this.variables = new HashMap<>();
	}

	/**
	 * Scan for build files and cache resulted meta data. This scanner looks for files in next directories: <code>res</code>,
	 * <code>script</code>, <code>lib</code> and <code>gen</code>, all direct child of project root directory.
	 */
	public void scan() {
		FilesScanner scanner = new FilesScanner();
		scanner.scan(new DirPath(this, CT.RESOURCE_DIR));
		scanner.scan(new DirPath(this, CT.SCRIPT_DIR));
		scanner.scan(new DirPath(this, CT.LIBRARY_DIR));
		scanner.scan(new DirPath(this, CT.GENERATED_DIR));
	}

	/**
	 * Get site build directory.
	 * 
	 * @return site build directory.
	 * @see #buildDir
	 */
	public File getBuildDir() {
		return buildDir;
	}

	/**
	 * Return true if this project has support for multiple locale settings.
	 * 
	 * @return true if project is multi-locale.
	 */
	public boolean isMultiLocale() {
		return descriptor.getLocales().size() > 1;
	}

	/**
	 * Get project layout files, in no particular order. Returned collection is not modifiable.
	 * 
	 * @return project layout files.
	 * @see #layouts
	 */
	public Set<LayoutFile> getLayoutFiles() {
		return Collections.unmodifiableSet(layouts);
	}

	/**
	 * Get project variables mapped to parent directories. Returned map is not modifiable. Every variables instance from map has
	 * a reference to project asset variables, used when variables miss a reference value.
	 * 
	 * @return project variables.
	 * @see #variables
	 */
	public Map<DirPath, IVariables> getVariables() {
		return Collections.unmodifiableMap(variables);
	}

	public String loadFile(String path) throws IOException {
		return Strings.load(getFile(path).toFile());
	}

	/**
	 * Project files scanner.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private class FilesScanner {
		/** Specialized file handlers. */
		private FilesHandler[] handlers = new FilesHandler[] { new LayoutsScanner(), new ThemeStylesScanner(), new VariablesScanner() };

		/**
		 * Scan given directory recursively till no more sub-directories.
		 * 
		 * @param dir directory to scan.
		 */
		public void scan(DirPath dir) {
			dir.files(new FilesHandler() {
				@Override
				public void onDirectory(DirPath dir) throws Exception {
					for (FilesHandler handler : handlers) {
						handler.onDirectory(dir);
					}
					scan(dir);
				}

				@Override
				public void onFile(FilePath file) throws Exception {
					for (FilesHandler handler : handlers) {
						handler.onFile(file);
					}
				}
			});
		}
	}

	/**
	 * Scanner for layout files. Layouts are HTM files describing UI elements.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private class LayoutsScanner extends FilesHandler {
		/**
		 * Add layout files to {@link Project#layouts} cache. By convention layout base name is the same as component name; also
		 * file should be layout, see {@link FilePath#isLayout()}.
		 */
		@Override
		public void onFile(FilePath file) throws Exception {
			if (file.isLayout() && file.isBaseName(file.getDirPath().getName()) && !file.isExcluded()) {
				layouts.add(new LayoutFile(BuilderProject.this, file));
			}
		}
	}

	/**
	 * Scanner for project variables. Variables are defined per directory and load values from all child XML files.
	 * 
	 * @author Iulian Rotaru
	 */
	private class VariablesScanner extends FilesHandler {
		/** Project assets variables. */
		private Variables assetVariables;

		/** Theme variables. */
		private Variables themeVariables;

		public VariablesScanner() {
			assetVariables = new Variables(BuilderProject.this);
			themeVariables = new Variables(BuilderProject.this);

			// add assets directory to theme variables
			// theme should be able to use global variables from assets
			themeVariables.setAssetVariables(assetVariables);

			variables.put(assetsDir, assetVariables);
			variables.put(themeDir, themeVariables);
		}

		/**
		 * Create empty variables instance and map to directory. Project generated files and excluded directories are skipped.
		 *
		 * @param dir current processed directory.
		 * @see Project#excludes
		 */
		@Override
		public void onDirectory(DirPath dir) throws Exception {
			if (variables.containsKey(dir)) {
				// test if variables cache already contains directory key in order to avoid overwrite
				return;
			}
			if (dir.isGenerated() || dir.isExcluded()) {
				// do not scan generated sources and excluded directories
				return;
			}
			if (dir.isAssets() || dir.isTheme()) {
				// asset and theme variables are created at this scanner instance creation
				return;
			}

			Variables dirVariables = new Variables(BuilderProject.this);
			dirVariables.setAssetVariables(assetVariables);
			dirVariables.setThemeVariables(themeVariables);
			variables.put(dir, dirVariables);
		}

		/**
		 * Load variable values from variables definition file to variables instance mapped to directory.
		 * 
		 * @param file current processed file.
		 */
		@Override
		public void onFile(FilePath file) throws Exception {
			if (file.isVariables() && !file.isExcluded()) {
				variables.get(file.getDirPath()).load(file);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tests Support

}
