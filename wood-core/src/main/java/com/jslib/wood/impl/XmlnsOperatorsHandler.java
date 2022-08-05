package com.jslib.wood.impl;

import javax.xml.xpath.XPathExpressionException;

import com.jslib.api.dom.Document;
import com.jslib.api.dom.EList;
import com.jslib.api.dom.Element;
import com.jslib.api.dom.NamespaceContext;
import com.jslib.lang.BugError;
import com.jslib.util.Params;
import com.jslib.wood.WOOD;

/**
 * Document handler for operator naming with XML name space. This document handler is the default naming strategy or enacted
 * when user explicitly select the {@link OperatorsNaming#XMLNS}. Name space should be declared using WOOD public URI:
 * <code>xmlns:wood="js-lib.com/wood"</code>.
 * 
 * @author Iulian Rotaru
 */
public class XmlnsOperatorsHandler implements IOperatorsHandler {
	/** Name space context used by document search by XPath expression with name space. */
	private static final NamespaceContext namespaceContext = new NamespaceContext() {
		@Override
		public String getNamespaceURI(String prefix) {
			return WOOD.NS;
		}
	};

	@Override
	public EList findByOperator(Document document, Operator operator) {
		Params.notNull(document, "Layout document");
		try {
			return document.findByXPathNS(namespaceContext, buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public EList findByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.findByXPathNS(namespaceContext, buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Document document, Operator operator) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPathNS(namespaceContext, buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.getByXPathNS(namespaceContext, buildXPath(operator));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Document document, Operator operator, String operand) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPathNS(namespaceContext, buildXPath(operator, operand));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public String getOperand(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		return element.getAttrNS(WOOD.NS, operator.value());
	}

	@Override
	public void removeOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		element.removeAttrNS(WOOD.NS, operator.value());
	}

	private String buildXPath(Operator operator, String... operand) {
		// descendant-or-self::node()[@wood:compo='res/compo/dialog']

		StringBuilder sb = new StringBuilder();
		sb.append("descendant-or-self::node()[@wood:");
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
