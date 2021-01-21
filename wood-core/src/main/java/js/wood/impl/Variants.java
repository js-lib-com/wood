package js.wood.impl;

import java.util.Locale;
import java.util.regex.Pattern;

import js.lang.BugError;
import js.wood.FilePath;
import js.wood.WoodException;

/**
 * Variant is file name qualifier that create identically semantic files but differently presented. Is legal to have multiple
 * variants list to a file.
 * <p>
 * Variants are encoded into file name using underscore as separator. This is the reason for file names restriction to use
 * underscore. Current implementation supports next variants:
 * <ul>
 * <li>locale - select a file for inclusion into a particular locale build,
 * <li>viewport size - viewport maximum width or height used for styles based on max- media queries,
 * <li>orientation - device orientation, <code>landscape</code> or <code>portrait</code>,
 * </ul>
 * <p>
 * There are also couple experimental variants in analysis:
 * <ul>
 * <li>device class - experimental, the class of the device running web application like <code>mobile</code> or
 * <code>desktop</code>,
 * <li>site style - experimental, overall user interface style selects UI controls set, for example classic or flat style,
 * <li>site theme - experimental, site theme, mainly colors, used by current site style.
 * </ul>
 * <p>
 * Current syntax, including experimental features:
 * 
 * <pre>
 * variants    = +variant                   ; one or more variants
 * variant     = V-SEP (locale / viewport / device / orientation / style / theme)
 * locale      = language ?('-' country)    ; locale variant has language and optional country separated by dash
 * language    = 2ALPHA                     ; lower case ISO 639 alpha-2 language code 
 * country     = 2ALPHA                     ; upper case ISO 3166 alpha-2 country code
 * viewport    = (V-CODE / H-CODE) 3*4DIGIT ; viewport maximum width or height variant
 * device      = MOBILE / DESKTOP / TV-SET  ; the class of the device running web application
 * orientation = PORTRAIT / LANDSCAPE       ; device orientation 
 * style       = 1*APLHA S-CODE             ; site style variant has an arbitrary name with "-style" suffix
 * theme       = 1*APLHA T-CODE             ; site theme variant has an arbitrary name with "-theme" suffix
 * 
 * ; terminal symbols definition
 * V-SEP     = "_"                          ; variant separator is underscore that is not valid in names
 * V-CODE    = "w"                          ; variant code for viewport maximum width
 * H-CODE    = "h"                          ; variant code for viewport maximum height
 * S-CODE    = "-style"                     ; site style variant code act as suffix
 * T-CODE    = "-theme"                     ; site theme variant code act as suffix
 * MOBILE    = "mobile"                     ; mobile device variant
 * DESKTOP   = "desktop"                    ; desktop device variant
 * TV-SET    = "tv-set"                     ; television set variant 
 * PORTRAIT  = "portrait"                   ; device is used in portrait mode, that is, vertical
 * LANDSCAPE = "landscape"                  ; device is used in landscape mode
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * Variants instance has not mutable state, therefore is thread safe.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class Variants {
	/**
	 * Separator used for variants list. Current implementation uses underscore; this is the reason for restriction to use
	 * underscore on path names.
	 */
	private static final String SEPARATOR = "_";

	/**
	 * Pattern for locale variant in format similar to <code>Accept-Language</code> HTML header. Strictly, locale variant is not
	 * identically HTML header format; it uses only 2 digit ISO 639 alpha-2 language code and additional information is always
	 * country code, into ISO 3166 alpha-2 format. Dash separator and country code are optional. For example <code>en</code> and
	 * <code>en-US</code> are both valid variants.
	 * <p>
	 * Language code has the same limitations regarding old code as {@link Locale} class as. It accepts new codes but always
	 * generate old ones. For example an hebrew file can use both <code>iw</code> and <code>he</code> language code for locale
	 * variant but generated build layout will always use <code>iw</code>.
	 */
	public static final Pattern LOCALE = Pattern.compile("^([a-z]{2})(?:\\-([A-Z]{2}))?$", Pattern.CASE_INSENSITIVE);

	/** Pattern for viewport width variant. */
	private static final Pattern VIEWPORT_WIDTH = Pattern.compile("^w(\\d{3,4})$");

	/** Pattern for viewport height variant. */
	private static final Pattern VIEWPORT_HEIGHT = Pattern.compile("^h(\\d{3,4})$");

	/** Locale variant. This variant is null if not set. */
	private final Locale locale;

	/** Value for viewport maximum width variant. This variant is zero if not set. */
	private final int viewportWidth;

	/** Value for viewport maximum height variant. This variant is zero if not set. */
	private final int viewportHeight;

	/** Screen viewport width variant value, see {@link Screen} for recognized types. {@link Screen#NONE} if not set. */
	private final Screen screen;

	/**
	 * Device orientation variant value, see {@link Orientation} for recognized types. {@link Orientation#NONE} if not set.
	 */
	private final Orientation orientation;

	/** Flag true only if this variants instance is empty, that is, initialized from null variants parameter. */
	private final boolean empty;

	/**
	 * Parse given variants list and initialize this instance state. Variants order in list in not imposed. Null variants
	 * parameter is accepted in which case an empty variants instance is created.
	 * <p>
	 * A not recognized variant rise exception. Note that <code>file</code> parameter is used just for exception tracking and
	 * that it can be null.
	 * 
	 * @param file the file qualified by given variants, used for logging,
	 * @param variants variants list separated by {@link #SEPARATOR}, possible null.
	 * @throws WoodException if a variant from list is not recognized.
	 */
	public Variants(FilePath file, String variants) throws WoodException {
		/** Pattern matcher used for variants parsing. */
		class Matcher {
			private java.util.regex.Matcher matcher;

			public boolean match(Pattern pattern, String variant) {
				matcher = pattern.matcher(variant);
				return matcher.find();
			}

			public String group(int group) {
				return matcher.group(group);
			}
		}

		Locale locale = null;
		int viewportWidth = 0;
		int viewportHeight = 0;
		Screen screen = Screen.NONE;
		Orientation orientation = Orientation.NONE;

		Matcher matcher = new Matcher();
		boolean empty = true;
		for (String variant : split(variants)) {
			empty = false;
			if (matcher.match(LOCALE, variant)) {
				// locale
				if (locale != null) {
					throw new WoodException("Invalid variants syntax |%s|. Multiple locale.", variants);
				}
				String country = matcher.group(2);
				if (country == null) {
					locale = new Locale(matcher.group(1));
				} else {
					locale = new Locale(matcher.group(1), country);
				}
			} else if (matcher.match(VIEWPORT_WIDTH, variant)) {
				// viewport width
				if (viewportWidth != 0) {
					throw new WoodException("Invalid variants syntax |%s|. Multiple viewport width.", variants);
				}
				viewportWidth = Integer.parseInt(matcher.group(1));
			} else if (matcher.match(VIEWPORT_HEIGHT, variant)) {
				// viewport height
				if (viewportHeight != 0) {
					throw new WoodException("Invalid variants syntax |%s|. Multiple viewport width.", variants);
				}
				viewportHeight = Integer.parseInt(matcher.group(1));
			} else if (matcher.match(Screen.PATTERN, variant)) {
				// screen width
				if (screen != Screen.NONE) {
					throw new WoodException("Invalid variants syntax |%s|. Multiple viewport width.", variants);
				}
				screen = Screen.forName(matcher.group(1));
			} else if (matcher.match(Orientation.PATTERN, variant)) {
				// device orientation
				if (orientation != Orientation.NONE) {
					throw new WoodException("Invalid variants syntax |%s|. Multiple viewport width.", variants);
				}
				orientation = Orientation.forName(matcher.group(1));
			} else {
				// unknown variants
				throw new WoodException("Not recognized variant |%s| on file |%s|.", variant, file);
			}
		}

		this.locale = locale;
		this.viewportWidth = viewportWidth;
		this.viewportHeight = viewportHeight;
		this.screen = screen;
		this.orientation = orientation;
		this.empty = empty;
	}

	/**
	 * Test constructor.
	 * 
	 * @param variants variants list separated by {@link #SEPARATOR}, possible null.
	 */
	public Variants(String variants) {
		this(null, variants);
	}

	/** Empty strings used to replace null variants. */
	private static final String[] EMPTY_VARIANTS = new String[0];

	/**
	 * Split variants using {@link #SEPARATOR}.
	 * 
	 * @param variants string of one or many variants, separated by {@link #SEPARATOR}.
	 * @return variants array.
	 */
	private static String[] split(String variants) {
		if (variants == null) {
			return EMPTY_VARIANTS;
		}
		return variants.split(SEPARATOR);
	}

	/**
	 * Get locale variant value or null if locale variant is missing.
	 * 
	 * @return locale value or null.
	 * @see #locale
	 */
	public Locale getLocale() {
		return locale;
	}

	/**
	 * Get variant value for viewport maximum width.
	 * 
	 * @return viewport maximum width value.
	 * @see #viewportWidth
	 */
	public int getViewportWidth() {
		return viewportWidth;
	}

	/**
	 * Get variant value for viewport maximum height.
	 * 
	 * @return viewport maximum height value.
	 * @see #viewportHeight
	 */
	public int getViewportHeight() {
		return viewportHeight;
	}

	/**
	 * Get value of screen viewport width variant, possible {@link Screen#NONE} for not set.
	 * 
	 * @return value of screen viewport width variant.
	 * @see #screen
	 */
	public Screen getScreen() {
		return screen;
	}

	/**
	 * Get value of device orientation, possible {@link Orientation#NONE}.
	 * 
	 * @return value of device orientation variant.
	 * @see #orientation
	 */
	public Orientation getOrientation() {
		return orientation;
	}

	/**
	 * Test if this variants has requested language. Language parameter may be null if projects is not multi-language. Returns
	 * true if requested language equals this variants one; this is also true if both are null.
	 * 
	 * @param language language to test, null for default.
	 * @return true if this variants has <code>language</code>.
	 * @see #language
	 */
	public boolean hasLocale(Locale locale) {
		if (this.locale == null) {
			return locale == null;
		}
		return this.locale.equals(locale);
	}

	/**
	 * Test if locale variant is present.
	 * 
	 * @return true if locale variant is present.
	 * @see #locale
	 */
	public boolean hasLocale() {
		return locale != null;
	}

	/**
	 * Test if viewport width variant is present.
	 * 
	 * @return true if viewport width variant is present.
	 * @see #viewportWidth
	 */
	public boolean hasViewportWidth() {
		return viewportWidth != 0;
	}

	/**
	 * Test if viewport height variant is present.
	 * 
	 * @return true if viewport height variant is present.
	 * @see #viewportHeight
	 */
	public boolean hasViewportHeight() {
		return viewportHeight != 0;
	}

	/**
	 * Test if screen width variant is present.
	 * 
	 * @return true if screen width variant is present.
	 * @see #screen
	 */
	public boolean hasScreen() {
		return screen != Screen.NONE;
	}

	/**
	 * Test if device orientation variant is present.
	 * 
	 * @return true if device orientation variant is present.
	 * @see #orientation
	 */
	public boolean hasOrientation() {
		return orientation != Orientation.NONE;
	}

	/**
	 * Test if this variants instance is empty, that is, it has no value set.
	 * 
	 * @return true if this variants is empty.
	 * @see #empty
	 */
	public boolean isEmpty() {
		return empty;
	}

	/**
	 * Screen dimensions variants. This variants class offer a convenient way to use viewport width with predefined values:
	 * <ul>
	 * <li><b>lgd</b> large - large viewport width - large desktop, tv-set, strictly greater than 1200px
	 * <li><b>nod</b> normal - normal viewport width - desktop, 992px to 1200px
	 * <li><b>mdd</b> medium - medium viewport width - laptop, 768px to 992px
	 * <li><b>smd</b> small - small viewport width - tablet, 480px to 768px
	 * <li><b>xsd</b> extra-small - extra small viewport width - phone, less than or equal 560px
	 * <ul>
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	public static enum Screen {
		/** Neutral value. */
		NONE(0),

		/** Large viewport width - large desktop, tv-set, strictly greater than 1200px. */
		LARGE(1200),

		/** Normal viewport width - desktop, 992px to 1200px. */
		NORMAL(1200),

		/** Medium viewport width - laptop, 768px to 992px. */
		MEDIUM(992),

		/** Small viewport width - tablet, 480px to 768px. */
		SMALL(768),

		/** Extra small viewport width - phone, less than or equal 560px. */
		EXTRA_SMALL(560);

		private int width;

		private Screen(int width) {
			this.width = width;
		}

		public int getWidth() {
			return width;
		}

		/** Pattern for screen class variant. */
		private static final Pattern PATTERN = Pattern.compile("^(lgd|nod|mdd|smd|xsd)$");

		/**
		 * Create screen class constant for name. Return {@link #NONE} if <code>screen</code> parameter is not recognized.
		 * 
		 * @param screen screen variant name.
		 * @return screen class constant.
		 */
		private static Screen forName(String screen) {
			// takes care to keep this method literals in sync with pattern constant

			switch (screen) {
			case "lgd":
				return LARGE;
			case "nod":
				return Screen.NORMAL;
			case "mdd":
				return MEDIUM;
			case "smd":
				return SMALL;
			case "xsd":
				return EXTRA_SMALL;

			default:
				// screen is already matched against PATTERN
				throw new BugError("Invalid screen pattern |%s|.", screen);
			}
		}
	}

	/**
	 * Device orientation variant values.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	public static enum Orientation {
		/** Device orientation value not recognized. */
		NONE,

		/** Device is used horizontally, that is, horizontal dimension is larger than the vertical one. */
		LANDSCAPE,

		/** Device is used vertically, vertical dimension is larger than the horizontal one. */
		PORTRAIT;

		/** Pattern for device orientation variant. */
		private static final Pattern PATTERN = Pattern.compile("^(landscape|portrait)$");

		/**
		 * Create device orientation constant for given name. Return {@link #NONE} if <code>orientation</code> parameter is not
		 * recognized.
		 * 
		 * @param orientation orientation value.
		 * @return device orientation constant.
		 */
		private static Orientation forName(String orientation) {
			// takes care to keep this method literals in sync with pattern constant
			switch (orientation) {
			case "landscape":
				return LANDSCAPE;
			case "portrait":
				return PORTRAIT;

			default:
				// orientation is already matched against PATTERN
				throw new BugError("Invalid device orientation pattern |%s|.", orientation);
			}
		}
	}
}
