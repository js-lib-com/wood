package js.wood.impl;

import javax.xml.xpath.XPathExpressionException;

import js.dom.Document;
import js.dom.EList;
import js.dom.Element;
import js.lang.BugError;
import js.util.Params;

/**
 * Operators handler for operator names prefixed with <code>data-</code>, HTML custom attribute prefix. This operators handler
 * is used when developer select {@link NamingStrategy#DATA_ATTR} naming strategy.
 * 
 * @author Iulian Rotaru
 */
public class DataAttrOperatorsHandler extends AttrOperatorsHandler {
	/** Prefix for custom HTML attribute. */
	private static final String DATA_PREFIX = "data-";

	@Override
	public EList findByOperator(Document document, Operator operator) {
		Params.notNull(document, "Layout document");
		try {
			return document.findByXPath(buildAttrXPath(DATA_PREFIX + operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public EList findByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.findByXPath(buildAttrXPath(DATA_PREFIX + operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Document document, Operator operator) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPath(buildAttrXPath(DATA_PREFIX + operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.getByXPath(buildAttrXPath(DATA_PREFIX + operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public String getOperand(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		return element.getAttr(DATA_PREFIX + operator.value());
	}

	@Override
	public Element getByOperator(Document document, Operator operator, String operand) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPath(buildAttrXPath(DATA_PREFIX + operator.value(), operand));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public void removeOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		element.removeAttr(DATA_PREFIX + operator.value());
	}
}
