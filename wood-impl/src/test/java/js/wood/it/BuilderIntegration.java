package js.wood.it;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.w3c.DocumentBuilderImpl;
import js.wood.Builder;
import js.wood.WoodTestCase;

import org.junit.Test;

public class BuilderIntegration extends WoodTestCase {
	private static final String[] LANGUAGES = new String[] { "de", "en", "fr", "ro" };

	private static final Map<String, String> TITLES = new HashMap<String, String>();
	static {
		TITLES.put("de", "Index Page Titel");
		TITLES.put("en", "Index Page");
		TITLES.put("fr", "Indice de titre de la page");
		TITLES.put("ro", "Titlul paginii index");
	}

	private static final Map<String, Integer> FILES_COUNT = new HashMap<String, Integer>();
	static {
		FILES_COUNT.put(".", 6); // 3 files and 3 directories
		FILES_COUNT.put("media", 16);
		FILES_COUNT.put("script", 12);
		FILES_COUNT.put("style", 13);
	}

	private static final String[] FILES = new String[] { "index.htm", //
			"video-player.htm", //
			"videos.htm", //
			"media/favicon.ico", //
			"media/asset_background.jpg", //
			"media/lib-paging_background.jpg", //
			"media/lib-paging_next-page.png", //
			"media/lib-paging_prev-page.png", //
			"media/page-index_header-bg.jpg", //
			"media/page-video-player_background.jpg", //
			"media/template-dialog_x-close-hover.jpg", //
			"media/template-dialog_x-close.jpg", //
			"media/template-page_background.jpg", //
			"media/template-page-icon_logo.png", //
			"media/template-page_logo.jpg", //
			"media/template-page_icon-logo.png", //
			"media/template-page_menu-bg.jpg", //
			"media/template-page_page.jpg", //
			"media/theme_background.jpg", //
			"script/gen.js.controller.MainController.js", //
			"script/hc.format.ReleasedDate.js", //
			"script/hc.page.Index.js", //
			"script/hc.view.DiscographyView.js", //
			"script/hc.view.VideoPlayer.js", //
			"script/js-lib.js", //
			"script/js.compo.Dialog.js", //
			"script/js.compo.ListCtrl.js", //
			"script/js.hood.MainMenu.js", //
			"script/js.hood.TopMenu.js", //
			"script/list-view.js", //
			"script/paging.js", //
			"style/compo-discography.css", //
			"style/theme-fx.css", //
			"style/lib-list-view.css", //
			"style/lib-paging.css", //
			"style/page-index.css", //
			"style/page-video-player.css", //
			"style/page-videos.css", //
			"style/theme-reset.css", //
			"style/template-dialog.css", //
			"style/template-page.css", //
			"style/template-sidebar-page.css", //
			"style/theme-form.css", //
			"style/theme-style.css" //
	};

	private static final String[] STYLES = new String[] { "media/favicon.ico", //
			"http://fonts.googleapis.com/css?family=Roboto", //
			"http://fonts.googleapis.com/css?family=Great+Vibes", //
			"style/theme-reset.css", //
			"style/theme-fx.css", //
			"style/theme-form.css", //
			"style/theme-style.css", //
			"style/template-dialog.css", //
			"style/lib-paging.css", //
			"style/lib-list-view.css", //
			"style/template-page.css", //
			"style/template-sidebar-page.css", //
			"style/page-index.css" };

	private static final String[] IMAGES = new String[] { "media/template-page_logo.jpg", //
			"media/template-page-icon_logo.png", //
			"media/template-page_page.jpg", //
			"media/template-page_icon-logo.png", //
			"media/lib-paging_prev-page.png", //
			"media/lib-paging_next-page.png", //
			"media/lib-paging_prev-page.png", //
			"media/lib-paging_next-page.png" };

	private static final String[] ANCHORS = new String[] { "Logout", //
			"Login", //
			"Register", //
			"Home", //
			"Videos", //
			"Mixes", //
			"News Feed", //
			"User Profile" //
	};

	@Test
	public void buildIntegration() throws IOException {
		Builder builder = new Builder(path("project"));
		builder.build();

		File buildDir = file("project/build/site/");

		for (String language : LANGUAGES) {
			for (String dir : FILES_COUNT.keySet()) {
				assertEquals((int) FILES_COUNT.get(dir), file(buildDir, language, dir).list().length);
			}

			for (String file : FILES) {
				assertTrue("File not found: " + file, file(buildDir, language, file).exists());
			}

			DocumentBuilder documentBuilder = new DocumentBuilderImpl();
			Document page = documentBuilder.loadHTML(file(buildDir, language, "index.htm"));
			assertEquals(TITLES.get(language), page.getByTag("title").getText());

			EList styles = page.findByTag("link");
			assertEquals(STYLES.length, styles.size());
			for (int i = 0; i < STYLES.length; ++i) {
				assertStyle(STYLES[i], styles, i);
			}

			EList elist = page.findByTag("script");
			List<String> scripts = new ArrayList<>();
			for (int i = 0; i < elist.size(); ++i) {
				scripts.add(elist.item(i).getAttr("src"));
			}
			assertEquals(10, scripts.size());

			assertTrue(scripts.indexOf("script/hc.page.Index.js") > scripts.indexOf("script/js-lib.js"));
			assertTrue(scripts.indexOf("script/hc.view.DiscographyView.js") > scripts.indexOf("script/js-lib.js"));
			assertTrue(scripts.indexOf("script/hc.view.DiscographyView.js") > scripts.indexOf("script/hc.view.VideoPlayer.js"));
			assertTrue(scripts.indexOf("script/hc.view.VideoPlayer.js") > scripts.indexOf("script/js-lib.js"));
			assertTrue(scripts.indexOf("script/hc.view.VideoPlayer.js") > scripts.indexOf("script/js.compo.Dialog.js"));
			assertTrue(scripts.indexOf("script/js.compo.Dialog.js") > scripts.indexOf("script/js-lib.js"));

			assertTrue(scripts.contains("script/js-lib.js"));
			assertTrue(scripts.contains("script/js.compo.Dialog.js"));
			assertTrue(scripts.contains("script/hc.view.VideoPlayer.js"));
			assertTrue(scripts.contains("script/js.hood.MainMenu.js"));
			assertTrue(scripts.contains("script/hc.page.Index.js"));
			assertTrue(scripts.contains("script/hc.view.DiscographyView.js"));
			assertTrue(scripts.contains("script/gen.js.controller.MainController.js"));
			assertTrue(scripts.contains("script/list-view.js"));
			assertTrue(scripts.contains("script/paging.js"));
			assertTrue(scripts.contains("script/js.hood.TopMenu.js"));

			EList images = page.findByTag("img");
			assertEquals(IMAGES.length, images.size());
			for (int i = 0; i < IMAGES.length; ++i) {
				assertImage(IMAGES[i], images, i);
			}

			EList anchors = page.findByTag("a");
			assertEquals(ANCHORS.length, anchors.size());
			for (int i = 0; i < ANCHORS.length; ++i) {
				assertAnchor(ANCHORS[i], anchors, i);
			}
		}
	}
}
