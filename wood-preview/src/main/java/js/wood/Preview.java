package js.wood;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import js.util.Strings;

/**
 * Preview wraps a component in standard HTML document and serialize it to given writer. This component preview class is
 * companion for {@link PreviewServlet}; it hides details regarding component layout dynamic generation.
 * <p>
 * When create preview instance a fully aggregated component instance is supplied. This component instance already have
 * variables injected and media file references resolved. Component layout supplies the body for generated preview document.
 * Header is inserted by {@link #serialize(Writer)} method logic. Note that for style and script links preview always uses URL
 * absolute path.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class Preview {
	/** Project reference. */
	private final Project project;

	/** Wrapped component. */
	private final Component compo;

	/**
	 * Runtime context path used to create links for page resource files - preview always uses URL absolute path in which this
	 * application context is included.
	 */
	private final String contextPath;

	/** Enable control script injection only if events stream servlet is declared on preview web.xml */
	private final boolean controlScript;

	/**
	 * Create component preview instance.
	 * 
	 * @param project WOOD project context,
	 * @param compo component,
	 * @param contextPath runtime context path,
	 * @param controlScript enable control script injection.
	 */
	public Preview(Project project, Component compo, String contextPath, boolean controlScript) {
		this.project = project;
		this.compo = compo;
		this.contextPath = contextPath;
		this.controlScript = controlScript;
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
		if (compo.getDisplay() != null) {
			head.addChild(doc.createElement("title").setText(compo.getDisplay()));
			head.addText("\r\n");
		}
		if (project.getAuthor() != null) {
			head.addChild(doc.createElement("meta", "name", "Author", "content", project.getAuthor()));
			head.addText("\r\n");
		}
		if (compo.getDescription() != null) {
			head.addChild(doc.createElement("meta", "name", "Description", "content", compo.getDescription()));
			head.addText("\r\n");
		}

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
		if (themeStyles.getReset() != null) {
			addStyle(doc, urlAbsolutePath(themeStyles.getReset()));
		}
		if (themeStyles.getFx() != null) {
			addStyle(doc, urlAbsolutePath(themeStyles.getFx()));
		}
		for (FilePath style : themeStyles.getStyles()) {
			addStyle(doc, urlAbsolutePath(style));
		}

		for (FilePath style : compo.getStyleFiles()) {
			addStyle(doc, urlAbsolutePath(style));
		}

		if (controlScript) {
			addControlScript(doc);
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

	/**
	 * Add meta element to page head.
	 * 
	 * @param doc page document,
	 * @param meta meta element descriptor.
	 */
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

	/**
	 * Add link element to page head.
	 * 
	 * @param doc page document,
	 * @param link link element descriptor.
	 */
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

	/**
	 * Set element attribute value, with alternative default if provided value is null. If value argument is null and no default
	 * is provided this method does nothing.
	 * 
	 * @param element page document element,
	 * @param name attribute name,
	 * @param value attribute value, possible null,
	 * @param defaultValue optional default value, used when provided attribute value is null.
	 */
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
	 * Control script is embedded into generated component preview and perform tool related tasks like page auto-reload.
	 * 
	 * @param doc HTML document.
	 * @throws IOException if script source reading fails.
	 */
	private void addControlScript(Document doc) throws IOException {
		Element scriptElement = doc.createElement("script", "type", "text/javascript");
		scriptElement.setText(Strings.load(Classes.getResourceAsReader("wood.js")));

		Element head = doc.getByTag("head");
		head.addChild(scriptElement);
		head.addText("\r\n");
	}

	private final List<String> processedScripts = new ArrayList<>();

	/**
	 * Add script element to HTML document head. If script is embedded its source file is loaded into script element text
	 * content.
	 * 
	 * @param doc HTML document,
	 * @param src the source of script.
	 * @throws IOException if script source loading fails.
	 */
	private void addScript(Document doc, IScriptDescriptor script) throws IOException {
		if (processedScripts.contains(script.getSource())) {
			return;
		}
		processedScripts.add(script.getSource());

		for (IScriptDescriptor dependency : project.getScriptDependencies(script.getSource())) {
			addScript(doc, dependency);
		}

		String src = script.getSource();
		assert src != null;
		Element head = doc.getByTag("head");
		Element scriptElement = head.getByAttr("src", src);
		if (scriptElement == null) {
			scriptElement = doc.createElement("script");
		}
		if (!script.isEmbedded()) {
			if (FilePath.accept(src)) {
				src = urlAbsolutePath(new FilePath(project, src));
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
	private String urlAbsolutePath(FilePath filePath) {
		StringBuilder builder = new StringBuilder();
		builder.append(contextPath);
		builder.append(FilePath.SEPARATOR_CHAR);
		builder.append(filePath.value());
		return builder.toString();
	}
}
