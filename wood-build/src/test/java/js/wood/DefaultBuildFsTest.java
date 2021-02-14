package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.util.Files;
import js.util.Strings;

public class DefaultBuildFsTest {
	private BuilderProject project;

	@Before
	public void beforeTest() throws Exception {
		project = new BuilderProject(new File("src/test/resources/project"));
		File buildDir = new File(project.getProjectDir(), CT.DEF_BUILD_DIR);
		if (buildDir.exists()) {
			Files.removeFilesHierarchy(buildDir);
		}
	}

	@Test
	public void layout() throws IOException {
		// force project descriptor locale to single locale
		List<Locale> locales = Arrays.asList(new Locale("en"));
		Classes.setFieldValue(Classes.getFieldValue(project, Project.class, "descriptor"), "locales", locales);

		Builder builder = new Builder(project);
		builder.build();

		assertTrue(dir("build/site").exists());
		assertTrue(dir("build/site", "player").exists());
		assertTrue(dir("build/site", "media").exists());
		assertTrue(dir("build/site", "script").exists());
		assertTrue(dir("build/site", "style").exists());
	}

	@Test
	public void layout_MultiLocale() throws IOException {
		Builder builder = new Builder(project);
		builder.build();

		for (String language : new String[] { "de", "en", "fr", "ro" }) {
			assertTrue(dir("build/site", language).exists());
			assertTrue(dir("build/site", language, "player").exists());
			assertTrue(dir("build/site", language, "media").exists());
			assertTrue(dir("build/site", language, "script").exists());
			assertTrue(dir("build/site", language, "style").exists());
		}
	}

	private File dir(String... segments) {
		File projectDir = new File("src/test/resources/" + project.getName());
		return new File(projectDir, Strings.join(segments, '/'));
	}

	@Test
	public void pageName() {
		File buildDir = new File(project.getProjectDir(), CT.DEF_BUILD_DIR);
		DefaultBuildFS buildFS = new DefaultBuildFS(buildDir, 0);
		assertThat(buildFS.formatPageName("index.htm"), equalTo("index.htm"));
	}

	@Test
	public void styleName() {
		File buildDir = new File(project.getProjectDir(), CT.DEF_BUILD_DIR);
		DefaultBuildFS buildFS = new DefaultBuildFS(buildDir, 0);

		assertThat(buildFS.formatStyleName(path("res/page/index/index.css")), equalTo("page-index.css"));
		assertThat(buildFS.formatStyleName(path("res/theme/style.css")), equalTo("theme-style.css"));
		assertThat(buildFS.formatStyleName(path("lib/video-player/style.css")), equalTo("lib-video-player-style.css"));

		// this condition is not really licit since style file cannot reside into source directory root
		// but is allowed by file path syntax and need to ensure styleName cope with it
		assertThat(buildFS.formatStyleName(path("res/style.css")), equalTo("style.css"));
		assertThat(buildFS.formatStyleName(path("lib/style.css")), equalTo("lib-style.css"));
	}

	@Test
	public void scriptName() {
		File buildDir = new File(project.getProjectDir(), CT.DEF_BUILD_DIR);
		DefaultBuildFS buildFS = new DefaultBuildFS(buildDir, 0);

		assertThat(buildFS.formatScriptName(path("script/hc/page/Index.js")), equalTo("hc.page.Index.js"));
		assertThat(buildFS.formatScriptName(path("lib/paging.js")), equalTo("paging.js"));
		assertThat(buildFS.formatScriptName(path("lib/js-lib/js-lib.js")), equalTo("js-lib.js"));
		assertThat(buildFS.formatScriptName(path("gen/js/wood/Controller.js")), equalTo("gen.js.wood.Controller.js"));
	}

	@Test
	public void mediaName() {
		File buildDir = new File(project.getProjectDir(), CT.DEF_BUILD_DIR);
		DefaultBuildFS buildFS = new DefaultBuildFS(buildDir, 0);

		assertThat(buildFS.formatMediaName(path("res/page/index/background.png")), equalTo("page-index_background.png"));
		assertThat(buildFS.formatMediaName(path("res/page/index/index.png")), equalTo("page-index_index.png"));
		assertThat(buildFS.formatMediaName(path("res/page/index/icon/logo.png")), equalTo("page-index-icon_logo.png"));
		assertThat(buildFS.formatMediaName(path("res/page/index/icon-logo.png")), equalTo("page-index_icon-logo.png"));
		assertThat(buildFS.formatMediaName(path("res/theme/background.png")), equalTo("theme_background.png"));
		assertThat(buildFS.formatMediaName(path("res/asset/background.png")), equalTo("asset_background.png"));
		assertThat(buildFS.formatMediaName(path("script/js/wood/player/background.png")), equalTo("js-wood-player_background.png"));
		assertThat(buildFS.formatMediaName(path("lib/paging/background.png")), equalTo("lib-paging_background.png"));
	}

	private FilePath path(String path) {
		return new FilePath(project, path);
	}
}
