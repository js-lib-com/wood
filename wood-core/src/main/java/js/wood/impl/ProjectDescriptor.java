package js.wood.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.regex.Matcher;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import js.util.Strings;
import js.wood.CT;
import js.wood.ILinkReference;
import js.wood.IMetaReference;
import js.wood.IScriptReference;
import js.wood.WoodException;

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
 *      &lt;analytics&gt;UA-12345678-1&lt;/analytics&gt;
 *      &lt;facebook&gt;123456789012345&lt;/facebook&gt;
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
 * 
 * @author Iulian Rotaru
 * @since 1.0
 * @see ComponentDescriptor
 */
public final class ProjectDescriptor {
	/** XML DOM document. */
	private Document doc;

	/**
	 * Mandatory project locale. This property should be present even if project has a single locale. This is because there is
	 * no way to reliable detect locale from resource files.
	 * <p>
	 * The first declared locale is always the default one; this holds true even if there is a single locale declared. Note that
	 * default locale is used for resources without locale variant.
	 */
	private List<Locale> locales = new ArrayList<Locale>();

	/**
	 * Create project descriptor instance. Every <em>WOOD</em> project should have descriptor with at least <locale> element
	 * declared.
	 *
	 * @param descriptorFile descriptor file, absolute path.
	 * @throws WoodException if descriptor file not found or locale property is missing.
	 */
	public ProjectDescriptor(File descriptorFile) throws WoodException {
		try {
			DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
			this.doc = builder.loadXML(descriptorFile);
		} catch (FileNotFoundException unused) {
			throw new WoodException("Missing project descriptor |%s|.", descriptorFile);
		}

		Element localeElement = this.doc.getByTag("locale");
		if (localeElement == null) {
			throw new WoodException("Invalid project descriptor. Missing <locale> element.");
		}
		Strings.split(localeElement.getText(), ',', ' ').forEach(languageTag -> locales.add(locale(languageTag)));
		if (locales.isEmpty()) {
			throw new WoodException("Invalid project descriptor. Empty <locale> element.");
		}
	}

	/**
	 * Create locale instance for given language tag.
	 * 
	 * @param languageTag language tag.
	 * @return locale instance for the language tag.
	 */
	private static final Locale locale(String languageTag) {
		// TODO: study if to replace this logic with Locale.Builder.setLanguageTag(java.lang.String)
		// WOOD locale is a subset of BCP language tag

		// Locale.Builder builder = new Locale.Builder();
		// builder.setLanguageTag(languageTag);
		// return builder.build();

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

	/**
	 * Get project author or null if not set.
	 * 
	 * @return project author or null.
	 */
	public String getAuthor() {
		return text("author", CT.DEF_AUTHOR);
	}

	/**
	 * Get project name or default value if <code>name</code> element is missing.
	 * 
	 * @param defaultValue default value.
	 * @return project name or default value.
	 */
	public String getName(String defaultValue) {
		return text("name", defaultValue);
	}

	/**
	 * Get project display or default value if <code>display</code> element is missing.
	 * 
	 * @param defaultValue default value.
	 * @return project display or default value.
	 */
	public String getDisplay(String defaultValue) {
		return text("display", defaultValue);
	}

	/**
	 * Get project description or default value if <code>description</code> element is missing.
	 * 
	 * @param defaultValue default value.
	 * @return project description or default value.
	 */
	public String getDescription(String defaultValue) {
		return text("description", defaultValue);
	}

	public String getTheme() {
		return text("theme", null);
	}

	/**
	 * Get site build directory path, relative to project root. Return default value if <code>site-dir</code> element is
	 * missing.
	 * 
	 * @param defaultValue default value.
	 * @return site build directory path or default value.
	 */
	public String getSiteDir(String defaultValue) {
		return text("site-dir", defaultValue);
	}

	/**
	 * Return project configured locales. Returned list has at least one record, even if <code>locale</code> element is missing
	 * from project configuration file. See {@link #locales} for details. Returned list is immutable.
	 * 
	 * @return project configured locales.
	 * @see #locales
	 */
	public List<Locale> getLocales() {
		return Collections.unmodifiableList(locales);
	}

	/**
	 * Get project configured default locale.
	 * 
	 * @return default locale.
	 */
	public Locale getDefaultLocale() {
		return locales.get(0);
	}

	public NamingStrategy getNamingStrategy() {
		return NamingStrategy.valueOf(text("naming-strategy", NamingStrategy.XMLNS.name()));
	}

	/**
	 * Get meta elements list declared into <code>meta</code> section. Returned elements list contains meta elements as they are
	 * into configuration file. If <code>meta</code> section is missing returned elements list is empty.
	 * 
	 * @return meta elements list, possible empty.
	 */
	public List<IMetaReference> getMetas() {
		List<IMetaReference> metas = new ArrayList<>();
		for (Element metaElement : doc.findByTag("meta")) {
			MetaReference meta = MetaReferenceFactory.create(metaElement);
			if (metas.contains(meta)) {
				throw new WoodException("Duplicate meta |%s| in project descriptor.", meta);
			}
			metas.add(meta);
		}
		return metas;
	}

	public List<ILinkReference> getLinks() {
		List<ILinkReference> links = new ArrayList<>();
		for (Element linkElement : doc.findByTag("link")) {
			LinkReference link = LinkReferenceFactory.create(linkElement);
			if (links.contains(link)) {
				throw new WoodException("Duplicate link |%s| in project descriptor.", link);
			}
			links.add(link);
		}
		return links;
	}

	public List<IScriptReference> getScripts() {
		List<IScriptReference> scripts = new ArrayList<>();
		for (Element scriptElement : doc.findByTag("script")) {
			ScriptReference script = ScriptReferenceFactory.create(scriptElement);
			if (scripts.contains(script)) {
				throw new WoodException("Duplicate script |%s| in project descriptor.", script);
			}
			scripts.add(script);
		}
		return scripts;
	}

	/**
	 * Get the list of paths excluded from build process. Returned list is empty if <code>excludes</code> element is not
	 * present.
	 * 
	 * @return excluded paths list, possible empty.
	 */
	public List<String> getExcludes() {
		Element el = doc.getByTag("excludes");
		if (el == null) {
			return Collections.emptyList();
		}
		return Strings.split(el.getText(), ',', ' ');
	}

	/**
	 * Return text value for element denoted by tag name or default value if element is missing.
	 * 
	 * @param tagName element tag name,
	 * @param defaultValue default value to use when element is missing.
	 * @return element text or default value.
	 */
	private String text(String tagName, String defaultValue) {
		Element el = doc.getByTag(tagName);
		return el != null ? el.getText() : defaultValue;
	}
}