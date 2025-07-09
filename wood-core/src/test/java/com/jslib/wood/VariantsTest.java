package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.wood.impl.MediaQueryDefinition;
import com.jslib.wood.impl.Variants;

@RunWith(MockitoJUnitRunner.class)
public class VariantsTest {
	@Mock
	private Project project;
	@Mock
	private FilePath file;

	@Before
	public void beforeTest() {
		when(file.getProject()).thenReturn(project);
	}

	@Test
	public void GivenSingleLanguageVariant_ThenIncludeLanguage() {
		// GIVEN
		List<String> languages = new ArrayList<>();
		languages.add("en-US");
		languages.add("en");
		languages.add("RO");
		languages.add("ro");
		languages.add("ro-md");
		languages.add("ro-MD");
		languages.add("RO-MD");

		for (String languagePattern : languages) {
			// WHEN
			Variants variants = new Variants(file, languagePattern);

			// THEN
			assertThat(variants.getLanguage(), equalTo(languagePattern));
		}
	}

	@Test(expected = WoodException.class)
	public void GivenMultipleLanguage_ThenWoodException() {
		new Variants(file, "jp_ro");
	}

	@Test
	public void GivenSingleMediaQueryDefinition_ThenVariantsWithMediaQuery() {
		// GIVEN
		List<MediaQueryDefinition> definitions = new ArrayList<>();
		definitions.add(new MediaQueryDefinition("w1200", "min-width: 1200px", 0));
		definitions.add(new MediaQueryDefinition("W992", "max-width: 992px", 0));
		definitions.add(new MediaQueryDefinition("h560", "min-height: 560px", 0));
		definitions.add(new MediaQueryDefinition("H1234", "max-height: 1234px", 0));
		definitions.add(new MediaQueryDefinition("landscape", "orientation: landscape", 0));
		definitions.add(new MediaQueryDefinition("portrait", "orientation: portrait", 0));

		for (MediaQueryDefinition definition : definitions) {
			when(project.getMediaQueryDefinition(definition.getAlias())).thenReturn(definition);

			// WHEN
			Variants variants = new Variants(file, definition.getAlias());

			// THEN
			assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
			assertThat(variants.getMediaQueries().getQueries().get(0).getAlias(), equalTo(definition.getAlias()));
			assertThat(variants.getMediaQueries().getQueries().get(0).getExpression(), equalTo(definition.getExpression()));
		}
	}

	@Test
	public void GivenMultipleMediaQueryDefinitions_ThenVariantsWithAllMediaQueries() {
		// GIVEN
		List<MediaQueryDefinition> definitions = new ArrayList<>();
		definitions.add(new MediaQueryDefinition("w1200", "min-width: 1200px", 0));
		definitions.add(new MediaQueryDefinition("landscape", "orientation: landscape", 1));
		definitions.add(new MediaQueryDefinition("r300", "resolution: 300dpi", 2));

		when(project.getMediaQueryDefinition("w1200")).thenReturn(definitions.get(0));
		when(project.getMediaQueryDefinition("landscape")).thenReturn(definitions.get(1));
		when(project.getMediaQueryDefinition("r300")).thenReturn(definitions.get(2));

		// WHEN
		Variants variants = new Variants(file, "w1200_landscape_r300");

		// THEN
		assertThat(variants.getMediaQueries().getQueries(), hasSize(3));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 1200px ) and ( orientation: landscape ) and ( resolution: 300dpi )"));
		assertThat(variants.getMediaQueries().getWeight(), equalTo(6L));
	}

	@Test(expected = WoodException.class)
	public void GivenMissingMediaQueryAlias_ThenWoodException() {
		new Variants(file, "w1200");
	}

	@Test
	public void GivenMediaQueryDefinitionAndLanguage_ThenLanguageAndMediaQueryIncluded() {
		// GIVEN
		MediaQueryDefinition definition = new MediaQueryDefinition("w1200", "min-width: 1200px", 0);
		when(project.getMediaQueryDefinition("w1200")).thenReturn(definition);

		// WHEN
		Variants variants = new Variants(file, "w1200_ro");

		// THEN
		assertThat(variants.getLanguage(), equalTo("ro"));
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 1200px )"));
		assertThat(variants.getMediaQueries().getWeight(), equalTo(1L));
	}

	/** Null variants parameter on constructor should create empty variants instance. */
	@Test
	public void GivenNullVariants_ThenEmptyMediaQueries() {
		// GIVEN

		// WHEN
		Variants variants = new Variants(file, null);

		// THEN
		assertThat(variants.getMediaQueries().getQueries(), empty());
	}
}
