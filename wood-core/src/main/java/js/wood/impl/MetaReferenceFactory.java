package js.wood.impl;

import js.dom.Element;
import js.wood.WoodException;

public class MetaReferenceFactory {
	public static MetaReference create(Element metaElement) {
		final String name = metaElement.getAttr("name");
		final String httpEquiv = metaElement.getAttr("http-equiv");
		final String property = metaElement.getAttr("property");
		if (name == null && httpEquiv == null && property == null) {
			throw new WoodException("Invalid descriptor. Missing 'name', 'http-equiv' or 'property' attribute from <meta> element.");
		}

		MetaReference meta = new MetaReference();
		meta.setName(name);
		meta.setHttpEquiv(httpEquiv);
		meta.setProperty(property);
		meta.setContent(metaElement.getAttr("content"));
		meta.setCharset(metaElement.getAttr("charset"));

		return meta;
	}
}
