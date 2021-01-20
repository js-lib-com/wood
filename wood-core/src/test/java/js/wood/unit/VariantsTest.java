package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import js.wood.impl.Variants;

public class VariantsTest {
	@Test
	public void variantsConstructor() {
		Variants variants = new Variants("xsd");
		assertEquals(Variants.Screen.EXTRA_SMALL, variants.getScreen());
	}

	@Test
	public void variantsPatterns() {
		Pattern pattern = Variants.Screen.PATTERN;
		assertTrue(match(pattern, "lgd"));
		assertTrue(match(pattern, "mdd"));
		assertTrue(match(pattern, "smd"));
		assertTrue(match(pattern, "xsd"));
	}

	private static boolean match(Pattern pattern, String variant) {
		Matcher matcher = pattern.matcher(variant);
		return matcher.find();
	}

}
