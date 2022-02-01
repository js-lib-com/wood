package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.impl.MediaQueryDefinition;
import js.wood.impl.Variants;

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
	public void locale() {
		Map<String, Locale> locales = new HashMap<>();
		locales.put("en-US", Locale.US);
		locales.put("en", Locale.ENGLISH);
		locales.put("RO", new Locale("ro"));
		locales.put("ro", new Locale("ro"));
		locales.put("ro-md", new Locale("ro", "MD"));
		locales.put("ro-MD", new Locale("ro", "MD"));
		locales.put("RO-MD", new Locale("ro", "MD"));

		for (String localePattern : locales.keySet()) {
			Variants variants = new Variants(file, localePattern);
			assertThat(variants.getLocale(), equalTo(locales.get(localePattern)));
		}
	}

	@Test(expected = WoodException.class)
	public void locale_Multiple() {
		new Variants(file, "jp_ro");
	}

	@Test
	public void mediaQueries() {
		List<MediaQueryDefinition> definitions = new ArrayList<>();
		definitions.add(new MediaQueryDefinition("w1200", "min-width: 1200px", 0));
		definitions.add(new MediaQueryDefinition("W992", "max-width: 992px", 0));
		definitions.add(new MediaQueryDefinition("h560", "min-height: 560px", 0));
		definitions.add(new MediaQueryDefinition("H1234", "max-height: 1234px", 0));
		definitions.add(new MediaQueryDefinition("landscape", "orientation: landscape", 0));
		definitions.add(new MediaQueryDefinition("portrait", "orientation: portrait", 0));

		for (MediaQueryDefinition definition : definitions) {
			when(project.getMediaQueryDefinition(definition.getAlias())).thenReturn(definition);
			Variants variants = new Variants(file, definition.getAlias());
			assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
			assertThat(variants.getMediaQueries().getQueries().get(0).getAlias(), equalTo(definition.getAlias()));
			assertThat(variants.getMediaQueries().getQueries().get(0).getExpression(), equalTo(definition.getExpression()));
		}
	}

	@Test
	public void mediaQueries_Multiple() {
		List<MediaQueryDefinition> definitions = new ArrayList<>();
		definitions.add(new MediaQueryDefinition("w1200", "min-width: 1200px", 0));
		definitions.add(new MediaQueryDefinition("landscape", "orientation: landscape", 1));
		definitions.add(new MediaQueryDefinition("r300", "resolution: 300dpi", 2));

		when(project.getMediaQueryDefinition("w1200")).thenReturn(definitions.get(0));
		when(project.getMediaQueryDefinition("landscape")).thenReturn(definitions.get(1));
		when(project.getMediaQueryDefinition("r300")).thenReturn(definitions.get(2));

		Variants variants = new Variants(file, "w1200_landscape_r300");
		assertThat(variants.getMediaQueries().getQueries(), hasSize(3));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 1200px ) and ( orientation: landscape ) and ( resolution: 300dpi )"));
		assertThat(variants.getMediaQueries().getWeight(), equalTo(6L));
	}

	@Test(expected = WoodException.class)
	public void mediaQueries_MissingDefinition() {
		new Variants(file, "w1200");
	}

	@Test
	public void locale_mediaQueries() {
		MediaQueryDefinition definition = new MediaQueryDefinition("w1200", "min-width: 1200px", 0);
		when(project.getMediaQueryDefinition("w1200")).thenReturn(definition);

		Variants variants = new Variants(file, "w1200_ro");
		assertThat(variants.getLocale(), equalTo(new Locale("ro")));
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 1200px )"));
		assertThat(variants.getMediaQueries().getWeight(), equalTo(1L));
	}

	/** Null variants parameter on constructor should create empty variants instance. */
	@Test
	public void nullVariants() {
		Variants variants = new Variants(file, null);
		assertThat(variants.hasLocale(), is(false));
		assertThat(variants.getMediaQueries().getQueries(), empty());
	}
}
