package js.wood.impl;

import java.util.ArrayList;
import java.util.List;

import js.dom.Element;

/**
 * Content fragment is a tree of HTML elements used to resolve template editables. It has a single root element and one or many
 * content elements, that are direct children of the root element. Root element has {@link Operator#TEMPLATE} operator to
 * declare the path to the template; this path is always relative to project root.
 * <p>
 * Content element has {@link Operator#CONTENT} operator that declare the editable region name, where it should be injected.
 * Content element and its descendants replaces the template editable element, with attributes merge.
 * <p>
 * It is legal for content fragment to not have editable area in which case entire fragment is treated as editable area.
 * 
 * @author Iulian Rotaru
 * @since 1.0.4
 */
public class ContentFragment {
	private final IOperatorsHandler operators;
	private final Element root;
	/**
	 * Template path as declared by {@link Operator#TEMPLATE} operator from root. It is the path to the template resolved by
	 * this content fragment.
	 */
	private final String templatePath;
	/**
	 * Editable region name as declared by {@link Operator#TEMPLATE} operator from this content fragment root. This property is
	 * optional with default to null. Anyway, if there is a single content element that do not have {@link Operator#CONTENT}
	 * operator this value is mandatory.
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
