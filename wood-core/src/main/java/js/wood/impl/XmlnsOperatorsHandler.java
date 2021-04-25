package js.wood.impl;

import javax.xml.xpath.XPathExpressionException;

import js.dom.Document;
import js.dom.EList;
import js.dom.Element;
import js.dom.NamespaceContext;
import js.lang.BugError;
import js.util.Params;
import js.wood.WOOD;

/**
 * Document handler for operator naming with XML name space. This document handler is the default naming strategy or enacted
 * when user explicitly select the {@link NamingStrategy#XMLNS}. Name space should be declared using WOOD public URI:
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
			return document.findByXPathNS(namespaceContext, buildAttrXPath(operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public EList findByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.findByXPathNS(namespaceContext, buildAttrXPath(operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Document document, Operator operator) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPathNS(namespaceContext, buildAttrXPath(operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		try {
			return element.getByXPathNS(namespaceContext, buildAttrXPath(operator.value()));
		} catch (XPathExpressionException e) {
			// XPath expression is hard coded
			throw new BugError(e);
		}
	}

	@Override
	public Element getByOperator(Document document, Operator operator, String operand) {
		Params.notNull(document, "Layout document");
		try {
			return document.getByXPathNS(namespaceContext, buildAttrXPath(operator.value(), operand));
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

	/**
	 * Build XPath expression for attribute names with name space.
	 * 
	 * @param name attribute name,
	 * @param value optional attribute value.
	 * @return XPath expression.
	 */
	private static String buildAttrXPath(String name, String... value) {
		StringBuilder sb = new StringBuilder();
		sb.append("descendant-or-self::node()[@wood:");
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
