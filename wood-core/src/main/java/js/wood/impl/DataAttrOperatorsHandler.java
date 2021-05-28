package js.wood.impl;

import java.util.Map;

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
public class DataAttrOperatorsHandler implements IOperatorsHandler {
	/** Prefix for custom HTML attribute. */
	private static final String DATA_PREFIX = "data-";

	private final Map<String, String> tagCompos;

	public DataAttrOperatorsHandler(Map<String, String> tagCompos) {
		this.tagCompos = tagCompos;
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
		String attrName = DATA_PREFIX + operator.value();
		switch (operator) {
		case COMPO:
			if (!element.hasAttr(attrName)) {
				return tagCompos.get(element.getTag());
			}
			// fall through next case

		default:
			return element.getAttr(attrName);
		}
	}

	@Override
	public void removeOperator(Element element, Operator operator) {
		Params.notNull(element, "Layout element");
		element.removeAttr(DATA_PREFIX + operator.value());
	}

	private String buildXPath(Operator operator, String... operand) {
		// descendant::tag1 | descendant::tag2 | descendant-or-self::node()[@data-compo='res/compo/dialog']

		StringBuilder sb = new StringBuilder();
		if (operator == Operator.COMPO) {
			for (String tag : tagCompos.keySet()) {
				sb.append("descendant::");
				sb.append(tag);
				sb.append(" | ");
			}
		}

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
