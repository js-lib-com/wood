package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.util.Strings;
import js.wood.Project;
import js.wood.WoodException;
import js.wood.impl.MediaQueries;
import js.wood.impl.MediaQueryDefinition;

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
	public void add() {
		assertTrue(mediaQueries.add("xsd"));
		assertThat(mediaQueries.getQueries(), not(empty()));
		assertThat(mediaQueries.getQueries(), hasSize(1));
	}

	@Test
	public void add_MissingAlias() {
		assertFalse(mediaQueries.add("fake"));
		assertThat(mediaQueries.getQueries(), empty());
	}

	@Test(expected = WoodException.class)
	public void add_Override() {
		mediaQueries.add("xsd");
		mediaQueries.add("xsd");
	}

	@Test(expected = IllegalArgumentException.class)
	public void add_Empty() {
		mediaQueries.add("");
	}

	@Test(expected = IllegalArgumentException.class)
	public void add_Null() {
		mediaQueries.add(null);
	}

	@Test
	public void getExpression() {
		mediaQueries.add("xsd");
		assertThat(mediaQueries.getExpression(), equalTo("( min-width: 560px )"));
		mediaQueries.add("portrait");
		assertThat(mediaQueries.getExpression(), equalTo("( min-width: 560px ) and ( orientation: portrait )"));
		mediaQueries.add("h800");
		assertThat(mediaQueries.getExpression(), equalTo("( min-width: 560px ) and ( orientation: portrait ) and ( min-height: 800px )"));
	}

	@Test
	public void getExpression_Comparator() {
		String[] patterns = new String[] { "lgd", "nod", "mdd", "smd", "xsd", "lgd_portrait", "nod_portrait", "mdd_portrait", "smd_portrait", "xsd_portrait", "lgd_landscape", "nod_landscape", "mdd_landscape", "smd_landscape", "xsd_landscape", "lgd_landscape_h800", "nod_landscape_h800", "mdd_landscape_h800", "smd_landscape_h800", "xsd_landscape_h800" };

		List<MediaQueries> definitions = new ArrayList<>();
		for (String pattern : patterns) {
			MediaQueries mediaQueries = new MediaQueries(project);
			for (String variant : Strings.split(pattern, '_')) {
				assertTrue("Variant not defined: " + variant, mediaQueries.add(variant));
			}
			definitions.add(mediaQueries);
		}

		Collections.sort(definitions, new Comparator<MediaQueries>() {
			@Override
			public int compare(MediaQueries o1, MediaQueries o2) {
				return o1.compareTo(o2);
			}
		});

		int index = 0;
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 560px )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 672px )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 800px )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 960px )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 1200px )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 560px ) and ( orientation: landscape )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 560px ) and ( orientation: portrait )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 672px ) and ( orientation: landscape )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 672px ) and ( orientation: portrait )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 800px ) and ( orientation: landscape )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 800px ) and ( orientation: portrait )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 960px ) and ( orientation: landscape )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 960px ) and ( orientation: portrait )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 1200px ) and ( orientation: landscape )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 1200px ) and ( orientation: portrait )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 560px ) and ( orientation: landscape ) and ( min-height: 800px )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 672px ) and ( orientation: landscape ) and ( min-height: 800px )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 800px ) and ( orientation: landscape ) and ( min-height: 800px )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 960px ) and ( orientation: landscape ) and ( min-height: 800px )"));
		assertThat(definitions.get(index++).getExpression(), equalTo("( min-width: 1200px ) and ( orientation: landscape ) and ( min-height: 800px )"));
	}

	@Test
	public void getWeight_Uniqueness() throws Exception {
		String[] patterns = new String[] { "lgd", "nod", "mdd", "smd", "xsd", "lgd_portrait", "nod_portrait", "mdd_portrait", "smd_portrait", "xsd_portrait", "lgd_landscape", "nod_landscape", "mdd_landscape", "smd_landscape", "xsd_landscape", "lgd_landscape_h800", "nod_landscape_h800", "mdd_landscape_h800", "smd_landscape_h800", "xsd_landscape_h800" };

		Set<Long> weights = new HashSet<>();
		for (String pattern : patterns) {
			mediaQueries = new MediaQueries(project);
			for (String variant : Strings.split(pattern, '_')) {
				assertTrue("Variant not defined: " + variant, mediaQueries.add(variant));
			}
			Long weight = mediaQueries.getWeight();
			if (!weights.add(weight)) {
				Assert.fail("Not unique value for variant weight.");
			}
		}
	}

	@Test
	public void getWeight_Order() throws Exception {
		class Item {
			String pattern;
			Long weight;

			public Item(String pattern, Long weight) {
				this.pattern = pattern;
				this.weight = weight;
			}
		}

		String[] patterns = new String[] { "lgd", "nod", "mdd", "smd", "xsd", "lgd_portrait", "nod_portrait", "mdd_portrait", "smd_portrait", "xsd_portrait", "lgd_landscape", "nod_landscape", "mdd_landscape", "smd_landscape", "xsd_landscape", "lgd_landscape_h800", "nod_landscape_h800", "mdd_landscape_h800", "smd_landscape_h800", "xsd_landscape_h800" };

		List<Item> items = new ArrayList<>();
		for (String pattern : patterns) {
			mediaQueries = new MediaQueries(project);
			for (String variant : Strings.split(pattern, '_')) {
				assertTrue("Variant not defined: " + variant, mediaQueries.add(variant));
			}
			Long weight = mediaQueries.getWeight();
			if (!items.add(new Item(pattern, weight))) {
				Assert.fail("Not unique value for variant weight.");
			}
		}

		items.sort(new Comparator<Item>() {
			@Override
			public int compare(Item o1, Item o2) {
				return o1.weight.compareTo(o2.weight);
			}
		});

		int index = 0;
		assertThat(items.get(index++).pattern, equalTo("xsd"));
		assertThat(items.get(index++).pattern, equalTo("smd"));
		assertThat(items.get(index++).pattern, equalTo("mdd"));
		assertThat(items.get(index++).pattern, equalTo("nod"));
		assertThat(items.get(index++).pattern, equalTo("lgd"));
		assertThat(items.get(index++).pattern, equalTo("xsd_landscape"));
		assertThat(items.get(index++).pattern, equalTo("xsd_portrait"));
		assertThat(items.get(index++).pattern, equalTo("smd_landscape"));
		assertThat(items.get(index++).pattern, equalTo("smd_portrait"));
		assertThat(items.get(index++).pattern, equalTo("mdd_landscape"));
		assertThat(items.get(index++).pattern, equalTo("mdd_portrait"));
		assertThat(items.get(index++).pattern, equalTo("nod_landscape"));
		assertThat(items.get(index++).pattern, equalTo("nod_portrait"));
		assertThat(items.get(index++).pattern, equalTo("lgd_landscape"));
		assertThat(items.get(index++).pattern, equalTo("lgd_portrait"));
		assertThat(items.get(index++).pattern, equalTo("xsd_landscape_h800"));
		assertThat(items.get(index++).pattern, equalTo("smd_landscape_h800"));
		assertThat(items.get(index++).pattern, equalTo("mdd_landscape_h800"));
		assertThat(items.get(index++).pattern, equalTo("nod_landscape_h800"));
		assertThat(items.get(index++).pattern, equalTo("lgd_landscape_h800"));
	}

	private static Map<String, MediaQueryDefinition> definitions() {
		Map<String, MediaQueryDefinition> definitions = new HashMap<>();
		definitions.put("xsd", new MediaQueryDefinition("xsd", "min-width: 560px", 0));
		definitions.put("smd", new MediaQueryDefinition("smd", "min-width: 672px", 1));
		definitions.put("mdd", new MediaQueryDefinition("mdd", "min-width: 800px", 2));
		definitions.put("nod", new MediaQueryDefinition("nod", "min-width: 960px", 3));
		definitions.put("lgd", new MediaQueryDefinition("lgd", "min-width: 1200px", 4));
		definitions.put("h800", new MediaQueryDefinition("h800", "min-height: 800px", 5));
		definitions.put("landscape", new MediaQueryDefinition("landscape", "orientation: landscape", 6));
		definitions.put("portrait", new MediaQueryDefinition("portrait", "orientation: portrait", 7));
		return definitions;
	}
}
