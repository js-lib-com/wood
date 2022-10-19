package com.jslib.wood.impl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.regex.Matcher;

import com.jslib.api.dom.Element;
import com.jslib.util.Strings;
import com.jslib.wood.CT;
import com.jslib.wood.FilePath;
import com.jslib.wood.WoodException;

/**
 * Project configuration file. This file is located into project root directory with the name <code>project.xml</code>. It is
 * mandatory and constructor throws exception if not found it. It contains customizable values for project.
 * <p>
 * Here are current implemented configuration values:
 * <table>
 * <tr>
 * <td><b>Element
 * <td><b>Description
 * <td><b>Sample Value
 * <td><b>Default Value
 * <tr>
 * <td>name
 * <td>project name used for internal representation
 * <td>test-project
 * <td>project directory name
 * <tr>
 * <td>display
 * <td>project name for user interface
 * <td>Test Project
 * <td>project name to title case
 * <tr>
 * <td>description
 * <td>project description for user interface
 * <td>Project used as fixture for unit testing.
 * <td>project display
 * <tr>
 * <td>language
 * <td>project language or comma separated languages list if project is multi-languages
 * <td>en, ro, de, jp
 * <td>
 * <tr>
 * <td>site-dir
 * <td>the path of directory used for site build, relative to project root
 * <td>build/site
 * <td>build/site
 * <tr>
 * <td>analytics
 * <td>Google Analytics account ID used to include analytics script in all pages
 * <td>UA-12345678-1
 * <td>
 * <tr>
 * <td>facebook
 * <td>Facebook SDK application ID used to include Facebook SDK initialization script in all pages
 * <td>123456789012345
 * <td>
 * <tr>
 * <td>meta
 * <td>page header meta elements ready to be inserted into generated site pages
 * <td>&lt;meta http-equiv="X-UA-Compatible" content="IE=9; IE=8; IE=7; IE=EDGE" /&gt;
 * <td>
 * <tr>
 * <td>font
 * <td>third party font URL usable for href attribute in page header link element
 * <td>http://fonts.googleapis.com/css?family=Open+Sans
 * <td>
 * <tr>
 * <td>excludes
 * <td>comma or space separated component paths excluded from build process; preview is still usable
 * <td>page/about, page/license
 * <td>
 * </table>
 * <p>
 * For convenience here is a sample configuration file.
 * 
 * <pre>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *  &lt;project&gt;
 *      &lt;name&gt;project&lt;/name&gt;
 *      &lt;display&gt;Test Project&lt;/display&gt;
 *      &lt;description&gt;Project used as fixture for unit testing.&lt;/description&gt;
 *      &lt;language&gt;en, ro&lt;/language&gt;
 *      
 *      &lt;site-dir&gt;build/site&lt;/site-dir&gt;
 *   
 *      &lt;metas&gt;
 *          &lt;meta http-equiv="X-UA-Compatible" content="IE=9; IE=8; IE=7; IE=EDGE" /&gt;
 *          &lt;meta name="viewport" content="width=device-width, initial-scale=1.0, maximum-scale=1.0" /&gt;
 *      &lt;/metas&gt;
 *  
 *      &lt;fonts&gt;
 *          &lt;font&gt;http://fonts.googleapis.com/css?family=Roboto&lt;/font&gt;
 *          &lt;font&gt;http://fonts.googleapis.com/css?family=Great+Vibes&lt;/font&gt;
 *      &lt;/fonts&gt;
 *      
 *      &lt;excludes&gt;page/about, compo/video-player&lt;/excludes&gt;
 *  &lt;/project&gt;
 * </pre>
 * <p>
 * Project descriptor instance has not mutable state, therefore is thread safe.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 * @see ComponentDescriptor
 */
public class ProjectDescriptor extends BaseDescriptor {
	/**
	 * Mandatory project locale list. Although this property is mandatory the underlying <code>locale</code> element is not - if
	 * is missing uses <code>en</code> as default locale.
	 * <p>
	 * The first declared locale is always the default one; this holds true even if there is a single locale declared. Note that
	 * default locale is used for resources without locale variant.
	 */
	private final List<Locale> locales;

	/** Optional media query definitions, defaults to empty list. */
	private final Set<MediaQueryDefinition> mediaQueries;

	public ProjectDescriptor(FilePath descriptorFile) {
		super(descriptorFile, descriptorFile.exists() ? descriptorFile.getReader() : null);

		this.locales = new ArrayList<>();
		this.mediaQueries = new HashSet<>();

		Strings.split(text("locale", "en"), ',', ' ').forEach(languageTag -> locales.add(locale(languageTag)));
		if (locales.isEmpty()) {
			throw new WoodException("Invalid project descriptor. Empty <locale> element.");
		}

		int index = 0;
		for (Element mediaQueryElement : this.doc.findByTag("media-query")) {
			final String alias = mediaQueryElement.getAttr("alias");
			final String expression = mediaQueryElement.getAttr("expression");
			if (!mediaQueries.add(new MediaQueryDefinition(alias, expression, ++index))) {
				throw new WoodException("Redefinition of the media query alias |%s|.", alias);
			}
		}
	}

	/**
	 * Create locale instance for given language tag.
	 * 
	 * @param languageTag language tag.
	 * @return locale instance for the language tag.
	 */
	private static final Locale locale(String languageTag) {
		Matcher matcher = Variants.LOCALE.matcher(languageTag);
		if (!matcher.find()) {
			throw new WoodException("Invalid language tag format |%s| into project config.", languageTag);
		}
		String country = matcher.group(2);
		if (country == null) {
			return new Locale(matcher.group(1));
		}
		return new Locale(matcher.group(1), country);
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
		return Collections.unmodifiableList(Strings.split(el.getText(), ',', ' '));
	}

	/**
	 * Get project authors list which may be empty if none declared.
	 * 
	 * @return project authors list, possible empty.
	 */
	public List<String> getAuthors() {
		return Strings.split(text("authors", ""), ',');
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

	public String getPwaWorker() {
		return text("pwa-worker", CT.DEF_PWA_WORKER_FILE);
	}

	/**
	 * Return project configured locales. Returned list has at least one record, even if <code>locale</code> element is missing
	 * from project configuration file. See {@link #locales} for details. Returned list is immutable.
	 * 
	 * @return unmodifiable list of project locales.
	 * @see #locales
	 */
	public List<Locale> getLocales() {
		return Collections.unmodifiableList(locales);
	}

	/**
	 * Get the media query definitions declared on this project descriptor or the default ones. Returned collection does not
	 * guarantees the order from descriptor but {@link MediaQueryDefinition} has its own weight derived from declaration order.
	 * <p>
	 * If no media query definition are declared on project descriptor return empty collection.
	 * 
	 * @return unmodifiable collection of media query definitions declared on this descriptor, possible empty.
	 */
	public Collection<MediaQueryDefinition> getMediaQueryDefinitions() {
		return Collections.unmodifiableCollection(mediaQueries);
	}
}
