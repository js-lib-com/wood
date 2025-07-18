package com.jslib.wood.impl;

import com.jslib.wood.*;
import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.DocumentBuilder;
import com.jslib.wood.dom.Element;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Common logic for both project and component descriptors.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
public abstract class BaseDescriptor {
    /**
     * Descriptor document builder.
     */
    private static final DocumentBuilder DOC_BUILDER = DocumentBuilder.getInstance();

    /**
     * Empty XML document used when component descriptor file is missing.
     */
    private static final Document EMPTY_DOC = DOC_BUILDER.createXML("compo");

    protected final Project project;
    protected final FilePath descriptorFile;

    /**
     * Descriptor DOM document.
     */
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
        this.project = descriptorFile.getProject();
        this.descriptorFile = descriptorFile;
        this.doc = doc;
    }

    /**
     * Get object title or null, if title is missing or not set. This property is loaded from <code>title</code> element.
     *
     * @return object title, possible null.
     */
    public String getTitle() {
        return text("title");
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
                throw new WoodException("Invalid descriptor file %s; missing 'name', 'http-equiv' or 'property' attribute from <meta> element", descriptorFile);
            }
            MetaDescriptor descriptor = MetaDescriptor.create(element);
            if (descriptors.contains(descriptor)) {
                throw new WoodException("Duplicate meta %s in project descriptor", descriptor);
            }
            descriptors.add(descriptor);
        }
        return descriptors;
    }

    public List<ILinkDescriptor> getLinkDescriptors() {
        List<ILinkDescriptor> descriptors = new ArrayList<>();
        for (Element element : doc.findByTag("link")) {
            String href = element.getAttr("href");
            if (href == null) {
                throw new WoodException("Invalid descriptor file %s; missing 'href' attribute from <link> element", descriptorFile);
            }
            if (FilePath.accept(href)) { // only local script
                FilePath scriptFile = project.createFilePath(href);
                if (!scriptFile.exists()) {
                    throw new WoodException("Missing link file %s declared by descriptor %s", href, descriptorFile);
                }
            }
            LinkDescriptor descriptor = LinkDescriptor.create(element);
            if (descriptors.contains(descriptor)) {
                throw new WoodException("Duplicate link %s in project descriptor", descriptor);
            }
            descriptors.add(descriptor);
        }
        return descriptors;
    }

    @SuppressWarnings("all")
    /**
     * Get scripts defined by this component descriptor, both third party and local scripts. Returns a list of absolute URLs
     * and/or relative paths in the order and in format defined into descriptor. There is no attempt to check path validity; it
     * is developer responsibility to ensure URLs and paths are correct and inclusion order is proper.
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
     *
     * @return scripts declared by this component descriptor.
     */
    public List<IScriptDescriptor> getScriptDescriptors() {
        List<IScriptDescriptor> descriptors = new ArrayList<>();
        for (Element element : doc.findByTag("script")) {
            String src = element.getAttr("src");
            if (src == null) {
                throw new WoodException("Invalid descriptor file %s; missing 'src' attribute from <script> element", descriptorFile);
            }
            if (FilePath.accept(src)) { // only local script
                FilePath scriptFile = project.createFilePath(src);
                if (!scriptFile.exists()) {
                    throw new WoodException("Missing script file %s declared by descriptor %s", src, descriptorFile);
                }
            }
            ScriptDescriptor descriptor = ScriptDescriptor.create(element);
            if (descriptors.contains(descriptor)) {
                throw new WoodException("Duplicate script %s in project descriptor", descriptor);
            }
            descriptors.add(descriptor);
        }
        return descriptors;
    }

    /**
     * Return text value for element denoted by tag name or default value if element is missing.
     *
     * @param tagName      element tag name,
     * @param defaultValue default value to use when element is missing.
     * @return element text or default value.
     */
    protected String text(String tagName, String defaultValue) {
        Element el = doc.getByTag(tagName);
        if (el == null) {
            return defaultValue;
        }
        String value = el.getText();
        return value.isEmpty() ? defaultValue : value;
    }

    protected String text(String tagName) {
        Element el = doc.getByTag(tagName);
        if (el == null) {
            return null;
        }
        String value = el.getTextContent();
        return value.isEmpty() ? null : value;
    }
}
