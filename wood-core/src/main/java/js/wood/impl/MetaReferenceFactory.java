package js.wood.impl;

import js.dom.Element;
import js.wood.WoodException;

public class MetaReferenceFactory {
	public static MetaReference create(Element metaElement) {
		final String name = metaElement.getAttr("name");
		final String httpEquiv = metaElement.getAttr("http-equiv");
		if (name == null && httpEquiv == null) {
			throw new WoodException("Invalid descriptor. Missing 'name' or 'http-equiv' attribute from <meta> element.");
		}

		MetaReference meta = new MetaReference();
		meta.setName(name);
		meta.setHttpEquiv(httpEquiv);
		meta.setContent(metaElement.getAttr("content"));
		meta.setCharset(metaElement.getAttr("charset"));

		return meta;
	}
}
