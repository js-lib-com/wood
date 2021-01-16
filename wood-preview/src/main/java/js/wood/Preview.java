package js.wood;

import java.io.IOException;
import java.io.Writer;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.Element;
import js.util.Classes;
import js.util.Strings;

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
		IProjectConfig config = project.getConfig();
		IComponentDescriptor descriptor = compo.getDescriptor();

		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		Document doc = builder.createHTML();

		Element html = doc.getRoot();
		html.setAttr("lang", config.getDefaultLocale().toLanguageTag());
		Element head = doc.createElement("head");
		Element body = doc.createElement("body");
		html.addChild(head).addChild(body);
		head.addText("\r\n");

		head.addChild(doc.createElement("meta", "http-equiv", "Content-Type", "content", "text/html; charset=UTF-8"));
		head.addText("\r\n");

		addChildren(head, config.getMetas(), "name");

		String defaultTitle = Strings.concat(project.getDisplay(), " / ", compo.getDisplay());
		String title = descriptor.getTitle(defaultTitle);
		head.addChild(doc.createElement("title").setText(title));
		head.addText("\r\n");

		String description = descriptor.getDescription(title);
		head.addChild(doc.createElement("meta", "name", "Description", "content", description));
		head.addText("\r\n");

		Element layout = compo.getLayout();
		if (layout.getTag().equals("body")) {
			body.replace(layout);
		} else {
			body.addChild(layout);
		}

		// styles link inclusion order is important:
		// 1. third party fonts
		// 2. reset.css
		// 3. fx.css
		// 4. theme styles - theme styles are in no particular order since they are independent of each other
		// 5. component styles - first used template and widgets styles then component

		addChildren(head, config.getStyles(), "href");

		for (IStyleReference scriptReference : compo.getDescriptorLinks()) {
			addStyle(doc, scriptReference);
		}

		for (String font : config.getFonts()) {
			addStyle(doc, font);
		}

		for (FilePath stylePath : project.previewThemeStyles()) {
			addStyle(doc, absoluteUrlPath(stylePath));
		}

		for (FilePath stylePath : compo.getStyleFiles()) {
			addStyle(doc, absoluteUrlPath(stylePath));
		}

		addChildren(body, config.getScripts(), "src");

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
		// component scripts - both 3pty and local, are available only for automatic discovery
		if (project.hasScriptDiscovery()) {
			for (String script : compo.getThirdPartyScripts()) {
				// do not convert to absolute URL path since third party scripts are already absolute URL
				addScript(doc, script);
			}

			// component instance for preview includes preview script and its dependencies, it any
			for (IScriptFile scriptFile : compo.getScriptFiles()) {
				addScript(doc, absoluteUrlPath(scriptFile.getSourceFile()));
			}
		} else {
			// if script discovery is enabled preview.js is included into compo.getScriptFiles()
			// takes care to include preview.js, if discovery is not enabled
			IScriptFile previewScript = project.getPreviewScript(compo.getPreviewScript());
			if (previewScript != null) {
				addScript(doc, absoluteUrlPath(compo.getPreviewScript()));
			}

		}
*/
		
		DefaultAttributes.update(doc);
		doc.serialize(writer, true);
	}

	private static void addStyle(Document doc, IStyleReference styleRef) {
		Element head = doc.getByTag("head");

		Element link = doc.createElement("link");
		link.setAttr("href", styleRef.getHref());
		if (styleRef.hasIntegrity()) {
			link.setAttr("integrity", styleRef.getIntegrity());
		}
		if (styleRef.hasCrossorigin()) {
			link.setAttr("crossorigin", styleRef.getCrossorigin());
		}
		link.setAttr("rel", "stylesheet");
		link.setAttr("type", "text/css");

		head.addChild(link);
		head.addText("\r\n");
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
	private static void addScript(Document doc, String src) {
		Element head = doc.getByTag("head");
		head.addChild(doc.createElement("script", "src", src, "type", "text/javascript"));
		head.addText("\r\n");
	}
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
}
