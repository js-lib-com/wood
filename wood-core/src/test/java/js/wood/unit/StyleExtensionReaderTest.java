package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.FileReader;
import java.io.IOException;
import java.io.StringWriter;
import java.util.Iterator;

import org.junit.Test;

import js.util.Files;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.StyleReader;
import js.wood.impl.StyleExtensionReader;

public class StyleExtensionReaderTest extends WoodTestCase{
	private Project project;

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}

	@Test
	public void styleExtensionReaderIterator() {
		Iterator<Character> iterator = newInstance("js.wood.impl.StyleExtensionReader$ExtensionsIterator");
		String[] properties = new String[] { "-moz-column-count", "-webkit-column-count" };
		invoke(iterator, "init", properties, "3");

		StringBuilder extensions = new StringBuilder();
		while (iterator.hasNext()) {
			extensions.append(iterator.next());
		}

		assertEquals("\r\n\t-moz-column-count: 3;\r\n\t-webkit-column-count: 3;", extensions.toString());
	}

	@Test
	public void styleExtensionReaderExtensions() {
		Object extensions = newInstance("js.wood.impl.StyleExtensionReader$Extensions");
		assertExtensions(extensions, "column-count: 3;", "\r\n\t-moz-column-count: 3;\r\n\t-webkit-column-count: 3;");
		assertExtensions(extensions, "column-width: auto;", "\r\n\t-moz-column-width: auto;\r\n\t-webkit-column-width: auto;");
		assertExtensions(extensions, "column-gap: 40px;", "\r\n\t-moz-column-gap: 40px;\r\n\t-webkit-column-gap: 40px;");
		assertExtensions(extensions, "column-fill: auto;", "\r\n\t-moz-column-fill: auto;");
	}

	private static void assertExtensions(Object extensions, String declaration, String expected) {
		Object builder = newInstance("js.wood.impl.StyleExtensionReader$DeclarationBuilder");

		invoke(builder, "reset");
		for (int i = 0; i < declaration.length(); ++i) {
			invoke(builder, "add", (int) declaration.charAt(i));
		}

		Iterator<Character> iterator = invoke(extensions, "getIterator", builder);
		StringBuilder test = new StringBuilder();
		while (iterator.hasNext()) {
			test.append(iterator.next());
		}

		assertEquals(expected, test.toString());
	}

	@Test
	public void styleExtensionReaderBuilder() {
		Object builder = newInstance("js.wood.impl.StyleExtensionReader$DeclarationBuilder");
		assertBuilder(builder, "column-count:3;", "column-count", "3");
		assertBuilder(builder, "column-count : 3;", "column-count", "3");
		assertBuilder(builder, "column-count\t:\t3;", "column-count", "3");
	}

	private static void assertBuilder(Object builder, String declaration, String property, String value) {
		invoke(builder, "reset");
		for (int i = 0; i < declaration.length(); ++i) {
			invoke(builder, "add", (int) declaration.charAt(i));
		}

		assertEquals(property, invoke(builder, "getProperty"));
		assertEquals(value, invoke(builder, "getValue"));
	}

	@Test
	public void styleExtensionReaderProcessing() throws IOException {
		project = project("styles");
		FilePath styleFile = filePath("res/index/index.css");
		StyleExtensionReader reader = new StyleExtensionReader(new FileReader(styleFile.toFile()));

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		String style = writer.toString();

		assertTrue(style.contains("min-height: 400px;"));
		assertTrue(style.contains("column-count: 4;"));
		assertTrue(style.contains("-moz-column-count: 4;"));
		assertTrue(style.contains("-webkit-column-count: 4;"));
	}

	@Test
	public void styleExtensionReaderVariantProcessing() throws IOException {
		project = project("styles");
		FilePath styleFile = filePath("res/index/index.css");
		StyleReader reader = new StyleReader(styleFile);

		StringWriter writer = new StringWriter();
		Files.copy(reader, writer);
		String style = writer.toString();

		// base style
		assertTrue(style.contains("min-height: 400px;"));
		assertTrue(style.contains("column-count: 4;"));
		assertTrue(style.contains("-moz-column-count: 4;"));
		assertTrue(style.contains("-webkit-column-count: 4;"));

		// mobile variant
		assertTrue(style.contains("column-count: 3;"));
		assertTrue(style.contains("-moz-column-count: 3;"));
		assertTrue(style.contains("-webkit-column-count: 3;"));
	}

}
