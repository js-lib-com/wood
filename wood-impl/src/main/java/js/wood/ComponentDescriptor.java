package js.wood;

import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.lang.BugError;
import js.util.Classes;

/**
 * Component descriptor contains properties customizable at component level. This is in contrast with {@link ProjectConfig} that
 * affects all components from project. Current implementation is actually used for pages; support for ordinary components is
 * expected.
 * <p>
 * Here are current implemented values:
 * <table border="1" style="border-collapse:collapse;">
 * <tr>
 * <td><b>Element
 * <td><b>Description
 * <td><b>Usage
 * <td><b>Sample Value
 * <tr>
 * <td>version
 * <td>component version especially useful for library components
 * <td>library logic does not use it but developer may want to know it
 * <td>1.2.3
 * <tr>
 * <td>title
 * <td>component title used to identify component on user interfaces
 * <td>current implementation uses title for page head <code>title</code> element
 * <td>Index Page
 * <tr>
 * <td>description
 * <td>component description is a concise explanation of the component content
 * <td>current implementation insert description into page head using <code>meta</code> element
 * <td>Index page description.
 * <tr>
 * <td>path
 * <td>directories path to store page layout into; build file system insert this directories path just before layout file
 * <td>useable for role based security supplied by servlet container
 * <td>/admin/
 * <tr>
 * <td>scripts
 * <td>contains path to third party scripts specific to component; both project file path and absolute URL are accepted
 * <td>scripts are included into page document in the defined order
 * <td>https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js
 * </table>
 * <p>
 * For convenience below is a sample configuration file. Element values can be replaced with string variables. For example
 * <code>title</code> value can be something like <code>@string/page-title</code>. This class uses {@link ReferencesResolver} to
 * replace variables with their defined values.
 * <p>
 * Third party scripts are usually linked at the page bottom, just before closing page body. Anyway, <code>script</code> element
 * has an optional boolean attribute, <code>append-to-head</code> to force including script link at the end of the page header.
 * 
 * <pre>
 *  &lt;?xml version="1.0" encoding="UTF-8"?&gt;
 *  &lt;page&gt;
 *      &lt;version&gt;1.2.3&lt;/version&gt;
 *      &lt;title&gt;Index Page&lt;/title&gt;
 *      &lt;description&gt;Index page description.&lt;/description&gt;
 *      &lt;path&gt;/admin/&lt;/path&gt;
 *      &lt;scripts&gt;
 *          &lt;script append-to-head="true"&gt;https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js&lt;/script&gt;
 *          &lt;script&gt;http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js&lt;/script&gt;
 *      &lt;/scripts&gt;
 *  &lt;/page&gt;
 * </pre>
 * 
 * <p>
 * Directories path for page layout files is described by <code>path</code> element. Usually page layout files are stored in a
 * directory configured by build file system implementation, see {@link BuildFS#getPageDir()}. For example, in default file
 * system pages are stored into build root. If <code>path</code> element is specified, given path is used to stored page layout
 * file. In example below, <code>user-manager.htm</code> page is stored under <code>admin</code> directory. This facility is
 * useable for role based security; it allows to group pages under given path and configure security constrains base on that
 * path.
 *
 * <pre>
 *  /
 *  /admin/
 *        +-user-manager.htm
 *        ~
 *  /info/
 *        +-log.viewer.htm
 *        ~                          
 *  /media/
 *  /script/
 *  /style/
 *  +-login.htm
 *  ~
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 * @see ProjectConfig
 */
public class ComponentDescriptor {
	/** Empty XML document used when component descriptor file is missing. */
	private static final Document EMPTY_DOC;
	static {
		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		EMPTY_DOC = builder.createXML("component");
	}

	/** XML DOM document. */
	private final Document doc;

	/** File path for component descriptor. It has owning component name and XML extension. */
	private final FilePath filePath;

	/** References handler used to actually process referenced resources; defined externally. */
	private final IReferenceHandler referenceHandler;

	/**
	 * Resolver parses element values and invoke {@link #referenceHandler} for discovered references, if any. It is necessary
	 * because is legal for element value to contain resource references.
	 */
	private final ReferencesResolver resolver;

