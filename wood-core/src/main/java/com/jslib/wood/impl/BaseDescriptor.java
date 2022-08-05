package com.jslib.wood.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

import org.xml.sax.SAXException;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.DocumentBuilder;
import com.jslib.api.dom.Element;
import com.jslib.util.Classes;
import com.jslib.wood.FilePath;
import com.jslib.wood.ILinkDescriptor;
import com.jslib.wood.IMetaDescriptor;
import com.jslib.wood.IScriptDescriptor;
import com.jslib.wood.WoodException;

/**
 * Common logic for both project and component descriptors.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
abstract class BaseDescriptor {
	/** Descriptor document builder. */
	private static DocumentBuilder DOC_BUILDER = Classes.loadService(DocumentBuilder.class);

	/** Empty XML document used when component descriptor file is missing. */
	private static final Document EMPTY_DOC = DOC_BUILDER.createXML("compo");

	/** Descriptor file, for logging purposes. */
	private final String descriptorFile;

	/** Descriptor DOM document. */
	protected final Document doc;

	protected BaseDescriptor(FilePath descriptorFile, Reader documentReader) {
		Document doc = EMPTY_DOC;
		if (documentReader != null) {
			try (Reader reader = documentReader) {
				doc = DOC_BUILDER.loadXML(reader);
			} catch (IOException | SAXException e) {
				throw new WoodException("Fail to load document %s: %s: %s", descriptorFile, e.getClass(), e.getMessage());
			}
		}
		this.descriptorFile = descriptorFile.value();
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
	public List<IMetaDescriptor> getMetaDescriptors() {
		List<IMetaDescriptor> descriptors = new ArrayList<>();
		for (Element element : doc.findByTag("meta")) {
			if (!(element.hasAttr("name") || element.hasAttr("http-equiv") || element.hasAttr("property"))) {
				throw new WoodException("Invalid descriptor file |%s|. Missing 'name', 'http-equiv' or 'property' attribute from <meta> element.", descriptorFile);
			}
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
			if (!element.hasAttr("href")) {
				throw new WoodException("Invalid descriptor file |%s|. Missing 'href' attribute from <link> element.", descriptorFile);
			}
			LinkDescriptor descriptor = LinkDescriptor.create(element);
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
			if (!element.hasAttr("src")) {
				throw new WoodException("Invalid descriptor file |%s|. Missing 'src' attribute from <script> element.", descriptorFile);
			}
			ScriptDescriptor descriptor = ScriptDescriptor.create(element);
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
}
