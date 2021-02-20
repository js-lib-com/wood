package js.wood.impl;

import js.dom.Element;
import js.util.Params;
import js.wood.ILinkDescriptor;
import js.wood.WoodException;

/**
 * Descriptor for page link element. This class is loaded from <code>link</code> element of project or page descriptor. All
 * standard attributes are supported.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class LinkDescriptor implements ILinkDescriptor {
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

	public static LinkDescriptor create(Element linkElement, ILinkDescriptor defaults) {
		final String href = linkElement.getAttr("href");
		if (href == null) {
			throw new WoodException("Invalid project configuration file. Missing 'href' attribute from <style> element.");
		}
		LinkDescriptor link = new LinkDescriptor(href);

		link.setHreflang(value(linkElement.getAttr("hreflang"), defaults.getHreflang()));
		link.setRelationship(value(linkElement.getAttr("rel"), defaults.getRelationship()));
		link.setMedia(value(linkElement.getAttr("media"), defaults.getMedia()));
		link.setReferrerPolicy(value(linkElement.getAttr("referrerpolicy"), defaults.getReferrerPolicy()));
		link.setDisabled(value(linkElement.getAttr("disabled"), defaults.getDisabled()));
		link.setType(value(linkElement.getAttr("type"), defaults.getType()));
		link.setAsType(value(linkElement.getAttr("as"), defaults.getAsType()));
		link.setPrefetch(value(linkElement.getAttr("prefetch"), defaults.getPrefetch()));
		link.setSizes(value(linkElement.getAttr("sizes"), defaults.getSizes()));
		link.setImageSizes(value(linkElement.getAttr("imagesizes"), defaults.getImageSizes()));
		link.setImageSrcSet(value(linkElement.getAttr("imagesrcset"), defaults.getImageSrcSet()));
		link.setTitle(value(linkElement.getAttr("title"), defaults.getTitle()));
		link.setIntegrity(value(linkElement.getAttr("integrity"), defaults.getIntegrity()));
		link.setCrossOrigin(value(linkElement.getAttr("crossorigin"), defaults.getCrossOrigin()));

		return link;
	}

	private static String value(String base, String defaults) {
		return base != null ? base : defaults;
	}
}