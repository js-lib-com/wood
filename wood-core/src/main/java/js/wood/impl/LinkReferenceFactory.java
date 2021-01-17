package js.wood.impl;

import js.dom.Element;
import js.wood.WoodException;

public class LinkReferenceFactory {
	public static LinkReference create(Element linkElement) {
		final String href = linkElement.getAttr("href");
		if (href == null) {
			throw new WoodException("Invalid project configuration file. Missing 'href' attribute from <style> element.");
		}
		LinkReference link = new LinkReference(href);

		link.setHreflang(linkElement.getAttr("hreflang"));
		link.setRelationship(linkElement.getAttr("rel"));
		link.setMedia(linkElement.getAttr("media"));
		link.setReferrerPolicy(linkElement.getAttr("referrerpolicy"));
		link.setDisabled(Boolean.parseBoolean(linkElement.getAttr("disabled")));
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
