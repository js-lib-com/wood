package com.jslib.wood.impl;

import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.EList;
import com.jslib.wood.dom.Element;

import javax.xml.xpath.XPathExpressionException;

/**
 * Operators handler implementation for operators with simple attribute name.
 * 
 * @author Iulian Rotaru
 */
public class AttrOperatorsHandler implements IOperatorsHandler {
	@Override
	public EList findByOperator(Document document, Operator operator) {
		assert document != null: "Layout document argument is null";
		try {
			return document.findByXPath(buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new IllegalStateException(e);
		}
	}

	@Override
	public EList findByOperator(Element element, Operator operator) {
		assert element != null: "Layout element argument is null";
		try {
			return element.findByXPath(buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Element getByOperator(Element element, Operator operator) {
		assert element != null: "Layout element argument is null";
		try {
			return element.getByXPath(buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new IllegalStateException(e);
		}
	}

	@Override
	public Element getByOperator(Document document, Operator operator, String operand) {
		assert document != null: "Layout document argument is null";
		try {
			return document.getByXPath(buildXPath(operator, operand));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new IllegalStateException(e);
		}
	}

	@Override
	public String getOperand(Element element, Operator operator) {
		assert element != null: "Layout element argument is null";
		return element.getAttr(operator.value());
	}

	@Override
	public void removeOperator(Element element, Operator operator) {
		assert element != null: "Layout element argument is null";
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
