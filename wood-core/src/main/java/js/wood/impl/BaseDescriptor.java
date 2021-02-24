package js.wood.impl;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.ArrayList;
import java.util.List;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import js.wood.FilePath;
import js.wood.ILinkDescriptor;
import js.wood.IMetaDescriptor;
import js.wood.IScriptDescriptor;
import js.wood.WoodException;

/**
 * Common logic for both component and project descriptors.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
abstract class BaseDescriptor {
	/** Empty XML document used when component descriptor file is missing. */
	private static final Document EMPTY_DOC;
	static {
		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		EMPTY_DOC = builder.createXML("component");
	}

	/** XML DOM document. */
	protected final Document doc;

	private final ILinkDescriptor linkDefaults;
	private final IScriptDescriptor scriptDefaults;

	protected BaseDescriptor(FilePath descriptorFile) {
		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		this.doc = descriptorFile.exists() ? builder.loadXML(descriptorFile.getReader()) : EMPTY_DOC;

		this.linkDefaults = new LinkDefaults(doc);
		this.scriptDefaults = new ScriptDefaults(doc);
	}

	protected BaseDescriptor(File descriptorFile) {
		Document doc;
		try {
			DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
			doc = builder.loadXML(descriptorFile);
		} catch (FileNotFoundException e) {
			doc = EMPTY_DOC;
		}

		this.doc = doc;
		this.linkDefaults = new LinkDefaults(doc);
		this.scriptDefaults = new ScriptDefaults(doc);
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
	public List<IMetaDescriptor> getMetaDescriptors() {
		List<IMetaDescriptor> descriptors = new ArrayList<>();
		for (Element element : doc.findByTag("meta")) {
			MetaDescriptor descriptor = MetaDescriptor.create(element);
			if (descriptors.contains(descriptor)) {
				throw new WoodException("Duplicate meta |%s| in project descriptor.", descriptor);
			}
			descriptors.add(descriptor);
		}
		return descriptors;
	}

	public List<ILinkDescriptor> getLinkDescriptors() {
		List<ILinkDescriptor> descriptors = new ArrayList<>();
		for (Element element : doc.findByTag("link")) {
			LinkDescriptor descriptor = LinkDescriptor.create(element, linkDefaults);
			if (descriptors.contains(descriptor)) {
				throw new WoodException("Duplicate link |%s| in project descriptor.", descriptor);
			}
			descriptors.add(descriptor);
		}
		return descriptors;
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
	 * {@link ScriptDescriptor#appendToHead}.
	 * 
	 * @return scripts declared by this component descriptor.
	 */
	public List<IScriptDescriptor> getScriptDescriptors() {
		List<IScriptDescriptor> descriptors = new ArrayList<>();
		for (Element element : doc.findByTag("script")) {
			ScriptDescriptor descriptor = ScriptDescriptor.create(element, scriptDefaults);
			if (descriptors.contains(descriptor)) {
				throw new WoodException("Duplicate script |%s| in project descriptor.", descriptor);
			}
			descriptors.add(descriptor);
		}
		return descriptors;
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

	private static class LinkDefaults implements ILinkDescriptor {
		private final Document doc;

		public LinkDefaults(Document doc) {
			this.doc = doc;
		}

		@Override
		public String getHref() {
			return text("href");
		}

		@Override
		public String getHreflang() {
			return text("hreflang");
		}

		@Override
		public String getRelationship() {
			return text("rel", "stylesheet");
		}

		@Override
		public String getType() {
			return text("type", "text/css");
		}

		@Override
		public String getMedia() {
			return text("media");
		}

		@Override
		public String getReferrerPolicy() {
			return text("referrerpolicy");
		}

		@Override
		public String getCrossOrigin() {
			return text("crossorigin");
		}

		@Override
		public String getIntegrity() {
			return text("integrity");
		}

		@Override
		public String getDisabled() {
			return text("disabled");
		}

		@Override
		public String getAsType() {
			return text("as");
		}

		@Override
		public String getPrefetch() {
			return text("prefetch");
		}

		@Override
		public String getSizes() {
			return text("sizes");
		}

		@Override
		public String getImageSizes() {
			return text("imagesizes");
		}

		@Override
		public String getImageSrcSet() {
			return text("imagesrcset");
		}

		@Override
		public String getTitle() {
			return text("title");
		}

		private String text(String attribute, String... defaults) {
			Element element = doc.getByTag("link-" + attribute);
			return element != null ? element.getTextContent() : defaults.length == 1 ? defaults[0] : null;
		}
	}

	private static class ScriptDefaults implements IScriptDescriptor {
		private final Document doc;

		public ScriptDefaults(Document doc) {
			this.doc = doc;
		}

		@Override
		public String getSource() {
			return text("src");
		}

		@Override
		public String getType() {
			return text("type", "text/javascript");
		}

		@Override
		public String getAsync() {
			return text("async");
		}

		@Override
		public String getDefer() {
			return text("defer", "true");
		}

		@Override
		public String getNoModule() {
			return text("nomodule");
		}

		@Override
		public String getNonce() {
			return text("nonce");
		}

		@Override
		public String getReferrerPolicy() {
			return text("referrerpolicy");
		}

		@Override
		public String getIntegrity() {
			return text("integrity");
		}

		@Override
		public String getCrossOrigin() {
			return text("crossorigin");
		}

		@Override
		public boolean isEmbedded() {
			return Boolean.parseBoolean(text("embedded"));
		}

		private String text(String attribute, String... defaults) {
			Element element = doc.getByTag("script-" + attribute);
			return element != null ? element.getTextContent() : defaults.length == 1 ? defaults[0] : null;
		}
	}
}
