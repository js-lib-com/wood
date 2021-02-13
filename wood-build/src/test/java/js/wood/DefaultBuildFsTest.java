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
import js.util.Strings;

public class DefaultBuildFsTest {
	private BuilderProject project;

	@Before
	public void beforeTest() throws Exception {
		project = new BuilderProject(new File("src/test/resources/project"));
	}

	@Test
	public void layout() throws IOException {
		List<Locale> locales = Arrays.asList(new Locale("en"));
		Classes.setFieldValue(Classes.getFieldValue(project, Project.class, "descriptor"), "locales", locales);
		
		Builder builder = new Builder(project);
		builder.build();

		assertTrue(dir("build/site").exists());
		assertTrue(dir("build/site", "media").exists());
		assertTrue(dir("build/site", "script").exists());
		assertTrue(dir("build/site", "style").exists());
	}

	@Test
	public void multiLanguageLayout() throws IOException {
		Builder builder = new Builder(project);
		builder.build();

		assertThat(builder.getSitePath(), equalTo("build/site/"));

		for (String language : new String[] { "de", "en", "fr", "ro" }) {
			assertTrue(dir("build/site", language).exists());
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
		DefaultBuildFS buildFS = new DefaultBuildFS(project, 0);
		assertThat(buildFS.formatPageName("index.htm"), equalTo("index.htm"));
	}

	@Test
	public void styleName() {
		DefaultBuildFS buildFS = new DefaultBuildFS(project, 0);

		assertThat(buildFS.formatStyleName(new FilePath(project, "res/page/index/index.css")), equalTo("page-index.css"));
		assertThat(buildFS.formatStyleName(new FilePath(project, "res/theme/style.css")), equalTo("theme-style.css"));
		assertThat(buildFS.formatStyleName(new FilePath(project, "lib/video-player/style.css")), equalTo("lib-video-player-style.css"));

		// this condition is not really licit since style file cannot reside into source directory root
		// but is allowed by file path syntax and need to ensure styleName cope with it
		assertThat(buildFS.formatStyleName(new FilePath(project, "res/style.css")), equalTo("style.css"));
		assertThat(buildFS.formatStyleName(new FilePath(project, "lib/style.css")), equalTo("lib-style.css"));
	}

	@Test
	public void scriptName() {
		DefaultBuildFS buildFS = new DefaultBuildFS(project, 0);

		assertThat(buildFS.formatScriptName(new FilePath(project, "script/hc/page/Index.js")), equalTo("hc.page.Index.js"));
		assertThat(buildFS.formatScriptName(new FilePath(project, "lib/paging.js")), equalTo("paging.js"));
		assertThat(buildFS.formatScriptName(new FilePath(project, "lib/js-lib/js-lib.js")), equalTo("js-lib.js"));
		assertThat(buildFS.formatScriptName(new FilePath(project, "gen/js/wood/Controller.js")), equalTo("gen.js.wood.Controller.js"));
	}

	@Test
	public void mediaName() {
		DefaultBuildFS buildFS = new DefaultBuildFS(project, 0);

		assertThat(buildFS.formatMediaName(new FilePath(project, "res/page/index/background.png")), equalTo("page-index_background.png"));
		assertThat(buildFS.formatMediaName(new FilePath(project, "res/page/index/index.png")), equalTo("page-index_index.png"));
		assertThat(buildFS.formatMediaName(new FilePath(project, "res/page/index/icon/logo.png")), equalTo("page-index-icon_logo.png"));
		assertThat(buildFS.formatMediaName(new FilePath(project, "res/page/index/icon-logo.png")), equalTo("page-index_icon-logo.png"));
		assertThat(buildFS.formatMediaName(new FilePath(project, "res/theme/background.png")), equalTo("theme_background.png"));
		assertThat(buildFS.formatMediaName(new FilePath(project, "res/asset/background.png")), equalTo("asset_background.png"));
		assertThat(buildFS.formatMediaName(new FilePath(project, "script/js/wood/player/background.png")), equalTo("js-wood-player_background.png"));
		assertThat(buildFS.formatMediaName(new FilePath(project, "lib/paging/background.png")), equalTo("lib-paging_background.png"));
	}
}
