package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import js.util.Classes;
import js.util.Strings;
import js.wood.BuildFS;
import js.wood.Builder;
import js.wood.Component;
import js.wood.DefaultBuildFS;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.WoodTestCase;

import org.junit.Before;
import org.junit.Test;

public class BuildFsTest extends WoodTestCase {
	private Project project;

	@Before
	public void beforeTest() throws Exception {
		project = project("project");
	}

	// ------------------------------------------------------
	// BuildFS

	private static class TestBuildFS extends BuildFS {
		public TestBuildFS(Project project) {
			super(project);
		}

		@Override
		protected File getPageDir(Component compo) {
			return createDirectory("htm");
		}

		@Override
		protected File getStyleDir() {
			return createDirectory("css");
		}

		@Override
		protected File getScriptDir() {
			return createDirectory("js");
		}

		@Override
		protected File getMediaDir() {
			return createDirectory("img");
		}

		@Override
		protected String formatPageName(String pageName) {
			return pageName;
		}

		@Override
		protected String formatStyleName(FilePath styleFile) {
			return styleFile.getName();
		}

		@Override
		protected String formatScriptName(FilePath scriptFile) {
			return scriptFile.getName();
		}

		@Override
		protected String formatMediaName(FilePath mediaFile) {
			return mediaFile.getName();
		}
	}

	@Test
	public void buildFsWritePage() throws IOException {
		// resetProjectLocales(project);
		// BuildFS buildFS = new TestBuildFS(project);
		// PageDocument page = newInstance(PageDocument.class);
		//
		// buildFS.writePage(null, page, "index.htm");
		// assertTrue(exists("htm/index.htm"));
		// assertFalse(exists("en/htm/index.htm"));
		//
		// buildFS.setLanguage("ro");
		// buildFS.writePage(page, "index.htm");
		// assertTrue(exists("ro/htm/index.htm"));
		//
		// buildFS.setBuildNumber(4);
		// buildFS.setLanguage(null);
		//
		// buildFS.writePage(page, "index.htm");
		// assertTrue(exists("htm/index-004.htm"));
		// assertFalse(exists("en/htm/index-004.htm"));
		//
		// buildFS.setLanguage("ro");
		// buildFS.writePage(page, "index.htm");
		// assertTrue(exists("ro/htm/index-004.htm"));
	}

