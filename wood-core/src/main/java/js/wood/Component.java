package js.wood;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
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
import js.wood.impl.FileType;
import js.wood.impl.IOperatorsHandler;
import js.wood.impl.LayoutParameters;
import js.wood.impl.Operator;
import js.wood.impl.ScriptDescriptor;

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
	private static final int MAX_NESTING_LEVELS = 8;

	private final DocumentBuilder documentBuilder;

	/** Parent project reference. */
	private final Project project;

	private final Factory factory;

	/** Operators handler created by project, based on the naming strategy selected by developer. */
	private final IOperatorsHandler operators;

	/** Component name. By convention is the name of component directory. */
	private final String name;

	/** Display is the component name formatted for user interface. */
	private final String display;

	private final String description;

	private final String securityRole;

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

	/**
	 * Descriptors for page meta elements, declared on this component descriptor. Meta descriptors order is preserved.
	 * <p>
	 * This field is optional with default to empty list.
	 */
	private final List<IMetaDescriptor> metaDescriptors = new ArrayList<>();

	/**
	 * Descriptors for page link elements, declared on this component descriptor. Link descriptors order is preserved. Link
	 * descriptors are not limited to styles.
	 * <p>
	 * This field is optional with default to empty list.
	 */
	private final List<ILinkDescriptor> linkDescriptors = new ArrayList<>();

	/**
	 * Descriptors for page script elements, declared on this component descriptor. Script descriptors order is preserved.
	 * <p>
	 * This field is optional with default to empty list.
	 */
	private final List<IScriptDescriptor> scriptDescriptors = new ArrayList<>();

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
		this(compoPath.getLayoutPathEx(), referenceHandler);
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
		this.factory = this.project.getFactory();
		this.operators = this.project.getOperatorsHandler();
		this.layoutParameters = new LayoutParameters();
		this.referenceHandler = referenceHandler;

		FilePath descriptorFile = layoutPath.cloneTo(FileType.XML);
		ComponentDescriptor descriptor = new ComponentDescriptor(descriptorFile, referenceHandler);

		this.baseLayoutPath = layoutPath;
		this.name = layoutPath.getBaseName();
		this.display = descriptor.getDisplay(Strings.concat(project.getDisplay(), " / ", Strings.toTitleCase(name)));
		this.description = descriptor.getDescription(this.display);
		this.securityRole = descriptor.getSecurityRole();

		// consolidate component layout from its templates and widgets
		// update internal styles list with components related style file
		layout = scanComponentsTree(baseLayoutPath, 0);

		addAll(metaDescriptors, descriptor.getMetaDescriptors());
		addAll(linkDescriptors, descriptor.getLinkDescriptors());
		addAll(scriptDescriptors, descriptor.getScriptDescriptors());
	}

	public void clean() {
		// templates realization is optional; remove empty editable elements
		for (Element editable : operators.findByOperator(layout, Operator.EDITABLE)) {
			assert editable.isEmpty();
			editable.remove();
		}

		// remove wood namespace declarations
		layout.getDocument().removeNamespaceDeclaration(WOOD.NS);
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
			CompoPath compoPath = factory.createCompoPath(operators.getOperand(widgetPathElement, Operator.COMPO));

			FilePath childLayoutPath = compoPath.getLayoutPath();
			if (!childLayoutPath.exists()) {
				throw new WoodException("Missing child component layout |%s| requested from parent |%s|.", childLayoutPath, layoutPath);
			}

			mergeDescriptor(childLayoutPath);

			// widget path element may have invocation parameters for widget layout customization
			// load parameters, if any, and on loadLayoutDocument passes them to source reader
			// source reader takes care to inject parameter values into widget layout
			layoutParameters.reload(operators.getOperand(widgetPathElement, Operator.PARAM));
			operators.removeOperator(widgetPathElement, Operator.PARAM);
			Element widgetLayout = scanComponentsTree(childLayoutPath, guardCount);

			// update widget path element attributes with values from the widget layout root
			// widget path element has precedence over widget layout attributes so that parent can control widget attributes
			addAttrs(widgetPathElement, widgetLayout.getAttrs());

			// remove all children from widget path element and insert the actual widget layout elements
			widgetPathElement.removeChildren();
			for (Element widgetChild : widgetLayout.getChildren()) {
				widgetPathElement.addChild(widgetChild);
			}

			operators.removeOperator(widgetPathElement, Operator.COMPO);
		}

		return layout.getRoot();
	}

	/**
	 * Load document layout resolving template(s), if the case and delegating resources processing on the fly to
	 * {@link #referenceHandler}. If present, template(s) are processed invoking this method recursively till entire hierarchy
	 * is done.
	 * <p>
	 * Returns loaded layout document. Anyway, if template is used, returns template document instead - with this layout content
	 * inserted in template editable area.
	 * <p>
	 * Document file is loaded using {@link SourceReader} decorator. Source reader detects resource references and invoke
	 * {@link #referenceHandler} that handle variables replacement and media files processing.
	 * <p>
	 * This method insert the related style file into {@link #styleReferences} list. By convention layout and style files have
	 * the same name; anyway, style file is not mandatory. Also takes care to insert style file path in the proper order,
	 * suitable for page header inclusion.
	 * 
	 * @param layoutPath component layout file path,
	 * @param guardCounter nesting level guard counter for protection against circular dependencies.
	 * @return layout document.
	 */
	private Document loadLayoutDocument(FilePath layoutPath, int guardCounter) {
		if (guardCounter++ == MAX_NESTING_LEVELS) {
			throw new WoodException("Circular templates references suspicion. Too many nesting levels on |%s|. Please check 'template' attributes!", layoutPath);
		}

		Reader reader = new SourceReader(layoutPath, layoutParameters, referenceHandler);
		Document layoutDoc;
		try {
			layoutDoc = project.hasNamespace() ? documentBuilder.loadXMLNS(reader) : documentBuilder.loadXML(reader);
		} catch (Exception e) {
			throw new WoodException("Invalid layout document |%s|.", layoutPath);
		}

		// component layout may have related style file; collect if into this base component used styles list
		collectRelatedStyle(layoutPath);

		// use 'template' operator to scan for content fragments; 'template' operator is mandatory on content fragment root
		EList contentFragments = operators.findByOperator(layoutDoc, Operator.TEMPLATE);
		if (contentFragments.isEmpty()) {
			// if there are no content fragments, currently loaded layout does not inherit from a template component
			return layoutDoc;
		}

		// if content fragment is the document root we have a stand alone content component
		// it is not allowed to have multiple content fragments in a stand alone content component
		ContentFragment contentFragment = new ContentFragment(operators, contentFragments.item(0));
		if (contentFragment.isRoot()) {
			for (int i = 1; i < contentFragments.size(); ++i) {
				Element inlineContentFragment = contentFragments.item(i);
				Document templateDoc = consolidateTemplate(layoutPath, new ContentFragment(operators, inlineContentFragment), guardCounter);
				inlineContentFragment.replace(templateDoc.getRoot());
			}

			// return consolidated template document
			return consolidateTemplate(layoutPath, contentFragment, guardCounter);
		}

		// at this point we have one or many inline content fragments
		// consolidate template for content fragment then replace it with consolidated document root
		for (Element inlineContentFragment : contentFragments) {
			Document templateDoc = consolidateTemplate(layoutPath, new ContentFragment(operators, inlineContentFragment), guardCounter);
			inlineContentFragment.replace(templateDoc.getRoot());
		}
		// return component layout document with inline content fragments replaces by consolidated template documents
		return layoutDoc;
	}

	/**
	 * Create template document declared by given content fragment and resolve editable areas. Component content fragment
	 * provides both information about template location and content elements to be injected into editable areas.
	 * 
	 * @param componentLayoutPath file path for component layout, for logging,
	 * @param contentFragment HTML fragment from component that defined content to be injected into template editable,
	 * @param guardCounter nesting level guard counter for protection against circular dependencies.
	 * @return newly created template document with editable areas resolved.
	 */
	private Document consolidateTemplate(FilePath componentLayoutPath, ContentFragment contentFragment, int guardCounter) {
		CompoPath templateCompoPath = factory.createCompoPath(contentFragment.getTemplatePath());
		FilePath templateLayoutPath = templateCompoPath.getLayoutPath();
		if (!templateLayoutPath.exists()) {
			throw new WoodException("Missing child component layout |%s| requested from parent |%s|.", templateLayoutPath, componentLayoutPath);
		}
		mergeDescriptor(templateLayoutPath);

		Document templateDoc = null;
		Editables editables = null;
		boolean cleanupEditables = false;
		for (Element contentElement : contentFragment.getContentElements()) {
			if (templateDoc == null) {
				// prepare layout parameters, possible empty, before loading template from source file
				// subsequent loadLayoutDocument passes parameters to source reader
				// source reader takes care to inject parameter values into template layout
				layoutParameters.reload(operators.getOperand(contentElement, Operator.PARAM));
				operators.removeOperator(contentElement, Operator.PARAM);

				templateDoc = loadLayoutDocument(templateLayoutPath, guardCounter);
				editables = new Editables(operators, templateDoc);
			}

			String editableName = contentFragment.getEditableName(contentElement);
			Element editableElement = editables.get(editableName);
			if (editableElement == null) {
				// it is legal to have empty template in which case consider entire template as editable
				// anyway, if template document has children is mandatory to have editable element
				if (templateDoc.getRoot().hasChildren()) {
					throw new WoodException("Missing editable element |%s#%s| requested from component |%s|.", templateCompoPath, editableName, componentLayoutPath);
				}
				editableElement = templateDoc.getRoot();
			}

			operators.removeOperator(editableElement, Operator.EDITABLE);
			operators.removeOperator(contentElement, Operator.TEMPLATE);
			operators.removeOperator(contentElement, Operator.CONTENT);

			if (editableElement.getParent() != null) {
				// insert content element - and all its descendants into template document, before editable element
				// then merge newly inserted content and editable elements attributes, but content attributes takes precedence
				editableElement.insertBefore(contentElement);
				addAttrs(editableElement.getPreviousSibling(), editableElement.getAttrs());
				cleanupEditables = true;
			} else {
				for (Element child : contentElement.getChildren()) {
					editableElement.addChild(child);
				}
				addAttrs(editableElement, contentElement.getAttrs(), true);
			}
		}

		if (cleanupEditables) {
			editables.remove();
		}
		return templateDoc;
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
		return securityRole;
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
		return display;
	}

	public String getDescription() {
		return description;
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

	/**
	 * Get descriptors for page meta elements, declared on this component descriptor. Meta descriptors order is preserved.
	 * <p>
	 * Return empty list if there are no meta descriptors declared on component descriptor. Returned list is not modifiable.
	 * 
	 * @return Immutable list of meta descriptors, possible empty.
	 * @see #metaDescriptors
	 */
	public List<IMetaDescriptor> getMetaDescriptors() {
		return Collections.unmodifiableList(metaDescriptors);
	}

	/**
	 * Get descriptors for page link elements, declared on this component descriptor. Link descriptors order is preserved.
	 * <p>
	 * Return empty list if there are no link descriptors declared on component descriptor. Returned list is not modifiable.
	 * 
	 * @return Immutable list of link descriptors, possible empty.
	 * @see #linkDescriptors
	 */
	public List<ILinkDescriptor> getLinkDescriptors() {
		return Collections.unmodifiableList(linkDescriptors);
	}

	/**
	 * Get component style files in the proper order for page document inclusion.
	 * 
	 * @return component style files.
	 * @see #styleFiles
	 */
	public List<FilePath> getStyleFiles() {
		return styleFiles;
	}

	/**
	 * Get descriptors for page script elements, declared on this component descriptor. script descriptors order is preserved.
	 * <p>
	 * Return empty list if there are no script descriptors declared on component descriptor. Returned list is not modifiable.
	 * 
	 * @return Immutable list of script descriptors, possible empty.
	 * @see #scriptDescriptors
	 */
	public List<IScriptDescriptor> getScriptDescriptors() {
		return Collections.unmodifiableList(scriptDescriptors);
	}

	/**
	 * Get descriptor for requested script file or null if not found.
	 * 
	 * @param fileName script file name.
	 * @return script descriptor or null if not defined.
	 */
	public IScriptDescriptor getScriptDescriptor(String fileName) {
		FilePath file = baseLayoutPath.getParentDirPath().getFilePath(fileName);
		return file.exists() ? new ScriptDescriptor(file.value()) : null;
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

		FilePath styleFile = sourceFile.cloneTo(FileType.STYLE);
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
	 * An compo path is a reference to a child component; this method returns defined compo paths. Returns a newly created array
	 * with all compo path elements found in given layout document. This method creates a new array because {@link EList} is
	 * live and we need to remove compo paths while iterating. Returns empty array if no compo found.
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

	private void mergeDescriptor(FilePath layoutPath) {
		FilePath descriptorFile = layoutPath.cloneTo(FileType.XML);
		ComponentDescriptor descriptor = new ComponentDescriptor(descriptorFile, referenceHandler);
		addAll(metaDescriptors, descriptor.getMetaDescriptors());
		addAll(linkDescriptors, descriptor.getLinkDescriptors());
		addAll(scriptDescriptors, descriptor.getScriptDescriptors());
	}

	/**
	 * Add attributes to element but do not overwrite existing ones. Anyway, if optional <code>overrides</code> flag is provided
	 * and is true, element existing attributes values are overridden. In any case <code>class</code> attributes are merged but
	 * there is no particular order guaranteed.
	 * 
	 * @param element target document element, whose attributes are updated,
	 * @param attrs list of attributes to add,
	 * @param overrides optional flag, true if given attributes should override element attributes.
	 */
	private static void addAttrs(Element element, Iterable<Attr> attrs, boolean... overrides) {
		for (Attr attr : attrs) {
			switch (attr.getName()) {
			case "class":
				Set<String> classes = new HashSet<>();
				classes.addAll(Strings.split(attr.getValue(), ' '));
				String elementClass = element.getAttr("class");
				if (elementClass != null) {
					classes.addAll(Strings.split(elementClass, ' '));
				}
				element.setAttr("class", Strings.join(classes, ' '));
				break;

			default:
				boolean override = overrides.length == 1 ? overrides[0] : false;
				String namespaceURI = attr.getNamespaceURI();
				if (namespaceURI != null) {
					if (override || !element.hasAttrNS(namespaceURI, attr.getName())) {
						element.setAttrNS(namespaceURI, attr.getName(), attr.getValue());
					}
				} else {
					if (override || !element.hasAttr(attr.getName())) {
						String[] names = attr.getName().split(":");
						element.setAttr(names[0], attr.getValue());
					}
				}
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
	private static class Editables {
		private final IOperatorsHandler operators;
		/** Template document. */
		private final Document template;

		/** Template editables. */
		private final Map<String, Element> editables = new HashMap<>();

		/**
		 * Create editables instance for given template layout.
		 * 
		 * @param template template layout document.
		 */
		public Editables(IOperatorsHandler operators, Document template) {
			this.operators = operators;
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
				if (editable == null) {
					return null;
				}
				if (editable.hasChildren()) {
					throw new WoodException("Template editable element |%s| is not allowed to have children.", editable);
				}
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

	private static class ContentFragment {
		private final IOperatorsHandler operators;
		private final Element root;
		/**
		 * Template path as declared by {@link Operator#TEMPLATE} operator from root. It is the path to the template resolved by
		 * this content fragment.
		 */
		private final String templatePath;
		/**
		 * Editable region name as declared by {@link Operator#TEMPLATE} operator from this content fragment root. This property
		 * is optional with default to null. Anyway, if there is a single content element that do not have
		 * {@link Operator#CONTENT} operator this value is mandatory.
		 */
		private final String templatePathEditableName;
		private final List<Element> contentElements;

		public ContentFragment(IOperatorsHandler operators, Element root) {
			this.operators = operators;
			this.root = root;

			// a content fragment has one or more content elements identified by 'content' operator
			// 'content' operator has the name for the editable area for which it provides content
			// if there is a single content element is allowed to combine template path and editable name
			// TEMPLATE_PATH # EDITABLE_NAME

			String templatePath = operators.getOperand(root, Operator.TEMPLATE);
			String templatePathEditableName = null;
			int separatorPosition = templatePath.indexOf('#');
			if (separatorPosition != -1) {
				templatePathEditableName = templatePath.substring(separatorPosition + 1);
				templatePath = templatePath.substring(0, separatorPosition);
			}

			this.templatePath = templatePath;
			this.templatePathEditableName = templatePathEditableName;

			// load all content elements from given content fragment
			this.contentElements = new ArrayList<>();
			for (Element contentElement : operators.findByOperator(root, Operator.CONTENT)) {
				this.contentElements.add(contentElement);
			}
			if (this.contentElements.isEmpty()) {
				this.contentElements.add(root);
			}
		}

		public String getTemplatePath() {
			return templatePath;
		}

		/**
		 * 
		 * @param contentElement content element from this content fragment.
		 * @return
		 */
		public String getEditableName(Element contentElement) {
			// load editable name from 'content' operator; if missing we should have editable name in the template operator
			String editableName = operators.getOperand(contentElement, Operator.CONTENT);
			if (editableName == null) {
				editableName = templatePathEditableName;
			}
			return editableName;
		}

		public boolean isRoot() {
			return root.getParent() == null;
		}

		public List<Element> getContentElements() {
			return contentElements;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Tests support

	FilePath getBaseLayoutPath() {
		return baseLayoutPath;
	}

}
