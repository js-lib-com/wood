package js.wood;

import java.io.Reader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import js.dom.Attr;
import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.Element;
import js.util.Classes;
import js.util.Strings;
import js.wood.impl.ComponentDescriptor;
import js.wood.impl.EditablePath;
import js.wood.impl.LayoutParameters;
import js.wood.impl.LayoutReader;
import js.wood.impl.Operator;
import js.wood.impl.ScriptReference;

/**
 * Meta-data about component consolidated instance. Component is a piece of user interface designed to interconnect with another
 * components. Following OOP paradigm there are two major relations: inheritance and composition. Inheritance is implemented via
 * templates mechanism whereas composition is achieved using widget components. This class discovers relations and consolidates
 * the component instance. On the fly collects data about used resources and takes care to inject layout parameters.
 * <p>
 * A component resides into own directory and has mandatory layout file (HTML5) and optional style (CSS3), script (JS) and
 * resource files - resources are variables and media files. A component may also have a descriptor. By convention component
 * name is the same as directory, layout, style, script and descriptor files basename.
 * <p>
 * This class deals with source files aggregation: it consolidates layout from its templates hierarchy and widgets tree and
 * collect used style files and script classes. But does not deal with resources references. For that purpose, component class
 * is configured with {@link IReferenceHandler} that is in charge with resource references processing: inject variables and
 * process media files. Resources processing is externalized because build and preview processes have different needs.
 * <p>
 * Implementation note. Resource references handling is performed on source file reading, on the fly. Component needs to load
 * its layout and all depending layout documents from file; while layout file is reading {@link #referenceHandler} resolve
 * variables and copy media files. The point is, after layout loaded, media files are already present into build directory and
 * loaded document has references resolved.
 * 
 * @author Iulian Rotaru
 */
public class Component {
	private final DocumentBuilder documentBuilder;

	/** Parent project reference. */
	private final Project project;

	/** Optional component descriptor, default to null. */
	private ComponentDescriptor descriptor;

	/** Operators handler created by project, based on the naming strategy selected by developer. */
	private final IOperatorsHandler operators;

	/** Component name. By convention is the name of component directory. */
	private final String name;

	/** Display is the component name formatted for user interface. */
	private final String display;

	/** This component base layout file path. */
	private final FilePath baseLayoutPath;

	/** Layout parameters used by source reader to inject parameter values. */
	private final LayoutParameters layoutParameters;

	/** External defined resource references handler in charge with resources processing. */
	private final IReferenceHandler referenceHandler;

	/**
	 * The list of style files used by this component - consolidated from all included components, in the proper oder for page
	 * document inclusion.
	 */
	private final List<FilePath> styleFiles = new ArrayList<>();

	private final List<IMetaReference> metaReferences = new ArrayList<>();

	/**
	 * Link references declared on this component and all included components descriptors. Link references order from
	 * descriptors is preserved. Link references are not limited to styles.
	 */
	private final List<ILinkReference> linkReferences = new ArrayList<>();

	private final List<IScriptReference> scriptReferences = new ArrayList<>();

	/**
	 * Consolidated layout for this component instance. It contains layouts from templates hierarchy and widgets tree. Also
	 * references are resolved, that is, references replaced with variables values and media URL paths.
	 */
	private Element layout;

	/**
	 * Create aggregated instance for component identified by given path. Just delegates
	 * {@link #Component(FilePath, IReferenceHandler)} with component layout path.
	 * 
	 * @param compoPath component path,
	 * @param referenceHandler resource references handler.
	 */
	public Component(CompoPath compoPath, IReferenceHandler referenceHandler) {
		this(compoPath.getLayoutPath(), referenceHandler);
	}