	@Test
	public void buildFsWriteFavicon() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project);

		assertFalse(exists("favicon.ico"));
		assertEquals("../img/favicon.ico", buildFS.writeFavicon(null, new FilePath(project, "res/asset/favicon.ico")));
		assertTrue(exists("img/favicon.ico"));
	}

	@Test
	public void buildFsWriteMedia() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project);
		buildFS.setLocale(new Locale("en"));
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertEquals("../img/background.jpg", buildFS.writePageMedia(null, mediaFile));
		assertEquals("../img/background.jpg", buildFS.writeStyleMedia(mediaFile));
		assertEquals("/project/img/background.jpg", buildFS.writeScriptMedia(mediaFile));
		assertTrue(exists("img/background.jpg"));

		buildFS.setBuildNumber(4);
		assertEquals("../img/background-004.jpg", buildFS.writePageMedia(null, mediaFile));
		assertEquals("../img/background-004.jpg", buildFS.writeStyleMedia(mediaFile));
		assertEquals("/project/img/background-004.jpg", buildFS.writeScriptMedia(mediaFile));
		assertTrue(exists("img/background-004.jpg"));
	}

	@Test
	public void buildFsWriteStyle() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project);
		FilePath styleFile = new FilePath(project, "res/theme/style.css");

		assertEquals("../css/style.css", buildFS.writeStyle(null, styleFile, nullReferenceHandler()));
		assertTrue(exists("css/style.css"));

		buildFS.setBuildNumber(4);
		assertEquals("../css/style-004.css", buildFS.writeStyle(null, styleFile, nullReferenceHandler()));
		assertTrue(exists("css/style-004.css"));
	}

	@Test
	public void buildFsWriteScript() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project);
		FilePath scriptFile = project.getFile("script/hc/page/Index.js");

		assertEquals("../js/Index.js", buildFS.writeScript(null, scriptFile, nullReferenceHandler()));
		assertTrue(exists("js/Index.js"));
		buildFS.setBuildNumber(4);
		assertEquals("../js/Index-004.js", buildFS.writeScript(null, scriptFile, nullReferenceHandler()));
		assertTrue(exists("js/Index-004.js"));
	}

	@Test
	public void buildFsDirFactory() throws Exception {
		BuildFS buildFS = new TestBuildFS(project);

		buildFS.setLocale(new Locale("en"));
		assertDir("src/test/resources/project/build/site/en/images", buildFS, "images");
		buildFS.setLocale(new Locale("ro"));
		assertDir("src/test/resources/project/build/site/ro/images", buildFS, "images");

		resetProjectLocales(project);
		try {
			buildFS.setLocale(null);
			fail("Null locale should rise illegal arguments exception.");
		} catch (IllegalArgumentException e) {
			assertEquals("Locale parameter is null.", e.getMessage());
		}
	}

	private boolean exists(String fileName) {
		return new File(project.getSiteDir(), fileName).exists();
	}

	private static void assertDir(String expected, BuildFS buildFS, String dirName) throws Exception {
		assertEquals(expected, Classes.invoke(buildFS, BuildFS.class, "createDirectory", dirName).toString().replace('\\', '/'));
	}

	// ------------------------------------------------------
	// DefaultBuildFS

	@Test
	public void defaultBuildFsLayout() throws IOException {
		Builder builder = new Builder(path("project"));
		project = field(builder, "project");
		resetProjectLocales(project);
		builder.build();

		assertTrue(dir("build/site").exists());
		assertTrue(dir("build/site", "media").exists());
		assertTrue(dir("build/site", "script").exists());
		assertTrue(dir("build/site", "style").exists());
	}

	@Test
	public void defaultBuildFsMultiLanguageLayout() throws IOException {
		Builder builder = new Builder(path("project"));
		project = field(builder, "project");
		builder.build();

		assertEquals("build/site/", builder.getSitePath());

		for (String language : new String[] { "de", "en", "fr", "ro" }) {
			assertTrue(dir("build/site", language).exists());
			assertTrue(dir("build/site", language, "media").exists());
			assertTrue(dir("build/site", language, "script").exists());
			assertTrue(dir("build/site", language, "style").exists());
		}
	}

	private File dir(String... segments) {
		File projectDir = file(project.getName());
		return new File(projectDir, Strings.join(segments, '/'));
	}

	@Test
	public void defaultBuildFsPageName() {
		BuildFS buildFS = new DefaultBuildFS(project);
		assertEquals("index.htm", invoke(buildFS, "formatPageName", "index.htm"));
	}

	@Test
	public void defaultBuildFsStyleName() {
		BuildFS buildFS = new DefaultBuildFS(project);

		assertEquals("page-index.css", invoke(buildFS, "formatStyleName", new FilePath(project, "res/page/index/index.css")));
		assertEquals("theme-style.css", invoke(buildFS, "formatStyleName", new FilePath(project, "res/theme/style.css")));
		assertEquals("lib-video-player-style.css", invoke(buildFS, "formatStyleName", new FilePath(project, "lib/video-player/style.css")));

		// this condition is not really licit since style file cannot reside into source directory root
		// but is allowed by file path syntax and need to ensure styleName cope with it
		assertEquals("style.css", invoke(buildFS, "formatStyleName", new FilePath(project, "res/style.css")));
		assertEquals("lib-style.css", invoke(buildFS, "formatStyleName", new FilePath(project, "lib/style.css")));
	}

	@Test
	public void defaultBuildFsScriptName() {
		BuildFS buildFS = new DefaultBuildFS(project);

		assertEquals("hc.page.Index.js", invoke(buildFS, "formatScriptName", new FilePath(project, "script/hc/page/Index.js")));
		assertEquals("paging.js", invoke(buildFS, "formatScriptName", new FilePath(project, "lib/paging.js")));
		assertEquals("js-lib.js", invoke(buildFS, "formatScriptName", new FilePath(project, "lib/js-lib/js-lib.js")));
		assertEquals("gen.js.wood.Controller.js", invoke(buildFS, "formatScriptName", new FilePath(project, "gen/js/wood/Controller.js")));
	}

	@Test
	public void defaultBuildFsMediaName() {
		BuildFS buildFS = new DefaultBuildFS(project);

		assertEquals("page-index_background.png", invoke(buildFS, "formatMediaName", new FilePath(project, "res/page/index/background.png")));
		assertEquals("page-index_index.png", invoke(buildFS, "formatMediaName", new FilePath(project, "res/page/index/index.png")));
		assertEquals("page-index-icon_logo.png", invoke(buildFS, "formatMediaName", new FilePath(project, "res/page/index/icon/logo.png")));
		assertEquals("page-index_icon-logo.png", invoke(buildFS, "formatMediaName", new FilePath(project, "res/page/index/icon-logo.png")));
		assertEquals("theme_background.png", invoke(buildFS, "formatMediaName", new FilePath(project, "res/theme/background.png")));
		assertEquals("asset_background.png", invoke(buildFS, "formatMediaName", new FilePath(project, "res/asset/background.png")));
		assertEquals("js-wood-player_background.png", invoke(buildFS, "formatMediaName", new FilePath(project, "script/js/wood/player/background.png")));
		assertEquals("lib-paging_background.png", invoke(buildFS, "formatMediaName", new FilePath(project, "lib/paging/background.png")));
	}
}
