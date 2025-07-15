package com.jslib.wood.build;

import com.jslib.wood.*;
import com.jslib.wood.lang.CheckedFunction;
import com.jslib.wood.util.StringsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.function.Function;

/**
 * Builder generates site files from project components. It reads component source and resource files from project, consolidates
 * them into pages and write to build file system.
 * <p>
 * Project builder for user interface resources. Builder acts as a bridge between {@link Project} and {@link BuildFS}. It reads
 * component source and resource files from project, consolidates them into pages then write to build site directory. Build
 * class implements {@link #onResourceReference(Reference, FilePath)} used to process resource references declared into source
 * files.
 * <p>
 * Using this class is pretty straightforward: create instance providing project directory then just invoke {@link #build()}.
 * Optionally, one may set the build number.
 *
 * <pre>
 * Builder builder = new Builder(projectDir);
 * builder.setBuildNumber(buildNumber);
 * builder.build();
 * ...
 * String sitePath = builder.getSitePath();
 * </pre>
 * <p>
 * Builder interface is designed for integration with external tools. There are extensions for Ant tasks and Eclipse plug-in
 * using this Builder class, but not part of this library distribution.
 *
 * @author Iulian Rotaru
 * @version draft
 */
public class Builder implements IReferenceHandler {
    private static final Logger log = LoggerFactory.getLogger(Builder.class);

    /**
     * Builder project instance.
     */
    private final BuilderProject project;

    /**
     * Build file system is there all pages and resources are created.
     */
    private final BuildFS buildFS;

    /**
     * Current processing component.
     */
    private Component currentComponent;

    /**
     * Current processing language variant.
     */
    private String language;

    /**
     * Construct builder instance. Create {@link Project} instance with given project root directory. Scan for project layout
     * and script files and initialize project pages and variables map. Create build FS instance taking care to create build
     * directory, if missing.
     *
     * @param config builder configuration.
     * @throws IOException if build directory creation fails.
     */
    public Builder(BuilderConfig config) throws IOException {
        log.trace("Builder(BuilderConfig config)");
        this.project = new BuilderProject(config.getProjectDir());
        this.project.create();

        File buildDir = this.project.getBuildDir().toFile();
        if (!buildDir.exists() && !buildDir.mkdirs()) {
            throw new IOException("Fail to create build directory " + buildDir);
        }
        this.buildFS = new DefaultBuildFS(buildDir, config.getBuildNumber());
    }

    /**
     * Test constructor.
     *
     * @param project builder project,
     * @param buildFS build file system.
     */
    Builder(BuilderProject project, BuildFS buildFS) {
        log.trace("Builder(BuilderProject project, BuildFS buildFS)");
        this.project = project;
        this.buildFS = buildFS;
    }

    /**
     * Get the path to the build directory. The Maven plugin uses this accessor.
     *
     * @return build directory path.
     */
    public FilePath getBuildDir() {
        return project.getBuildDir();
    }

    /**
     * Run project building process. Uses project detected languages to initialize current processing {@link #language} then
     * delegates {@link #buildPage(Component)} for every discovered page.
     *
     * @throws IOException for error related to underlying file system operations.
     */
    public void build() throws IOException {
        log.trace("build()");
        for (String language : project.getLanguages()) {
            this.language = language;
            if (project.isMultiLanguage()) {
                buildFS.setLanguage(language);
            }

            if (project.getPwaWorker().exists()) {
                try (SourceReader reader = new SourceReader(project.getPwaWorker(), this)) {
                    buildFS.writePwaWorker(reader);
                }
            }

            for (CompoPath page : project.getPages()) {
                Component pageComponent = new Component(page, this);
                currentComponent = pageComponent;
                pageComponent.scan();
                buildPage(pageComponent);
            }
        }
    }

