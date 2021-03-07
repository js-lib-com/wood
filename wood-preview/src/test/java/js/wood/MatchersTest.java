package js.wood;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Before;
import org.junit.Test;

public class MatchersTest {
	private Matchers matchers;

	@Before
	public void beforeTest() {
		matchers = new Matchers();
	}

	@Test
	public void startsWith() {
		matchers.addPattern("abc*");

		assertTrue(matchers.match("abc"));
		assertTrue(matchers.match("abcx"));

		assertFalse(matchers.match("xabcx"));
		assertFalse(matchers.match("xabc"));
		assertFalse(matchers.match("xyz"));
	}

	@Test
	public void endsWith() {
		matchers.addPattern("*abc");

		assertTrue(matchers.match("abc"));
		assertTrue(matchers.match("xabc"));

		assertFalse(matchers.match("xabcx"));
		assertFalse(matchers.match("abcx"));
		assertFalse(matchers.match("xyz"));
	}

	@Test
	public void contains() {
		matchers.addPattern("*abc*");

		assertTrue(matchers.match("abc"));
		assertTrue(matchers.match("xabcx"));
		assertTrue(matchers.match("abcx"));
		assertTrue(matchers.match("xabc"));

		assertFalse(matchers.match("xyz"));
	}

	@Test
	public void equals() {
		matchers.addPattern("abc");

		assertTrue(matchers.match("abc"));

		assertFalse(matchers.match("xabc"));
		assertFalse(matchers.match("abcx"));
		assertFalse(matchers.match("xabcx"));
		assertFalse(matchers.match("xyz"));
	}

	@Test
	public void multiple() {
		matchers.addPattern("*abc", "abc*");

		assertTrue(matchers.match("abc"));
		assertTrue(matchers.match("abcx"));
		assertTrue(matchers.match("xabc"));

		assertFalse(matchers.match("xabcx"));
		assertFalse(matchers.match("xyz"));
	}

	@Test
	public void requestPathMatcher() {
		String urlPatterns = "*.rmi,*.xsp,*/captcha/image/*,*/rest/*";
		matchers.addPattern(urlPatterns.split(","));

		assertTrue(matchers.match("res/page/com/kidscademy/ServiceController/getFeedbackData.rmi"));
	}

	@Test
	public void emptyPatterns() {
		assertTrue(matchers.match("anything"));
		assertTrue(matchers.match(""));
		assertTrue(matchers.match(null));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullString() {
		matchers.addPattern("abc");
		matchers.match(null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyString() {
		matchers.addPattern("abc");
		matchers.match("");
	}
}
