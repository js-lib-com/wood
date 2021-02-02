package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasItem;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import java.io.IOException;
import java.io.StringWriter;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.util.Files;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.StyleReader;

public class StyleReaderTest {
	private Project project;

	@Before
	public void beforeTest() {
		project = new Project("src/test/resources/styles");
	}

	@Test
	public void constructor() {
		FilePath styleFile = new FilePath(project, "res/page/page.css");
		StyleReader reader = new StyleReader(styleFile);

		assertNotNull(Classes.getFieldValue(reader, "reader"));
		assertEquals("BASE_CONTENT", Classes.getFieldValue(reader, "state").toString());

		List<FilePath> variants = Classes.getFieldValue(reader, "variants");
		assertThat(variants, notNullValue());
		assertThat(variants.size(), equalTo(14));

		assertThat(variants, hasItem(key("res/page/page_lgd.css")));
		assertThat(variants, hasItem(key("res/page/page_nod.css")));

		assertThat(variants, hasItem(key("res/page/page_mdd.css")));
		assertThat(variants, hasItem(key("res/page/page_mdd_portrait.css")));
		assertThat(variants, hasItem(key("res/page/page_mdd_landscape.css")));

		assertThat(variants, hasItem(key("res/page/page_smd.css")));
		assertThat(variants, hasItem(key("res/page/page_smd_portrait.css")));
		assertThat(variants, hasItem(key("res/page/page_smd_landscape.css")));

		assertThat(variants, hasItem(key("res/page/page_xsd.css")));
		assertThat(variants, hasItem(key("res/page/page_xsd_portrait.css")));
		assertThat(variants, hasItem(key("res/page/page_xsd_landscape.css")));

		assertThat(variants, hasItem(key("res/page/page_w1200.css")));
		assertThat(variants, hasItem(key("res/page/page_w800.css")));
		assertThat(variants, hasItem(key("res/page/page_h600.css")));
	}

	private FilePath key(String path) {
		return new FilePath(project, path);
	}

	@Test
	public void processing() throws IOException {
		FilePath styleFile = new FilePath(project, "res/page/page.css");
		StyleReader reader = new StyleReader(styleFile);

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		String style = writer.toString();

		assertThat(style, containsString("@style/dialog"));
		assertThat(style, containsString("width: 960px;"));
		assertThat(style, containsString("width: @eval(add @dimen/mobile-width @dimen/desktop-width);"));
		assertThat(style, containsString("@media screen and ( min-width: 1200px ) {"));
		assertThat(style, containsString("width: @dimen/desktop-width;"));
		assertThat(style, containsString("@media screen and ( min-width: 800px ) {"));
		assertThat(style, containsString("width: @dimen/mobile-width;"));
		assertThat(style, containsString("@media screen and ( min-height: 600px ) {"));
		assertThat(style, containsString("height: @dimen/mobile-height;"));
		assertThat(style, containsString("@media screen and ( min-height: 768px ) and ( orientation: portrait ) {"));
		assertThat(style, containsString("@media screen and ( min-height: 768px ) {"));
	}
}