    /**
     * Build page identified by given component path and copy to build directory. Component should be designed for page
     * generation so that it should have body root or to use a template that has body.
     * <p>
     * This method create a page document with the component instance and initialize it as follows:
     * <ul>
     * <li>create X(HT)ML document with header and body loaded from component layout,
     * <li>set content type, metas from project configuration and page title,
     * <li>add header styles in order: third party fonts, reset and key-frames, component styles and themes,
     * <li>include script files at the body end,
     * <li>update default values for missing attributes,
     * <li>serialize document to build page directory.
     * </ul>
     * <p>
     * Be aware that {@link #language} should be properly initialized before calling this method, if project has multi-language
     * support.
     *
     * @param pageComponent page component.
     * @throws IOException if files operation fails.
     */
    void buildPage(Component pageComponent) throws IOException {
        log.trace("buildPage(Component pageComponent)");
        log.debug("Building page {}", pageComponent);

        PageDocument pageDocument = new PageDocument(pageComponent);
        pageDocument.setLanguage(language);
        pageDocument.setContentType("text/html; charset=UTF-8");
        pageDocument.setTitle(pageComponent.getTitle());
        pageDocument.setDescription(pageComponent.getDescription());
        pageDocument.setAuthors(project.getAuthors());

        for (IMetaDescriptor meta : project.getMetaDescriptors()) {
            pageDocument.addMeta(meta);
        }
        for (IMetaDescriptor meta : pageComponent.getMetaDescriptors()) {
            pageDocument.addMeta(meta);
        }

        if (project.getPwaManifest().exists()) {
            try (SourceReader reader = new SourceReader(project.getPwaManifest(), this)) {
                pageDocument.addPwaManifest(buildFS.writePwaManifest(reader));
            }
        }
        if (project.getFavicon().exists()) {
            pageDocument.addFavicon(buildFS.writeFavicon(pageComponent, project.getFavicon()));
        }

        // links order:
        // 1. external links defined by project
        // 2. external links defined by page
        // 3. var.css
        // 4. default.css
        // 5. fx.css
        // 6. theme styles - theme styles are in no particular order since they are independent of each other
        // 7. component styles - first use template and child component styles then parent component

        for (ILinkDescriptor link : project.getLinkDescriptors()) {
            pageDocument.addLink(link, exlambda(file -> buildFS.writeStyle(pageComponent, file, this)));
        }
        for (ILinkDescriptor link : pageComponent.getLinkDescriptors()) {
            pageDocument.addLink(link, exlambda(file -> buildFS.writeStyle(pageComponent, file, this)));
        }

        ThemeStyles themeStyles = project.getThemeStyles();
        if (themeStyles.getVariables() != null) {
            pageDocument.addStyle(buildFS.writeStyle(pageComponent, themeStyles.getVariables(), this));
        }
        if (themeStyles.getDefaultStyles() != null) {
            pageDocument.addStyle(buildFS.writeStyle(pageComponent, themeStyles.getDefaultStyles(), this));
        }
        if (themeStyles.getAnimations() != null) {
            pageDocument.addStyle(buildFS.writeStyle(pageComponent, themeStyles.getAnimations(), this));
        }
        for (FilePath styleFile : themeStyles.getStyles()) {
            pageDocument.addStyle(buildFS.writeStyle(pageComponent, styleFile, this));
        }

        for (FilePath styleFile : pageComponent.getStyleFiles()) {
            pageDocument.addStyle(buildFS.writeStyle(pageComponent, styleFile, this));
        }

        FilePath pwaLoader = project.getPwaLoader();
        if (pwaLoader.exists()) {
            addScript(pageComponent, pageDocument, project.createScriptDescriptor(pwaLoader, true));
        }
        for (IScriptDescriptor script : project.getScriptDescriptors()) {
            addScript(pageComponent, pageDocument, script);
        }
        for (IScriptDescriptor script : pageComponent.getScriptDescriptors()) {
            addScript(pageComponent, pageDocument, script);
        }

        buildFS.writePage(pageComponent, pageDocument.getDocument());
    }

