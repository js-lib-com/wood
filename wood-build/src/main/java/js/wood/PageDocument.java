package js.wood;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.lang.Handler;
import js.util.Classes;
import js.util.Params;

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
	/** Page component from which this page document loads its content. */
	private final Component component;

	/** X(HT)ML document. */
	private final Document doc;

	/** HTML root element. */
	private final Element html;

	/** HTML head element. */
	private final Element head;

	/**
	 * Create X(HT)ML document instance, add head element and copy component layout as HTML body.
	 * 
	 * @param component component instance containing body layout.
	 */
	public PageDocument(Component component) {
		Params.notNull(component, "Component");
		this.component = component;

		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
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
		Params.notNullOrEmpty(language, "Language");
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
		Params.notNullOrEmpty(contentType, "Content type");
		head.addChild(doc.createElement("meta", "http-equiv", "Content-Type", "content", contentType));
		head.addText("\r\n");
	}

	/**
	 * Set page author. Add meta element to page head, with name <code>Author</code> and provided author value as content.
	 * 
	 * @param author page author, null ignored.
	 */
	public void setAuthor(String author) {
		if (author != null) {
			head.addChild(doc.createElement("meta", "name", "Author", "content", author));
			head.addText("\r\n");
		}
	}

	/**
	 * Set page header title. Add <code>title</code> element to this page header.
	 * 
	 * @param title page title, null not accepted.
	 * @throws IllegalArgumentException if title parameter is null or empty.
	 */
	public void setTitle(String title) {
		Params.notNullOrEmpty(title, "Title");
		head.addChild(doc.createElement("title").setText(title));
		head.addText("\r\n");
	}

	/**
	 * Add page description meta element. Create meta element and append it to page head, with name <code>Description</code> and
	 * provided description value as content.
	 * 
	 * @param description page description, null not accepted.
	 * @throws IllegalArgumentException if description parameter is null or empty.
	 */
	public void setDescription(String description) {
		Params.notNullOrEmpty(description, "Description");
		head.addChild(doc.createElement("meta", "name", "Description", "content", description));
		head.addText("\r\n");
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
		Params.notNullOrEmpty(path, "Favicon path");
		head.addChild(doc.createElement("link", "href", path, "rel", "shortcut icon", "type", "image/x-icon"));
		head.addText("\r\n");
	}

	public void addManifest(String path) {
		Params.notNullOrEmpty(path, "Manifest path");
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
		Params.notNull(meta, "Meta reference");

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
	 * Add link element to this page head. Create <code>link</code> element and set all attributes provided by link reference
	 * parameter. There is no validation on link reference; attributes are set exactly as provided.
	 * <p>
	 * This setter is not used only for external style sheets; all standard links attributes are supported. See
	 * {@link ILinkDescriptor} for a list of supported attributes.
	 * 
	 * @param link non null link reference.
	 * @throws IllegalArgumentException if link reference parameter is null or <code>href</code> attribute is null or empty.
	 */
	public void addLink(ILinkDescriptor link) {
		Params.notNull(link, "Link reference");
		final String href = link.getHref();
		Params.notNullOrEmpty(href, "Link HREF");

		Element linkElement = doc.createElement("link", "href", href);

		setAttr(linkElement, "hreflang", link.getHreflang());
		setAttr(linkElement, "rel", link.getRelationship(), "stylesheet");
		setAttr(linkElement, "type", link.getType(), "text/css");
		setAttr(linkElement, "media", link.getMedia());
		setAttr(linkElement, "referrerpolicy", link.getReferrerPolicy());
		setAttr(linkElement, "crossorigin", link.getCrossOrigin());
		setAttr(linkElement, "integrity", link.getIntegrity());
		setAttr(linkElement, "disabled", link.getDisabled());
		setAttr(linkElement, "as", link.getAsType());
		setAttr(linkElement, "sizes", link.getSizes());
		setAttr(linkElement, "imagesizes", link.getImageSizes());
		setAttr(linkElement, "imagesrcset", link.getImageSrcSet());
		setAttr(linkElement, "title", link.getTitle());

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
		Params.notNullOrEmpty(href, "Style HREF");
		head.addChild(doc.createElement("link", "href", href, "rel", "stylesheet", "type", "text/css"));
		head.addText("\r\n");
	}

	private final List<String> processedScripts = new ArrayList<>();

	/**
	 * Add script element to this page head. Create <code>script</code> element and set attributes provided by script reference
	 * parameter. There is no validation on link reference; attributes are set exactly as provided. See
	 * {@link IScriptDescriptor} for a list of supported attributes.
	 * <p>
	 * If {@link IScriptDescriptor#getSource()} is a file path relative this project, as accepted by
	 * {@link FilePath#accept(String)}, invoke {@link Handler#handle(Object)} with {@link FilePath} created from script source
	 * attribute. Handler returned value is used to replace current script source.
	 * <p>
	 * If script is embedded, see {@link IScriptDescriptor#isEmbedded()}, load script as text content to created script element,
	 * using {@link BuilderProject#loadFile(String)}. In this case {@link IScriptDescriptor#getSource()} is project relative
	 * path of the script file from where text content is loaded.
	 * 
	 * @param script script reference,
	 * @param handler file handler.
	 * @throws IllegalArgumentException if script or handler parameter is null or script source is missing.
	 * @throws IOException if script is embedded and script content loading fails.
	 */
	public void addScript(IScriptDescriptor script, Handler<String, FilePath> handler) throws IOException {
		Params.notNull(script, "Script reference");
		Params.notNull(script.getSource(), "The source of script");
		Params.notNull(handler, "File handler");

		if (processedScripts.contains(script.getSource())) {
			return;
		}
		processedScripts.add(script.getSource());

		final BuilderProject project = (BuilderProject) component.getProject();
		String src = script.getSource();
		if (!script.isEmbedded()) {
			if (FilePath.accept(src)) {
				src = handler.handle(project.createFilePath(src));
			}
			// dynamic scripts are not declared on page head; they are loaded by custom script loaders, e.g. ServiceLoader
			if (script.isDynamic()) {
				return;
			}
		}

		Element scriptElement = doc.createElement("script");
		if (!script.isEmbedded()) {
			scriptElement.setAttr("src", src);
		}

		setAttr(scriptElement, "type", script.getType(), "text/javascript");
		setAttr(scriptElement, "async", script.getAsync());
		if (!script.isEmbedded()) {
			setAttr(scriptElement, "defer", script.getDefer());
		}
		setAttr(scriptElement, "nomodule", script.getNoModule());
		setAttr(scriptElement, "nonce", script.getNonce());
		setAttr(scriptElement, "referrerpolicy", script.getReferrerPolicy());
		setAttr(scriptElement, "crossorigin", script.getCrossOrigin());
		setAttr(scriptElement, "integrity", script.getIntegrity());

		if (script.isEmbedded()) {
			scriptElement.setText(project.loadFile(src));
		}

		head.addChild(scriptElement);
		head.addText("\r\n");
	}

	/**
	 * Add named attribute to DOM element with given value. Attribute value can be null in which case optional default value is
	 * used. If value parameter is null and default value is not provided this method does nothing.
	 * 
	 * @param element target DOM element,
	 * @param name attribute name,
	 * @param value attribute value, possible null,
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
