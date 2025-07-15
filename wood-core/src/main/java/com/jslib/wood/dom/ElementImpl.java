package com.jslib.wood.dom;

import com.jslib.wood.util.StringsUtil;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Document element implementation.
 *
 * @author Iulian Rotaru
 */
final class ElementImpl implements Element {
    /**
     * Owner document.
     */
    private final DocumentImpl ownerDoc;
    /**
     * Wrapped W3C DOM Node interface.
     */
    private org.w3c.dom.Element node;

    /**
     * Construct element for W3C DOM Node.
     *
     * @param ownerDoc owner document.
     * @param node     wrapped W3C DOM Node interface.
     */
    public ElementImpl(Document ownerDoc, Node node) {
        this.ownerDoc = (DocumentImpl) ownerDoc;
        this.node = (org.w3c.dom.Element) node;
    }

    /**
     * Low level ;-) access to W3C DOM Node interface.
     *
     * @return this element wrapped node.
     */
    public Node getNode() {
        return node;
    }

    @Override
    public Element addChild(Element... child) {
        for (Element el : child) {
            assert el != null : "Element is null";
            if (el.getDocument() != ownerDoc) {
                el = ownerDoc.importElement(el);
            }
            node.appendChild(node(el));
        }
        return this;
    }

    /**
     * Attribute name for CSS class.
     */
    private static final String ATTR_CLASS = "class";

    @Override
    public Element addCssClass(String cssClass) {
        cssClass = cssClass.trim();
        if (!hasCssClass(cssClass)) {
            String existingCssClass = node.getAttribute(ATTR_CLASS);
            if (!existingCssClass.isEmpty()) {
                cssClass = existingCssClass + ' ' + cssClass;
            }
            node.setAttribute(ATTR_CLASS, cssClass);
        }
        return this;
    }

    @Override
    public Element toggleCssClass(String cssClass) {
        if (hasCssClass(cssClass)) {
            removeCssClass(cssClass);
        } else {
            addCssClass(cssClass);
        }
        return this;
    }

    @Override
    public Element clone(boolean deep) {
        return ownerDoc.getElement(node.cloneNode(deep));
    }

    @Override
    public EList findByCssClass(String cssClass) {
        assert cssClass != null && !cssClass.isEmpty() : "CSS class argument is null or empty";
        try {
            return findByXPath(XPATH.getElementsByClassName(cssClass));
        } catch (XPathExpressionException e) {
            // XPath expression is build internally and cannot fail
            throw new IllegalStateException(e);
        }
    }

    @Override
    public EList findByTag(String tagName) {
        assert tagName != null && !tagName.isEmpty() : "Tag name argument is null or empty";
        return ownerDoc.createEList(node.getElementsByTagName(tagName));
    }

    @Override
    public EList findByXPath(String xpath, Object... args) throws XPathExpressionException {
        assert xpath != null && !xpath.isEmpty() : "XPath argument is null or empty";
        return ownerDoc.createEList(XPATH.evaluateXPathNodeList(node, xpath, args));
    }

    @Override
    public EList findByXPathNS(NamespaceContext namespaceContext, String xpath, Object... args) throws XPathExpressionException {
        assert namespaceContext != null : "Namespace context argument is null";
        assert xpath != null && !xpath.isEmpty() : "XPath argument is null or empty";
        return ownerDoc.createEList(XPATH.evaluateXPathNodeListNS(node, namespaceContext, xpath, args));
    }

