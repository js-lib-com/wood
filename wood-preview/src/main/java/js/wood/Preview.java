package js.wood;

import java.io.IOException;
import java.io.Writer;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;

/**
 * Component preview wraps component in standard HTML document and serialize it to given writer. This component preview class is
 * companion for {@link PreviewServlet}; it hides details regarding component layout dynamic generation.
 * <p>
 * When create preview instance a fully aggregated component instance is supplied. This component instance already have
 * variables injected and media file path resolved. Component layout supplies the body for generated preview document. Header is
 * inserted by {@link #serialize(Writer)} method logic. Note that for style and script links preview always uses absolute URL
 * path.
 * 
 * @author Iulian Rotaru
 */
public final class Preview {
	private final String contextPath;

	/** Project reference. */
	private final PreviewProject project;

	/** Wrapped component. */
	private final Component compo;

	/**
	 * Create component preview instance.
	 * 
	 * @param compo component.
	 */
	public Preview(String contextPath, PreviewProject project, Component compo) {
		this.contextPath = contextPath;
		this.project = project;
		this.compo = compo;
	}

	/**
	 * Create HTML document wrapping this instance component, insert header meta elements, style and script links and serialize
	 * to given writer.
	 * 
	 * @param writer character writer.
	 * @throws IOException if document serialization fails.
	 */
	public void serialize(Writer writer) throws IOException {
		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		Document doc = builder.createHTML();

		Element html = doc.getRoot();
		html.setAttr("lang", project.getDefaultLocale().toLanguageTag());

		Element head = doc.createElement("head");
		Element body = doc.createElement("body");
		html.addChild(head).addChild(body);
		head.addText("\r\n");

		head.addChild(doc.createElement("meta", "http-equiv", "Content-Type", "content", "text/html; charset=UTF-8"));
		head.addText("\r\n");
		head.addChild(doc.createElement("title").setText(compo.getDisplay()));
		head.addText("\r\n");
		if (project.getAuthor() != null) {
			head.addChild(doc.createElement("meta", "name", "Author", "content", project.getAuthor()));
			head.addText("\r\n");
		}
		head.addChild(doc.createElement("meta", "name", "Description", "content", compo.getDescription()));
		head.addText("\r\n");

		for (IMetaDescriptor meta : project.getMetaDescriptors()) {
			addMeta(doc, meta);
		}
		for (IMetaDescriptor meta : compo.getMetaDescriptors()) {
			addMeta(doc, meta);
		}

		Element layout = compo.getLayout();
		if (layout.getTag().equals("body")) {
			body.replace(layout);
		} else {
			body.addChild(layout);
		}

		// links order:
		// 1. external links defined by project
		// 2. external links defined by page
		// 3. reset.css
		// 4. fx.css
		// 5. theme styles - theme styles are in no particular order since they are independent of each other
		// 6. component styles - first use template and child component styles then parent component

		for (ILinkDescriptor link : project.getLinkDescriptors()) {
			addLink(doc, link);
		}
		for (ILinkDescriptor link : compo.getLinkDescriptors()) {
			addLink(doc, link);
		}

		ThemeStyles themeStyles = project.getThemeStyles();
		if (themeStyles.reset != null) {
			addStyle(doc, absoluteUrlPath(themeStyles.reset));
		}
		if (themeStyles.fx != null) {
			addStyle(doc, absoluteUrlPath(themeStyles.fx));
		}
		for (FilePath style : themeStyles.styles) {
			addStyle(doc, absoluteUrlPath(style));
		}

		for (FilePath style : compo.getStyleFiles()) {
			addStyle(doc, absoluteUrlPath(style));
		}

		for (IScriptDescriptor script : project.getScriptDescriptors()) {
			addScript(doc, script);
		}
		for (IScriptDescriptor script : compo.getScriptDescriptors()) {
			addScript(doc, script);
		}

		IScriptDescriptor previewScript = compo.getScriptDescriptor(CT.PREVIEW_SCRIPT);
		if (previewScript != null) {
			addScript(doc, previewScript);
		}

		doc.serialize(writer, true);
	}

