package com.jslib.wood.dom;

import org.apache.html.dom.HTMLDocumentImpl;
import org.w3c.dom.NamedNodeMap;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import javax.xml.xpath.XPathExpressionException;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.io.StringWriter;
import java.io.Writer;
import java.nio.charset.StandardCharsets;


/**
 * Master document implementation.
 *
 * @author Iulian Rotaru
 */
final class DocumentImpl implements Document {
    /**
     * Back reference key is used to store element instance to W3C node, as user defined data.
     */
    private static final String BACK_REF = "__js_element__";

    /**
     * Wrapped W3C DOM document object.
     */
    private final org.w3c.dom.Document doc;

    /**
     * Construct document object wrapping native W3C DOM document.
     *
     * @param doc native DOM document.
     */
    public DocumentImpl(org.w3c.dom.Document doc) {
        this.doc = doc;
    }

    @Override
    public boolean isXML() {
        return !(doc instanceof HTMLDocumentImpl);
    }

    /**
     * Get the element associated to node. Returns the element bound to given node. If no element instance found, create a
     * new {@link Element} instance, bound it to node then returns it. Returns null is given node is undefined or null.
     * <p>
     * Element instance is saved on node using {@link Node#setUserData(String, Object, org.w3c.dom.UserDataHandler)} and
     * reused. See {@link #BACK_REF} for key used to store element instance.
     *
     * @param node native W3C DOM Node.
     * @return element wrapping the given node or null.
     */
    Element getElement(Node node) {
        if (node == null) {
            return null;
        }
        // element instance is cached as node user defined data and reused
        Object value = node.getUserData(BACK_REF);
        if (value != null) {
            return (Element) value;
        }
        Element el = new ElementImpl(this, node);
        node.setUserData(BACK_REF, el, null);
        return el;
    }

    /**
     * Overload of the {@link #getElement(Node)} method using first node from given W3C DOM nodes list. Returns null if
     * <code>nodeList</code> parameter is empty.
     *
     * @param nodeList native DOM nodes list, possible empty.
     * @return element instance or null.
     * @throws IllegalArgumentException if nodes list parameter is null.
     */
    Element getElement(NodeList nodeList) {
        assert nodeList != null : "Nodes list argument is null";
        if (nodeList.getLength() == 0) {
            return null;
        }
        return getElement(nodeList.item(0));
    }

    /**
     * Elements list factory. Create a new list of elements wrapping native W3C DOM nodes. If <code>nodeList</code>
     * parameter has no items returned elements list is empty.
     *
     * @param nodeList native DOM nodes list.
     * @return newly created elements list, possible empty.
     * @throws IllegalArgumentException if nodes list parameter is null.
     */
    EList createEList(NodeList nodeList) {
        assert nodeList != null : "Nodes list argument is null";
        return new EListImpl(this, nodeList);
    }

    /**
     * Low level ;-) access to W3C DOM Document interface.
     *
     * @return wrapped W3C DOM document.
     */
    public org.w3c.dom.Document getDocument() {
        return doc;
    }

    @Override
    public Element createElement(String tagName, String... attrNameValues) {
        assert tagName != null && !tagName.isEmpty() : "Tag name argument is null or empty";
        assert attrNameValues.length % 2 == 0 : "Missing value for last attribute.";

        Element el = getElement(doc.createElement(tagName));
        if (attrNameValues.length > 0) {
            el.setAttrs(attrNameValues);
        }
        return el;
    }

    @Override
    public Element importElement(Element el) {
        assert el != null : "Element argument is null";
        assert el.getDocument() != this : "Element already belongs to this document.";
        return getElement(doc.importNode(((ElementImpl) el).getNode(), true));
    }

    @Override
    public Element getRoot() {
        return getElement(doc.getDocumentElement());
    }

    @Override
    public Element getById(String id) {
        assert id != null && !id.isEmpty() : "ID argument is null";
        return getElement(doc.getElementById(id));
    }

    @Override
    public Element getByTag(String tagName) {
        assert tagName != null && !tagName.isEmpty() : "Tag name argument is null or empty";
        return getElement(doc.getElementsByTagName(tagName));
    }

    @Override
    public EList findByCssClass(String cssClass) {
        assert cssClass != null : "CSS class argument is null";
        if (cssClass.isEmpty()) {
            return createEList(new NodeListImpl());
        }
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
        return createEList(doc.getElementsByTagName(tagName));
    }

