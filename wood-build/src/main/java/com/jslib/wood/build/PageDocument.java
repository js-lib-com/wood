package com.jslib.wood.build;

import com.jslib.wood.*;
import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.DocumentBuilder;
import com.jslib.wood.dom.Element;
import com.jslib.wood.util.StringsUtil;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * HTML page document created by build process and serialized to build directory. This class just supplies specialized setters
 * for page DOM document. Is the caller responsibility to ensure proper order when invoke setters.
 * <p>
 * Page document class has no mutable state and is thread safe.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
class PageDocument {
    /**
     * Page component from which this page document loads its content.
     */
    private final Component component;

    /**
     * X(HT)ML document.
     */
    private final Document doc;

    /**
     * HTML root element.
     */
    private final Element html;

    /**
     * HTML head element.
     */
    private final Element head;

    /**
     * Create X(HT)ML document instance, add head element and copy component layout as HTML body.
     *
     * @param component component instance containing body layout.
     */
    public PageDocument(Component component) {
        assert component != null : "Component argument is null";
        this.component = component;

        DocumentBuilder builder = DocumentBuilder.getInstance();
        this.doc = builder.createHTML();

        this.html = this.doc.getRoot();
        this.html.addText("\r\n");

        this.head = this.doc.createElement("head");
        this.html.addChild(this.head);
        this.html.addText("\r\n");
        this.head.addText("\r\n");

        this.html.addChild(this.doc.importElement(component.getLayout()));
        this.html.addText("\r\n");
    }

    /**
     * Return this page document.
     *
     * @return this page document.
     * @see #doc
     */
    public Document getDocument() {
        return doc;
    }

    /**
     * Set <code>lang</code> attribute value to HTML root element. Given language should be ISO 639-1 code; it is caller
     * responsibility to provide the correct value.
     *
     * @param language non null ISO 639-1 language code.
     * @throws IllegalArgumentException if language parameter is null or empty.
     */
    public void setLanguage(String language) {
        assert language != null && !language.isEmpty() : "Language argument is null or empty";
        html.setAttr("lang", language);
    }

    /**
     * Set document content type. This tool library always uses <code>text/html; charset=UTF-8</code> but this policy is
     * enforced by {@link Builder}.
     *
     * @param contentType non null content type value.
     * @throws IllegalArgumentException if content type parameter is null or empty.
     */
    public void setContentType(String contentType) {
        assert contentType != null && !contentType.isEmpty() : "Content type argument is null or empty";
        head.addChild(doc.createElement("meta", "http-equiv", "Content-Type", "content", contentType));
        head.addText("\r\n");
    }

    /**
     * Set page authors. Add meta element to page head, with name <code>Author</code> and provided authors, comma joined, as
     * content. If given authors list is empty this method does nothing
     *
     * @param authors list of authors, possible empty.
     */
    public void setAuthors(List<String> authors) {
        switch (authors.size()) {
            case 0:
                return;

            case 1:
                head.addChild(doc.createElement("meta", "name", "Author", "content", authors.get(0)));
                break;

            default:
                String content = "co-authored by " + StringsUtil.join(authors, ", ");
                head.addChild(doc.createElement("meta", "name", "Author", "content", content));
                break;
        }
        head.addText("\r\n");
    }

    /**
     * Set page header title. Add <code>title</code> element to this page header.
     *
     * @param title page title, null not accepted.
     * @throws IllegalArgumentException if title parameter is null or empty.
     */
    public void setTitle(String title) {
        assert title != null && !title.isEmpty() : "Title argument is null or empty";
        head.addChild(doc.createElement("title").setText(title));
        head.addText("\r\n");
    }

    /**
     * Add page description meta element. Create meta element and append it to page head, with name <code>Description</code> and
     * provided description value as content.
     * <p>
     * If description argument is null page meta description is not set.
     *
     * @param description page description, null ignored.
     */
    public void setDescription(String description) {
        if (description != null) {
            head.addChild(doc.createElement("meta", "name", "Description", "content", description));
            head.addText("\r\n");
        }
    }

    /**
     * Add link for page favorite icon. Append link element to page head, with <code>rel</code> attribute set to
     * <code>shortcut icon</code> and <code>type</code> attribute set to <code>image/x-icon</code>. Provided favicon path is set
     * to <code>href</code> attribute.
     *
     * @param path path to favorite icon.
     * @throws IllegalArgumentException if favicon path parameter is null or empty.
     */
    public void addFavicon(String path) {
        assert path != null && !path.isEmpty() : "Favicon path argument is null or empty";
        head.addChild(doc.createElement("link", "href", path, "rel", "shortcut icon", "type", "image/x-icon"));
        head.addText("\r\n");
    }

    public void addPwaManifest(String path) {
        assert path != null && !path.isEmpty() : "Manifest path argument is null or empty";
        head.addChild(doc.createElement("link", "href", path, "rel", "manifest"));
        head.addText("\r\n");
    }

