package com.jslib.wood.impl;

import com.jslib.wood.IMetaDescriptor;
import com.jslib.wood.dom.Element;

/**
 * Descriptor for page meta element. This class is loaded from <code>meta</code> element of project or page descriptor. All
 * standard attributes are supported.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class MetaDescriptor implements IMetaDescriptor {
	private String name;
	private String httpEquiv;
	private String property;
	private String content;
	private String charset;

	public void setName(String name) {
		this.name = name;
	}

	@Override
	public String getName() {
		return name;
	}

	public void setHttpEquiv(String httpEquiv) {
		this.httpEquiv = httpEquiv;
	}

	@Override
	public String getHttpEquiv() {
		return httpEquiv;
	}

	public void setProperty(String property) {
		this.property = property;
	}

	@Override
	public String getProperty() {
		return property;
	}

	public void setContent(String content) {
		this.content = content;
	}

	@Override
	public String getContent() {
		return content;
	}

	public void setCharset(String charset) {
		this.charset = charset;
	}

	@Override
	public String getCharset() {
		return charset;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((charset == null) ? 0 : charset.hashCode());
		result = prime * result + ((content == null) ? 0 : content.hashCode());
		result = prime * result + ((property == null) ? 0 : property.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		MetaDescriptor other = (MetaDescriptor) obj;
		if (charset == null) {
			if (other.charset != null)
				return false;
		} else if (!charset.equals(other.charset))
			return false;
		if (content == null) {
			if (other.content != null)
				return false;
		} else if (!content.equals(other.content))
			return false;
		if (property == null) {
            return other.property == null;
		} else return property.equals(other.property);
    }

	@Override
	public String toString() {
		return name != null ? name : httpEquiv;
	}

	public static MetaDescriptor create(Element metaElement) {
		MetaDescriptor meta = new MetaDescriptor();
		meta.setName(metaElement.getAttr("name"));
		meta.setHttpEquiv(metaElement.getAttr("http-equiv"));
		meta.setProperty(metaElement.getAttr("property"));
		meta.setContent(metaElement.getAttr("content"));
		meta.setCharset(metaElement.getAttr("charset"));

		return meta;
	}
}
