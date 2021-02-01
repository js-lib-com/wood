package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.util.Strings;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.impl.MediaQueries;
import js.wood.impl.Variants;

@Ignore
@RunWith(MockitoJUnitRunner.class)
public class MediaQueriesTest {
	@Mock
	private Project project;

	private MediaQueries mediaQueries;

	@Test
	public void getWeight_Uniqueness() throws Exception {
		String[] patterns = new String[] { "lgd", "nod", "mdd", "smd", "xsd", "lgd_portrait", "nod_portrait", "mdd_portrait", "smd_portrait", "xsd_portrait", "lgd_landscape", "nod_landscape", "mdd_landscape", "smd_landscape", "xsd_landscape", "w1200", "w800", "w600", "lgd_landscape_h800", "nod_landscape_h800", "mdd_landscape_h800", "smd_landscape_h800", "xsd_landscape_h800" };

		Set<Long> weights = new HashSet<>();
		for (String pattern : patterns) {
			mediaQueries = new MediaQueries(project);
			for (String variant : Strings.split(pattern, '_')) {
				mediaQueries.add(variant);
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

		String[] patterns = new String[] { "lgd", "nod", "mdd", "smd", "xsd", "lgd_portrait", "nod_portrait", "mdd_portrait", "smd_portrait", "xsd_portrait", "lgd_landscape", "nod_landscape", "mdd_landscape", "smd_landscape", "xsd_landscape", "w1200", "w800", "w600", "lgd_landscape_h800", "nod_landscape_h800", "mdd_landscape_h800", "smd_landscape_h800", "xsd_landscape_h800" };

		List<Item> items = new ArrayList<>();
		for (String pattern : patterns) {
			Variants variants = new Variants(new FilePath(project, "page.css"), pattern);
			Long weight = variants.getMediaQueries().getWeight();
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

		assertThat(items.get(0).pattern, equalTo("xsd"));
		assertThat(items.get(1).pattern, equalTo("w600"));
		assertThat(items.get(2).pattern, equalTo("smd"));
		assertThat(items.get(3).pattern, equalTo("w800"));
		assertThat(items.get(4).pattern, equalTo("mdd"));
		assertThat(items.get(5).pattern, equalTo("lgd"));
		assertThat(items.get(6).pattern, equalTo("nod"));
		assertThat(items.get(7).pattern, equalTo("w1200"));
		assertThat(items.get(8).pattern, equalTo("xsd_landscape"));
		assertThat(items.get(9).pattern, equalTo("xsd_landscape_h800"));
		assertThat(items.get(10).pattern, equalTo("smd_landscape"));
		assertThat(items.get(11).pattern, equalTo("smd_landscape_h800"));
		assertThat(items.get(12).pattern, equalTo("mdd_landscape"));
		assertThat(items.get(13).pattern, equalTo("mdd_landscape_h800"));
		assertThat(items.get(14).pattern, equalTo("lgd_landscape"));
		assertThat(items.get(15).pattern, equalTo("nod_landscape"));
		assertThat(items.get(16).pattern, equalTo("lgd_landscape_h800"));
		assertThat(items.get(17).pattern, equalTo("nod_landscape_h800"));
		assertThat(items.get(18).pattern, equalTo("xsd_portrait"));
		assertThat(items.get(19).pattern, equalTo("smd_portrait"));
		assertThat(items.get(20).pattern, equalTo("mdd_portrait"));
		assertThat(items.get(21).pattern, equalTo("lgd_portrait"));
		assertThat(items.get(22).pattern, equalTo("nod_portrait"));
	}
}