    @Override
    public Element getByAttr(String name, String... value) {
        try {
            return getByXPath(XPATH.getElementsByAttrNameValue(name, value));
        } catch (XPathExpressionException e) {
            // XPath expression is build internally and cannot fail
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Iterable<Attr> getAttrs() {
        List<Attr> attrs = new ArrayList<>();
        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0, l = attributes.getLength(); i < l; i++) {
            org.w3c.dom.Attr a = (org.w3c.dom.Attr) attributes.item(i);
            attrs.add(new AttrImpl(a.getNamespaceURI(), a.getNodeName(), a.getNodeValue().trim()));
        }
        return Collections.unmodifiableList(attrs);
    }

    @Override
    public String getAttr(String name) {
        assert name != null && !name.isEmpty() : "Attribute name argument is null or empty";
        String s = node.getAttribute(name);
        return s.isEmpty() ? null : s;
    }

    @Override
    public String getAttrNS(String namespaceURI, String name) {
        if (namespaceURI == null) {
            return getAttr(name);
        }
        assert name != null && !name.isEmpty() : "Attribute name argument is null or empty";
        String s = node.getAttributeNS(namespaceURI, name);
        return s.isEmpty() ? null : s;
    }

    @Override
    public Element getByCssClass(String cssClass) {
        assert cssClass != null && !cssClass.isEmpty() : "CSS class argument is null or empty";
        try {
            return ownerDoc.getElement(XPATH.evaluateXPathNode(node, XPATH.getElementsByClassName(cssClass)));
        } catch (XPathExpressionException e) {
            // XPath expression is build internally and cannot fail
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Element getByTag(String tagName) {
        assert tagName != null && !tagName.isEmpty() : "Tag name argument is null or empty";
        return ownerDoc.getElement(node.getElementsByTagName(tagName));
    }

    @Override
    public Element getByXPath(String xpath, Object... args) throws XPathExpressionException {
        assert xpath != null && !xpath.isEmpty() : "XPath argument is null or empty";
        return ownerDoc.getElement(XPATH.evaluateXPathNode(node, xpath, args));
    }

    @Override
    public Element getByXPathNS(NamespaceContext namespaceContext, String xpath, Object... args) throws XPathExpressionException {
        assert namespaceContext != null : "Namespace context argument is null";
        assert xpath != null && !xpath.isEmpty() : "XPath argument is null or empty";
        return ownerDoc.getElement(XPATH.evaluateXPathNodeNS(node, namespaceContext, xpath, args));
    }

    @Override
    public Element getByXPathNS(String namespaceURI, String xpath, Object... args) throws XPathExpressionException {
        assert namespaceURI != null && !namespaceURI.isEmpty() : "Namespace URI argument is null or empty";
        assert xpath != null && !xpath.isEmpty() : "XPath argument is null or empty";
        return ownerDoc.getElement(XPATH.evaluateXPathNodeNS(node, new NamespaceContext() {
            @Override
            public String getNamespaceURI(String prefix) {
                // it is expected to be used on documents with a single namespace
                return namespaceURI;
            }
        }, xpath, args));
    }

    @Override
    public EList getChildren() {
        NodeListImpl nodeList = new NodeListImpl();
        Node n = node.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE) {
                nodeList.add(n);
            }
            n = n.getNextSibling();
        }
        return new EListImpl(ownerDoc, nodeList);
    }

    @Override
    public Iterable<ChildNode> getChildNodes() {
        List<ChildNode> childNodes = new ArrayList<>();

        Node n = node.getFirstChild();
        while (n != null) {
            if (n.getNodeType() == Node.ELEMENT_NODE || n.getNodeType() == Node.TEXT_NODE) {
                childNodes.add(new ChildNodeImpl(n));
            }
            n = n.getNextSibling();
        }

        return childNodes;
    }

    @Override
    public Document getDocument() {
        return ownerDoc;
    }

    @Override
    public Element getFirstChild() {
        Node n = node.getFirstChild();
        if (n == null) {
            return null;
        }
        while (n.getNodeType() != Node.ELEMENT_NODE) {
            n = n.getNextSibling();
            if (n == null) {
                return null;
            }
        }
        return ownerDoc.getElement(n);
    }

    @Override
    public Element getLastChild() {
        Node n = node.getLastChild();
        if (n == null) {
            return null;
        }
        while (n.getNodeType() != Node.ELEMENT_NODE) {
            n = n.getPreviousSibling();
            if (n == null) {
                return null;
            }
        }
        return ownerDoc.getElement(n);
    }

    @Override
    public Element getNextSibling() {
        Node n = node.getNextSibling();
        if (n == null) {
            return null;
        }
        while (n.getNodeType() != Node.ELEMENT_NODE) {
            n = n.getNextSibling();
            if (n == null) {
                return null;
            }
        }
        return ownerDoc.getElement(n);
    }

    @Override
    public Element getParent() {
        Node n = node.getParentNode();
        // parent can be null if this node is not part of a document tree
        if (n == null) {
            return null;
        }
        // parent can be a document if this element is html root
        return n.getNodeType() == Node.ELEMENT_NODE ? ownerDoc.getElement(n) : null;
    }

    @Override
    public Element getPreviousSibling() {
        Node n = node.getPreviousSibling();
        if (n == null) {
            return null;
        }
        while (n.getNodeType() != Node.ELEMENT_NODE) {
            n = n.getPreviousSibling();
            if (n == null) {
                return null;
            }
        }
        return ownerDoc.getElement(n);
    }

    @Override
    public String getTag() {
        return node.getNodeName().toLowerCase();
    }

    @Override
    public Element renameElement(String tagName) {
        ownerDoc.getDocument().renameNode(node, null, tagName);
        return this;
    }

    @Override
    public String getText() {
        return node.getTextContent();
    }

    @Override
    public String getTextContent() {
        StringBuilder builder = new StringBuilder();
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            Node node = nodes.item(i);
            if (node.getNodeType() == Node.TEXT_NODE) {
                builder.append(node.getNodeValue());
            }
        }
        return builder.toString();
    }

