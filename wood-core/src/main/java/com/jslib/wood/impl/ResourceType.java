package com.jslib.wood.impl;

/**
 * Resource type, both variables and files. Resources are external entities referenced from source files. There are two major
 * kinds: variables and resource files. Variables are text replaced with their value, resource file references are replaced by
 * URL path, absolute or relative.
 * 
 * @author Iulian Rotaru
 */
public enum ResourceType {
	// VARIABLES

	/** Plain string values mainly for multi-languages support. */
	STRING,

	/**
	 * Same as {@link #STRING} but allows for HTML format. Text variable value is a HTML fragment that is imported in place of
	 * variable reference. For this reason it is valid only in places where child elements are legal.
	 */
	TEXT,

	/** A reusable color, mainly for styles. Color format is that defined by CSS standard. */
	COLOR,

	/** A style dimension or position. It is a numeric value and units with format defined by CSS standard. */
	DIMEN,

	/** Links references to local or third part resources, mainly for <code>href</code> attribute. */
	LINK,

	/** Tool-tip value. */
	TIP,

	// RESOURCE FILES

	/** Image file stored on server and linked via URLs from source files, be it layout, style or script. */
	IMAGE,

	/** The same as {@link #IMAGE} but with audio content. */
	AUDIO,

	/** The same as {@link #IMAGE} but with video content. */
	VIDEO,

	/** Font family file loaded from server and declared by <code>@font-face</code> style rule. */
	FONT,

	/** Generic file, for example license text file. */
	FILE,

	/** Unknown type makes reference using this type as invalid. */
	UNKNOWN;

	/**
	 * Test if resource type is a variable. Current implementation consider as variable next types: {@link #STRING},
	 * {@link #TEXT}, {@link #COLOR}, {@link #DIMEN}, {@link #STYLE}, {@link #LINK} and {@link #TIP}.
	 * 
	 * @return true if resource is a variable.
	 */
	public boolean isVariable() {
		return this == STRING || this == TEXT || this == COLOR || this == DIMEN || this == LINK || this == TIP;
	}

	/**
	 * Test if resource type is a resource file. Current implementation consider as resource file next types: {@link #IMAGE},
	 * {@link #AUDIO}, {@link #VIDEO}, {@link #FONT} and {@link #FILE}.
	 * 
	 * @return true if resource is a file.
	 */
	public boolean isFile() {
		return this == IMAGE || this == AUDIO || this == VIDEO || this == FONT || this == FILE;
	}

	/**
	 * Test if resource type is a media file. Current implementation consider as media file next types: {@link #IMAGE},
	 * {@link #AUDIO} and {@link #VIDEO}.
	 * 
	 * @return true if resource is media file.
	 */
	public boolean isMediaFile() {
		return this == IMAGE || this == AUDIO || this == VIDEO;
	}

	/**
	 * Create resource type enumeration from type value, not case sensitive. Returns {@link #UNKNOWN} if given resource type
	 * parameter does not denote an enumeration constant.
	 * 
	 * @param type value of resource type.
	 * @return resource type enumeration, possible {@link #UNKNOWN}.
	 */
	public static ResourceType getValueOf(String type) {
		try {
			return ResourceType.valueOf(type.toUpperCase());
		} catch (Exception unused) {
		}
		return UNKNOWN;
	}

	// WARN: keep variable names in sync with resource type constants
	private static String[] variables = new String[] { "string", "text", "color", "dimen", "link", "tip" };

	public static String[] variables() {
		return variables;
	}
}