	/**
	 * Create component instance identified by its layout file path. This constructor completely initializes all its fields:
	 * consolidates its layout and fill styles and scripts lists . While reading layout files {@link #referenceHandler} is
	 * invoked, that, on its turn resolves resource references on the fly.
	 * <p>
	 * This constructor is designed for preview process; it allows for preview script inclusion into script dependencies
	 * scanning.
	 * 
	 * @param layoutPath component layout file path,
	 * @param referenceHandler resource references handler,
	 */
	public Component(FilePath layoutPath, IReferenceHandler referenceHandler) {
		this.documentBuilder = Classes.loadService(DocumentBuilder.class);

		this.project = layoutPath.getProject();
		this.operators = this.project.getOperatorsHandler();
		this.layoutParameters = new LayoutParameters();
		this.referenceHandler = referenceHandler;

		this.baseLayoutPath = layoutPath;
		this.name = layoutPath.getBaseName();
		this.display = Strings.toTitleCase(name);

		FilePath descriptorFile = layoutPath.getDirPath().getFilePath(layoutPath.getDirPath().getName() + CT.DOT_XML_EXT);
		this.descriptor = new ComponentDescriptor(descriptorFile, referenceHandler);
	}

	public void scan(boolean includePreviewScript) {
		// consolidate component layout from its templates and widgets
		// update internal styles list with components related style file
		layout = scanComponentsTree(baseLayoutPath, 0);

		// after layout consolidated scan it for included scripts and class and formatter declarations
		// 'script' is used only to declare script dependencies and remove after processing
		// 'class' and 'format' are j(s)-script library operators and need to be preserved

		Element pageClassEl = operators.getByOperator(layout, Operator.SCRIPT);
		if (pageClassEl != null) {
			operators.removeOperator(pageClassEl, Operator.SCRIPT);
		}

		for (Element editableEl : operators.findByOperator(layout, Operator.EDITABLE)) {
			if (editableEl.isEmpty()) {
				editableEl.remove();
			} else {
				operators.removeOperator(editableEl, Operator.EDITABLE);
			}
		}

		addAll(metaReferences, descriptor.getMetas());
		addAll(linkReferences, descriptor.getLinks());
		addAll(scriptReferences, descriptor.getScripts());
	}

	/**
	 * Scan project resources and library directories for requested component layout, resolving its dependencies. Scanning
	 * process is recursively, in depth-first order, and actually solves two kinds of dependencies: templates hierarchy and
	 * child widgets tree. Templates hierarchy processing is delegated to {@link #loadLayoutDocument(FilePath, int)} whereas
	 * widgets tree is handled by this method logic.
	 * <p>
	 * Widgets are processed in the order from this parent layout, self-invoking this method recursively. This way an widget may
	 * have other child widgets creating a not restricted complex widgets tree.
	 * <p>
	 * Note that the actual HTML document file reading is performed by {@link SourceReader} that detects resource references and
	 * invoke configured {@link #referenceHandler}; on its turn resource references handler takes care to inject variables and
	 * process media files.
	 * 
	 * @param layoutPath layout file path,
	 * @param guardCount circular reference guard.
	 * @return component layout with templates hierarchy and widgets tree resolved.
	 */
	private Element scanComponentsTree(FilePath layoutPath, int guardCount) {
		if (guardCount++ == 8) {
			throw new WoodException("Circular compo references suspicion. Too many nesting levels on |%s|. Please check wood:widget attributes!", layoutPath);
		}

		// components tree scanning is an recursive process that happens in two steps:
		// 1. getLayoutDocument() takes care to process component templates hierarchy, if this base component uses
		// 2. this method logic scans recursively for and insert widget components - for short widgets

		// if the case, returned component layout has templates elements with base component content inserted
		Document layout = loadLayoutDocument(layoutPath, guardCount);

		// scan for widget components referenced by this base component
		// an external widget component is identified by widget path, which is a standard project file path

		// widget path element is part of base component and contains the widget path
		// it acts as insertion point; is where widget layout is inserted
		for (Element widgetPathElement : getCompoPathElements(layout)) {

			// widget layout is the root of widget layout definition
			// it is loaded recursively in depth-first order so that when a widget level is returned all its
			// descendants are guaranteed to be resolved
			CompoPath compoPath = new CompoPath(project, operators.getOperand(widgetPathElement, Operator.COMPO));

			FilePath descriptorFile = compoPath.getFilePath(compoPath.getName() + CT.DOT_XML_EXT);
			ComponentDescriptor descriptor = new ComponentDescriptor(descriptorFile, referenceHandler);
			addAll(metaReferences, descriptor.getMetas());
			addAll(linkReferences, descriptor.getLinks());
			addAll(scriptReferences, descriptor.getScripts());

			// widget path element may have invocation parameters for widget layout customization
			// load parameters, if any, and on loadLayoutDocument passes them to source reader
			// source reader takes care to inject parameter values into widget layout
			layoutParameters.load(operators.getOperand(widgetPathElement, Operator.PARAM));
			operators.removeOperator(widgetPathElement, Operator.PARAM);
			Element widgetLayout = scanComponentsTree(compoPath.getLayoutPath(), guardCount);

			// update widget path element attributes with values from the widget layout root
			// widget path element has precedence over widget layout attributes so that parent can control widget attributes
			mergeAttrs(widgetPathElement, widgetLayout.getAttrs());

			// remove all children from widget path element and insert the actual widget layout elements
			widgetPathElement.removeChildren();
			for (Element widgetChild : widgetLayout.getChildren()) {
				widgetPathElement.addChild(widgetChild);
			}

			operators.removeOperator(widgetPathElement, Operator.COMPO);
		}

		// component layout is loaded with LayoutReader decorator that insert XML declaration and root element
		// this is needed because component layout file may have more HTML fragments and XML requires single root
		return layout.getRoot().getFirstChild();
	}

