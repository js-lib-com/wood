package js.wood;

public interface ILinkDescriptor {
	String getHref();

	String getHreflang();

	String getRelationship();

	String getType();

	String getMedia();

	String getReferrerPolicy();

	String getCrossOrigin();

	String getIntegrity();

	String getDisabled();

	String getAsType();

	String getPrefetch();

	String getSizes();

	String getImageSizes();

	String getImageSrcSet();

	String getTitle();
	
	boolean isStyleSheet();
}
