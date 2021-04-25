package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Test;

import js.dom.Document;
import js.dom.EList;
import js.util.Files;

public class BuilderIntegrationTest {
	private File buildDir;

	@Test
	public void constructor() throws IOException {
		Builder builder = builder("project");

		BuilderProject project = builder.getProject();
		assertNotNull(project);

		BuildFS buildFS = builder.getBuildFS();
		assertNotNull(buildFS);

		Collection<CompoPath> pages = builder.getProject().getPages();
		assertNotNull(pages);
		assertThat(pages, hasSize(3));
		assertTrue(pages.contains(new CompoPath(project, "res/page/index")));
		assertTrue(pages.contains(new CompoPath(project, "res/page/video-player")));
		assertTrue(pages.contains(new CompoPath(project, "res/page/videos")));

		Map<DirPath, Variables> variables = builder.getProject().getVariables();
		assertNotNull(variables);
		assertFalse(variables.isEmpty());

		DirPath themeDir = new DirPath(project, "res/theme/");
		assertNotNull(variables.get(themeDir));
	}

	@Test
	public void build() throws IOException {
		BuilderProject project = project("project");

		// initialize probes
		final List<Locale> locales = new ArrayList<>();
		final List<String> pageFileNames = new ArrayList<>();

		BuildFS buildFS = new DefaultBuildFS(buildDir, 0) {
			@Override
			public void setLocale(Locale locale) {
				super.setLocale(locale);
				locales.add(locale);
			}

			@Override
			public void writePage(Component page, Document document) throws IOException {
				pageFileNames.add(page.getLayoutFileName());
			}
		};
		Builder builder = new Builder(project, buildFS);
		builder.build();

		assertThat(locales, hasSize(4));

		assertTrue(locales.contains(new Locale("en")));
		assertTrue(locales.contains(new Locale("de")));
		assertTrue(locales.contains(new Locale("fr")));
		assertTrue(locales.contains(new Locale("ro")));

		assertThat(pageFileNames, hasSize(12));
		Collections.sort(pageFileNames);

		String[] expectedFileNames = new String[] { "index.htm", "video-player.htm", "videos.htm" };
		for (int i = 0; i < 12; ++i) {
			assertThat(pageFileNames.get(i), equalTo(expectedFileNames[i / 4]));
		}
	}

	@Test
	public void buildPage() throws Exception {
		BuilderProject project = project("project");

		BuildFS buildFS = new DefaultBuildFS(buildDir, 0) {
			@Override
			public void writePage(Component page, Document document) throws IOException {
				try {
					assertPageDocument(document);
				} catch (XPathExpressionException e) {
					fail();
				}
			}
		};

		Builder builder = new Builder(project, buildFS);
		Locale locale = new Locale("en");
		builder.setLocale(locale);
		buildFS.setLocale(locale);

		CompoPath indexPage = new CompoPath(project, "res/page/index");
		builder.buildPage(new Component(indexPage, builder));
	}

