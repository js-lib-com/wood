package com.jslib.wood.impl;

import javax.xml.xpath.XPathExpressionException;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.EList;
import com.jslib.api.dom.Element;
import com.jslib.lang.BugError;
import com.jslib.util.Params;

/**
 * Operators handler for operator names prefixed with <code>data-</code>, HTML custom attribute prefix. This operators handler
 * is used when developer select {@link OperatorsNaming#DATA_ATTR} naming strategy.
 * 
 * @author Iulian Rotaru
 */
public class DataAttrOperatorsHandler implements IOperatorsHandler {
	/** Prefix for custom HTML attribute. */
	private static final String DATA_PREFIX = "data-";

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
		String attrName = DATA_PREFIX + operator.value();
		return element.getAttr(attrName);
	}

	@Override
	public void removeOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		element.removeAttr(DATA_PREFIX + operator.value());
	}

	private String buildXPath(Operator operator, String... operand) {
		// descendant-or-self::node()[@data-compo='res/compo/dialog']

		StringBuilder sb = new StringBuilder();
		sb.append("descendant-or-self::node()[@");
		sb.append(DATA_PREFIX);
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
