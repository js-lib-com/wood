package js.wood.impl;

import js.dom.Element;
import js.wood.ILinkReference;
import js.wood.WoodException;

public class LinkReferenceFactory {
	public static LinkReference create(Element linkElement, ILinkReference defaults) {
		final String href = linkElement.getAttr("href");
		if (href == null) {
			throw new WoodException("Invalid project configuration file. Missing 'href' attribute from <style> element.");
		}
		LinkReference link = new LinkReference(href);

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
