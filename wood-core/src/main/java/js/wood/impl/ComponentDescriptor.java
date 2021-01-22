package js.wood.impl;

import java.io.FileNotFoundException;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.util.Classes;
import js.wood.FilePath;
import js.wood.IReferenceHandler;
import js.wood.WoodException;

/**
 * Component descriptor contains properties customizable at component level. This is in contrast with {@link ProjectDescriptor}
 * that affects all components from project. Current implementation is actually used for pages; support for ordinary components
 * is expected.
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
 * usable for role based security; it allows to group pages under given path and configure security constrains base on that
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
 * <p>
 * Component descriptor instance has not mutable state, therefore is thread safe.
 * 
 * @author Iulian Rotaru
 * @version final
 * @see ProjectDescriptor
 */
public class ComponentDescriptor extends BaseDescriptor {
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
		super(document(filePath));
		this.filePath = filePath;
		this.referenceHandler = referenceHandler;
		this.resolver = new ReferencesResolver();
	}

	/** Empty XML document used when component descriptor file is missing. */
	private static final Document EMPTY_DOC;
	static {
		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		EMPTY_DOC = builder.createXML("component");
	}

	/**
	 * Create optional component descriptor document. Return empty document if descriptor file is missing.
	 * 
	 * @param descriptorFile component descriptor file, absolute path.
	 * @return component descriptor document, possible empty.
	 */
	private static Document document(FilePath descriptorFile) {
		try {
			DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
			return descriptorFile.exists() ? builder.loadXML(descriptorFile.toFile()) : EMPTY_DOC;
		} catch (FileNotFoundException unused) {
			throw new WoodException("Missing component descriptor file |%s| although it exists.", descriptorFile);
		}
	}

	/**
	 * Get component version or null if missing or not set. This property is loaded from <code>version</code> element. Note that
	 * version property is especially useful for library components.
	 * 
	 * @return component version or null.
	 */
	public String getVersion() {
		return text("version", null);
	}

	/**
	 * Get directory path to store page layout file or supplied default value if path is missing. This property is loaded from
	 * <code>path</code> element.
	 * 
	 * @param defaultValue default path value.
	 * @return page layout directory path or supplied default value.
	 */
	public String getSecurityRole() {
		return text("security-role", null);
	}

	/**
	 * Return text value from element identified by tag name or given default value, if element not found or its text content is
	 * not set. Uses {@link ReferencesResolver} to resolve value references, if any.
	 * 
	 * @param tagName tag name identifying desired element,
	 * @param defaultValue default value, returned when no value.
	 * @return element text value or given default value.
	 */
	protected String text(String tagName, String defaultValue) {
		String value = super.text(tagName, null);
		return value != null ? resolver.parse(value, filePath, referenceHandler) : defaultValue;
	}
}
