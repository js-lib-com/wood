package com.jslib.wood;

public interface IScriptDescriptor {
	/** Script source is local file path or the URL from where third script is to be loaded. */
	String getSource();

	String getType();

	String getAsync();

	String getDefer();

	String getNoModule();

	String getNonce();

	String getReferrerPolicy();

	String getIntegrity();

	String getCrossOrigin();

	boolean isEmbedded();

	boolean isDynamic();
}