    /**
     * Add meta element to this page head. Create <code>meta</code> element and set all attributes provided by meta reference
     * parameter. There is no validation on meta reference; attributes are set exactly as provided.
     *
     * @param meta non null meta reference.
     * @throws IllegalArgumentException if meta reference parameter is null or empty.
     */
    public void addMeta(IMetaDescriptor meta) {
        assert meta != null : "Meta reference argument is null";

        Element metaElement = doc.createElement("meta");
        setAttr(metaElement, "name", meta.getName());
        setAttr(metaElement, "http-equiv", meta.getHttpEquiv());
        setAttr(metaElement, "property", meta.getProperty());
        setAttr(metaElement, "content", meta.getContent());
        setAttr(metaElement, "charset", meta.getCharset());

        head.addChild(metaElement);
        head.addText("\r\n");
    }

    /**
     * Add link element to this page head. Create <code>link</code> element and set all attributes provided by link descriptor
     * parameter. There is no validation on link reference; attributes are set exactly as provided.
     * <p>
     * This setter is not used only for external style sheets; all standard links attributes are supported. See
     * {@link ILinkDescriptor} for a list of supported attributes.
     *
     * @param linkDescriptor link descriptor.
     * @throws IllegalArgumentException if link reference parameter is null or <code>href</code> attribute is null or empty.
     */
    public void addLink(ILinkDescriptor linkDescriptor, Function<FilePath, String> fileHandler) {
        assert linkDescriptor != null : "Link descriptor argument is null";
        assert linkDescriptor.getHref() != null && !linkDescriptor.getHref().isEmpty() : "Link description HREF is null or empty";
        assert fileHandler != null : "File handler argument";

        String href = linkDescriptor.getHref();
        if (linkDescriptor.isStyleSheet() && FilePath.accept(href)) {
            final BuilderProject project = (BuilderProject) component.getProject();
            href = fileHandler.apply(project.createFilePath(href));
        }

        Element linkElement = doc.createElement("link", "href", href);

        setAttr(linkElement, "hreflang", linkDescriptor.getHreflang());
        setAttr(linkElement, "rel", linkDescriptor.getRelationship(), "stylesheet");
        setAttr(linkElement, "type", linkDescriptor.getType(), "text/css");
        setAttr(linkElement, "media", linkDescriptor.getMedia());
        setAttr(linkElement, "referrerpolicy", linkDescriptor.getReferrerPolicy());
        setAttr(linkElement, "crossorigin", linkDescriptor.getCrossOrigin());
        setAttr(linkElement, "integrity", linkDescriptor.getIntegrity());
        setAttr(linkElement, "disabled", linkDescriptor.getDisabled());
        setAttr(linkElement, "as", linkDescriptor.getAsType());
        setAttr(linkElement, "sizes", linkDescriptor.getSizes());
        setAttr(linkElement, "imagesizes", linkDescriptor.getImageSizes());
        setAttr(linkElement, "imagesrcset", linkDescriptor.getImageSrcSet());
        setAttr(linkElement, "title", linkDescriptor.getTitle());

        head.addChild(linkElement);
        head.addText("\r\n");
    }

    /**
     * Append style link element to this page head. Create <code>link</code> element with <code>rel</code> attribute set to
     * <code>stylesheet</code> and <code>type</code> attribute set to <code>text/css</code>. Provided style file path is set to
     * <code>href</code> attribute.
     *
     * @param href style file URL path.
     * @throws IllegalArgumentException if hyper-reference parameter is null or empty.
     */
    public void addStyle(String href) {
        assert href != null && !href.isEmpty() : "Style HREF argument is null or empty";
        head.addChild(doc.createElement("link", "href", href, "rel", "stylesheet", "type", "text/css"));
        head.addText("\r\n");
    }

    private final List<String> processedScripts = new ArrayList<>();

