package js.wood.impl;

import js.util.Params;
import js.wood.ILinkReference;

public class LinkReference implements ILinkReference {
	private String href;
	private String hreflang;
	private String relationship;
	private String media;
	private String referrerPolicy;
	private String disabled;
	private String type;
	private String asType;
	private String prefetch;
	private String sizes;
	private String imageSizes;
	private String imageSrcSet;
	private String title;
	private String integrity;
	private String crossorigin;

	public LinkReference(String href) {
		Params.notNullOrEmpty(href, "Style href");
		this.href = href;
	}

	@Override
	public String getHref() {
		return href;
	}

	public void setHreflang(String hreflang) {
		this.hreflang = hreflang;
	}

	@Override
	public String getHreflang() {
		return hreflang;
	}

	public void setRelationship(String relationship) {
		this.relationship = relationship;
	}

	@Override
	public String getRelationship() {
		return relationship;
	}

	public void setMedia(String media) {
		this.media = media;
	}

	@Override
	public String getMedia() {
		return media;
	}

	public void setReferrerPolicy(String referrerPolicy) {
		this.referrerPolicy = referrerPolicy;
	}

	@Override
	public String getReferrerPolicy() {
		return referrerPolicy;
	}

	public void setDisabled(String disabled) {
		this.disabled = disabled;
	}

	@Override
	public String getDisabled() {
		return disabled;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String getType() {
		return type;
	}

	public void setAsType(String asType) {
		this.asType = asType;
	}

	@Override
	public String getAsType() {
		return asType;
	}

	public void setPrefetch(String prefetch) {
		this.prefetch = prefetch;
	}

	@Override
	public String getPrefetch() {
		return prefetch;
	}

	public void setSizes(String sizes) {
		this.sizes = sizes;
	}

	@Override
	public String getSizes() {
		return sizes;
	}

	public void setImageSizes(String imageSizes) {
		this.imageSizes = imageSizes;
	}

	@Override
	public String getImageSizes() {
		return imageSizes;
	}

	public void setImageSrcSet(String imageSrcSet) {
		this.imageSrcSet = imageSrcSet;
	}

	@Override
	public String getImageSrcSet() {
		return imageSrcSet;
	}

	public void setTitle(String title) {
		this.title = title;
	}

	@Override
	public String getTitle() {
		return title;
	}

	public void setIntegrity(String integrity) {
		this.integrity = integrity;
	}

	@Override
	public String getIntegrity() {
		return integrity;
	}

	public void setCrossOrigin(String crossorigin) {
		this.crossorigin = crossorigin;
	}

	@Override
	public String getCrossOrigin() {
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
		LinkReference other = (LinkReference) obj;
		if (href == null) {
			if (other.href != null)
				return false;
		} else if (!href.equals(other.href))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return href;
	}
}