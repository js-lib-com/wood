package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.endsWith;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.Reader;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.WoodException;
import js.wood.impl.FileType;
import js.wood.impl.Reference;
import js.wood.impl.ResourceType;
import js.wood.impl.Variants;

public class FilePathTest {
	private Project project;

	@Before
	public void beforeTest() {
		project = new Project(new File("src/test/resources/project"));
	}

	@Test
	public void pattern() {
		Pattern pattern = Classes.getFieldValue(FilePath.class, "PATTERN");
		assertThat(pattern, notNullValue());

		assertPattern(pattern, "res/path/compo/compo.htm", "res/path/compo/", "compo", null, "htm");
		assertPattern(pattern, "res/path/compo/compo_port.htm", "res/path/compo/", "compo", "port", "htm");
		assertPattern(pattern, "res/path/second-compo/second-compo.css", "res/path/second-compo/", "second-compo", null, "css");
		assertPattern(pattern, "res/path/second-compo/second-compo_w800.css", "res/path/second-compo/", "second-compo", "w800", "css");
		assertPattern(pattern, "lib/js-lib.js", "lib/", "js-lib", null, "js");
		assertPattern(pattern, "script/js/format/RichText.js", "script/js/format/", "RichText", null, "js");
		assertPattern(pattern, "gen/js/widget/Paging.js", "gen/js/widget/", "Paging", null, "js");
		assertPattern(pattern, "res/path/compo/background_port_ro.png", "res/path/compo/", "background", "port_ro", "png");
		assertPattern(pattern, "res/3pty-scripts/3pty-scripts.htm", "res/3pty-scripts/", "3pty-scripts", null, "htm");
	}

	private static void assertPattern(Pattern pattern, String value, String... groups) {
		Matcher m = pattern.matcher(value);
		assertThat(m.find(), equalTo(true));
		assertThat(groups[0], equalTo(m.group(1)));
		assertThat(groups[1], equalTo(m.group(2)));
		assertThat(groups[2], equalTo(m.group(3)));
		assertThat(groups[3], equalTo(m.group(4)));
	}

	@Test
	public void constructor() {
		assertFilePath("res/compo/discography/discography_ro.css", "res/compo/discography/", "discography.css", FileType.STYLE, "ro");
		assertFilePath("res/compo/discography/strings.xml", "res/compo/discography/", "strings.xml", FileType.XML, null);
		assertFilePath("res/compo/discography/logo_de.png", "res/compo/discography/", "logo.png", FileType.MEDIA, "de");
		assertFilePath("lib/js-lib.js", "lib/", "js-lib.js", FileType.SCRIPT, null);
		assertFilePath("script/js/compo/Dialog.js", "script/js/compo/", "Dialog.js", FileType.SCRIPT, null);
	}

	private void assertFilePath(String pathValue, String path, String fileName, FileType fileType, String language) {
		FilePath p = new FilePath(project, pathValue);
		assertThat(p.value(), equalTo(pathValue));
		assertThat(p.getParentDirPath().value(), equalTo(path));
		assertThat(p.getName(), equalTo(fileName));
		assertThat(p.getType(), equalTo(fileType));
		assertThat(p.getVariants(), notNullValue());
		if (language != null) {
			assertThat(p.getVariants().getLocale(), equalTo(new Locale(language)));
		} else {
			assertThat(p.getVariants().getLocale(), nullValue());
		}
	}

	@Test(expected = WoodException.class)
	public void constructor_InvlaidPath() {
		new FilePath(project, "http://server/path");
	}

	@Test
	public void baseName() {
		FilePath path = new FilePath(project, "res/compo/discography/discography.css");
		assertThat(path.getBaseName(), equalTo("discography"));
		assertThat(path.isBaseName("discography"), equalTo(true));
		assertThat(path.isBaseName(new Reference(path, ResourceType.STRING, "discography")), equalTo(true));

		path = new FilePath(project, "res/compo/discography/strings_de.xml");
		assertThat(path.getBaseName(), equalTo("strings"));
		assertThat(path.isBaseName("strings"), equalTo(true));
	}

	@Test
	public void cloneToStyle() {
		FilePath layoutFile = new FilePath(project, "res/compo/discography/discography_w800.htm");
		FilePath styleFile = layoutFile.cloneToStyle();
		assertThat(layoutFile.isLayout(), equalTo(true));
		assertThat(styleFile.isStyle(), equalTo(true));
		assertThat(layoutFile.getBaseName(), equalTo(styleFile.getBaseName()));
		assertThat(layoutFile.getParentDirPath(), equalTo(styleFile.getParentDirPath()));
		assertThat(layoutFile.getVariants().getLocale(), equalTo(styleFile.getVariants().getLocale()));
		assertThat(layoutFile.getVariants().getMediaQueries(), equalTo(styleFile.getVariants().getMediaQueries()));
	}

	@Test
	public void getDirPath() {
		FilePath path = new FilePath(project, "res/asset/background.jpg");
		assertThat(path.getParentDirPath().value(), equalTo("res/asset/"));
	}