	/**
	 * Create component descriptor instance and initialize it from given file. Values defined by descriptor may contain resource
	 * references that need to be resolved. This descriptor uses external defined references handler just for that.
	 * 
	 * @param filePath descriptor file path,
	 * @param referenceHandler resource references handler.
	 */
	public ComponentDescriptor(FilePath filePath, IReferenceHandler referenceHandler) {
		this.filePath = filePath;
		this.referenceHandler = referenceHandler;
		this.resolver = new ReferencesResolver();
		try {
			DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
			this.doc = this.filePath.exists() ? builder.loadXML(this.filePath.toFile()) : EMPTY_DOC;
		} catch (FileNotFoundException unused) {
			throw new WoodException("Missing project configuration file |%s| although it exists.", this.filePath);
		}
	}

	/**
	 * Get component version or null if missing or not set. This property is loaded from <code>version</code> element. Note that
	 * version property is especially useful for library components.
	 * 
	 * @return component version or null.
	 */
	public String getVersion() {
		return value("version", null);
	}

	/**
	 * Get component title or given default value, if title is missing or not set. This property is loaded from
	 * <code>title</code> element.
	 * 
	 * @param defaultValue default title value.
	 * @return component title or supplied default value.
	 */
	public String getTitle(String defaultValue) {
		return value("title", defaultValue);
	}

	/**
	 * Get component description or given default value, if description is missing or not set. This property is loaded from
	 * <code>description</code> element.
	 * 
	 * @param defaultValue default description value.
	 * @return component description or supplied default value.
	 */
	public String getDescription(String defaultValue) {
		return value("description", defaultValue);
	}

	/**
	 * Get directory path to store page layout file or supplied default value if path is missing. This property is loaded from
	 * <code>path</code> element.
	 * 
	 * @param defaultValue default path value.
	 * @return page layout directory path or supplied default value.
	 */
	public String getPath(String defaultValue) {
		return value("path", defaultValue);
	}

	public List<StyleReference> getStyles() {
		// do not attempt to cache script paths since this method is expected to be used only once

		Element stylesEl = doc.getByTag("links");
		if (stylesEl == null) {
			return Collections.emptyList();
		}

		List<StyleReference> styles = new ArrayList<>();
		for (Element styleEl : stylesEl.getChildren()) {
			String href = styleEl.getAttr("href");
			if (href == null) {
				throw new BugError("Invalid style reference on component descriptor |%s|. Missing style hyper-reference.", filePath);
			}

			String integrity = styleEl.getAttr("integrity");
			String crossorigin = styleEl.getAttr("crossorigin");
			styles.add(new StyleReference(href, integrity, crossorigin));
		}
		return styles;
	}

	/**
	 * Get scripts defined by this component descriptor, both third party and local scripts. Returns a list of absolute URLs
	 * and/or relative paths in the order and in format defined into descriptor. There is no attempt to check path validity; it
	 * is developer responsibility to ensure URLs and paths are correct and inclusion order is proper.
	 * <p>
	 * Note that local paths are used only if project script dependency strategy is {@link ScriptDependencyStrategy#DESCRIPTOR}.
	 * <p>
	 * Here is expected scripts descriptor format.
	 * 
	 * <pre>
	 * &lt;scripts&gt;
	 *    &lt;script append-to-head="true"&gt;https://ajax.googleapis.com/ajax/libs/jquery/1.11.1/jquery.min.js&lt;/script&gt;
	 *    &lt;script&gt;http://maxcdn.bootstrapcdn.com/bootstrap/3.3.5/js/bootstrap.min.js&lt;/script&gt;
	 *    &lt;script&gt;lib/js-lib/js-lib.js&lt;/script&gt;
	 *    &lt;script&gt;gen/com/kidscademy/AdminService.js&lt;/script&gt;
	 *    &lt;script&gt;script/com/kidscademy/admin/FormPage.js&lt;/script&gt;
	 * &lt;/scripts&gt;
	 * </pre>
	 * <p>
	 * If optional attribute <code>append-to-head</code> is present into <code>script</code> element enable
	 * {@link ScriptReference#appendToHead}.
	 * 
	 * @return scripts declared by this component descriptor.
	 */
	public List<ScriptReference> getScripts() {
		// do not attempt to cache script paths since this method is expected to be used only once

		Element scriptsEl = doc.getByTag("scripts");
		if (scriptsEl == null) {
			return Collections.emptyList();
		}

		List<ScriptReference> scripts = new ArrayList<>();
		for (Element scriptEl : scriptsEl.getChildren()) {
			String src = scriptEl.getText();
			if (src.isEmpty()) {
				src = scriptEl.getAttr("src");
				if (src == null) {
					throw new BugError("Invalid script reference on component descriptor |%s|. Missing script source.", filePath);
				}
			}

			String integrity = scriptEl.getAttr("integrity");
			String crossorigin = scriptEl.getAttr("crossorigin");
			boolean defer = Boolean.parseBoolean(scriptEl.getAttr("defer"));
			boolean appendToHead = Boolean.parseBoolean(scriptEl.getAttr("append-to-head"));

			scripts.add(new ScriptReference(src, integrity, crossorigin, defer, appendToHead));
		}
		return scripts;
	}

