package com.jslib.wood.preview;

import org.junit.Before;
import org.junit.Test;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class MatchersTest {
    private Matchers matchers;

    @Before
    public void beforeTest() {
        matchers = new Matchers();
    }

    @Test
    public void GivenStartsWithPattern_WhenPositiveMatcherMatch_ThenFound() {
        // GIVEN
        matchers.addPattern("abc*");

        // WHEN - THEN
        assertTrue(matchers.match("abc"));
        assertTrue(matchers.match("abcx"));
    }

    @Test
    public void GivenStartsWithPattern_WhenNegativeMatcherMatch_ThenNotFound() {
        // GIVEN
        matchers.addPattern("abc*");

        // WHEN - THEN
        assertFalse(matchers.match("xabcx"));
        assertFalse(matchers.match("xabc"));
        assertFalse(matchers.match("xyz"));
    }

    @Test
    public void GivenEndsWithPattern_WhenPositiveMatcherMatch_ThenFound() {
        // GIVEN
        matchers.addPattern("*abc");

        // WHEN - THEN
        assertTrue(matchers.match("abc"));
        assertTrue(matchers.match("xabc"));
    }

    @Test
    public void GivenEndsWithPattern_WhenNegativeMatcherMatch_ThenNotFound() {
        // GIVEN
        matchers.addPattern("*abc");

        // WHEN - THEN
        assertFalse(matchers.match("xabcx"));
        assertFalse(matchers.match("abcx"));
        assertFalse(matchers.match("xyz"));
    }

    @Test
    public void GivenContainsPattern_WhenPositiveMatcherMatch_ThenFound() {
        // GIVEN
        matchers.addPattern("*abc*");

        // WHEN - THEN
        assertTrue(matchers.match("abc"));
        assertTrue(matchers.match("xabcx"));
        assertTrue(matchers.match("abcx"));
        assertTrue(matchers.match("xabc"));
    }

    @Test
    public void GivenContainsPattern_WhenNegativeMatcherMatch_ThenNotFound() {
        // GIVEN
        matchers.addPattern("*abc*");

        // WHEN - THEN
        assertFalse(matchers.match("xyz"));
    }

    @Test
    public void GivenEqualsPattern_WhenPositiveMatcherMatch_ThenFound() {
        // GIVEN
        matchers.addPattern("abc");

        // WHEN - THEN
        assertTrue(matchers.match("abc"));
    }

    @Test
    public void GivenEqualsPattern_WhenNegativeMatcherMatch_ThenNotFound() {
        // GIVEN
        matchers.addPattern("abc");

        // WHEN - THEN
        assertFalse(matchers.match("xabc"));
        assertFalse(matchers.match("abcx"));
        assertFalse(matchers.match("xabcx"));
        assertFalse(matchers.match("xyz"));
    }

    @Test
    public void GivenMultiplePattern_WhenPositiveMatcherMatch_ThenFound() {
        // GIVEN
        matchers.addPattern("*abc", "abc*");

        // WHEN - THEN
        assertTrue(matchers.match("abc"));
        assertTrue(matchers.match("abcx"));
        assertTrue(matchers.match("xabc"));
    }

    @Test
    public void GivenMultiplePattern_WhenNegativeMatcherMatch_ThenNotFound() {
        // GIVEN
        matchers.addPattern("*abc", "abc*");

        // WHEN - THEN
        assertFalse(matchers.match("xabcx"));
        assertFalse(matchers.match("xyz"));
    }

    @Test
    public void GivenRequestPathMatcher_WhenMatcherMatch_ThenFound() {
        // GIVEN
        String urlPatterns = "*.rmi,*.xsp,*/captcha/image/*,*/rest/*";
        matchers.addPattern(urlPatterns.split(","));

        // WHEN
        boolean match = matchers.match("res/page/com/kidscademy/ServiceController/getFeedbackData.rmi");

        // THEN
        assertTrue(match);
    }

    @Test
    public void GivenEmptyPattern_WhenMatcherMatch_ThenRejectAll() {
        // GIVEN
        String pattern = "anything";

        // WHEN
        boolean match = matchers.match(pattern);

        // THEN
        assertFalse(match);
    }

    @Test(expected = AssertionError.class)
    public void GivenNullParameter_WhenMatcherMatch_ThenAssertionError() {
        // GIVEN
        matchers.addPattern("abc");

        // WHEN
        matchers.match(null);

        // THEN
    }

    @Test(expected = AssertionError.class)
    public void GivenEmptyStringParameter_WhenMatcherMatch_ThenAssertionError() {
        // GIVEN
        matchers.addPattern("abc");

        // WHEN
        matchers.match("");

        // THEN
    }
}