	/**
	 * Load document layout resolving template(s), if the case and delegates resources processing, on the fly to
	 * {@link #referenceHandler}. If present, template(s) are processed invoking this method recursively till entire hierarchy
	 * is done.
	 * <p>
	 * Returns loaded layout document. Anyway, if template is used, returns template document instead - with this layout content
	 * inserted in template editable area.
	 * <p>
	 * Document file is loaded using {@link LayoutReader} and {@link SourceReader} decorators. Layout reader insert XML
	 * declaration and document root; component layout is child to injected document root. Source reader detects resource
	 * references and invoke {@link #referenceHandler} that handle variables replacement and media files processing.
	 * <p>
	 * This method insert the related style file into {@link #styleReferences} list. By convention layout and style files have
	 * the same name; anyway, style file is not mandatory. Also takes care to insert style file path in the proper order,
	 * suitable for page header inclusion.
	 * 
	 * @param layoutPath component layout path,
	 * @param guardCount circular reference guard.
	 * @return layout document.
	 */
	private Document loadLayoutDocument(FilePath layoutPath, int guardCount) {
		if (guardCount++ == 8) {
			throw new WoodException("Circular templates references suspicion. Too many nesting levels on |%s|. Please check 'template' attributes!", layoutPath);
		}

		Reader reader = new LayoutReader(new SourceReader(layoutPath, layoutParameters, referenceHandler));
		Document layout = project.hasNamespace() ? documentBuilder.loadXMLNS(reader) : documentBuilder.loadXML(reader);
		if (!layout.getRoot().hasChildren()) {
			throw new WoodException("Empty layout |%s|.", layoutPath);
		}

		// component layout may have related style file; collect if into this base component used styles list
		collectRelatedStyle(layoutPath);

		// content element is the root of content layout designed to replace an editable element from a template
		// a content element is an element with 'template' attribute
		// if this layout has no content elements it does not use templates and return it as it is

		// 'template' attribute contains the path to template named editable area
		EList contentElements = operators.findByOperator(layout, Operator.TEMPLATE);
		if (contentElements.isEmpty()) {
			return layout;
		}

		// current component describe content for a template because it has at least one reference to an editable element
		// an editable element is a template hole that need to be filled
		// since templates can have more than one editable element, editable path should identify both the template and
		// editable element: EDITABLE_PATH := TEMPLATE_PATH # EDITABLE_NAME

		// current component should define content for all editable elements from template
		// if not it become a template too and creates a hierarchy of templates similar to class inheritance

		// a content layout file may contain content elements for multiple editable elements from template
		// but all pointing to the same template layout file
		// uses first content element to acquire template layout path and load template layout document
		// also load layout parameters from content element data-param operator
		// layout parameters are used by layout source file reader to inject parameter values

		Document template = null;
		Editables editables = null;

		for (Element contentElement : contentElements) {
			EditablePath editablePath = new EditablePath(layoutPath, operators.getOperand(contentElement, Operator.TEMPLATE));

			if (template == null) {
				// prepare layout parameters, possible empty, before loading template from source file
				// subsequent loadLayoutDocument passes parameters to source reader
				// source reader takes care to inject parameter values into template layout
				layoutParameters.load(operators.getOperand(contentElement, Operator.PARAM));
				operators.removeOperator(contentElement, Operator.PARAM);

				template = loadLayoutDocument(editablePath.getLayoutPath(), guardCount);
				editables = new Editables(template);
			}

			operators.removeOperator(contentElement, Operator.TEMPLATE);
			Element editableElement = editables.get(editablePath.getEditableName());
			if (editableElement == null) {
				throw new WoodException("Invalid template source code. Missing editable |%s| requested from component |%s|.", editablePath, layoutPath);
			}

			// insert content element - and all its descendants into template document, before editable element
			// then merge newly inserted content and editable elements attributes, but content attributes takes precedence
			editableElement.insertBefore(contentElement);
			mergeAttrs(editableElement.getPreviousSibling(), editableElement.getAttrs());
		}

		editables.remove();
		return template;
	}

