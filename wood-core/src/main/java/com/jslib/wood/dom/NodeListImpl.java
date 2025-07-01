package com.jslib.wood.dom;

import java.util.ArrayList;
import java.util.List;

import org.w3c.dom.Node;
import org.w3c.dom.NodeList;

/**
 * Nodes list.
 * 
 * @author Iulian Rotaru
 */
final class NodeListImpl implements NodeList {
	/** Internal list of nodes. */
	private final List<Node> nodes = new ArrayList<>();

	/**
	 * Add node to this list.
	 * 
	 * @param node W3C DOM node to add.
	 */
	public void add(Node node) {
		nodes.add(node);
	}

	/**
	 * Get this nodes list length.
	 */
	@Override
	public int getLength() {
		return nodes.size();
	}

	/**
	 * Get node from position.
	 * 
	 * @param index node position.
	 */
	@Override
	public Node item(int index) {
		return nodes.get(index);
	}
}
