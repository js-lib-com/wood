package com.jslib.wood.dom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * HTML documents global constants.
 * 
 * @author Iulian Rotaru
 */
public final class HTML {
	/** HTML tags declared without content. */
	public static final Collection<String> EMPTY_TAGS = new ArrayList<>();
	static {
		EMPTY_TAGS.add("area");
		EMPTY_TAGS.add("base");
		EMPTY_TAGS.add("basefont");
		EMPTY_TAGS.add("br");
		EMPTY_TAGS.add("col");
		EMPTY_TAGS.add("frame");
		EMPTY_TAGS.add("hr");
		EMPTY_TAGS.add("img");
		EMPTY_TAGS.add("input");
		EMPTY_TAGS.add("isindex");
		EMPTY_TAGS.add("link");
		EMPTY_TAGS.add("meta");
		EMPTY_TAGS.add("param");
	}

	/** Default attributes value. */
	public static final Map<String, String> DEFAULT_ATTRS = new HashMap<>();
	static {
		DEFAULT_ATTRS.put("span", "1");
		DEFAULT_ATTRS.put("rowspan", "1");
		DEFAULT_ATTRS.put("colspan", "1");
		DEFAULT_ATTRS.put("xml:space", "preserve");
		DEFAULT_ATTRS.put("shape", "rect");
		DEFAULT_ATTRS.put("valuetype", "data");
	}

	/** HTML elements with text nodes serialized with no escape. */
	public static final Collection<String> RAW_TAGS = new ArrayList<>();
	static {
		RAW_TAGS.add("script");
	}

	/** Disable default constructor synthesis. */
	private HTML() {
	}
}
