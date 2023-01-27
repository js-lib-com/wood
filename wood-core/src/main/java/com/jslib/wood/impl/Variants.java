package com.jslib.wood.impl;

import java.util.Locale;
import java.util.regex.Pattern;

import com.jslib.util.Strings;
import com.jslib.wood.FilePath;
import com.jslib.wood.WoodException;

/**
 * Variants qualify file path so that one can create group of files with the same semantic content but differently presented,
 * for example string variables for multi-language support.
 * 
 * Variants are appended to file names, separated by underscore (_) character. Is legal to have multiple variants, but every
 * variant preceded by its separator - underscore (_) character. Currently supported variants are language and media queries for
 * style files.
 *
 * Language variants are used for projects with internationalization support. This should be declared on language property from
 * {@link ProjectDescriptor}. A not declared language variant is ignored.
 * 
 * Pattern for language variant has format similar to <code>Accept-Language</code> HTML header. Strictly speaking, language
 * variant format is not identical with HTML header; it uses only 2 digit ISO 639 alpha-2 language code and additional
 * information is always country code, into ISO 3166 alpha-2 format. Dash separator and country code are optional. For example,
 * <code>ro</code> and <code>ro-RO</code> are both valid variants.
 * 
 * Media queries enable different styles based on device properties, for example screen width and height. On the other hand,
 * WOOD allows to split media queries styles on style file variants with custom alias, for example
 * page/index/index_portrait.css.
 * 
 * In order to use a media query file variant it should also be declared on project descriptor, media-query element; alias
 * attribute is the file variant and expression attribute is media feature always related to screen media type.
 * 
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
	 * Pattern for language variant in format similar to <code>Accept-Language</code> HTML header. Strictly, language variant is
	 * not identically HTML header format; it uses only 2 digit ISO 639 alpha-2 language code and additional information is
	 * always country code, into ISO 3166 alpha-2 format. Dash separator and country code are optional. For example
	 * <code>en</code> and <code>en-US</code> are both valid variants.
	 * <p>
	 * Language code has the same limitations regarding old code as {@link Locale} class as. It accepts new codes but always
	 * generate old ones. For example an hebrew file can use both <code>iw</code> and <code>he</code> code language variant but
	 * generated build layout will always use <code>iw</code>.
	 */
	public static final Pattern LANGUAGE_PATTERN = Pattern.compile("^([a-z]{2})(?:\\-([A-Z]{2}))?$", Pattern.CASE_INSENSITIVE);

	/** Language variant. This variant is null if not set. */
	private final String language;

	private final MediaQueries mediaQueries;

	/** Flag true only if this variants instance is empty, that is, initialized from null variants parameter. */
	private final boolean empty;

	/**
	 * Parse given variants list and initialize this instance state. Variants order in list in not imposed. Null variants
	 * parameter is accepted in which case an empty variants instance is created.
	 * <p>
	 * A not recognized variant rise exception. Note that <code>file</code> parameter is used just for exception tracking and
	 * that it can be null.
	 * 
	 * @param file the file path decorated with given variants list,
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

		String language = null;
		this.mediaQueries = new MediaQueries(file.getProject());

		Matcher matcher = new Matcher();
		boolean empty = true;
		for (String variant : split(variants)) {
			empty = false;
			if (matcher.match(LANGUAGE_PATTERN, variant)) {
				// language
				if (language != null) {
					throw new WoodException("Invalid variants syntax |%s|. Multiple language variants.", variants);
				}
				String country = matcher.group(2);
				if (country == null) {
					language = matcher.group(1);
				} else {
					language = Strings.concat(matcher.group(1), '-', country);
				}

			} else if (mediaQueries.add(variant)) {
				// media query
			} else {
				// unknown variants
				throw new WoodException("Not recognized variant |%s| on file |%s|.", variant, file);
			}
		}

		this.language = language;
		this.empty = empty;
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
	 * Get language variant value or null if language variant is missing.
	 * 
	 * @return language value or null.
	 * @see #language
	 */
	public String getLanguage() {
		return language;
	}

	public boolean hasMediaQueries() {
		return !mediaQueries.isEmpty();
	}

	public MediaQueries getMediaQueries() {
		return mediaQueries;
	}

	/**
	 * Test if this variants has requested language. Language parameter may be null if projects is not multi-language. Returns
	 * true if requested language equals this variants one; this is also true if both are null.
	 * 
	 * @param language language to test, null for default.
	 * @return true if this variants has <code>language</code>.
	 * @see #language
	 */
	public boolean hasLanguage(String language) {
		if (this.language == null) {
			return language == null;
		}
		return this.language.equals(language);
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
}
