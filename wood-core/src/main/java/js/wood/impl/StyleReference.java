package js.wood.impl;

import js.wood.IStyleReference;

public class StyleReference implements IStyleReference {
	private final String href;
	private final String integrity;
	private final String crossorigin;

	public StyleReference(String href, String integrity, String crossorigin) {

		this.href = href;
		this.integrity = integrity;
		this.crossorigin = crossorigin;
	}

	@Override
	public String getHref() {
		return href;
	}

	@Override
	public boolean hasIntegrity() {
		return integrity != null;
	}

	@Override
	public String getIntegrity() {
		return integrity;
	}

	@Override
	public boolean hasCrossorigin() {
		return crossorigin != null;
	}

	@Override
	public String getCrossorigin() {
		return crossorigin;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((href == null) ? 0 : href.hashCode());
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
		StyleReference other = (StyleReference) obj;
		if (href == null) {
			if (other.href != null)
				return false;
		} else if (!href.equals(other.href))
			return false;
		return true;
	}

}