    @Override
    public boolean hasAttr(String name) {
        return !node.getAttribute(name).isEmpty();
    }

    @Override
    public boolean hasAttrNS(String namespaceURI, String name) {
        return !node.getAttributeNS(namespaceURI, name).isEmpty();
    }

    @Override
    public boolean hasChildren() {
        NodeList nodes = node.getChildNodes();
        for (int i = 0; i < nodes.getLength(); i++) {
            if (nodes.item(i).getNodeType() == Node.ELEMENT_NODE) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean isEmpty() {
        return node.getChildNodes().getLength() == 0;
    }

    /**
     * Regular expression for leading white spaces.
     */
    private static final String LEADING_SPACE_REX = "(?:^|\\s+)";
    /**
     * Regular expression for trailing white spaces.
     */
    private static final String TRAILING_SPACE_REX = "(?:\\s+|$)";

    @Override
    public boolean hasCssClass(String classToMatch) {
        String classes = node.getAttribute(ATTR_CLASS);
        if (classes.isEmpty()) {
            return false;
        }
        Pattern pattern = Pattern.compile(StringsUtil.concat(LEADING_SPACE_REX, StringsUtil.escapeRegExp(classToMatch), TRAILING_SPACE_REX));
        Matcher matcher = pattern.matcher(classes);
        return matcher.find();
    }

    @Override
    public Element removeCssClass(String classToRemove) {
        String classes = node.getAttribute(ATTR_CLASS);
        if (classes.isEmpty()) {
            return this;
        }
        Pattern pattern = Pattern.compile(StringsUtil.concat(LEADING_SPACE_REX, StringsUtil.escapeRegExp(classToRemove), TRAILING_SPACE_REX));
        Matcher matcher = pattern.matcher(classes);
        node.setAttribute(ATTR_CLASS, matcher.replaceFirst(" ").trim());
        return this;
    }

    @Override
    public Element insertBefore(Element sibling) {
        assert sibling != null : "Sibling element argument is null";
        if (sibling.getDocument() != ownerDoc) {
            sibling = ownerDoc.importElement(sibling);
        }
        Node parent = node.getParentNode();
        if (parent == null) {
            throw new IllegalStateException("Missing parent node.");
        }
        parent.insertBefore(node(sibling), node);
        return this;
    }

    @Override
    public void remove() {
        Node parentNode = node.getParentNode();
        if (parentNode != null) {
            node.getParentNode().removeChild(node);
        }
        node = null;
    }

    @Override
    public Element removeAttr(String name) {
        assert name != null && !name.isEmpty() : "Attribute name argument is null or empty";
        node.removeAttribute(name);
        return this;
    }

    @Override
    public Element removeAttrNS(String namespaceURI, String name) {
        if (namespaceURI == null) {
            return removeAttr(name);
        }
        assert name != null && !name.isEmpty() : "Attribute name argument is null or empty";
        node.removeAttributeNS(namespaceURI, name);
        return this;
    }

    @Override
    public Element removeChildren() {
        while (node.hasChildNodes()) {
            node.removeChild(node.getFirstChild());
        }
        return this;
    }

    @Override
    public Element removeText() {
        NodeList children = node.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            Node child = children.item(i);
            if (child.getNodeType() == Node.TEXT_NODE) {
                child.getParentNode().removeChild(child);
                --i;
            }
        }
        return this;
    }

    @Override
    public void replace(Element replacement) {
        assert replacement != null : "Replacement element argument is null";
        if (replacement.getDocument() != ownerDoc) {
            replacement = ownerDoc.importElement(replacement);
        }
        node.getParentNode().replaceChild(node(replacement), node);
        node = (org.w3c.dom.Element) node(replacement);
    }

    @Override
    public Element replaceChild(Element replacement, Element existing) {
        assert replacement != null : "Replacement element argument is null";
        assert existing != null : "Existing element argument is null";
        if (replacement.getDocument() != ownerDoc) {
            replacement = ownerDoc.importElement(replacement);
        }
        node.replaceChild(node(replacement), node(existing));
        return this;
    }

    @Override
    public Element setAttr(String name, String value) {
        assert name != null && !name.isEmpty() : "Attribute name argument is null or empty";
        assert value != null : "Attribute value argument is null";
        node.setAttribute(name, value);
        return this;
    }

    @Override
    public Element setAttrNS(String namespaceURI, String name, String value) throws IllegalArgumentException {
        if (namespaceURI == null) {
            return setAttr(name, value);
        }
        assert name != null && !name.isEmpty() : "Attribute name argument is null or empty";
        assert value != null : "Attribute value argument is null";

        if (name.indexOf(':') != -1) {
            node.setAttributeNS(namespaceURI, name, value);
            return this;
        }

        NamedNodeMap attributes = node.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            final Node attribute = attributes.item(i);
            if (namespaceURI.equals(attribute.getNamespaceURI()) && name.equals(attribute.getLocalName())) {
                attribute.setNodeValue(value);
                return this;
            }
        }
        throw new IllegalArgumentException("Missing prefix from attribute name.");
    }

    @Override
    public Element setAttrs(String... nameValuePairs) {
        assert nameValuePairs.length % 2 == 0 : "Missing value for last attribute.";
        for (int i = 0, l = nameValuePairs.length - 1; i < l; i += 2) {
            assert nameValuePairs[i + 1] != null : "Attribute value argument is null";
            node.setAttribute(nameValuePairs[i], nameValuePairs[i + 1]);
        }
        return this;
    }

    @Override
    public Element addText(String text) {
        node.appendChild(ownerDoc.getDocument().createTextNode(text));
        return this;
    }

    @Override
    public Element setText(String text) {
        node.setTextContent(text);
        return this;
    }

    /**
     * Element string representation.
     */
    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append(node.getNodeName());
        NamedNodeMap attrs = node.getAttributes();
        for (int i = 0; i < attrs.getLength(); i++) {
            sb.append(' ');
            sb.append(attrs.item(i).getNodeName());
            sb.append('=');
            sb.append('\'');
            sb.append(attrs.item(i).getTextContent());
            sb.append('\'');
        }
        return sb.toString();
    }

    private static Node node(Element el) {
        return ((ElementImpl) el).node;
    }
}
