package com.jslib.wood.build;

import com.jslib.wood.*;

import java.io.File;
import java.util.*;

/**
 * WOOD {@link Project} extension for build process.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
class BuilderProject extends Project {
    /**
     * Variables declared on assets directory. Asset variables are used when source directory variables miss a certain value.
     */
    private final Variables assetVariables;

    /**
     * Cache for resource variables. Parse variables declared on a source file directory and store in a dictionary with
     * directory as key.
     */
    private final Map<FilePath, Variables> variables;

    /**
     * Project page components.
     */
    private final List<CompoPath> pages;

    /**
     * Construct builder project and scan project file system for theme styles, variables and page components.
     *
     * @param projectDir project root directory.
     */
    public BuilderProject(File projectDir) {
        super(projectDir);

        this.assetVariables = new Variables(getAssetDir());
        this.variables = new HashMap<>();
        this.pages = new ArrayList<>();

        registerVisitor(new FilePathVisitor(variables, pages));
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
    public Variables getVariables(FilePath dir) {
        return variables.get(dir);
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
     * Return true if this project has support for multiple languages.
     *
     * @return true if project is multi-language.
     */
    public boolean isMultiLanguage() {
        return getLanguages().size() > 1;
    }

    // --------------------------------------------------------------------------------------------
    // support for scanning project file system

    static class FilePathVisitor implements IFilePathVisitor {
        private final Map<FilePath, Variables> variables;
        private final List<CompoPath> pages;

        public FilePathVisitor(Map<FilePath, Variables> variables, List<CompoPath> pages) {
            this.variables = variables;
            this.pages = pages;
        }

        @Override
        public void visitFile(Project project, FilePath file) {
            final FilePath parentDir = file.getParentDir();
            if (parentDir == null) {
                return;
            }

            // variables definition files are XML files with root element one of defined resource type variables
            if (file.isXml(Reference.Type.variables())) {
                Variables parentDirVariables = variables.get(parentDir);
                if (parentDirVariables == null) {
                    parentDirVariables = new Variables();
                    variables.put(parentDir, parentDirVariables);
                }
                parentDirVariables.load(file);
                return;
            }

            // XML component descriptor for pages has root 'page'
            if (file.hasBaseName(parentDir.getName()) && file.isXml("page")) {
                pages.add(project.createCompoPath(parentDir.value()));
            }
        }
    }
}