	private static void assertPageDocument(Document doc) throws XPathExpressionException {
		assertThat(doc.getByTag("title").getText(), equalTo("Test Project / Index"));

		EList metas = doc.findByTag("meta");
		assertThat(metas.size(), equalTo(5));

		assertThat(metas.item(0).getAttr("content"), equalTo("text/html; charset=UTF-8"));
		assertThat(metas.item(0).getAttr("http-equiv"), equalTo("Content-Type"));
		assertThat(metas.item(1).getAttr("content"), equalTo("j(s)-lib"));
		assertThat(metas.item(1).getAttr("name"), equalTo("Author"));
		assertThat(metas.item(2).getAttr("content"), equalTo("Index page description."));
		assertThat(metas.item(2).getAttr("name"), equalTo("Description"));
		assertThat(metas.item(3).getAttr("content"), equalTo("IE=9; IE=8; IE=7; IE=EDGE"));
		assertThat(metas.item(3).getAttr("http-equiv"), equalTo("X-UA-Compatible"));
		assertThat(metas.item(4).getAttr("content"), equalTo("width=device-width, initial-scale=1.0, maximum-scale=1.0"));
		assertThat(metas.item(4).getAttr("name"), equalTo("viewport"));

		EList styles = doc.findByTag("link");
		assertThat(styles.size(), equalTo(13));

		int index = 0;
		assertStyle("media/favicon.ico", styles, index++);
		assertStyle("http://fonts.googleapis.com/css?family=Roboto", styles, index++);
		assertStyle("http://fonts.googleapis.com/css?family=Great+Vibes", styles, index++);
		assertStyle("style/res-theme_reset.css", styles, index++);
		assertStyle("style/res-theme_fx.css", styles, index++);

		// site styles order, beside reset and fx is not guaranteed
		assertNotNull(doc.getByXPath("//LINK[@href='style/res-theme_form.css']"));
		assertNotNull(doc.getByXPath("//LINK[@href='style/res-theme_style.css']"));

		index += 2; // skip form.css and fx.css
		assertStyle("style/res-template_dialog.css", styles, index++);
		assertStyle("style/lib_paging.css", styles, index++);
		assertStyle("style/lib_list-view.css", styles, index++);
		assertStyle("style/res-template_page.css", styles, index++);
		assertStyle("style/res-template_sidebar-page.css", styles, index++);
		assertStyle("style/res-page_index.css", styles, index++);

		EList elist = doc.findByTag("script");
		List<String> scripts = new ArrayList<>();
		for (int i = 0; i < elist.size(); ++i) {
			scripts.add(elist.item(i).getAttr("src"));
		}
		assertThat(scripts, hasSize(10));

		assertTrue(scripts.indexOf("script/script.hc.page.Index.js") > scripts.indexOf("script/lib.js-lib.js"));
		assertTrue(scripts.indexOf("script/script.hc.view.DiscographyView.js") > scripts.indexOf("script/lib.js-lib.js"));
		assertTrue(scripts.indexOf("script/script.hc.view.DiscographyView.js") > scripts.indexOf("script/script.hc.view.VideoPlayer.js"));
		assertTrue(scripts.indexOf("script/script.hc.view.VideoPlayer.js") > scripts.indexOf("script/lib.js-lib.js"));
		// assertTrue(scripts.indexOf("script/hc.view.VideoPlayer.js") > scripts.indexOf("script/js.compo.Dialog.js"));
		// assertTrue(scripts.indexOf("script/js.compo.Dialog.js") > scripts.indexOf("script/js-lib.js"));

		assertTrue(scripts.contains("script/lib.js-lib.js"));
		assertTrue(scripts.contains("script/script.js.compo.Dialog.js"));
		assertTrue(scripts.contains("script/script.hc.view.VideoPlayer.js"));
		assertTrue(scripts.contains("script/script.js.hood.MainMenu.js"));
		assertTrue(scripts.contains("script/script.hc.page.Index.js"));
		assertTrue(scripts.contains("script/script.hc.view.DiscographyView.js"));
		assertTrue(scripts.contains("script/gen.js.controller.MainController.js"));
		assertTrue(scripts.contains("script/lib.list-view.js"));
		assertTrue(scripts.contains("script/lib.paging.js"));
		assertTrue(scripts.contains("script/script.js.hood.TopMenu.js"));

		EList anchors = doc.findByTag("a");
		assertThat(anchors.size(), equalTo(8));

		index = 0;
		assertAnchor("Logout", anchors, index++);
		assertAnchor("Login", anchors, index++);
		assertAnchor("Register", anchors, index++);
		assertAnchor("Home", anchors, index++);
		assertAnchor("Videos", anchors, index++);
		assertAnchor("Mixes", anchors, index++);
		assertAnchor("News Feed", anchors, index++);
		assertAnchor("User Profile", anchors, index++);

		EList images = doc.findByTag("img");
		assertThat(images.size(), equalTo(8));

		index = 0;
		assertImage("media/res-template-page_logo.jpg", images, index++);
		assertImage("media/res-template-page-icon_logo.png", images, index++);
		assertImage("media/res-template-page_page.jpg", images, index++);
		assertImage("media/res-template-page_icon-logo.png", images, index++);
		assertImage("media/lib-paging_prev-page.png", images, index++);
		assertImage("media/lib-paging_next-page.png", images, index++);
		assertImage("media/lib-paging_prev-page.png", images, index++);
		assertImage("media/lib-paging_next-page.png", images, index++);
	}

	// --------------------------------------------------------------------------------------------
	
	private BuilderProject project(String projectName) {
		try {
			File projectRoot = new File("src/test/resources/" + projectName);
			buildDir = new File(projectRoot, BuildFS.DEF_BUILD_DIR);
			BuilderProject project = new BuilderProject(projectRoot, buildDir);
			if (buildDir.exists()) {
				Files.removeFilesHierarchy(buildDir);
			}
			return project;
		} catch (IOException e) {
			e.printStackTrace();
			fail("Fail to create project instance.");
		}

		throw new IllegalStateException();
	}

	private Builder builder(String projectDir, int... buildNumber) throws IOException {
		BuilderConfig config = new BuilderConfig();
		config.setProjectDir(new File("src/test/resources/" + projectDir));
		buildDir = new File(config.getProjectDir(), "build/site");
		config.setBuildDir(buildDir);
		Files.removeFilesHierarchy(config.getBuildDir());
		if (buildNumber.length == 1) {
			config.setBuildNumber(buildNumber[0]);
		}
		return new Builder(config);
	}

	// --------------------------------------------------------------------------------------------

	private static void assertStyle(String expected, EList styles, int index) {
		assertThat(styles.item(index).getAttr("href"), equalTo(expected));
	}

	private static void assertAnchor(String expected, EList anchors, int index) {
		assertThat(anchors.item(index).getText(), equalTo(expected));
	}

	private static void assertImage(String expected, EList images, int index) {
		assertThat(images.item(index).getAttr("src"), equalTo(expected));
	}
}
