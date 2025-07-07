package com.jslib.wood.dom;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

import java.util.Iterator;

/**
 * List of elements implementation.
 *
 * @author Iulian Rotaru
 */
final class EListImpl implements EList {
    /**
     * Owner document.
     */
    private final Document ownerDoc;
    /**
     * Wrapped W3C DOM NodeList interface.
     */
    private final NodeList nodeList;

    /**
     * Construct elements list instance.
     *
     * @param ownerDoc owner document,
     * @param nodeList nodes list.
     * @throws IllegalArgumentException if any argument is null.
     */
    public EListImpl(Document ownerDoc, NodeList nodeList) throws IllegalArgumentException {
        assert ownerDoc != null : "Owner document argument is null";
        assert nodeList != null : "Node list argument is null";
        this.ownerDoc = ownerDoc;
        this.nodeList = nodeList;
    }

    @Override
    public boolean isEmpty() {
        return nodeList.getLength() == 0;
    }

    @Override
    public Element item(int index) {
        return new ElementImpl(ownerDoc, nodeList.item(index));
    }

    @Override
    public void remove() {
        while (nodeList.getLength() > 0) {
            nodeList.item(0).getParentNode().removeChild(nodeList.item(0));
        }
    }

    @Override
    public int size() {
        return nodeList.getLength();
    }

    @Override
    public Iterator<Element> iterator() {
        return new NodeListIterator();
    }

    /**
     * Nodes list iterator used by elements list.
     *
     * @author Iulian Rotaru
     */
    private class NodeListIterator implements Iterator<Element> {
        /**
         * Internal nodes index.
         */
        private int index = 0;
        /**
         * Current node.
         */
        private Node node;

        /**
         * Test if this iterator has more nodes.
         */
        public boolean hasNext() {
            if (index == nodeList.getLength()) {
                return false;
            }
            node = nodeList.item(index++);
            // there are obscure conditions when node can be null at this point
            // discovered when running on real life documents but not understood
            // is like node list can hold null nodes...
            return node != null;
        }

        /**
         * Get next node wrapped into document element.
         */
        public Element next() {
            return ((DocumentImpl) ownerDoc).getElement(node);
        }

        /**
         * Remove operation is not supported.
         */
        public void remove() {
            throw new UnsupportedOperationException("Nodes list iterator does not support node removing.");
        }
    }
}
