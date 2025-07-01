package com.jslib.wood.dom;

/**
 * An element attribute is a name / value tuple, both strings. For documents supporting namespaces attribute may have a
 * namespace URI.
 *
 * @author Iulian Rotaru
 * @version final
 */
public interface Attr {
    /**
     * Optional namespace URI used on documents with namespace support. On XML source attribute with namespace URI uses an
     * alias name prefix, for brevity - e.g. <code>wood</code>. Prefix is mapped on namespace URI by namespace
     * declaration, <code>xmlns:wood="js-lib.com/wood"</code>. This getter returns <code>js-lib.com/wood</code>.
     *
     * @return namespace URI or null.
     */
    String getNamespaceURI();

    /**
     * Get attribute name. Implementation should guarantee that returned string is not null and is trimmed.
     *
     * @return attribute name.
     */
    String getName();

    /**
     * Get attribute value. Implementation should guarantee that returned string is not null and is trimmed.
     *
     * @return attribute value.
     */
    String getValue();
}
