package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.util.Classes;
import js.wood.impl.FileType;
import js.wood.impl.MediaQueryDefinition;
import js.wood.impl.Variants;

@RunWith(MockitoJUnitRunner.class)
public class FilePathTest {
	@Mock
	private Project project;

	@Before
	public void beforeTest() {
		when(project.getProjectRoot()).thenReturn(new File("."));
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
		FilePath path = new FilePath(project, "res/compo/compo.css");
		assertThat(path.getBaseName(), equalTo("compo"));
		assertThat(path.hasBaseName("compo"), equalTo(true));

		path = new FilePath(project, "res/compo/strings_de.xml");
		assertThat(path.getBaseName(), equalTo("strings"));
		assertThat(path.hasBaseName("strings"), equalTo(true));
	}

	@Test
	public void cloneToStyle() {
		when(project.getMediaQueryDefinition("w800")).thenReturn(new MediaQueryDefinition("w800", "", 0));

		FilePath layoutFile = new FilePath(project, "res/compo/compo_w800.htm");
		FilePath styleFile = layoutFile.cloneTo(FileType.STYLE);
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
		assertFalse(new FilePath(project, "res/compo/compo.css").isMedia());

		assertTrue(new FilePath(project, "res/compo/compo.htm").isLayout());
		assertFalse(new FilePath(project, "res/compo/compo.css").isLayout());

		assertTrue(new FilePath(project, "res/compo/compo.css").isStyle());
		assertFalse(new FilePath(project, "res/compo/preview.js").isStyle());

		assertTrue(new FilePath(project, "res/compo/preview.js").isScript());
		assertFalse(new FilePath(project, "res/compo/preview.css").isScript());

		assertTrue(new FilePath(project, "res/compo/preview.js").isPreviewScript());
		assertFalse(new FilePath(project, "res/compo/compo.js").isPreviewScript());

		assertTrue(new FilePath(project, "res/compo/compo.xml").isComponentDescriptor());
		assertFalse(new FilePath(project, "res/compo/compo.htm").isComponentDescriptor());
		assertFalse(new FilePath(project, "res/compo/strings.xml").isComponentDescriptor());

		assertTrue(new FilePath(project, "res/compo/strings.xml").isVariables());
		assertFalse(new FilePath(project, "res/compo/compo.xml").isVariables());
		assertFalse(new FilePath(project, "res/compo/compo.htm").isVariables());
	}

	@Test
	public void hasVariants() {
		assertThat(new FilePath(project, "res/compo/strings.xml").hasVariants(), equalTo(false));
		assertThat(new FilePath(project, "res/compo/strings_de.xml").hasVariants(), equalTo(true));
	}

	@Test
	public void getVariants() {
		when(project.getMediaQueryDefinition("w800")).thenReturn(new MediaQueryDefinition("w800", "min-width: 800px", 0));
		when(project.getMediaQueryDefinition("h800")).thenReturn(new MediaQueryDefinition("h800", "min-height: 800px", 1));
		when(project.getMediaQueryDefinition("lgd")).thenReturn(new MediaQueryDefinition("lgd", "min-width: 992px", 1));
		when(project.getMediaQueryDefinition("portrait")).thenReturn(new MediaQueryDefinition("portrait", "orientation: portrait", 1));

		FilePath path = new FilePath(project, "res/compo/strings_de.xml");
		assertThat(path.hasVariants(), equalTo(true));
		Variants variants = path.getVariants();
		assertThat(variants, notNullValue());
		assertThat(variants.getLocale(), equalTo(new Locale("de")));
		assertThat(variants.getMediaQueries().isEmpty(), is(true));

		path = new FilePath(project, "res/compo/compo_w800.css");
		variants = path.getVariants();
		assertThat(variants.getLocale(), nullValue());
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px )"));

		path = new FilePath(project, "res/compo/compo_h800.css");
		variants = path.getVariants();
		assertThat(variants.getLocale(), nullValue());
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-height: 800px )"));

		path = new FilePath(project, "res/compo/colors_ro_w800.xml");
		variants = path.getVariants();
		assertThat(variants.getLocale(), equalTo(new Locale("ro")));
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px )"));

		path = new FilePath(project, "res/compo/colors_w800_ro.xml");
		variants = path.getVariants();
		assertThat(variants.getLocale(), equalTo(new Locale("ro")));
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px )"));

		path = new FilePath(project, "res/compo/compo_lgd.css");
		variants = path.getVariants();
		assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 992px )"));

		path = new FilePath(project, "res/compo/colors_w800_portrait_ro.xml");
		variants = path.getVariants();
		assertThat(variants.getLocale(), equalTo(new Locale("ro")));
		assertThat(variants.getMediaQueries().getQueries(), hasSize(2));
		assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px ) and ( orientation: portrait )"));
	}

	@Test(expected = WoodException.class)
	public void getVariants_notRecognized() {
		new FilePath(project, "res/compo/colors_q800.xml");
	}

	/** Attempting to create reader from this test case fails with file not found. */
	@Test(expected = WoodException.class)
	public void getReader() {
		FilePath path = new FilePath(project, "res/asset/background.jpg");
		path.getReader();
	}

	@Test
	public void accept() {
		assertTrue(FilePath.accept("res/template/page/page.htm"));
		assertTrue(FilePath.accept("lib/js-lib.js"));
		assertTrue(FilePath.accept("res/compo/video-player/video-player.xml"));
		assertTrue(FilePath.accept("lib/js-lib.1.2.3.js"));
		assertTrue(FilePath.accept("template/page/page_ro.htm"));
		assertTrue(FilePath.accept("dir/template/page/page_w800_ro.htm"));
		assertTrue(FilePath.accept("project.xml"));
		
		assertFalse(FilePath.accept("res/template/page/page.htm#body"));
		assertFalse(FilePath.accept("dir/template/page#body"));
	}
}
