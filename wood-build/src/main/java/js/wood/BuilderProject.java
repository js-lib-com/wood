package js.wood;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import js.util.Files;
import js.util.Strings;
import js.wood.impl.FileType;
import js.wood.impl.FilesHandler;
import js.wood.impl.Variables;

/**
 * WOOD {@link Project} extension for build process.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class BuilderProject extends Project {
	private final ThemeStyles themeStyles;

	/** Cache for resource variables. */
	private final Map<DirPath, Variables> variables;

	/**
	 * Test constructor.
	 * 
	 * @param projectDir project root directory.
	 */
	public BuilderProject(File projectDir) {
		super(projectDir);
		this.themeStyles = new ThemeStyles(getThemeDir());
		this.variables = new HashMap<>();
	}

	/**
	 * Scan for build files and cache resulted meta data. This scanner looks for files in next directories: <code>res</code>,
	 * <code>script</code>, <code>lib</code> and <code>gen</code>, all direct child of project root directory.
	 */
	public void scan() {
		FilesScanner scanner = new FilesScanner();
		scanner.scan(new DirPath(this, "res"));
		scanner.scan(new DirPath(this, CT.SCRIPT_DIR));
		scanner.scan(new DirPath(this, CT.LIBRARY_DIR));
		scanner.scan(new DirPath(this, CT.GENERATED_DIR));
	}

	public Collection<CompoPath> getPages() throws IOException {
		Collection<CompoPath> pages = new ArrayList<>();
		scanPages(getProjectDir(), pages);
		return pages;
	}

	private void scanPages(File currentDir, Collection<CompoPath> pages) throws IOException {
		for (File file : currentDir.listFiles()) {
			// ignores hidden files and directories
			if (file.getName().charAt(0) == '.') {
				continue;
			}
			// if directory go depth-first
			if (file.isDirectory()) {
				scanPages(file, pages);
				continue;
			}

			// detect page descriptor file
			// by convention page descriptor basename is the same as its parent directory name
			if (!Files.basename(file).equals(file.getParentFile().getName())) {
				continue;
			}
			// accept only XML files
			if (FileType.forExtension(Files.getExtension(file)) != FileType.XML) {
				continue;
			}
			// accept only XML root 'page'
			try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
				String line = reader.readLine();
				if (line.startsWith("<?")) {
					line = reader.readLine();
					if (!line.startsWith("<page>")) {
						continue;
					}
				}
			}
			CompoPath compoPath = new CompoPath(this, Files.getRelativePath(projectDir, file.getParentFile(), true));
			if (!compoPath.isExcluded()) {
				pages.add(compoPath);
			}
		}

	}

	/**
	 * Return true if this project has support for multiple locale settings.
	 * 
	 * @return true if project is multi-locale.
	 */
	public boolean isMultiLocale() {
		return descriptor.getLocales().size() > 1;
	}

	@Override
	public ThemeStyles getThemeStyles() {
		return themeStyles;
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
		private FilesHandler[] handlers = new FilesHandler[] { new VariablesScanner() };

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
				variables.get(file.getParentDirPath()).load(file);
			}
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tests Support

}