    /**
     * Add script element to this page head. Create <code>script</code> element and set attributes provided by script descriptor
     * parameter. There is no validation on script descriptor; attributes are set exactly as provided. See
     * {@link IScriptDescriptor} for a list of supported attributes.
     * <p>
     * If {@link IScriptDescriptor#getSource()} is a file path relative this project, as accepted by
     * {@link FilePath#accept(String)}, invoke {@link Function#apply(Object)} with {@link FilePath} created from script source
     * attribute. File handler returned value is used to replace current script source.
     * <p>
     * If script is embedded, see {@link IScriptDescriptor#isEmbedded()}, load script as text content to created script element,
     * using {@link BuilderProject#loadFile(String)}. In this case {@link IScriptDescriptor#getSource()} is project relative
     * path of the script file from where text content is loaded.
     *
     * @param scriptDescriptor script descriptor,
     * @param fileHandler      file handler, used only if script source is a local file.
     * @throws IllegalArgumentException if script or handler parameter is null or script source is missing.
     * @throws IOException              if script is embedded and script content loading fails.
     */
    public void addScript(IScriptDescriptor scriptDescriptor, Function<FilePath, String> fileHandler) throws IOException {
        assert scriptDescriptor != null : "Script descriptor argument is null";
        assert scriptDescriptor.getSource() != null && !scriptDescriptor.getSource().isEmpty() : "Script source argument is null or empty";
        assert fileHandler != null : "File handler argument is null";

        if (processedScripts.contains(scriptDescriptor.getSource())) {
            return;
        }
        processedScripts.add(scriptDescriptor.getSource());

        final BuilderProject project = (BuilderProject) component.getProject();
        String src = scriptDescriptor.getSource();
        if (!scriptDescriptor.isEmbedded()) {
            if (FilePath.accept(src)) {
                src = fileHandler.apply(project.createFilePath(src));
            }
            // dynamic scripts are not declared on page head; they are loaded by custom script loaders, e.g. ServiceLoader
            if (scriptDescriptor.isDynamic()) {
                return;
            }
        }

        Element scriptElement = doc.createElement("script");
        if (!scriptDescriptor.isEmbedded()) {
            scriptElement.setAttr("src", src);
        }

        setAttr(scriptElement, "type", scriptDescriptor.getType(), "text/javascript");
        setAttr(scriptElement, "async", scriptDescriptor.getAsync());
        if (!scriptDescriptor.isEmbedded()) {
            setAttr(scriptElement, "defer", scriptDescriptor.getDefer());
        }
        setAttr(scriptElement, "nomodule", scriptDescriptor.getNoModule());
        setAttr(scriptElement, "nonce", scriptDescriptor.getNonce());
        setAttr(scriptElement, "referrerpolicy", scriptDescriptor.getReferrerPolicy());
        setAttr(scriptElement, "crossorigin", scriptDescriptor.getCrossOrigin());
        setAttr(scriptElement, "integrity", scriptDescriptor.getIntegrity());

        if (scriptDescriptor.isEmbedded()) {
            scriptElement.setText(project.loadFile(src));
        }

        head.addChild(scriptElement);
        head.addText("\r\n");
    }

    /**
     * Add script to page document.
     *
     * @param scriptDescriptor script descriptor,
     * @param src              script source text,
     * @param text             optional script code, null only if script is embedded.
     */
    public void addScript(IScriptDescriptor scriptDescriptor, String src, String text) {
        assert scriptDescriptor != null : "Script descriptor argument is null";
        assert scriptDescriptor.getSource() != null && !scriptDescriptor.getSource().isEmpty() : "Script source argument is null or empty";
        assert src != null && !src.isEmpty() : "Script source argument is null or empty";

        if (processedScripts.contains(scriptDescriptor.getSource())) {
            return;
        }
        processedScripts.add(scriptDescriptor.getSource());

        // dynamic scripts are not declared on page head; they are loaded by custom script loaders, e.g. ServiceLoader
        if (scriptDescriptor.isDynamic()) {
            return;
        }

        Element scriptElement = doc.createElement("script");
        if (!scriptDescriptor.isEmbedded()) {
            scriptElement.setAttr("src", src);
        }

        setAttr(scriptElement, "type", scriptDescriptor.getType(), "text/javascript");
        setAttr(scriptElement, "async", scriptDescriptor.getAsync());
        if (!scriptDescriptor.isEmbedded()) {
            setAttr(scriptElement, "defer", scriptDescriptor.getDefer());
        }
        setAttr(scriptElement, "nomodule", scriptDescriptor.getNoModule());
        setAttr(scriptElement, "nonce", scriptDescriptor.getNonce());
        setAttr(scriptElement, "referrerpolicy", scriptDescriptor.getReferrerPolicy());
        setAttr(scriptElement, "crossorigin", scriptDescriptor.getCrossOrigin());
        setAttr(scriptElement, "integrity", scriptDescriptor.getIntegrity());

        if (scriptDescriptor.isEmbedded()) {
            assert text != null;
            scriptElement.setText(text);
        }

        head.addChild(scriptElement);
        head.addText("\r\n");
    }

    /**
     * Add named attribute to DOM element with given value. Attribute value can be null in which case optional default value is
     * used. If value parameter is null and default value is not provided this method does nothing.
     *
     * @param element      target DOM element,
     * @param name         attribute name,
     * @param value        attribute value, possible null,
     * @param defaultValue optional default value, used when <code>value</code> is null.
     */
    private static void setAttr(Element element, String name, String value, String... defaultValue) {
        if (value == null && defaultValue.length == 1) {
            value = defaultValue[0];
        }
        if (value != null) {
            element.setAttr(name, value);
        }
    }
}
