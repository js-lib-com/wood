package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import js.wood.WoodException;
import js.wood.impl.Variants;

public class VariantsTest {
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
			Variants variants = new Variants(localePattern);
			assertThat(variants.getLocale(), equalTo(locales.get(localePattern)));
		}
	}

	@Test(expected = WoodException.class)
	public void locale_Multiple() {
		new Variants("jp_ro");
	}

	@Test
	public void viewportWidth() {
		Map<String, Integer> widths = new HashMap<>();
		widths.put("w1200", 1200);
		widths.put("w992", 992);
		widths.put("w768", 768);
		widths.put("w560", 560);
		widths.put("w1234", 1234);

		for (String widthPattern : widths.keySet()) {
			Variants variants = new Variants(widthPattern);
			assertTrue(variants.hasViewportWidth());
			assertThat(variants.getViewportWidth(), equalTo(widths.get(widthPattern)));
		}
	}

	@Test(expected = WoodException.class)
	public void viewportWidth_Multiple() {
		new Variants("w1200_w992");
	}

	@Test(expected = WoodException.class)
	public void viewportWidth_TooFewDigits() {
		new Variants("w99");
	}

	@Test(expected = WoodException.class)
	public void viewportWidth_TooManyDigits() {
		new Variants("w10000");
	}

	@Test
	public void viewportHeight() {
		Map<String, Integer> heights = new HashMap<>();
		heights.put("h400", 400);
		heights.put("h600", 600);
		heights.put("h800", 800);
		heights.put("h1024", 1024);

		for (String heightPattern : heights.keySet()) {
			Variants variants = new Variants(heightPattern);
			assertThat(variants.getViewportHeight(), equalTo(heights.get(heightPattern)));
		}
	}

	@Test(expected = WoodException.class)
	public void viewportHeight_Multiple() {
		new Variants("h400_h600");
	}

	@Test(expected = WoodException.class)
	public void viewportheight_TooFewDigits() {
		new Variants("h99");
	}

	@Test(expected = WoodException.class)
	public void viewportHeight_TooManyDigits() {
		new Variants("h10000");
	}

	@Test
	public void screen() {
		Map<String, Variants.Screen> screens = new HashMap<>();
		screens.put("lgd", Variants.Screen.LARGE);
		screens.put("nod", Variants.Screen.NORMAL);
		screens.put("mdd", Variants.Screen.MEDIUM);
		screens.put("smd", Variants.Screen.SMALL);
		screens.put("xsd", Variants.Screen.EXTRA_SMALL);

		for (String pattern : screens.keySet()) {
			Variants variants = new Variants(pattern);
			assertTrue(variants.hasScreen());
			assertThat(variants.getScreen(), equalTo(screens.get(pattern)));
		}
	}

	@Test
	public void screen_Width() {
		Map<String, Integer> resolutions = new HashMap<>();
		resolutions.put("lgd", 1200);
		resolutions.put("nod", 1200);
		resolutions.put("mdd", 992);
		resolutions.put("smd", 768);
		resolutions.put("xsd", 560);

		for (String name : resolutions.keySet()) {
			Variants variants = new Variants(name);
			assertTrue(variants.hasScreen());
			assertThat(variants.getScreen(), notNullValue());
			assertThat(variants.getScreen().getWidth(), equalTo(resolutions.get(name)));
		}
	}

	@Test(expected = WoodException.class)
	public void screen_Multiple() {
		new Variants("lgd_nod");
	}

	@Test
	public void orientation() {
		Map<String, Variants.Orientation> orientations = new HashMap<>();
		orientations.put("landscape", Variants.Orientation.LANDSCAPE);
		orientations.put("portrait", Variants.Orientation.PORTRAIT);

		for (String pattern : orientations.keySet()) {
			Variants variants = new Variants(pattern);
			assertTrue(variants.hasOrientation());
			assertThat(variants.getOrientation(), equalTo(orientations.get(pattern)));
		}
	}

	@Test(expected = WoodException.class)
	public void orientation_Multiple() {
		new Variants("landscape_portrait");
	}

	@Test
	public void locale_viewportWidth() {
		Variants variants = new Variants("en-US_w992");
		assertThat(variants.hasLocale(), is(true));
		assertThat(variants.hasViewportWidth(), is(true));
		assertThat(variants.hasViewportHeight(), is(false));
		assertThat(variants.hasScreen(), is(false));
		assertThat(variants.hasOrientation(), is(false));
		assertThat(variants.getLocale(), equalTo(Locale.US));
		assertThat(variants.getViewportWidth(), equalTo(992));
	}

	@Test
	public void locale_viewportHeight() {
		Variants variants = new Variants("en-US_h600");
		assertThat(variants.hasLocale(), is(true));
		assertThat(variants.hasViewportWidth(), is(false));
		assertThat(variants.hasViewportHeight(), is(true));
		assertThat(variants.hasScreen(), is(false));
		assertThat(variants.hasOrientation(), is(false));
		assertThat(variants.getLocale(), equalTo(Locale.US));
		assertThat(variants.getViewportHeight(), equalTo(600));
	}

	@Test
	public void locale_viewportWidth_viewportHeight() {
		Variants variants = new Variants("en-US_w992_h600");
		assertThat(variants.hasLocale(), is(true));
		assertThat(variants.hasViewportWidth(), is(true));
		assertThat(variants.hasViewportHeight(), is(true));
		assertThat(variants.hasScreen(), is(false));
		assertThat(variants.hasOrientation(), is(false));
		assertThat(variants.getLocale(), equalTo(Locale.US));
		assertThat(variants.getViewportWidth(), equalTo(992));
		assertThat(variants.getViewportHeight(), equalTo(600));
	}

	@Test
	public void locale_screen() {
		Variants variants = new Variants("en-US_lgd");
		assertThat(variants.hasLocale(), is(true));
		assertThat(variants.hasViewportWidth(), is(false));
		assertThat(variants.hasViewportHeight(), is(false));
		assertThat(variants.hasScreen(), is(true));
		assertThat(variants.hasOrientation(), is(false));
		assertThat(variants.getLocale(), equalTo(Locale.US));
		assertThat(variants.getScreen(), equalTo(Variants.Screen.LARGE));
	}

	@Test
	public void locale_screen_orientation() {
		Variants variants = new Variants("en-US_lgd_landscape");
		assertThat(variants.hasLocale(), is(true));
		assertThat(variants.hasViewportWidth(), is(false));
		assertThat(variants.hasViewportHeight(), is(false));
		assertThat(variants.hasScreen(), is(true));
		assertThat(variants.hasOrientation(), is(true));
		assertThat(variants.getLocale(), equalTo(Locale.US));
		assertThat(variants.getScreen(), equalTo(Variants.Screen.LARGE));
		assertThat(variants.getOrientation(), equalTo(Variants.Orientation.LANDSCAPE));
	}

	@Test(expected = WoodException.class)
	public void invalid() {
		new Variants("fake");
	}

	/** Null variants parameter on constructor should create empty variants instance. */
	@Test
	public void nullVariants() {
		Variants variants = new Variants(null);
		assertThat(variants.hasLocale(), is(false));
		assertThat(variants.hasViewportWidth(), is(false));
		assertThat(variants.hasViewportHeight(), is(false));
		assertThat(variants.hasScreen(), is(false));
		assertThat(variants.hasOrientation(), is(false));
	}
}
