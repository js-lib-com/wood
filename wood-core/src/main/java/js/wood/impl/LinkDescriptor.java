package js.wood.impl;

import js.dom.Element;
import js.util.Params;
import js.wood.ILinkDescriptor;

/**
 * Descriptor for page link element. This class is loaded from <code>link</code> element of project or page descriptor. All
 * standard attributes are supported.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class LinkDescriptor implements ILinkDescriptor {
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

	public LinkDescriptor(String href) {
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
		LinkDescriptor other = (LinkDescriptor) obj;
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

	public static LinkDescriptor create(Element linkElement) {
		final String href = linkElement.getAttr("href");
		assert href != null;
		LinkDescriptor link = new LinkDescriptor(href);

		link.setHreflang(linkElement.getAttr("hreflang"));
		link.setRelationship(linkElement.getAttr("rel"));
		link.setMedia(linkElement.getAttr("media"));
		link.setReferrerPolicy(linkElement.getAttr("referrerpolicy"));
		link.setDisabled(linkElement.getAttr("disabled"));
		link.setType(linkElement.getAttr("type"));
		link.setAsType(linkElement.getAttr("as"));
		link.setPrefetch(linkElement.getAttr("prefetch"));
		link.setSizes(linkElement.getAttr("sizes"));
		link.setImageSizes(linkElement.getAttr("imagesizes"));
		link.setImageSrcSet(linkElement.getAttr("imagesrcset"));
		link.setTitle(linkElement.getAttr("title"));
		link.setIntegrity(linkElement.getAttr("integrity"));
		link.setCrossOrigin(linkElement.getAttr("crossorigin"));

		return link;
	}
}