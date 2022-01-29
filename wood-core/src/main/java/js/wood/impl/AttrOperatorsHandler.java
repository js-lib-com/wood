package js.wood.impl;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import js.dom.Document;
import js.dom.EList;
import js.dom.Element;
import js.lang.BugError;
import js.util.Params;
import js.wood.CompoPath;

/**
 * Operators handler implementation for operators with simple attribute name.
 * 
 * @author Iulian Rotaru
 */
public class AttrOperatorsHandler implements IOperatorsHandler {
	private final Map<String, CompoPath> tagCompoPaths;
	private final Map<String, CompoPath> tagTemplatePaths;

	public AttrOperatorsHandler(Map<String, CompoPath> tagCompoPaths, Map<String, CompoPath> tagTemplatePaths) {
		this.tagCompoPaths = tagCompoPaths;
		this.tagTemplatePaths = tagTemplatePaths;
	}

	@Override
	public EList findByOperator(Document document, Operator operator) {
		Params.notNull(document, "Layout document");
		try {
			return document.findByXPath(buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public EList findByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.findByXPath(buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Document document, Operator operator) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPath(buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.getByXPath(buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Document document, Operator operator, String operand) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPath(buildXPath(operator, operand));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public String getOperand(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		switch (operator) {
		case COMPO:
			if (!element.hasAttr(operator.value())) {
				CompoPath compoPath = tagCompoPaths.get(element.getTag());
				return compoPath != null ? compoPath.value() : null;
			}
			return element.getAttr(operator.value());

		case TEMPLATE:
			if (!element.hasAttr(operator.value())) {
				CompoPath compoPath = tagTemplatePaths.get(element.getTag());
				return compoPath != null ? compoPath.value() : null;
			}
			return element.getAttr(operator.value());

		default:
			return element.getAttr(operator.value());
		}
	}

	@Override
	public void removeOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		element.removeAttr(operator.value());
	}

	private String buildXPath(Operator operator, String... operand) {
		// descendant::tag1 | descendant::tag2 | descendant-or-self::node()[@compo='res/compo/dialog']

		StringBuilder sb = new StringBuilder();
		if (operator == Operator.COMPO) {
			for (String tag : tagCompoPaths.keySet()) {
				sb.append("descendant::");
				sb.append(tag);
				sb.append(" | ");
			}
		}
		if (operator == Operator.TEMPLATE) {
			for (String tag : tagTemplatePaths.keySet()) {
				sb.append("descendant::");
				sb.append(tag);
				sb.append(" | ");
			}
		}

		sb.append("descendant-or-self::node()[@");
		sb.append(operator.value());
		if (operand.length == 1) {
			sb.append("='");
			sb.append(operand[0]);
			sb.append("'");
		}
		sb.append("]");

		return sb.toString();
	}
}