    @Override
    public EList findByXPath(String xpath, Object... args) throws XPathExpressionException {
        assert xpath != null && !xpath.isEmpty() : "XPath argument is null or empty";
        return createEList(XPATH.evaluateXPathNodeList(doc, xpath, args));
    }

    @Override
    public EList findByXPathNS(NamespaceContext namespaceContext, String xpath, Object... args) throws XPathExpressionException {
        assert namespaceContext != null : "Namespace context argument is null";
        assert xpath != null && !xpath.isEmpty() : "XPath argument is null or empty";
        return createEList(XPATH.evaluateXPathNodeListNS(doc, namespaceContext, xpath, args));
    }

    @Override
    public Element getByCssClass(String cssClass) {
        assert cssClass != null && !cssClass.isEmpty() : "CSS class argument is null or empty";

        try {
            return getByXPath(XPATH.getElementsByClassName(cssClass));
        } catch (XPathExpressionException e) {
            // XPath expression is build internally and cannot fail
            throw new IllegalStateException(e);
        }
    }

    @Override
    public Element getByXPath(String xpath, Object... args) throws XPathExpressionException {
        assert xpath != null && !xpath.isEmpty() : "XPath argument is null or empty";
        return getElement(XPATH.evaluateXPathNode(doc, xpath, args));
    }

    @Override
    public Element getByXPathNS(NamespaceContext namespaceContext, String xpath, Object... args) throws XPathExpressionException {
        assert namespaceContext != null : "Namespace context argument is null";
        assert xpath != null && !xpath.isEmpty() : "XPath argument is null or empty";
        return getElement(XPATH.evaluateXPathNodeNS(doc, namespaceContext, xpath, args));
    }

    @Override
    public void dump() {
        try {
            Serializer serializer = new Serializer(new OutputStreamWriter(System.out, StandardCharsets.UTF_8));
            serializer.serialize(this);
        } catch (Exception e) {
            // hard to believe standard out will fail to write
            throw new IllegalStateException(e);
        }
    }

    @Override
    public void serialize(Writer writer, Object... flags) throws IOException {
        boolean closeWriter = flags.length > 0 && (boolean) flags[0];
        boolean xmlDeclaration = flags.length <= 1 || (boolean) flags[1];

        Serializer serializer = new Serializer(writer);
        serializer.setXmlDeclaration(xmlDeclaration);

        if (closeWriter) {
            try {
                serializer.serialize(this);
            } finally {
                writer.close();
            }
        } else {
            serializer.serialize(this);
        }
    }

    @Override
    public void removeNamespaceDeclaration(String namespaceURI) {
        assert namespaceURI != null && !namespaceURI.isEmpty() : "Namespace URI argument is null or empty";
        removeNamespaceDeclarations(doc.getDocumentElement(), namespaceURI);
    }

    /**
     * Recursively search for namespace declaration on requested URI and remove it. Iterate all element attributes for one
     * with name beginning with <code>xmlns:</code> and value equal to requested namespace URI. If found remove the
     * attribute and break iteration loop since an element can have a single namespace declaration for a given URI.
     * <p>
     * After processing current element attributes continue recursively with child elements.
     *
     * @param element      current element.
     * @param namespaceURI namespace URI used by namespace declaration.
     */
    private static void removeNamespaceDeclarations(org.w3c.dom.Element element, String namespaceURI) {
        NamedNodeMap attributes = element.getAttributes();
        for (int i = 0; i < attributes.getLength(); ++i) {
            final Node attribute = attributes.item(i);
            final String name = attribute.getNodeName();
            if (namespaceURI.equals(attribute.getNodeValue()) && name.startsWith("xmlns:")) {
                // an element can have only one declaration for specific namespace URI
                element.removeAttribute(name);
                break;
            }
        }

        NodeList children = element.getChildNodes();
        for (int i = 0; i < children.getLength(); ++i) {
            final Node child = children.item(i);
            if (child instanceof org.w3c.dom.Element) {
                removeNamespaceDeclarations((org.w3c.dom.Element) child, namespaceURI);
            }
        }
    }

    @Override
    public String stringify() {
        try (StringWriter writer = new StringWriter()) {
            serialize(writer);
            return writer.toString();
        } catch (IOException e) {
            // there is no reason for string writer to have IO fails
            throw new IllegalStateException(e);
        }
    }
}
