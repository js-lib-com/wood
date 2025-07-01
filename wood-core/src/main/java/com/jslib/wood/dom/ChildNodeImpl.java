package com.jslib.wood.dom;

import org.w3c.dom.Node;

class ChildNodeImpl implements ChildNode {
    private final Node node;

	public ChildNodeImpl(Node node) {
        this.node = node;
	}

	@Override
	public boolean isElement() {
		return node.getNodeType() == Node.ELEMENT_NODE;
	}

	@Override
	public boolean isText() {
		return node.getNodeType() == Node.TEXT_NODE;
	}
}
