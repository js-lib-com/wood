package js.wood;

import java.io.IOException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.List;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
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
	/** Project reference. */
	private final PreviewProject project;

	/** Wrapped component. */
	private final Component compo;

	/**
	 * Create component preview instance.
	 * 
	 * @param compo component.
	 */
	public Preview(PreviewProject project, Component compo) {
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
		head.addChild(doc.createElement("title").setText(compo.getTitle()));
		head.addText("\r\n");
		head.addChild(doc.createElement("meta", "name", "Author", "content", project.getAuthor()));
		head.addText("\r\n");
		head.addChild(doc.createElement("meta", "name", "Description", "content", compo.getDescription()));
		head.addText("\r\n");

		for (IMetaReference meta : project.getMetaReferences()) {
			addMeta(doc, meta);
		}
		for (IMetaReference meta : compo.getMetaReferences()) {
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

		for (ILinkReference link : project.getLinkReferences()) {
			addLink(doc, link);
		}
		for (ILinkReference link : compo.getLinkReferences()) {
			addLink(doc, link);
		}

		ThemeStyles themeStyles = new ThemeStyles(project.getThemeStyles());
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

		addChildren(body, project.getGlobalScripts(), "src");

		// scripts listed on component descriptor are included in the order they are listed
		// for script dependencies discovery this scripts list may be empty
		for (IScriptReference scriptReference : compo.getDescriptorScripts()) {
			// component descriptor third party scripts accept both project file path and absolute URL
			// if file path is used, convert it to absolute URL path, otherwise leave it as it is since points to foreign server
			String scriptPath = scriptReference.getSource();
			if (FilePath.accept(scriptPath)) {
				scriptPath = absoluteUrlPath(scriptPath);
			}
			addScript(doc, scriptPath, scriptReference);
		}

		/*
		 * // component scripts - both 3pty and local, are available only for automatic discovery if
		 * (project.hasScriptDiscovery()) { for (String script : compo.getThirdPartyScripts()) { // do not convert to absolute
		 * URL path since third party scripts are already absolute URL addScript(doc, script); }
		 * 
		 * // component instance for preview includes preview script and its dependencies, it any for (IScriptFile scriptFile :
		 * compo.getScriptFiles()) { addScript(doc, absoluteUrlPath(scriptFile.getSourceFile())); } } else { // if script
		 * discovery is enabled preview.js is included into compo.getScriptFiles() // takes care to include preview.js, if
		 * discovery is not enabled IScriptFile previewScript = project.getPreviewScript(compo.getPreviewScript()); if
		 * (previewScript != null) { addScript(doc, absoluteUrlPath(compo.getPreviewScript())); }
		 * 
		 * }
		 */

		DefaultAttributes.update(doc);
		doc.serialize(writer, true);
	}

	private static void addMeta(Document doc, IMetaReference meta) {
		Element head = doc.getByTag("head");

		final String name = meta.getName();
		final String httpEquiv = meta.getHttpEquiv();
		Element metaElement = null;
		if (name != null) {
			metaElement = head.getByAttr("name", name);
		}
		if (httpEquiv != null) {
			metaElement = head.getByAttr("http-equiv", httpEquiv);
		}
		if (metaElement == null) {
			metaElement = doc.createElement("meta");
		}

		setAttr(metaElement, "name", name);
		setAttr(metaElement, "http-equiv", httpEquiv);
		setAttr(metaElement, "content", meta.getContent());
		setAttr(metaElement, "charset", meta.getCharset());

		head.addChild(metaElement);
		head.addText("\r\n");

	}

	private static void addLink(Document doc, ILinkReference link) {
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

		if (link.isDisabled()) {
			linkElement.setAttr("disabled", "true");
		}

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
	 */
	/*
	 * private static void addScript(Document doc, String src) { Element head = doc.getByTag("head");
	 * head.addChild(doc.createElement("script", "src", src, "type", "text/javascript")); head.addText("\r\n"); }
	 */

	private static void addScript(Document doc, String src, IScriptReference scriptRef) {
		// preview always adds scripts to page head
		// next commented out statement is to show that 'scriptRef.isAppendToHead()' is ignored
		// Element parent = doc.getByTag(scriptRef.isAppendToHead() ? "head" : "body");
		Element parent = doc.getByTag("head");

		Element script = doc.createElement("script");
		script.setAttr("src", src);
		if (scriptRef.hasIntegrity()) {
			script.setAttr("integrity", scriptRef.getIntegrity());
		}
		if (scriptRef.hasCrossorigin()) {
			script.setAttr("crossorigin", scriptRef.getCrossorigin());
		}
		if (scriptRef.isDefer()) {
			script.setAttr("defer", "defer");
		}
		script.setAttr("type", "text/javascript");

		parent.addChild(script);
		parent.addText("\r\n");
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
		builder.append(Path.SEPARATOR);
		builder.append(project.getPreviewName());
		builder.append(Path.SEPARATOR);
		builder.append(filePath);
		return builder.toString();
	}

	private static void addChildren(Element parent, EList children, String keyAttr) {
		for (Element child : children) {
			String keyValue = child.getAttr(keyAttr);
			if (parent.getByAttr(keyAttr, keyValue) != null) {
				continue;
			}
			parent.addChild(child);
			parent.addText("\r\n");
		}
	}

	private static class ThemeStyles {
		public final FilePath reset;
		public final FilePath fx;
		public final List<FilePath> styles = new ArrayList<>();

		public ThemeStyles(List<FilePath> themeStyles) {
			FilePath reset = null;
			FilePath fx = null;
			for (FilePath style : themeStyles) {
				switch (style.getName()) {
				case CT.RESET_CSS:
					reset = style;
					break;

				case CT.FX_CSS:
					fx = style;
					break;

				default:
					this.styles.add(style);
				}
			}
			this.reset = reset;
			this.fx = fx;
		}
	}
}
