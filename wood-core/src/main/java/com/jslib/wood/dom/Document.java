package com.jslib.wood.dom;

import java.io.IOException;
import java.io.Writer;

import javax.xml.xpath.XPathExpressionException;

/**
 * Document interface is a simplified morph of W3C DOM Document. Basically a document is a tree of elements with a
 * unique root. It supplies getter for root element and methods for element creation, import and searching.
 * <p>
 * All search operations are performed using depth-first algorithm, i.e. starts from root and explores as far as possible
 * along each branch before backtracking. There are basically two kinds of search: <code>getBy</code> and
 * <code>findBy</code>. First always returns an {@link Element} or null while the second returns {@link EList}, possible
 * empty. One can use {@link EList#isEmpty} to check if <code>findBy</code> actually found something.
 * <p>
 * Where reasonable, search methods has variant for namespace. As stated not all search methods have support for name
 * space; for example searching by CSS class has no namespace since <code>class</code> attribute name is only in global
 * scope. All methods handling namespaces follows W3C DOM convention, i.e. has the name suffixed with <code>NS</code>.
 * First parameter is always namespace. If namespace parameter is null, namespace aware methods degenerate to their not
 * namespace counterpart, e.g. <code>getByTagNS(null, "p")</code> delegates <code>getByTag("p")</code>.
 * <p>
 * If document is parsed without namespace support, element 'NS' getters always returns null and 'NS' finders always
 * return empty list. Also, on document without namespace support, element and attribute names include prefix. For
 * example, <code>ns:name</code> element has null namespace and local name 'ns:name'. When searching for element use
 * 'ns:name' for name and search method without <code>NS</code> suffix.
 *
 * @author Iulian Rotaru
 */
public interface Document {
    /**
     * Test if this document is an XML instance.
     *
     * @return true if this document is XML.
     */
    boolean isXML();

    /**
     * Create element and set attributes. Create an element of requested tag owned by this document. Also set attributes
     * values if optional attribute name/value pairs are present; throws illegal argument if a name/value is incomplete,
     * i.e. odd number of arguments. It is user code responsibility to supply attribute name and value in proper order.
     * <p>
     * Note that newly created element is not part of document tree until explicitly add or insert it as a child to a
     * parent. So, elements creation follows the same W3C DOM pattern: create the element then add it as a child.
     *
     * <pre>
     * Element p = doc.createElement(&quot;p&quot;, &quot;id&quot;, &quot;paragraph-id&quot;, &quot;title&quot;, &quot;tooltip description&quot;);
     * body.addChild(p);
     * </pre>
     *
     * @param tagName        tag name for element to be created,
     * @param attrNameValues optional pairs of attribute name followed by value.
     * @return newly created element.
     * @throws IllegalArgumentException if <code>attrNameValues</code> parameter is not complete.
     * @see Element#setAttr(String, String)
     * @see Element#setAttrs(String...)
     */
    Element createElement(String tagName, String... attrNameValues) throws IllegalArgumentException;

    /**
     * Import element. Import an element that belongs to another document and return it. Note that newly imported element
     * is not part of this document tree until explicitly appended or inserted to a parent element. Also import is always
     * deep, that is, element children are imported too.
     *
     * <pre>
     * Element el = doc.importElement(foreignDoc.getByTag(&quot;Luke Skywalker&quot;));
     * doc.getByTag(&quot;Dark Vader&quot;).addChild(el);
     * </pre>
     *
     * @param el foreign element.
     * @return newly imported element.
     */
    Element importElement(Element el);

    /**
     * Retrieve the root of this document tree.
     *
     * @return this document root.
     */
    Element getRoot();

    /**
     * Get the element with specified ID. This method looks for an attribute with type ID, usually named <code>id</code>.
     * Attribute type is set at document validation using DTD or schema information. Trying to use this method on a
     * document without schema always returns null.
     *
     * @param id element ID to look for.
     * @return element with specified ID or null.
     */
    Element getById(String id);

    /**
     * Search entire document for elements with given tag name and return the first element found. Returns null if there
     * is no element with requested tag name. Note that wild card asterisk (*) matches all tags in which case first child
     * is returned.
     * <p>
     * On XML documents tag name is case-sensitive whereas in HTML is not. For consistency’s sake is recommended to always
     * consider tags name as case-sensitive. Also, if document contains namespaces but is parsed without namespace
     * support, tag name for namespace elements should use namespace prefix.
     *
     * @param tagName case-sensitive tag name to search for.
     * @return first element with specified tag or null.
     */
    Element getByTag(String tagName);

    /**
     * Find elements by tag. Return all elements from this document having specified tag name. Returns empty list if there
     * is no element with requested tag name. Note that wild card asterisk (*) for tag name matches all tags in which
     * case all elements are returned.
     * <p>
     * On XML documents tag name is case-sensitive whereas in HTML is not. For consistency’s sake is recommended to always
     * consider tags name as case-sensitive. If document contains namespaces but is parsed without namespace support, tag
     * name for namespace elements should use namespace prefix.
     *
     * @param tagName tag name to search for, case-sensitive.
     * @return list of found elements, possible empty.
     */
    EList findByTag(String tagName);

