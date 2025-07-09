package com.jslib.wood.impl;

import com.jslib.wood.Project;
import com.jslib.wood.WoodException;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.HashMap;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class MediaQueriesTest {
	@Mock
	private Project project;

	private MediaQueries mediaQueries;

	@Before
	public void beforeTest() {
		Map<String, MediaQueryDefinition> definitions = definitions();
		for (String alias : definitions.keySet()) {
			when(project.getMediaQueryDefinition(alias)).thenReturn(definitions.get(alias));
		}

		mediaQueries = new MediaQueries(project);
	}

	@Test
	public void GivenDefinedAlias_WhenAddMediaQueryDefinition_ThenAdded() {
		// GIVEN
		String alias = "xsd";

		// WHEN
		boolean result = mediaQueries.add(alias);

		// THEN
		assertTrue(result);
		assertThat(mediaQueries.getQueries(), hasSize(1));

		MediaQueryDefinition query = mediaQueries.getQueries().get(0);
		assertThat(query.getAlias(), equalTo(alias));
		assertThat(query.getMedia(), equalTo("screen"));
		assertThat(query.getExpression(), equalTo("min-width: 560px"));
	}

	@Test
	public void GivenNotDefinedAlias_WhenAddMediaQueryDefinition_ThenNotAdded() {
		// GIVEN
		String alias = "fake";

		// WHEN
		boolean result = mediaQueries.add(alias);

		// THEN
		assertFalse(result);
		assertThat(mediaQueries.getQueries(), empty());
	}

	@Test(expected = WoodException.class)
	public void GivenAliasOverride_WhenAddMediaQueryDefinition_ThenWoodException() {
		// GIVEN
		String alias = "xsd";

		// WHEN
		mediaQueries.add(alias);
		mediaQueries.add(alias);

		// THEN
	}

	@Test(expected = AssertionError.class)
	public void GivenEmptyAlias_WhenAddMediaQueryDefinition_ThenAssertionError() {
		// GIVEN
		String alias = "";

		// WHEN
		mediaQueries.add(alias);

		// THEN
	}

	@SuppressWarnings("ConstantConditions")
	@Test(expected = AssertionError.class)
	public void GivenNullAlias_WhenAddMediaQueryDefinition_ThenAssertionError() {
		// GIVEN
		String alias = null;

		// WHEN
		mediaQueries.add(alias);

		// THEN
	}

	@Test
	public void GivenMultipleAliases_WhenGetExpression_TheListOfConditions() {
		// GIVEN
		mediaQueries.add("xsd");
		mediaQueries.add("portrait");
		mediaQueries.add("h800");

		// WHEN
		String expression = mediaQueries.getExpression();

		// THEN
		assertThat(expression, equalTo("( min-width: 560px ) and ( orientation: portrait ) and ( min-height: 800px )"));
	}

	@Test
	public void GivenMultipleAliasesOnScreenMedia_WhenGetMedia_TheScreen() {
		// GIVEN
		mediaQueries.add("xsd");
		mediaQueries.add("portrait");
		mediaQueries.add("h800");

		// WHEN
		String expression = mediaQueries.getMedia();

		// THEN
		assertThat(expression, equalTo("screen"));
	}

	@Test
	public void GivenAliasOnPrintMedia_WhenGetMedia_ThePrint() {
		// GIVEN
		mediaQueries.add("A4");

		// WHEN
		String expression = mediaQueries.getMedia();

		// THEN
		assertThat(expression, equalTo("print"));
	}

	@Test
	public void GivenMultipleAliasesOnScreenAndPrintMedia_WhenGetMedia_TheAll() {
		// GIVEN
		mediaQueries.add("xsd");
		mediaQueries.add("A4");

		// WHEN
		String expression = mediaQueries.getMedia();

		// THEN
		assertThat(expression, equalTo("all"));
	}

	private static Map<String, MediaQueryDefinition> definitions() {
		Map<String, MediaQueryDefinition> definitions = new HashMap<>();
		definitions.put("xsd", new MediaQueryDefinition("xsd","screen", "min-width: 560px"));
		definitions.put("smd", new MediaQueryDefinition("smd","screen", "min-width: 672px"));
		definitions.put("mdd", new MediaQueryDefinition("mdd","screen", "min-width: 800px"));
		definitions.put("nod", new MediaQueryDefinition("nod","screen", "min-width: 960px"));
		definitions.put("lgd", new MediaQueryDefinition("lgd","screen", "min-width: 1200px"));
		definitions.put("h800", new MediaQueryDefinition("h800","screen", "min-height: 800px"));
		definitions.put("landscape", new MediaQueryDefinition("landscape","screen", "orientation: landscape"));
		definitions.put("portrait", new MediaQueryDefinition("portrait","screen", "orientation: portrait"));
		definitions.put("A4", new MediaQueryDefinition("A4","print", null));
		return definitions;
	}
}