	/**
	 * Return parent project to which this component belongs to.
	 * 
	 * @return parent project.
	 * @see #project
	 */
	public Project getProject() {
		return project;
	}

	/**
	 * Return page security role or null if security role is not defined. This property has meaning only on page components.
	 * <p>
	 * Security role is declared on page components and is used by site builder to create specific sub-directories where to
	 * store role related files.
	 * 
	 * @return page security role or null if not defined.
	 */
	public String getSecurityRole() {
		// TODO: rename <path> element from descriptor to <security-role>
		return descriptor.getSecurityRole();
	}

	/**
	 * Get this component name.
	 * 
	 * @return component name.
	 * @see #name
	 */
	public String getName() {
		return name;
	}

	public String getDisplay() {
		return descriptor.getDisplay(Strings.concat(project.getDisplay(), " / ", display));
	}

	public String getDescription() {
		return descriptor.getDescription(getDisplay());
	}

	/**
	 * Get aggregated layout element.
	 * 
	 * @return this component layout HTML fragment.
	 * @see #layout
	 */
	public Element getLayout() {
		return layout;
	}

	/**
	 * Get the name of the component layout file.
	 * 
	 * @return layout file name.
	 */
	public String getLayoutFileName() {
		return baseLayoutPath.getName();
	}

	public List<IMetaReference> getMetaReferences() {
		return metaReferences;
	}

	public List<ILinkReference> getLinkReferences() {
		return linkReferences;
	}

	/**
	 * Get component style files in the proper order for page document inclusion.
	 * 
	 * @return component style files.
	 * @see #styleReferences
	 */
	public List<FilePath> getStyleFiles() {
		return styleFiles;
	}

	public List<IScriptReference> getScriptReferences() {
		return scriptReferences;
	}

	/**
	 * Get the file path for optional preview style. Preview style is used only by preview process and provides styles for unit
	 * testing context.
	 * <p>
	 * Returned file path is optional, that is, designed file is not mandatory to exist. Is caller responsibility to ensure file
	 * exist before using it.
	 * 
	 * @return component style preview file.
	 */
	public FilePath getPreviewStyle() {
		return baseLayoutPath.getDirPath().getFilePath(CT.PREVIEW_STYLE);
	}

	/**
	 * Get the file path for optional preview script. Preview script describe unit testing logic; it is used by preview process
	 * to literally replace component script.
	 * <p>
	 * Returned file path is optional, that is, designed file is not mandatory to exist. Is caller responsibility to ensure file
	 * exist before using it.
	 * 
	 * @return component script preview file or null if not defined.
	 */
	public IScriptReference getPreviewScript() {
		FilePath file = baseLayoutPath.getDirPath().getFilePath(CT.PREVIEW_SCRIPT);
		return file.exists() ? new ScriptReference(file.value()) : null;
	}

	/**
	 * Return this component {@link #display}, that is used also as string representation.
	 * 
	 * @return component display.
	 * @see #display
	 */
	@Override
	public String toString() {
		return display;
	}

	// --------------------------------------------------------
	// Internal helper methods