    /**
     * Get element by XPath. Evaluate XPath expression and return first element found. Returns null if XPath evaluation
     * has no results. Note that XPath expression is case-sensitive; this is especially relevant for HTML documents, that
     * uses upper case for tag names.
     * <p>
     * XPath expression <code>xpath</code> can be formatted as supported by {@link String#format} in which case
     * <code>args</code> arguments should be supplied.
     *
     * @param xpath XPath expression to evaluate,
     * @param args  optional arguments if <code>xpath</code> is formatted.
     * @return first element found or null.
     * @throws XPathExpressionException if given XPath expression is not valid.
     */
    Element getByXPath(String xpath, Object... args) throws XPathExpressionException;

    /**
     * Evaluate XPath expression with namespace prefixes and return first element found. Returns null if XPath evaluation
     * has no results. Note that XPath expression is case-sensitive; this is especially relevant for HTML documents that
     * uses upper case for tag names.
     * <p>
     * Name space context maps prefixes to namespace URI. See {@link NamespaceContext} for a discussion about expressions
     * with namespace.
     * <p>
     * XPath expression <code>xpath</code> can be formatted as supported by {@link String#format} in which case
     * <code>args</code> arguments should be supplied.
     *
     * @param namespaceContext namespace context maps prefixes to namespace URI,
     * @param xpath            XPath expression to evaluate,
     * @param args             optional arguments if <code>xpath</code> is formatted.
     * @return first element found or null.
     * @throws XPathExpressionException if given XPath expression is not valid.
     */
    Element getByXPathNS(NamespaceContext namespaceContext, String xpath, Object... args) throws XPathExpressionException;

    /**
     * Evaluate XPath expression and return the list of found elements. Returns empty list if XPath evaluation has no
     * results. Note that XPath expression is case-sensitive; this is especially relevant for HTML documents that uses
     * upper case for tag names.
     * <p>
     * XPath expression <code>xpath</code> can be formatted as supported by {@link String#format} in which case
     * <code>args</code> arguments should be supplied.
     *
     * @param xpath XPath expression to evaluate,
     * @param args  optional arguments if <code>xpath</code> is formatted.
     * @return list of found elements, possible empty.
     * @throws XPathExpressionException if given XPath expression is not valid.
     */
    EList findByXPath(String xpath, Object... args) throws XPathExpressionException;

    /**
     * Evaluate XPath expression with namespace prefixes and return the list of found elements. Returns empty list if
     * XPath evaluation has no results. Note that XPath expression is case-sensitive; this is especially relevant for HTML
     * documents that uses upper case for tag names.
     * <p>
     * Name space context maps prefixes to namespace URI. See {@link NamespaceContext} for a discussion about expressions
     * with namespace.
     * <p>
     * XPath expression <code>xpath</code> can be formatted as supported by {@link String#format} in which case
     * <code>args</code> arguments should be supplied.
     *
     * @param namespaceContext namespace context,
     * @param xpath            XPath expression to evaluate,
     * @param args             optional arguments if <code>xpath</code> is formatted.
     * @return list of found elements, possible empty.
     * @throws XPathExpressionException if given XPath expression is not valid.
     */
    EList findByXPathNS(NamespaceContext namespaceContext, String xpath, Object... args) throws XPathExpressionException;

    /**
     * Get element by CSS class. Retrieve first element possessing requested CSS class. Returns null if there is no
     * element with such CSS class.
     *
     * @param cssClass CSS class to search for.
     * @return found element or null.
     */
    Element getByCssClass(String cssClass);

    /**
     * Find elements by CSS class. Retrieve elements possessing given CSS class. Returns empty list if given CSS class is
     * null or empty or there is no element with such CSS class.
     *
     * @param cssClass CSS class to search for.
     * @return list of found elements, possible empty.
     */
    EList findByCssClass(String cssClass);

    /**
     * Serialize this document to standard output. Mainly for quick debugging.
     */
    void dump();

    /**
     * Serialize this document and optionally close destination writer. Serialize this document to given writer. If
     * optional <code>close-writer</code> flag is present and is true closes destination writer after serialization
     * completes.
     * <p>
     * This method supports a variable number of optional flags. All flags have sensible default value and can be missing.
     * Also flags order matters. Here are the flags supported by current implementation:
     * <ul>
     * <li>close-write: boolean flag, default to false. If true close the writer after serialization complete.
     * <li>xml-declaration: boolean flag, default to true. Controls if XML declaration is included before document root.
     * If this flag is false XML declaration is not included into serialized XML stream.
     * </ul>
     * It is the caller responsibility to provide correct flags order and type.
     *
     * @param writer destination writer,
     * @param flags  variable number of optional flags.
     * @throws IOException if writing operation fails.
     */
    void serialize(Writer writer, Object... flags) throws IOException;

    /**
     * Remove namespace declaration for requested namespace URI. Usually there is a single namespace declaration on an XML
     * document. Anyway, if there are multiple declarations for the same namespace URI this method remove them all. This
     * method has effect only on documents with support for namespace.
     * <p>
     * Namespace declaration is an element attribute with special syntax:
     * <code>xmlns-literal:user-defined-prefix="namespace-uri"</code>, where xmlns-literal is the reserved keyword
     * <code>xmlns</code> plus prefix that is user defined. Namespace declaration creates a bound between prefix and
     * namespace URI. Although on XML source author uses prefix for brevity on for element and attribute names, internally
     * DOM keeps the namespace URI to avoid dependency on user defined prefix.
     *
     * @param namespaceURI namespace URI.
     * @since 1.2
     */
    void removeNamespaceDeclaration(String namespaceURI);

    /**
     * Handy utility for document serialization on string, mostly for testing and debugging.
     *
     * @return document string representation.
     */
    String stringify();
}
