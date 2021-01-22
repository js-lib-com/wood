package js.wood.impl;

import java.util.ArrayList;
import java.util.List;

import js.dom.Document;
import js.dom.Element;
import js.wood.ILinkReference;
import js.wood.IMetaReference;
import js.wood.IScriptReference;
import js.wood.WoodException;

public abstract class BaseDescriptor {
	/** XML DOM document. */
	protected final Document doc;

	protected BaseDescriptor(Document doc) {
		this.doc = doc;
	}

	/**
	 * Get object display or given default value, if display is missing or not set. This property is loaded from
	 * <code>display</code> element.
	 * 
	 * @param defaultValue default display value.
	 * @return object display or supplied default value.
	 */
	public String getDisplay(String defaultValue) {
		return text("display", defaultValue);
	}

	/**
	 * Get object description or given default value, if description is missing or not set. This property is loaded from
	 * <code>description</code> element.
	 * 
	 * @param defaultValue default description value.
	 * @return object description or supplied default value.
	 */
	public String getDescription(String defaultValue) {
		return text("description", defaultValue);
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
	 * Return text value for element denoted by tag name or default value if element is missing.
	 * 
	 * @param tagName element tag name,
	 * @param defaultValue default value to use when element is missing.
	 * @return element text or default value.
	 */
	protected String text(String tagName, String defaultValue) {
		Element el = doc.getByTag(tagName);
		if (el == null) {
			return defaultValue;
		}
		String value = el.getText();
		return !value.isEmpty() ? value : defaultValue;
	}
}