	/**
	 * Collect style file related to given source file, be it layout or script file. A component layout or script file may have
	 * a related style. A related style file have the same base name as source file but with style extension. This method uses
	 * {@link FilePath#cloneToStyle()} to get related style file.
	 * <p>
	 * If related style file exists add it to this component used style files list, see {@link #styleReferences}. Takes care to
	 * keep styles proper order for page document and preview inclusion.
	 * 
	 * @param sourceFile source file.
	 */
	private void collectRelatedStyle(FilePath sourceFile) {
		// by convention component, component layout, script file and style sheet share the same name
		// for example, component res/path/compo has res/path/compo/compo.htm layout, res/path/compo/compo.js script and
		// res/path/compo/compo.css style

		FilePath styleFile = sourceFile.cloneToStyle();
		if (styleFile.exists() && !styleFiles.contains(styleFile)) {
			// component style files are linked into build and preview document header
			// in the order from this component styles list, first style file on top

			// we need to ensure templates, widgets and scripts related style files are included before component base style
			// for this reason we insert discovered style files at the styles list beginning

			styleFiles.add(0, styleFile);
		}
	}

	/** Constant for empty element array. */
	private static final Element[] EMPTY_ARRAY = new Element[0];

	/**
	 * An compo path is a reference to an external component; this method returns defined compo paths. Returns a newly created
	 * array with all compo path elements found in given layout document. This method creates a new array because {@link EList}
	 * is live and we need to remove compo paths while iterating. Returns empty array if no compo found.
	 * <p>
	 * Compo path element is identified by attribute with name <code>wood:compo</code>.
	 * 
	 * @param layout layout document.
	 * @return widgets array possible empty.
	 */
	private Element[] getCompoPathElements(Document layout) {
		EList elist = operators.findByOperator(layout, Operator.COMPO);
		if (elist.isEmpty()) {
			return EMPTY_ARRAY;
		}
		Element[] array = new Element[elist.size()];
		for (int i = 0; i < elist.size(); ++i) {
			array[i] = elist.item(i);
		}
		return array;
	}

	/**
	 * Add attributes to element but do not overwrite existing ones. If attribute to add is CSS class add newly class values but
	 * also takes care to not overwrite existing ones.
	 * 
	 * @param element document element,
	 * @param attrs attributes list to add.
	 */
	private static void mergeAttrs(Element element, Iterable<Attr> attrs) {
		for (Attr attr : attrs) {
			if (attr.getName().equals("class")) {
				// merge attributes and element CSS classes but do not overwrite element classes
				Set<String> classes = new HashSet<String>();
				classes.addAll(Strings.split(attr.getValue(), ' '));
				String elementClass = element.getAttr("class");
				if (elementClass != null) {
					classes.addAll(Strings.split(elementClass, ' '));
				}
				element.setAttr("class", Strings.join(classes, ' '));
				continue;
			}
			if (!element.hasAttr(attr.getName())) {
				element.setAttr(attr.getName(), attr.getValue());
			}
		}
	}

	private static <T> void addAll(List<T> target, List<T> source) {
		for (T item : source) {
			if (!target.contains(item)) {
				target.add(item);
			}
		}
	}

	// ------------------------------------------------------
	// Internal classes

	/**
	 * Template editable elements. This class supplies method to retrieve editable element by name and remove all template
	 * editable elements, after template processing complete.
	 * 
	 * @author Iulian Rotaru
	 */
	private class Editables {
		/** Template document. */
		private Document template;

		/** Template editables. */
		private Map<String, Element> editables = new HashMap<String, Element>();

		/**
		 * Create editables instance for given template layout.
		 * 
		 * @param template template layout document.
		 */
		public Editables(Document template) {
			this.template = template;
		}

		/**
		 * Return editable element identified by given name or null if not found.
		 * 
		 * @param editableName editable name.
		 * @return editable element or null.
		 */
		public Element get(String editableName) {
			Element editable = editables.get(editableName);
			if (editable == null) {
				editable = operators.getByOperator(template, Operator.EDITABLE, editableName);
				editables.put(editableName, editable);
			}
			return editable;
		}

		/**
		 * Remove all this template editable elements. This method is invoked after template processing complete.
		 */
		public void remove() {
			for (Element editable : editables.values()) {
				editable.remove();
			}
		}
	}
}
