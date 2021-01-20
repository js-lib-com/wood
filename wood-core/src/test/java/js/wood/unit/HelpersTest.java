package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Iterator;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Test;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.w3c.DocumentBuilderImpl;
import js.log.Log;
import js.log.LogFactory;
import js.util.Files;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.StyleReader;
import js.wood.impl.FileType;
import js.wood.impl.LayoutReader;
import js.wood.impl.ResourceType;
import js.wood.impl.StyleExtensionReader;
import js.wood.impl.Variants;

public class HelpersTest extends WoodTestCase {
	private static final Log log = LogFactory.getLog(HelpersTest.class);

	private Project project;

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}

	// ------------------------------------------------------
	// Variants

	@Test
	public void variantsConstructor() {
		log.trace("testVariantsConstructor()");
		Variants variants = new Variants("xsd");
		assertEquals(Variants.Screen.EXTRA_SMALL, variants.getScreen());
	}

	@Test
	public void variantsPatterns() {
		log.trace("testVariantsPatterns()");
		Pattern pattern = Variants.Screen.PATTERN;
		assertTrue(match(pattern, "lgd"));
		assertTrue(match(pattern, "mdd"));
		assertTrue(match(pattern, "smd"));
		assertTrue(match(pattern, "xsd"));
	}

	private static boolean match(Pattern pattern, String variant) {
		Matcher matcher = pattern.matcher(variant);
		return matcher.find();
	}

	// ------------------------------------------------------
	// LayoutReader

	@Test
	public void layoutReader() throws IOException {
		log.trace("testLayoutReader()");
		Reader reader = new StringReader("<h1>header 1</h1><h2>header 2</h2>");
		Reader layoutReader = new LayoutReader(reader);

		StringWriter stringWriter = new StringWriter();
		Files.copy(layoutReader, stringWriter);

		DocumentBuilder builder = new DocumentBuilderImpl();
		Document doc = builder.parseXML(stringWriter.toString());
		assertEquals("layout", doc.getRoot().getTag());
		assertEquals("header 1", doc.getRoot().getByTag("h1").getText());
		assertEquals("header 2", doc.getRoot().getByTag("h2").getText());
	}

	// ------------------------------------------------------
	// StyleReader

	@Test
	public void styleReaderConstructor() {
		log.trace("testStyleReaderConstructor()");
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
		log.trace("testStyleReaderProcessing()");
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

	// ------------------------------------------------------
	// StyleExtensionReader

	@Test
	public void styleExtensionReaderIterator() {
		log.trace("testStyleExtensionReaderIterator()");
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
		log.trace("testStyleExtensionReaderExtensions()");
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
		log.trace("testStyleExtensionReaderBuilder()");
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
		log.trace("testStyleExtensionReaderProcessing()");
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
		log.trace("testStyleExtensionReaderVariantProcessing()");
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

	// ------------------------------------------------------
	// ResourceType

	@Test
	public void resourceTypeValueOf() {
		log.trace("testResourceTypeValueOf()");
		assertEquals(ResourceType.STRING, ResourceType.getValueOf("string"));
		assertEquals(ResourceType.STRING, ResourceType.getValueOf("STRING"));
		assertEquals(ResourceType.UNKNOWN, ResourceType.getValueOf("STRINGx"));
	}

	@Test
	public void resourceTypePredicates() {
		log.trace("testResourceTypePredicates()");
		assertTrue(ResourceType.STRING.isVariable());
		assertTrue(ResourceType.TEXT.isVariable());
		assertTrue(ResourceType.COLOR.isVariable());
		assertTrue(ResourceType.DIMEN.isVariable());
		assertTrue(ResourceType.STYLE.isVariable());

		assertFalse(ResourceType.IMAGE.isVariable());
		assertFalse(ResourceType.AUDIO.isVariable());
		assertFalse(ResourceType.VIDEO.isVariable());
		assertFalse(ResourceType.UNKNOWN.isVariable());

		assertTrue(ResourceType.IMAGE.isMedia());
		assertTrue(ResourceType.AUDIO.isMedia());
		assertTrue(ResourceType.VIDEO.isMedia());

		assertFalse(ResourceType.STRING.isMedia());
		assertFalse(ResourceType.TEXT.isMedia());
		assertFalse(ResourceType.COLOR.isMedia());
		assertFalse(ResourceType.DIMEN.isMedia());
		assertFalse(ResourceType.STYLE.isMedia());
		assertFalse(ResourceType.UNKNOWN.isMedia());
	}

	// ------------------------------------------------------
	// FileType

	@Test
	public void fileTypeForExtension() {
		log.trace("testFileTypeForExtension()");
		assertEquals(FileType.LAYOUT, FileType.forExtension("htm"));
		assertEquals(FileType.STYLE, FileType.forExtension("css"));
		assertEquals(FileType.SCRIPT, FileType.forExtension("js"));
		assertEquals(FileType.XML, FileType.forExtension("xml"));
		assertEquals(FileType.MEDIA, FileType.forExtension(null));
		assertEquals(FileType.MEDIA, FileType.forExtension(""));
		assertEquals(FileType.MEDIA, FileType.forExtension("png"));
		assertEquals(FileType.MEDIA, FileType.forExtension("avi"));
	}

	@Test
	public void fileTypeEquals() {
		log.trace("testFileTypeEquals()");
		assertTrue(FileType.LAYOUT.equals(new File("path/file.htm")));
		assertTrue(FileType.STYLE.equals(new File("path/file.css")));
		assertTrue(FileType.SCRIPT.equals(new File("path/file.js")));
		assertTrue(FileType.XML.equals(new File("path/file.xml")));
		assertTrue(FileType.MEDIA.equals(new File("path/file")));
		assertTrue(FileType.MEDIA.equals(new File("path/file.png")));
		assertTrue(FileType.MEDIA.equals(new File("path/file.avi")));

		assertFalse(FileType.LAYOUT.equals(new File("path/file.css")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file.js")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file.xml")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file")));
	}
}
