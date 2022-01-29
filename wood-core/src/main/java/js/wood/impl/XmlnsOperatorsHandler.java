package js.wood.impl;

import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import js.dom.Document;
import js.dom.EList;
import js.dom.Element;
import js.dom.NamespaceContext;
import js.lang.BugError;
import js.util.Params;
import js.wood.CompoPath;
import js.wood.WOOD;

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

	private final Map<String, CompoPath> tagCompoPaths;
	private final Map<String, CompoPath> tagTemplatePaths;

	public XmlnsOperatorsHandler(Map<String, CompoPath> tagCompoPaths, Map<String, CompoPath> tagTemplatePaths) {
		this.tagCompoPaths = tagCompoPaths;
		this.tagTemplatePaths = tagTemplatePaths;
	}

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
		switch (operator) {
		case COMPO:
			if (!element.hasAttrNS(WOOD.NS, operator.value())) {
				CompoPath compoPath = tagCompoPaths.get(element.getTag());
				return compoPath != null ? compoPath.value() : null;
			}
			return element.getAttrNS(WOOD.NS, operator.value());

		case TEMPLATE:
			if (element.getParent() != null && !element.hasAttrNS(WOOD.NS, operator.value())) {
				CompoPath compoPath = tagTemplatePaths.get(element.getTag());
				return compoPath != null ? compoPath.value() : null;
			}
			return element.getAttrNS(WOOD.NS, operator.value());

		default:
			return element.getAttrNS(WOOD.NS, operator.value());
		}
	}

	@Override
	public void removeOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		element.removeAttrNS(WOOD.NS, operator.value());
	}

	private String buildXPath(Operator operator, String... operand) {
		// descendant::tag1 | descendant::tag2 | descendant-or-self::node()[@wood:compo='res/compo/dialog']

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
