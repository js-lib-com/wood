package com.jslib.wood.dom;

import java.io.InputStream;
import java.net.URL;

/**
 * Resources constants and helpers.
 * 
 * @author Iulian Rotaru
 */
public final class Resources {
	public static final String XHTML11_DTD = "/js/dom/w3c/resources/xhtml11.dtd";
	public static final String XHTML1_STRICT_DTD = "/js/dom/w3c/resources/xhtml1-strict.dtd";
	public static final String XHTML1_TRANSITIONAL_DTD = "/js/dom/w3c/resources/xhtml1-transitional.dtd";
	public static final String XHTML_INLSTYLE_1_MOD = "/js/dom/w3c/resources/xhtml-inlstyle-1.mod";
	public static final String XHTML_LAT1_ENT = "/js/dom/w3c/resources/xhtml-lat1.ent";
	public static final String XHTML_SYMBOL_ENT = "/js/dom/w3c/resources/xhtml-symbol.ent";
	public static final String XHTML_SPECIAL_ENT = "/js/dom/w3c/resources/xhtml-special.ent";
	public static final String XHTML_FRAMEWORK_1_MOD = "/js/dom/w3c/resources/xhtml-framework-1.mod";
	public static final String XHTML_DATATYPES_1_MOD = "/js/dom/w3c/resources/xhtml-datatypes-1.mod";
	public static final String XHTML_QNAME_1_MOD = "/js/dom/w3c/resources/xhtml-qname-1.mod";
	public static final String XHTML_EVENTS_1_MOD = "/js/dom/w3c/resources/xhtml-events-1.mod";
	public static final String XHTML_EVENTS_BASIC_1_MOD = "/js/dom/w3c/resources/xhtml-events-basic-1.mod";
	public static final String WEB_APP_2_3_DTD = "/js/dom/w3c/resources/web-app_2_3.dtd";

	/**
	 * Get entity resolver resource.
	 * 
	 * @param resource resource public ID.
	 * @return entity resolver URL.
	 */
	public static URL url(String resource) {
		return Resources.class.getResource(resource);
	}

	/**
	 * Get entity resolver resource stream.
	 * 
	 * @param resource resource public ID.
	 * @return entity resolver stream.
	 */
	public static InputStream stream(String resource) {
		return Resources.class.getResourceAsStream(resource);
	}

	/** Prevent default constructor synthesis. */
	private Resources() {
	}
}
