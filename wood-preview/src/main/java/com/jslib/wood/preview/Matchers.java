package com.jslib.wood.preview;

import java.util.ArrayList;
import java.util.List;

/**
 * Test a given string against a list of patterns. Matchers register a list of patterns, see {@link #addPattern(String...)} and
 * test a given string value in pattern declaration order till found first match. If no pattern match string value is rejected.
 * <p>
 * Currently implemented patterns:
 * <ul>
 * <li>abc* - string value equals or starts with 'abc'
 * <li>*abc - string value equals or ends with 'abc'
 * <li>*abc* - string value equals or contains 'abc'
 * <li>abc - string value is strictly equal to 'abc'
 * </ul>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class Matchers {
	private final List<IMatcher> matchers = new ArrayList<>();

	/**
	 * Register patterns. This method can be called repeatedly with variable number of patterns. Patterns order is preserved. If
	 * <code>patterns</code> argument is empty this method does nothing.
	 * 
	 * @param patterns one or more patterns to register.
	 */
	public void addPattern(String... patterns) {
		for (String pattern : patterns) {
			IMatcher matcher;
			if (pattern.startsWith("*")) {
				if (pattern.endsWith("*")) {
					matcher = new Contains(pattern.substring(1, pattern.length() - 1));
				} else {
					matcher = new EndsWith(pattern.substring(1));
				}
			} else {
				if (pattern.endsWith("*")) {
					matcher = new StartsWith(pattern.substring(0, pattern.length() - 1));
				} else {
					matcher = new Equals(pattern);
				}
			}
			matchers.add(matcher);
		}
	}

	/**
	 * Test given string value against patterns list till found first that match - in which care return true. Pattern iteration
	 * order is the declaration order - see {@link #addPattern(String...)}. Return false if no pattern match.
	 * 
	 * @param string string value to test.
	 * @return true if string value match a pattern.
	 */
	public boolean match(String string) {
		assert string != null && !string.isEmpty(): "String to match argument is null or empty";
		for (IMatcher matcher : matchers) {
			if (matcher.match(string)) {
				return true;
			}
		}
		return false;
	}

	// --------------------------------------------------------------------------------------------

	/**
	 * A matcher has a pattern and a predicate that test a given string value against stored pattern.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private interface IMatcher {
		/**
		 * Test given string value against stored pattern.
		 * 
		 * @param string string value to test.
		 * @return true if string value matches the pattern.
		 */
		boolean match(String string);
	}

	/**
	 * Test if given string value starts with a pattern.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private static class StartsWith implements IMatcher {
		private final String pattern;

		public StartsWith(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean match(String string) {
			return string.startsWith(pattern);
		}
	}

	/**
	 * Test if given string value ends with a pattern.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private static class EndsWith implements IMatcher {
		private final String pattern;

		public EndsWith(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean match(String string) {
			return string.endsWith(pattern);
		}
	}

	/**
	 * Test if given string value contains a pattern.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private static class Contains implements IMatcher {
		private final String pattern;

		public Contains(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean match(String string) {
			return string.contains(pattern);
		}
	}

	/**
	 * Test if given string value is strictly equal to a pattern.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private static class Equals implements IMatcher {
		private final String pattern;

		public Equals(String pattern) {
			this.pattern = pattern;
		}

		@Override
		public boolean match(String string) {
			return string.equals(pattern);
		}
	}
}