	@Test
	public void predicates() {
		assertTrue(new FilePath(project, "res/asset/background.jpg").isMedia());
		assertFalse(new FilePath(project, "res/compo/discography/discography.css").isMedia());

		assertTrue(new FilePath(project, "res/compo/discography/discography.htm").isLayout());
		assertFalse(new FilePath(project, "res/compo/discography/discography.css").isLayout());

		assertTrue(new FilePath(project, "res/compo/discography/discography.css").isStyle());
		assertFalse(new FilePath(project, "res/compo/discography/preview.js").isStyle());

		assertTrue(new FilePath(project, "res/compo/discography/preview.js").isScript());
		assertFalse(new FilePath(project, "res/compo/discography/preview.css").isScript());

		assertTrue(new FilePath(project, "res/compo/discography/preview.js").isPreviewScript());
		assertFalse(new FilePath(project, "res/compo/discography/discography.js").isPreviewScript());

		assertTrue(new FilePath(project, "res/compo/discography/discography.xml").isComponentDescriptor());
		assertFalse(new FilePath(project, "res/compo/discography/discography.htm").isComponentDescriptor());
		assertFalse(new FilePath(project, "res/compo/discography/strings.xml").isComponentDescriptor());

		assertTrue(new FilePath(project, "res/compo/discography/strings.xml").isVariables());
		assertFalse(new FilePath(project, "res/compo/discography/discography.xml").isVariables());
		assertFalse(new FilePath(project, "res/compo/discography/discography.htm").isVariables());
	}

	@Test
	public void hasVariants() {
		assertThat(new FilePath(project, "res/compo/discography/strings.xml").hasVariants(), equalTo(false));
		assertThat(new FilePath(project, "res/compo/discography/strings_de.xml").hasVariants(), equalTo(true));
	}

	@Test
	public void getVariants() {
		FilePath path = new FilePath(project, "res/compo/discography/strings_de.xml");
		assertThat(path.hasVariants(), equalTo(true));
		Variants variants = path.getVariants();
		assertThat(variants, notNullValue());
		assertThat(variants.getLocale(), equalTo(new Locale("de")));
		assertThat(variants.getMediaQueries().isEmpty(), is(true));

		path = new FilePath(project, "res/compo/discography/discography_w800.css");
		variants = path.getVariants();
		assertThat(variants.getLocale(), nullValue());
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px )"));

		path = new FilePath(project, "res/compo/discography/discography_h800.css");
		variants = path.getVariants();
		assertThat(variants.getLocale(), nullValue());
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-height: 800px )"));

		path = new FilePath(project, "res/compo/discography/colors_ro_w800.xml");
		variants = path.getVariants();
		assertThat(variants.getLocale(), equalTo(new Locale("ro")));
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px )"));

		path = new FilePath(project, "res/compo/discography/colors_w800_ro.xml");
		variants = path.getVariants();
		assertThat(variants.getLocale(), equalTo(new Locale("ro")));
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px )"));

		path = new FilePath(project, "res/compo/discography/discography_lgd.css");
		variants = path.getVariants();
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 992px )"));

		path = new FilePath(project, "res/compo/discography/colors_w800_portrait_ro.xml");
		variants = path.getVariants();
		assertThat(variants.getLocale(), equalTo(new Locale("ro")));
		assertThat(variants.getMediaQueries().getQueries(), hasSize(2));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px ) and ( orientation: portrait )"));
	}

	@Test(expected = WoodException.class)
	public void getVariants_notRecognized() {
		new FilePath(project, "res/compo/discography/colors_q800.xml");
	}

	@Test
	public void resolvedOnAssetFile() throws Throwable {
		FilePath path = new FilePath(project, "res/asset/background.jpg");
		File file = path.toFile();
		assertThat(file, notNullValue());
		assertThat(file.toString(), endsWith("project\\res\\asset\\background.jpg"));
	}

	@Test
	public void getReader() {
		FilePath path = new FilePath(project, "res/asset/background.jpg");
		Reader reader = path.getReader();
		assertThat(reader, notNullValue());
	}

	@Test(expected = WoodException.class)
	public void getReader_MissingFile() {
		FilePath path = new FilePath(project, "res/asset/missing.jpg");
		path.getReader();
	}

	@Test
	public void accept() {
		assertTrue(FilePath.accept("res/template/page/page.htm"));
		assertTrue(FilePath.accept("lib/js-lib.js"));
		assertTrue(FilePath.accept("res/compo/video-player/video-player.xml"));
		assertTrue(FilePath.accept("lib/js-lib.1.2.3.js"));
		assertFalse(FilePath.accept("template/page/page.htm"));
		assertFalse(FilePath.accept("res/template/page/page.htm#body"));
		assertFalse(FilePath.accept("dir/template/page/page.htm"));
		assertFalse(FilePath.accept("dir/template/page#body"));
	}
}
