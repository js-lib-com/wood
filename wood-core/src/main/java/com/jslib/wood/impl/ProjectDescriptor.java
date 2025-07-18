package com.jslib.wood.impl;

import com.jslib.wood.CT;
import com.jslib.wood.FilePath;
import com.jslib.wood.WoodException;
import com.jslib.wood.dom.Element;
import com.jslib.wood.util.ClassesUtil;
import com.jslib.wood.util.StringsUtil;

import java.io.File;
import java.util.*;

/**
 * Project descriptor is an XML file that contains global project properties similar in structure with
 * {@link ComponentDescriptor} . In fact there may be properties present on both project and component descriptor, in which case
 * component takes precedence.
 * <p>
 * Project descriptor file is located into project root directory with the name <code>project.xml</code>. Since there are
 * sensible default value for all configurations this file is optional.
 * <p>
 * Project descriptor instance has not mutable state, therefore is thread safe.
 *
 * @author Iulian Rotaru
 * @see ComponentDescriptor
 * @since 1.0
 */
public class ProjectDescriptor extends BaseDescriptor {
    /**
     * Mandatory project language(s) list. Although this property is mandatory the underlying <code>language</code> element is
     * not - if is missing uses <code>en</code> as default language.
     * <p>
     * The first declared language is the default one; this holds true even if there is a single language declared. Note that
     * default language is used for resources without language variant.
     */
    private final List<String> languages;

    /**
     * Optional media query definitions, defaults to empty list.
     */
    private final Set<MediaQueryDefinition> mediaQueries;

    public ProjectDescriptor(FilePath descriptorFile) {
        super(descriptorFile, descriptorFile.exists() ? descriptorFile.getReader() : null);

        this.languages = StringsUtil.split(text("language", "en"), ',', ' ');
        if (languages.isEmpty()) {
            throw new WoodException("Invalid project descriptor; empty <language> element");
        }

        this.mediaQueries = new HashSet<>();

        for (Element mediaQueryElement : this.doc.findByTag("media-query")) {
            String alias = mediaQueryElement.getAttr("alias");
            String media = mediaQueryElement.getAttr("media");
            if (media == null) {
                media = "screen";
            }
            String expression = mediaQueryElement.getAttr("expression");
            if (!mediaQueries.add(new MediaQueryDefinition(alias, media, expression))) {
                throw new WoodException("Redefinition of the media query alias %s", alias);
            }
        }
    }

    // --------------------------------------------------------------------------------------------

    /**
     * Get naming strategy used to declare <em>WOOD</em> operators. Property name is <code>operators</code> and supported values
     * are defined by {@link OperatorsNaming} enumeration.
     * <p>
     * If no naming strategy is declared on project descriptor uses {@link OperatorsNaming#DATA_ATTR}.
     *
     * @return naming strategy for <em>WOOD</em> operators.
     */
    public OperatorsNaming getOperatorsNaming() {
        return OperatorsNaming.valueOf(text("operators", OperatorsNaming.DATA_ATTR.name()));
    }

    public String getBuildDir() {
        return text("build-dir", CT.DEF_BUILD_DIR);
    }

    public String getAssetDir() {
        return text("asset-dir", CT.DEF_ASSET_DIR);
    }

    public String getThemeDir() {
        return text("theme-dir", CT.DEF_THEME_DIR);
    }

    /**
     * Get the list of pages excluded from build process. Returned list is empty if <code>excludes</code> element is not
     * present.
     *
     * @return unmodifiable excluded paths list, possible empty.
     */
    public List<String> getExcludeDirs() {
        Element el = doc.getByTag("exclude-dirs");
        if (el == null) {
            return Collections.emptyList();
        }
        return Collections.unmodifiableList(StringsUtil.split(el.getText(), ',', ' '));
    }

    /**
     * Get project authors list which may be empty if none declared.
     *
     * @return project authors list, possible empty.
     */
    public List<String> getAuthors() {
        return StringsUtil.split(text("authors", ""), ',');
    }

    /**
     * Get favicon path, relative to project root, as defined by <code>favicon</code> element. Return default value
     * <code>favicon.ico</code> if element is not defined on project descriptor.
     *
     * @return favicon path.
     */
    public String getFavicon() {
        return text("favicon", CT.DEF_FAVICON_FILE);
    }

    /**
     * Get PWA manifest path, relative to project root, as defined by <code>pwa-manifest</code> element. Return default value
     * <code>manifest.json</code> if element is not defined on project descriptor.
     *
     * @return manifest path.
     */
    public String getPwaManifest() {
        return text("pwa-manifest", CT.DEF_PWA_MANIFEST_FILE);
    }

    /**
     * Get file path for PWA script loader, relative to project root, as defined by <code>pwa-loader</code> element. Return
     * default value <code>loader.js</code> if element is not defined on project descriptor.
     *
     * @return PWA loader path.
     */
    public String getPwaLoader() {
        return text("pwa-loader", CT.DEF_PWA_LOADER_FILE);
    }

    /**
     * Get file path for PWA worker script, relative to project root, as defined by <code>pwa-worker</code> element. Return
     * default value <code>worker.js</code> if element is not defined on project descriptor.
     *
     * @return PWA worker path.
     */
    public String getPwaWorker() {
        return text("pwa-worker", CT.DEF_PWA_WORKER_FILE);
    }

    /**
     * Return project configured languages. Returned list has at least one record, even if <code>language</code> element is
     * missing from project configuration file. See {@link #languages} for details. Returned list is immutable.
     *
     * @return unmodifiable list of project languages.
     * @see #languages
     */
    public List<String> getLanguage() {
        return Collections.unmodifiableList(languages);
    }

    /**
     * Get the media query definitions declared on this project descriptor or the default ones. Returned collection does not
     * guarantee the order from descriptor but {@link MediaQueryDefinition} has its own weight derived from declaration order.
     * <p>
     * If no media query definition are declared on project descriptor return empty collection.
     *
     * @return unmodifiable collection of media query definitions declared on this descriptor, possible empty.
     */
    public Collection<MediaQueryDefinition> getMediaQueryDefinitions() {
        return Collections.unmodifiableCollection(mediaQueries);
    }

    public String getValue(String name) {
        String value = text(name);
        if (value != null) {
            return value;
        }

        if (name.equals("title")) {
            // getTitle returns null when title element is not defined but for PWA we need a value
            // infer it from project root directory name
            File projectRoot = descriptorFile.getProject().getProjectRoot();
            return projectRoot != null ? StringsUtil.toTitleCase(projectRoot.getName()) : null;
        }

        try {
            return ClassesUtil.getGetter(getClass(), StringsUtil.toMemberName(name)).invoke(this).toString();
        } catch (Exception e) {
            return null;
        }
    }
}
