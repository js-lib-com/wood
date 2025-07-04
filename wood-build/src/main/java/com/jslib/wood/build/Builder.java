package com.jslib.wood.build;

import com.jslib.wood.*;
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
        this.project = project;
        this.buildFS = buildFS;
    }

    /**
     * Get the path to the build directory. The Maven plugin uses this accessor.
     *
     * @return build directory path.
     */
    @SuppressWarnings("unused")
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
            if (!addScript(pageComponent, pageDocument, script)) {
                throw new WoodException("Missing script %s declared by project descriptor.", script.getSource());
            }
        }
        for (IScriptDescriptor script : pageComponent.getScriptDescriptors()) {
            if (!addScript(pageComponent, pageDocument, script)) {
                throw new WoodException("Missing script %s declared by descriptor for page component %s.", script.getSource(), pageComponent.getName());
            }
        }

        buildFS.writePage(pageComponent, pageDocument.getDocument());
    }

    private boolean addScript(Component pageComponent, PageDocument pageDocument, IScriptDescriptor script) throws IOException {
        for (IScriptDescriptor dependency : project.getScriptDependencies(script.getSource())) {
            return addScript(pageComponent, pageDocument, dependency);
        }

        String src = script.getSource();
        String text = null;
        if (FilePath.accept(src)) {
            // only local script
            FilePath scriptFile = project.createFilePath(script.getSource());
            if (!scriptFile.exists()) {
                return false;
            }
            try (SourceReader reader = new SourceReader(scriptFile, this)) {
                if (script.isEmbedded()) {
                    // src value does not matter if script is embedded
                    text = StringsUtil.load(reader);
                } else {
                    src = buildFS.writeScript(pageComponent, reader);
                    // text value remains null for linked script
                }
            }
        }
        // for third party script src remains as declared on script descriptor and text to null

        pageDocument.addScript(script, src, text);
        return true;
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
                throw new WoodException("Missing variable value for reference |%s:%s|.", sourceFile, reference);
            }
            return value;
        }

        if (reference.isProject()) {
            String value = project.getDescriptor().getValue(reference.getName());
            if (value == null) {
                throw new WoodException("Missing project descriptor value for reference |%s:%s|.", sourceFile, reference);
            }
            return value;
        }

        if (reference.isLayoutFile()) {
            return buildFS.getPageLayout(project.createCompoPath(reference.getValue()));
        }

        // here reference is a resource file

        FilePath resourceFile = project.getResourceFile(language, reference, sourceFile);
        if (resourceFile == null) {
            throw new WoodException("Missing resource file for reference |%s:%s|.", sourceFile, reference);
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

    // --------------------------------------------------------------------------------------------

    /**
     * Functional interface that accepts checked exception.
     *
     * @param <T> the type of the input to the function
     * @param <R> the type of the result of the function
     * @author Iulian Rotaru
     */
    @FunctionalInterface
    public interface CheckedFunction<T, R> {
        R apply(T argument) throws Exception;
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

    BuilderProject getProject() {
        return project;
    }

    BuildFS getBuildFS() {
        return buildFS;
    }

    @SuppressWarnings("all")
    void setCurrentComponent(Component currentComponent) {
        this.currentComponent = currentComponent;
    }

    @SuppressWarnings("all")
    void setLanguage(String language) {
        this.language = language;
    }
}
