package js.wood.impl;

import js.wood.IMetaReference;

public class MetaReference implements IMetaReference {
	private String name;
	private String httpEquiv;
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
		result = prime * result + ((httpEquiv == null) ? 0 : httpEquiv.hashCode());
		result = prime * result + ((name == null) ? 0 : name.hashCode());
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
		MetaReference other = (MetaReference) obj;
		if (httpEquiv == null) {
			if (other.httpEquiv != null)
				return false;
		} else if (!httpEquiv.equals(other.httpEquiv))
			return false;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return name != null ? name : httpEquiv;
	}
}