	/**
	 * Return text value from element or null if element not found or value not set. Uses {@link ReferencesResolver} to resolve
	 * value references, if any.
	 * 
	 * @param tag tag name identifying desired element.
	 * @return element text value or null.
	 */
	private String value(String tag, String defaultValue) {
		Element el = doc.getByTag(tag);
		if (el == null) {
			return defaultValue;
		}
		String value = el.getText();
		if (value.isEmpty()) {
			return defaultValue;
		}
		return resolver.parse(value, filePath, referenceHandler);
	}

	public static class StyleReference {
		private final String href;
		private final String integrity;
		private final String crossorigin;

		public StyleReference(String href, String integrity, String crossorigin) {

			this.href = href;
			this.integrity = integrity;
			this.crossorigin = crossorigin;
		}

		public String getHref() {
			return href;
		}

		public boolean hasIntegrity() {
			return integrity != null;
		}

		public String getIntegrity() {
			return integrity;
		}

		public boolean hasCrossorigin() {
			return crossorigin != null;
		}

		public String getCrossorigin() {
			return crossorigin;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((href == null) ? 0 : href.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			StyleReference other = (StyleReference) obj;
			if (href == null) {
				if (other.href != null)
					return false;
			} else if (!href.equals(other.href))
				return false;
			return true;
		}

	}

	/**
	 * Third party script descriptor contains script source and flag to append to document head. This class is loaded from
	 * <code>scripts</code> section of the component descriptor, see snippet below. It is used to declare third party scripts.
	 * <p>
	 * 
	 * <pre>
	 * &lt;scripts&gt;
	 *    &lt;script&gt;http://code.jquery.com/jquery-1.7.min.js&lt;/script&gt;
	 *    &lt;script&gt;http://sixqs.com/site/js/lib/qtip.js&lt;/script&gt;
	 * &lt;/scripts&gt;
	 * </pre>
	 * 
	 * @author Iulian Rotaru
	 */
	public static class ScriptReference {
		/** Script source is the URL from where third script is to be loaded. */
		private final String source;

		private final String integrity;
		private final String crossorigin;
		private final boolean defer;
		
		/**
		 * Usually scripts are inserted into page document at the bottom, after body content. This flag is used to force script
		 * loading on document header.
		 */
		private final boolean appendToHead;

		public ScriptReference(String source, String integrity, String crossorigin, boolean defer, boolean appendToHead) {
			this.source = source;
			this.integrity = integrity;
			this.crossorigin = crossorigin;
			this.defer=defer;
			this.appendToHead = appendToHead;
		}

		public String getSource() {
			return source;
		}

		public boolean hasIntegrity() {
			return integrity != null;
		}

		public String getIntegrity() {
			return integrity;
		}

		public boolean hasCrossorigin() {
			return crossorigin != null;
		}

		public String getCrossorigin() {
			return crossorigin;
		}

		public boolean isDefer() {
			return defer;
		}

		public boolean isAppendToHead() {
			return appendToHead;
		}

		@Override
		public int hashCode() {
			final int prime = 31;
			int result = 1;
			result = prime * result + ((source == null) ? 0 : source.hashCode());
			return result;
		}

		@Override
		public boolean equals(Object obj) {
			if (this == obj)
				return true;
			if (obj == null)
				return false;
			if (getClass() != obj.getClass())
				return false;
			ScriptReference other = (ScriptReference) obj;
			if (source == null) {
				if (other.source != null)
					return false;
			} else if (!source.equals(other.source))
				return false;
			return true;
		}
	}
}
