package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.IOException;
import java.io.StringWriter;
import java.util.Map;

import org.junit.Test;

import js.util.Files;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.StyleReader;

public class StyleReaderTest extends WoodTestCase {
	private Project project;

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}

	@Test
	public void styleReaderConstructor() {
		project = project("styles");
		FilePath styleFile = filePath("res/page/page.css");
		StyleReader reader = new StyleReader(styleFile);

		assertNotNull(field(reader, "reader"));
		assertEquals("BASE_CONTENT", field(reader, "state").toString());

		Map<FilePath, String> variants = field(reader, "variants");
		assertNotNull(variants);
		assertEquals(12, variants.size());

		assertTrue(variants.keySet().contains(filePath("res/page/page_mdd.css")));
		assertTrue(variants.keySet().contains(filePath("res/page/page_mdd_portrait.css")));
		assertTrue(variants.keySet().contains(filePath("res/page/page_mdd_landscape.css")));

		assertTrue(variants.keySet().contains(filePath("res/page/page_smd.css")));
		assertTrue(variants.keySet().contains(filePath("res/page/page_smd_portrait.css")));
		assertTrue(variants.keySet().contains(filePath("res/page/page_smd_landscape.css")));

		assertTrue(variants.keySet().contains(filePath("res/page/page_xsd.css")));
		assertTrue(variants.keySet().contains(filePath("res/page/page_xsd_portrait.css")));
		assertTrue(variants.keySet().contains(filePath("res/page/page_xsd_landscape.css")));

		assertTrue(variants.keySet().contains(filePath("res/page/page_w1200.css")));
		assertTrue(variants.keySet().contains(filePath("res/page/page_w800.css")));
		assertTrue(variants.keySet().contains(filePath("res/page/page_h600.css")));
	}

	@Test
	public void styleReaderProcessing() throws IOException {
		project = project("styles");
		FilePath styleFile = filePath("res/page/page.css");
		StyleReader reader = new StyleReader(styleFile);

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		String style = writer.toString();

		assertTrue(style.contains("@style/dialog"));
		assertTrue(style.contains("width: 960px;"));
		assertTrue(style.contains("width: @eval(add @dimen/mobile-width @dimen/desktop-width);"));
		assertTrue(style.contains("@media screen and (max-width : 1200px) {"));
		assertTrue(style.contains("width: @dimen/desktop-width;"));
		assertTrue(style.contains("@media screen and (max-width : 800px) {"));
		assertTrue(style.contains("width: @dimen/mobile-width;"));
		assertTrue(style.contains("@media screen and (max-height : 600px) {"));
		assertTrue(style.contains("height: @dimen/mobile-height;"));
		// assertTrue(style.contains("@media screen and (max-device-width : 767px) and (orientation: portrait) {"));
		// assertTrue(style.contains("@media screen and (max-device-width : 767px) {"));
		// assertTrue(style.contains("@media screen and (min-device-width : 768px) {"));
	}
}
