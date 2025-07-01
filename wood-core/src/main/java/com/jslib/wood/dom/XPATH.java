package com.jslib.wood.dom;

import com.jslib.wood.util.StringsUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPath;
import javax.xml.xpath.XPathConstants;
import javax.xml.xpath.XPathExpressionException;
import javax.xml.xpath.XPathFactory;

/**
 * Constant XPath expressions used internally by DOM package.
 *
 * @author Iulian Rotaru
 */
final class XPATH {
    private static final Logger log = LoggerFactory.getLogger(XPATH.class);

    /**
     * Build XPath expression for elements with attribute name and optional value.
     *
     * @param name  attribute name,
     * @param value optional attribute value.
     * @return XPath expression.
     */
    static String getElementsByAttrNameValue(String name, String... value) {
        StringBuilder xpath = new StringBuilder();
        xpath.append("descendant-or-self::node()");
        xpath.append("[@");
        xpath.append(name);
        if (value.length == 1) {
            xpath.append("='");
            xpath.append(value[0]);
            xpath.append("'");
        }
        xpath.append("]");
        return xpath.toString();
    }

    /**
     * Build XPath expression for elements with class name. Element class is and attribute with the name 'class' that can
     * contain space separated class names.
     *
     * @param className class name.
     * @return XPath expression.
     */
    static String getElementsByClassName(String className) {
        StringBuilder xpath = new StringBuilder();
        xpath.append("descendant-or-self::node()");
        xpath.append("[contains(");
        xpath.append("concat(' ', normalize-space(@class), ' '),");
        xpath.append("' ");
        xpath.append(className);
        xpath.append(" '");
        xpath.append(")]");
        return xpath.toString();
    }

    /**
     * Evaluate XPath expression expecting a single result node.
     *
     * @param contextNode evaluation context node,
     * @param expression  XPath expression, formatting tags supported,
     * @param args        optional formatting arguments.
     * @return evaluation result as element, possible null.
     * @throws XPathExpressionException if expression cannot be evaluated.
     */
    static Node evaluateXPathNode(Node contextNode, String expression, Object... args) throws XPathExpressionException {
        return XPATH.evaluateXPathNodeNS(contextNode, null, expression, args);
    }

    /**
     * Namespace aware variant for {@link #evaluateXPathNode(Node, String, Object...)}.
     *
     * @param contextNode      evaluation context node,
     * @param namespaceContext namespace context maps prefixes to namespace URIs,
     * @param expression       XPath expression, formatting tags supported,
     * @param args             optional formatting arguments.
     * @return evaluation result as element, possible null.
     * @throws XPathExpressionException if expression cannot be evaluated.
     */
    static Node evaluateXPathNodeNS(Node contextNode, NamespaceContext namespaceContext, String expression, Object... args) throws XPathExpressionException {
        if (args.length > 0) {
            expression = StringsUtil.format(expression, args);
        }

        Node node;
        XPath xpath = XPathFactory.newInstance().newXPath();
        if (namespaceContext != null) {
            xpath.setNamespaceContext(namespaceContext);
        }
        Object result = xpath.evaluate(expression, contextNode, XPathConstants.NODE);
        if (result == null) {
            return null;
        }
        node = (Node) result;
        if (node.getNodeType() != Node.ELEMENT_NODE) {
            log.debug("XPath expression {} on {} yields a node that is not element. Force to null.", xpath, contextNode);
            return null;
        }
        return node;
    }

    /**
     * Evaluate XPath expression expected to return nodes list. Evaluate expression and return result nodes as elements
     * list.
     *
     * @param contextNode evaluation context node,
     * @param expression  XPath expression, formatting tags supported,
     * @param args        optional formatting arguments.
     * @return list of result elements, possible empty.
     * @throws XPathExpressionException if expression cannot be evaluated.
     */
    static NodeList evaluateXPathNodeList(Node contextNode, String expression, Object... args) throws XPathExpressionException {
        return XPATH.evaluateXPathNodeListNS(contextNode, null, expression, args);
    }

    /**
     * Namespace aware variant of {@link #evaluateXPathNodeList(Node, String, Object...)}.
     *
     * @param contextNode      evaluation context node,
     * @param namespaceContext namespace context maps prefixes to namespace URIs,
     * @param expression       XPath expression with optional formatting tags,
     * @param args             optional formatting arguments.
     * @return list of result elements, possible empty.
     * @throws XPathExpressionException if expression cannot be evaluated.
     */
    static NodeList evaluateXPathNodeListNS(Node contextNode, NamespaceContext namespaceContext, String expression, Object... args) throws XPathExpressionException {
        if (args.length > 0) {
            expression = StringsUtil.format(expression, args);
        }

        NodeList nodeList = null;
        XPath xpath = XPathFactory.newInstance().newXPath();
        if (namespaceContext != null) {
            xpath.setNamespaceContext(namespaceContext);
        }
        Object result = xpath.evaluate(expression, contextNode, XPathConstants.NODESET);
        if (result != null) {
            nodeList = (NodeList) result;
        }

        if (nodeList == null) {
            nodeList = EMPTY_NODE_LIST;
        }
        return nodeList;
    }

    /**
     * Empty nodes list constant.
     */
    private static final NodeList EMPTY_NODE_LIST = new EmptyNodeList();

    /**
     * Empty node list returned by <code>find</code> methods when no item fulfill select criteria.
     *
     * @author Iulian Rotaru
     */
    private static class EmptyNodeList implements NodeList {
        /**
         * Since this node list implementation is always empty this operation is unsupported.
         *
         * @throws UnsupportedOperationException this operation is not supported.
         */
        @Override
        public Node item(int index) throws UnsupportedOperationException {
            throw new UnsupportedOperationException();
        }

        /**
         * Always returns zero to indicate this node list is empty.
         *
         * @return always zero.
         */
        @Override
        public int getLength() {
            return 0;
        }
    }

    /**
     * Forbid default constructor synthesis.
     */
    private XPATH() {
    }
}
