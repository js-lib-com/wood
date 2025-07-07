package com.jslib.wood.dom;

import java.util.Iterator;

/**
 * Abstract implementation for Java XML name space context. This class implements null methods and allows to override only the
 * actual needed ones.
 * <p>
 * XML name space context is used in conjunction with XPath evaluation when expression contains name space prefixes. An XML
 * document may contain multiple name spaces and is possible to have XPath expression with multiple prefixes, for example
 * <code>books:booklist/science:book</code>. On the other hand evaluation process needs the name space URI that is the only way
 * to identify the name space since the prefix is document / user specific. This class is uses to resolve name space prefixes to
 * its mapped URI.
 * <p>
 * In sample code <code>books</code> prefix is mapped to <code>BOOKS_URI</code> and <code>science</code> prefix to
 * <code>SCIENCE_URI</code> name space URI.
 * 
 * <pre>
 * Element el = doc.getByXPathNS(new NamespaceContext() {
 * 	&#064;Override
 * 	public String getNamespaceURI(String prefix) {
 * 		if (prefix.equals(&quot;books&quot;)) {
 * 			return BOOKS_URI;
 * 		}
 * 		if (prefix.equals(&quot;science&quot;)) {
 * 			return SCIENCE_URI;
 * 		}
 * 		return null;
 * 	}
 * }, &quot;books:booklist/science:book&quot;);
 * </pre>
 * 
 * @author Iulian Rotaru
 * @version final
 */
public abstract class NamespaceContext implements javax.xml.namespace.NamespaceContext {
	/**
	 * Get namespace URI bound to a prefix in the current scope.
	 * 
	 * @param prefix prefix to look up.
	 * @return namespace URI bound to prefix in the current scope.
	 * @throws IllegalArgumentException if <code>prefix</code> is null.
	 */
	@Override
	public String getNamespaceURI(String prefix) {
		return null;
	}

	/**
	 * Get prefix bound to namespace URI in the current scope.
	 * 
	 * @param namespaceURI URI of namespace to lookup.
	 * @return prefix bound to namespace URI in current context.
	 * @throws IllegalArgumentException if <code>namespaceURI</code> is null.
	 */
	@Override
	public String getPrefix(String namespaceURI) {
		return null;
	}

	/**
	 * Get all prefixes bound to a namespace URI in the current scope.
	 * 
	 * @param namespaceURI URI of namespace to lookup.
	 * @return iterator for all prefixes bound to the namespace URI in the current scope.
	 * @throws IllegalArgumentException if <code>namespaceURI</code> is null.
	 */
	@Override
	public Iterator<String> getPrefixes(String namespaceURI) {
		return null;
	}
}
