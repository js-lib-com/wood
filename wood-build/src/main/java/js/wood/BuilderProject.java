package js.wood;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import js.wood.impl.ResourceType;

/**
 * WOOD {@link Project} extension for build process.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class BuilderProject extends Project {
	public static BuilderProject create(File projectDir) {
		BuilderProject project = new BuilderProject(projectDir);
		project.postCreate();
		return project;
	}

	/**
	 * Variables declared on assets directory. Asset variables are used when source directory variables miss a certain value.
	 */
	private final Variables assetVariables;

	/**
	 * Cache for resource variables. Parse variables declared on a source file directory and store in a dictionary with
	 * directory as key.
	 */
	private final Map<FilePath, Variables> variables;

	/** Project page components. */
	private final List<CompoPath> pages;

	/**
	 * Construct builder project and scan project file system for theme styles, variables and page components.
	 * 
	 * @param projectDir project root directory.
	 */
	private BuilderProject(File projectDir) {
		super(projectDir);

		this.assetVariables = new Variables(getAssetDir());
		this.variables = new HashMap<>();
		this.pages = new ArrayList<>();

		registerScanHandler(new FilePathVisitor(this, variables, pages));
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
	public Map<FilePath, Variables> getVariables() {
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
		return createFilePath(path).load();
	}

	// --------------------------------------------------------------------------------------------
	// support for scanning project file system

	static class FilePathVisitor implements IFilePathVisitor {
		private final Project project;
		private final Map<FilePath, Variables> variables;
		private final List<CompoPath> pages;

		public FilePathVisitor(Project project, Map<FilePath, Variables> variables, List<CompoPath> pages) {
			this.project = project;
			this.variables = variables;
			this.pages = pages;
		}

		@Override
		public void visitFile(FilePath file) throws Exception {
			final FilePath parentDir = file.getParentDir();
			if (parentDir == null) {
				return;
			}

			// variables definition files are XML files with root element one of defined resource type variables
			if (file.isXML(ResourceType.variables())) {
				Variables parentDirVariables = variables.get(parentDir);
				if (parentDirVariables == null) {
					parentDirVariables = new Variables(project);
					variables.put(parentDir, parentDirVariables);
				}
				parentDirVariables.load(file);
				return;
			}

			// XML component descriptor for pages has root 'page'
			if (file.hasBaseName(parentDir.getName()) && file.isXML("page")) {
				pages.add(project.createCompoPath(parentDir.value()));
				return;
			}
		}
	}
}