	private static void addMeta(Document doc, IMetaDescriptor meta) {
		Element head = doc.getByTag("head");

		final String name = meta.getName();
		final String httpEquiv = meta.getHttpEquiv();
		final String property = meta.getProperty();

		Element metaElement = null;
		if (name != null) {
			metaElement = head.getByAttr("name", name);
		}
		if (httpEquiv != null) {
			metaElement = head.getByAttr("http-equiv", httpEquiv);
		}
		if (property != null) {
			metaElement = head.getByAttr("property", property);
		}
		if (metaElement == null) {
			metaElement = doc.createElement("meta");
		}

		setAttr(metaElement, "name", name);
		setAttr(metaElement, "http-equiv", httpEquiv);
		setAttr(metaElement, "property", property);
		setAttr(metaElement, "content", meta.getContent());
		setAttr(metaElement, "charset", meta.getCharset());

		head.addChild(metaElement);
		head.addText("\r\n");

	}

	private static void addLink(Document doc, ILinkDescriptor link) {
		Element head = doc.getByTag("head");

		Element linkElement = doc.createElement("link");
		linkElement.setAttr("href", link.getHref());

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

	private static void setAttr(Element element, String name, String value, String... defaultValue) {
		if (value == null && defaultValue.length == 1) {
			value = defaultValue[0];
		}
		if (value != null) {
			element.setAttr(name, value);
		}
	}

	/**
	 * Add style link element to HTML document head.
	 * 
	 * @param doc HTML document,
	 * @param href style sheet hyper-reference.
	 */
	private static void addStyle(Document doc, String href) {
		Element head = doc.getByTag("head");
		head.addChild(doc.createElement("link", "href", href, "rel", "stylesheet", "type", "text/css"));
		head.addText("\r\n");
	}

	/**
	 * Add script element to HTML document head.
	 * 
	 * @param doc HTML document,
	 * @param src the source of script.
	 * @throws IOException
	 */
	private void addScript(Document doc, IScriptDescriptor script) throws IOException {
		String src = script.getSource();
		assert src != null;
		Element head = doc.getByTag("head");
		Element scriptElement = head.getByAttr("src", src);
		if (scriptElement == null) {
			scriptElement = doc.createElement("script");
		}
		if (!script.isEmbedded()) {
			if (FilePath.accept(src)) {
				src = absoluteUrlPath(new FilePath(project, src));
			}
			scriptElement.setAttr("src", src);
		}

		setAttr(scriptElement, "type", script.getType(), "text/javascript");
		setAttr(scriptElement, "async", script.getAsync());
		setAttr(scriptElement, "defer", script.getDefer());
		setAttr(scriptElement, "nomodule", script.getNoModule());
		setAttr(scriptElement, "nonce", script.getNonce());
		setAttr(scriptElement, "referrerpolicy", script.getReferrerPolicy());
		setAttr(scriptElement, "crossorigin", script.getCrossOrigin());
		setAttr(scriptElement, "integrity", script.getIntegrity());

		if (script.isEmbedded()) {
			scriptElement.setText(new FilePath(project, script.getSource()).load());
		}

		head.addChild(scriptElement);
		head.addText("\r\n");
	}

	/**
	 * Build absolute URL path for given file path. Returned path contains project context but not protocol or host name.
	 * 
	 * @param filePath file path.
	 * @return file absolute URL path.
	 */
	private String absoluteUrlPath(FilePath filePath) {
		return absoluteUrlPath(filePath.value());
	}

	/**
	 * Build absolute URL path for given file path value. Returned path contains project context but not protocol or host name.
	 * 
	 * @param filePath file path value.
	 * @return file absolute URL path.
	 */
	private String absoluteUrlPath(String filePath) {
		StringBuilder builder = new StringBuilder();
		builder.append(contextPath);
		builder.append(Path.SEPARATOR_CHAR);
		builder.append(filePath);
		return builder.toString();
	}
}
