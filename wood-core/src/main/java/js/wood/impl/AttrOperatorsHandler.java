package js.wood.impl;

import javax.xml.xpath.XPathExpressionException;

import js.dom.Document;
import js.dom.EList;
import js.dom.Element;
import js.lang.BugError;
import js.util.Params;

/**
 * Operators handler implementation for operators with simple attribute name.
 * 
 * @author Iulian Rotaru
 */
public class AttrOperatorsHandler implements IOperatorsHandler {
	@Override
	public EList findByOperator(Document document, Operator operator) {
		Params.notNull(document, "Layout document");
		try {
			return document.findByXPath(buildAttrXPath(operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public EList findByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.findByXPath(buildAttrXPath(operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Document document, Operator operator) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPath(buildAttrXPath(operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.getByXPath(buildAttrXPath(operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public String getOperand(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		return element.getAttr(operator.value());
	}

	@Override
	public Element getByOperator(Document document, Operator operator, String operand) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPath(buildAttrXPath(operator.value(), operand));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public void removeOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		element.removeAttr(operator.value());
	}

	/**
	 * Create XPath expression for selecting element by attribute name and optional value.
	 * 
	 * @param name attribute name,
	 * @param value attribute value.
	 * @return XPath expression.
	 */
	protected static String buildAttrXPath(String name, String... value) {
		StringBuilder sb = new StringBuilder();
		sb.append("descendant-or-self::node()[@");
		sb.append(name);
		if (value.length == 1) {
			sb.append("='");
			sb.append(value[0]);
			sb.append("'");
		}
		sb.append("]");
		return sb.toString();
	}
}