    /**
     * Helper for adding scripts to the page document. In addition to creating the script element in the page document,
     * this method ensures that the script is written to the build file system. If the script is embedded, its source
     * code is also included directly in the page document.
     *
     * @param pageComponent page component from which page document is created,
     * @param pageDocument  page document under construction, that is, updated in current building step,
     * @param script        descriptor for script to be added to page document and possible to write to build filesystem.
     * @throws IOException if write on build filesystem fails.
     */
    private void addScript(Component pageComponent, PageDocument pageDocument, IScriptDescriptor script) throws IOException {
        log.trace("addScript(Component pageComponent, PageDocument pageDocument, IScriptDescriptor script)");

        for (IScriptDescriptor dependency : project.getScriptDependencies(script.getSource())) {
            addScript(pageComponent, pageDocument, dependency);
            return;
        }

        String relativeSource = script.getSource();
        String sourceCode = null;
        if (FilePath.accept(relativeSource)) {
            // only local script
            FilePath scriptFile = project.createFilePath(script.getSource());
            assert scriptFile.exists() : "Missing script file " + scriptFile;
            try (SourceReader reader = new SourceReader(scriptFile, this)) {
                if (script.isEmbedded()) {
                    // relative source does not matter if script is embedded
                    sourceCode = StringsUtil.load(reader);
                } else {
                    relativeSource = buildFS.writeScript(pageComponent, reader);
                    // source code remains null for linked script
                }
            }
        }
        // for third party script relative source remains as declared on script descriptor and source code to null

        pageDocument.addScript(script, relativeSource, sourceCode);
    }

    /**
     * Resource reference handler invoked by {@link SourceReader} when discover a resource reference into source file. Returned
     * value is used by source reader to replace the reference, into source file. If reference points to a variable, returns its
     * value or null if not found. If media file, copy it to build media directory then return build media file path.
     *
     * @param reference  resource reference,
     * @param sourceFile source file where <code>reference</code> is used.
     * @return value to replace reference on source file.
     * @throws IOException   if media file write operation fails.
     * @throws WoodException if directory variables or media file is missing.
     */
    @Override
    public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException, WoodException {
        if (reference.isVariable()) {
            String value = null;
            Variables dirVariables = project.getVariables(sourceFile.getParentDir());
            // source parent directory can be null in which case dirVariables also null
            if (dirVariables != null) {
                value = dirVariables.get(language, reference, sourceFile, this);
            }
            if (value == null) {
                value = project.getAssetVariables().get(language, reference, sourceFile, this);
            }
            if (value == null) {
                throw new WoodException("Missing variable value for reference %s:%s", sourceFile, reference);
            }
            return value;
        }

        if (reference.isProject()) {
            String value = project.getDescriptor().getValue(reference.getName());
            if (value == null) {
                throw new WoodException("Missing project descriptor value for reference %s:%s", sourceFile, reference);
            }
            return value;
        }

        if (reference.isLayoutFile()) {
            return buildFS.getPageLayout(project.createCompoPath(reference.getValue()));
        }

        // here reference is a resource file

        FilePath resourceFile = project.getResourceFile(language, reference, sourceFile);
        if (resourceFile == null) {
            throw new WoodException("Missing resource file for reference %s:%s", sourceFile, reference);
        }

        if (reference.isMediaFile()) {
            if (sourceFile.isManifest()) {
                return buildFS.writeManifestMedia(resourceFile);
            }
            if (sourceFile.isComponentDescriptor()) {
                return buildFS.writePageMedia(currentComponent, resourceFile);
            }

            switch (sourceFile.getType()) {
                case LAYOUT:
                    return buildFS.writePageMedia(currentComponent, resourceFile);

                case STYLE:
                    return buildFS.writeStyleMedia(resourceFile);

                case SCRIPT:
                    return buildFS.writeScriptMedia(resourceFile);

                default:
            }
        }

        if (reference.isFontFile() && sourceFile.isStyle()) {
            // font files can be referenced only from style files
            // in this case resource file is the font file loaded from style file parent or from project assets
            return buildFS.writeFontFile(resourceFile);
        }

        if (reference.isGenericFile()) {
            switch (sourceFile.getType()) {
                case LAYOUT:
                    return buildFS.writePageFile(currentComponent, resourceFile);

                case SCRIPT:
                    return buildFS.writeScriptFile(resourceFile);

                default:
                    break;
            }
        }

        if (reference.isStyleFile()) {
            return buildFS.writeShadowStyle(currentComponent, resourceFile);
        }

        return null;
    }

    private static <T, R> Function<T, R> exlambda(CheckedFunction<T, R> handler) {
        return argument -> {
            try {
                return handler.apply(argument);
            } catch (Exception ex) {
                throw new WoodException(ex);
            }
        };
    }

    // --------------------------------------------------------------------------------------------
    // Test Support

    @SuppressWarnings("all")
    void setLanguage(String language) {
        this.language = language;
    }
}
