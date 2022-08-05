package com.jslib.wood.impl;

import javax.xml.xpath.XPathExpressionException;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.EList;
import com.jslib.api.dom.Element;
import com.jslib.lang.BugError;
import com.jslib.util.Params;

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
		return element.getAttr(operator.value());
	}

	@Override
	public void removeOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		element.removeAttr(operator.value());
	}

	private String buildXPath(Operator operator, String... operand) {
		// descendant-or-self::node()[@compo='res/compo/dialog']

		StringBuilder sb = new StringBuilder();
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
