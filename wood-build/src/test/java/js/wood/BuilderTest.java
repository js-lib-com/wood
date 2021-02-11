package js.wood;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.w3c.DocumentBuilderImpl;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;
import js.wood.impl.Reference;
import js.wood.impl.ResourceType;

public class BuilderTest {
	@Test
	public void constructor() throws FileNotFoundException {
		Builder builder = new Builder(projectDir("project"));

		BuilderProject project = builder.getProject();
		assertNotNull(project);

		BuildFS buildFS = builder.getBuildFS();
		assertNotNull(buildFS);

		Collection<CompoPath> pages = builder.getPages();
		assertNotNull(pages);
		assertEquals(3, pages.size());
		assertTrue(pages.contains(new CompoPath(project, "page/index")));
		assertTrue(pages.contains(new CompoPath(project, "page/video-player")));
		assertTrue(pages.contains(new CompoPath(project, "page/videos")));

		Map<DirPath, IVariables> variables = builder.getVariables();
		assertNotNull(variables);
		assertFalse(variables.isEmpty());

		DirPath themeDir = new DirPath(project, "res/theme");
		assertNotNull(variables.get(themeDir));
	}

	@Test
	public void constructor_RootContext() throws FileNotFoundException {
		Builder builder = new Builder(projectDir("root-project"));

		BuilderProject project = builder.getProject();
		assertNotNull(project);

		BuildFS buildFS = builder.getBuildFS();
		assertNotNull(buildFS);

		Collection<CompoPath> pages = builder.getPages();
		assertNotNull(pages);
		assertEquals(3, pages.size());
		assertTrue(pages.contains(new CompoPath(project, "page/index")));
		assertTrue(pages.contains(new CompoPath(project, "page/video-player")));
		assertTrue(pages.contains(new CompoPath(project, "page/videos")));

		Map<DirPath, IVariables> variables = builder.getVariables();
		assertNotNull(variables);
		assertFalse(variables.isEmpty());

		DirPath themeDir = new DirPath(project, "res/theme");
		assertNotNull(variables.get(themeDir));
	}

	@Test
	public void build() throws IOException {
		BuilderProject project = project("project");

		// initialize probes
		final List<Locale> locales = new ArrayList<Locale>();
		final List<String> pageFileNames = new ArrayList<String>();

		BuildFS buildFS = new DefaultBuildFS(project, 0) {
			@Override
			public void setLocale(Locale locale) {
				super.setLocale(locale);
				locales.add(locale);
			}

			@Override
			public void writePage(Component compo, PageDocument page) throws IOException {
				pageFileNames.add(compo.getLayoutFileName());
			}
		};
		Builder builder = new Builder(project, buildFS);
		builder.build();

		assertEquals(4, locales.size());

		assertTrue(locales.contains(new Locale("en")));
		assertTrue(locales.contains(new Locale("de")));
		assertTrue(locales.contains(new Locale("fr")));
		assertTrue(locales.contains(new Locale("ro")));

		assertEquals(12, pageFileNames.size());
		Collections.sort(pageFileNames);

		String[] expectedFileNames = new String[] { "index.htm", "video-player.htm", "videos.htm" };
		for (int i = 0; i < 12; ++i) {
			assertEquals(expectedFileNames[i / 4], pageFileNames.get(i));
		}
	}

	@Test
	public void build_RootContext() throws IOException {
		BuilderProject project = project("root-project");

		// initialize probes
		final List<Locale> locales = new ArrayList<>();
		final List<String> pageFileNames = new ArrayList<>();

		BuildFS buildFS = new DefaultBuildFS(project, 0) {
			@Override
			public void setLocale(Locale locale) {
				super.setLocale(locale);
				locales.add(locale);
			}

			@Override
			public void writePage(Component compo, PageDocument page) throws IOException {
				pageFileNames.add(compo.getLayoutFileName());
			}
		};

		Builder builder = new Builder(project, buildFS);
		builder.build();

		assertEquals(4, locales.size());

		assertTrue(locales.contains(new Locale("en")));
		assertTrue(locales.contains(new Locale("de")));
		assertTrue(locales.contains(new Locale("fr")));
		assertTrue(locales.contains(new Locale("ro")));

		assertEquals(12, pageFileNames.size());
		Collections.sort(pageFileNames);

		String[] expectedFileNames = new String[] { "index.htm", "video-player.htm", "videos.htm" };
		for (int i = 0; i < 12; ++i) {
			assertEquals(expectedFileNames[i / 4], pageFileNames.get(i));
		}
	}

	@Test
	public void buildPage() throws Exception {
		BuilderProject project = project("project");

		BuildFS buildFS = new DefaultBuildFS(project, 0) {
			@Override
			public void writePage(Component compo, PageDocument page) throws IOException {
				assertPageDocument(page);
			}
		};

		Builder builder = new Builder(project, buildFS);
		Locale locale = new Locale("en");
		builder.setLocale(locale);
		buildFS.setLocale(locale);

		CompoPath indexPage = new CompoPath(project, "page/index");
		Classes.invoke(builder, "buildPage", indexPage);
	}

	@Test
	public void buildPage_RootContext() throws Exception {
		BuilderProject project = project("project");

		BuildFS buildFS = new DefaultBuildFS(project, 0) {
			@Override
			public void writePage(Component compo, PageDocument page) throws IOException {
				assertPageDocument(page);
			}
		};

		Builder builder = new Builder(project, buildFS);
		Locale locale = new Locale("en");
		builder.setLocale(locale);
		buildFS.setLocale(locale);

		CompoPath indexPage = new CompoPath(project, "page/index");
		Classes.invoke(builder, "buildPage", indexPage);
	}

	private static void assertPageDocument(PageDocument page) {
		Document doc = page.getDocument();

		assertEquals("Test Project / Index", doc.getByTag("title").getText());

		EList metas = doc.findByTag("meta");
		assertEquals(5, metas.size());

		assertEquals("text/html; charset=UTF-8", metas.item(0).getAttr("content"));
		assertEquals("Content-Type", metas.item(0).getAttr("http-equiv"));
		assertEquals("j(s)-lib", metas.item(1).getAttr("content"));
		assertEquals("Author", metas.item(1).getAttr("name"));
		assertEquals("Index page description.", metas.item(2).getAttr("content"));
		assertEquals("Description", metas.item(2).getAttr("name"));
		assertEquals("IE=9; IE=8; IE=7; IE=EDGE", metas.item(3).getAttr("content"));
		assertEquals("X-UA-Compatible", metas.item(3).getAttr("http-equiv"));
		assertEquals("width=device-width, initial-scale=1.0, maximum-scale=1.0", metas.item(4).getAttr("content"));
		assertEquals("viewport", metas.item(4).getAttr("name"));

		EList styles = doc.findByTag("link");
		assertEquals(13, styles.size());

		int index = 0;
		assertStyle("media/favicon.ico", styles, index++);
		assertStyle("http://fonts.googleapis.com/css?family=Roboto", styles, index++);
		assertStyle("http://fonts.googleapis.com/css?family=Great+Vibes", styles, index++);
		assertStyle("style/theme-reset.css", styles, index++);
		assertStyle("style/theme-fx.css", styles, index++);

		// site styles order, beside reset and fx is not guaranteed
		assertNotNull(doc.getByXPath("//LINK[@href='style/theme-form.css']"));
		assertNotNull(doc.getByXPath("//LINK[@href='style/theme-style.css']"));

		index += 2; // skip form.css and fx.css
		assertStyle("style/template-dialog.css", styles, index++);
		assertStyle("style/lib-paging.css", styles, index++);
		assertStyle("style/lib-list-view.css", styles, index++);
		assertStyle("style/template-page.css", styles, index++);
		assertStyle("style/template-sidebar-page.css", styles, index++);
		assertStyle("style/page-index.css", styles, index++);

		EList elist = doc.findByTag("script");
		List<String> scripts = new ArrayList<>();
		for (int i = 0; i < elist.size(); ++i) {
			scripts.add(elist.item(i).getAttr("src"));
		}
		assertEquals(10, scripts.size());

		assertTrue(scripts.indexOf("script/hc.page.Index.js") > scripts.indexOf("script/js-lib.js"));
		assertTrue(scripts.indexOf("script/hc.view.DiscographyView.js") > scripts.indexOf("script/js-lib.js"));
		assertTrue(scripts.indexOf("script/hc.view.DiscographyView.js") > scripts.indexOf("script/hc.view.VideoPlayer.js"));
		assertTrue(scripts.indexOf("script/hc.view.VideoPlayer.js") > scripts.indexOf("script/js-lib.js"));
		// assertTrue(scripts.indexOf("script/hc.view.VideoPlayer.js") > scripts.indexOf("script/js.compo.Dialog.js"));
		// assertTrue(scripts.indexOf("script/js.compo.Dialog.js") > scripts.indexOf("script/js-lib.js"));

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

		EList anchors = doc.findByTag("a");
		assertEquals(8, anchors.size());

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
		assertEquals(8, images.size());

		index = 0;
		assertImage("media/template-page_logo.jpg", images, index++);
		assertImage("media/template-page-icon_logo.png", images, index++);
		assertImage("media/template-page_page.jpg", images, index++);
		assertImage("media/template-page_icon-logo.png", images, index++);
		assertImage("media/lib-paging_prev-page.png", images, index++);
		assertImage("media/lib-paging_next-page.png", images, index++);
		assertImage("media/lib-paging_prev-page.png", images, index++);
		assertImage("media/lib-paging_next-page.png", images, index++);
	}

	@Test
	public void resourceReference() throws IOException {
		Builder builder = new Builder(projectDir("project"));
		Project project = builder.getProject();

		FilePath source = new FilePath(project, "res/page/index/index.htm");
		IReference reference = new Reference(source, ResourceType.STRING, "title");

		assertValue("Index Page", builder, "en", reference, source);
		assertValue("Indexseite", builder, "de", reference, source);
		assertValue("Index Page", builder, "en", reference, source);
		assertValue("Page Index", builder, "fr", reference, source);
		assertValue("Pagina Index", builder, "ro", reference, source);

		source = new FilePath(project, "res/page/index/index.css");
		assertValue("Index Page", builder, "en", reference, source);
		assertValue("Indexseite", builder, "de", reference, source);
		assertValue("Index Page", builder, "en", reference, source);
		assertValue("Page Index", builder, "fr", reference, source);
		assertValue("Pagina Index", builder, "ro", reference, source);

		reference = new Reference(source, ResourceType.IMAGE, "logo");

		source = new FilePath(project, "res/template/page/page.htm");
		assertValue("media/template-page_logo.jpg", builder, "en", reference, source);
		assertValue("media/template-page_logo.jpg", builder, "de", reference, source);

		source = new FilePath(project, "res/template/page/page.css");
		assertValue("../media/template-page_logo.jpg", builder, "en", reference, source);
		assertValue("../media/template-page_logo.jpg", builder, "de", reference, source);

		source = new FilePath(project, "res/template/page/page.js");
		assertValue("/project/en/media/template-page_logo.jpg", builder, "en", reference, source);
		assertValue("/project/de/media/template-page_logo.jpg", builder, "de", reference, source);
	}

	@Test
	public void resourceReference_RootContext() throws IOException {
		Builder builder = new Builder(projectDir("root-project"));
		Project project = builder.getProject();

		FilePath source = new FilePath(project, "res/page/index/index.htm");
		IReference reference = new Reference(source, ResourceType.STRING, "title");

		assertValue("Index Page", builder, "en", reference, source);
		assertValue("Indexseite", builder, "de", reference, source);
		assertValue("Index Page", builder, "en", reference, source);
		assertValue("Page Index", builder, "fr", reference, source);
		assertValue("Pagina Index", builder, "ro", reference, source);

		source = new FilePath(project, "res/page/index/index.css");
		assertValue("Index Page", builder, "en", reference, source);
		assertValue("Indexseite", builder, "de", reference, source);
		assertValue("Index Page", builder, "en", reference, source);
		assertValue("Page Index", builder, "fr", reference, source);
		assertValue("Pagina Index", builder, "ro", reference, source);

		reference = new Reference(source, ResourceType.IMAGE, "logo");

		source = new FilePath(project, "res/template/page/page.htm");
		assertValue("media/template-page_logo.jpg", builder, "en", reference, source);
		assertValue("media/template-page_logo.jpg", builder, "de", reference, source);

		source = new FilePath(project, "res/template/page/page.css");
		assertValue("../media/template-page_logo.jpg", builder, "en", reference, source);
		assertValue("../media/template-page_logo.jpg", builder, "de", reference, source);

		source = new FilePath(project, "res/template/page/page.js");
		assertValue("/root-project/en/media/template-page_logo.jpg", builder, "en", reference, source);
		assertValue("/root-project/de/media/template-page_logo.jpg", builder, "de", reference, source);
	}

	private static void assertValue(String expected, Builder builder, String languageTag, IReference reference, FilePath source) throws IOException {
		Locale locale = new Locale(languageTag);
		builder.setLocale(locale);
		BuildFS buildFS = builder.getBuildFS();
		buildFS.setLocale(locale);

		assertEquals(expected, builder.onResourceReference(reference, source));
	}

	@Test
	public void strings() throws IOException {
		Builder builder = new Builder(projectDir("strings"));
		BuilderProject project = builder.getProject();
		File buildDir = project.getSiteDir();

		builder.build();

		DocumentBuilder documentBuilder = new DocumentBuilderImpl();
		Document doc = documentBuilder.loadHTML(new File(buildDir, "de/compo.htm"));
		assertString("Zuhause", doc, "//HEADER");
		assertString("copyright © j(s)-lib tools ® 2013", doc, "//FOOTER");

		assertString("Komponente Bildunterschrift", doc, "//SECTION/H1");
		assertString("Widget Bildunterschrift", doc, "//SECTION/SECTION/H1");
		assertString("Bibliothekskomponente Bildunterschrift", doc, "//SECTION/SECTION/SECTION/H1");

		assertTooltip("component tooltip", doc, "//SECTION/P");
		assertTooltip("widget werkzeug", doc, "//SECTION/SECTION/P");
		assertTooltip("library component tooltip", doc, "//SECTION/SECTION/SECTION/P");

		assertText("Dies ist <EM>Beschreibung</EM> <STRONG>Bestandteil</STRONG>.", doc, "//SECTION/P");
		assertText("Dies ist <STRONG>Widget</STRONG> <EM>Beschreibung</EM>.", doc, "//SECTION/SECTION/P");
		assertText("Dies ist <STRONG>Bibliothekskomponente</STRONG> <EM>Beschreibung</EM>.", doc, "//SECTION/SECTION/SECTION/P");

		doc = documentBuilder.loadHTML(new File(buildDir, "en/compo.htm"));
		assertString("Home", doc, "//HEADER");
		assertString("copyright © j(s)-lib tools ® 2013", doc, "//FOOTER");

		assertString("Component Legend", doc, "//SECTION/H1");
		assertString("Widget Caption", doc, "//SECTION/SECTION/H1");
		assertString("Library Component Caption", doc, "//SECTION/SECTION/SECTION/H1");

		assertTooltip("component tooltip", doc, "//SECTION/P");
		assertTooltip("widget tooltip", doc, "//SECTION/SECTION/P");
		assertTooltip("library component tooltip", doc, "//SECTION/SECTION/SECTION/P");

		assertText("This is <STRONG>component</STRONG> <EM>description</EM>.", doc, "//SECTION/P");
		assertText("This is <STRONG>widget</STRONG> <EM>description</EM>.", doc, "//SECTION/SECTION/P");
		assertText("This is <STRONG>library component</STRONG> <EM>description</EM>.", doc, "//SECTION/SECTION/SECTION/P");

		doc = documentBuilder.loadHTML(new File(buildDir, "ro/compo.htm"));
		assertString("Acasă", doc, "//HEADER");
		assertString("copyright © j(s)-lib tools ® 2013", doc, "//FOOTER");

		assertString("Legendă component", doc, "//SECTION/H1");
		assertString("Legendă widget", doc, "//SECTION/SECTION/H1");
		assertString("Legendă component din librarie", doc, "//SECTION/SECTION/SECTION/H1");

		assertTooltip("tooltip component", doc, "//SECTION/P");
		assertTooltip("tooltip widget", doc, "//SECTION/SECTION/P");
		assertTooltip("tooltip component din librarie", doc, "//SECTION/SECTION/SECTION/P");

		assertText("Aceasta este <EM>descrierea</EM> <STRONG>componentului</STRONG>.", doc, "//SECTION/P");
		assertText("Acesta este <EM>descrierea</EM> <STRONG>widget</STRONG>.", doc, "//SECTION/SECTION/P");
		assertText("Acesta este <EM>descrierea</EM> <STRONG>componentului din librărie</STRONG>.", doc, "//SECTION/SECTION/SECTION/P");

		String script = Strings.load(new File(buildDir, "de/script/js.wood.Compo.js"));
		assertTrue(script.contains("this.setCaption('Hello');"));
		assertTrue(script.contains("this.setName('Komponentennamen');"));
		assertTrue(script.contains("this.setDescription('Dies ist <strong>Skript</strong> <em>Beschreibung</em>.');"));

		script = Strings.load(new File(buildDir, "en/script/js.wood.Compo.js"));
		assertTrue(script.contains("this.setCaption('Hello');"));
		assertTrue(script.contains("this.setName('Component Name');"));
		assertTrue(script.contains("this.setDescription('This is <strong>script</strong> <em>description</em>.');"));

		script = Strings.load(new File(buildDir, "ro/script/js.wood.Compo.js"));
		assertTrue(script.contains("this.setCaption('Hello');"));
		assertTrue(script.contains("this.setName('Nume component');"));
		assertTrue(script.contains("this.setDescription('Acesta este <em>descrierea</em> <strong>scriptului</strong>.');"));
	}

	private static void assertString(String expected, Document doc, String xpath) {
		assertEquals(expected, doc.getByXPath(xpath).getText());
	}

	private static void assertText(String expected, Document doc, String xpath) {
		assertEquals(expected, doc.getByXPath(xpath).getRichText());
	}

	private static void assertTooltip(String expected, Document doc, String xpath) {
		assertEquals(expected, doc.getByXPath(xpath).getAttr("title"));
	}

	@Test
	public void styles() throws Exception {
		Builder builder = new Builder(projectDir("styles"));
		BuilderProject project = builder.getProject();
		File buildDir = project.getSiteDir();

		builder.build();

		File pageFile = new File(buildDir, "index.htm");
		assertTrue(pageFile.exists());

		assertTrue(new File(buildDir, "style/theme-reset.css").exists());
		assertTrue(new File(buildDir, "style/theme-fx.css").exists());
		assertTrue(new File(buildDir, "style/compo.css").exists());
		assertTrue(new File(buildDir, "style/page.css").exists());
		assertTrue(new File(buildDir, "style/index.css").exists());
		assertTrue(new File(buildDir, "style/theme-form.css").exists());
		assertTrue(new File(buildDir, "style/theme-styles.css").exists());

		DocumentBuilder documentBuilder = new DocumentBuilderImpl();
		Document doc = documentBuilder.loadHTML(pageFile);
		EList styles = doc.findByTag("link");
		assertEquals(7, styles.size());

		int index = 0;
		assertStyle("style/theme-reset.css", styles, index++);
		assertStyle("style/theme-fx.css", styles, index++);

		// site styles order, beside reset and fx is not guaranteed
		assertNotNull(doc.getByXPath("//LINK[@href='style/theme-form.css']"));
		assertNotNull(doc.getByXPath("//LINK[@href='style/theme-styles.css']"));

		index += 2; // skip form.css and fx.css
		assertStyle("style/compo.css", styles, index++);
		assertStyle("style/page.css", styles, index++);
		assertStyle("style/index.css", styles, index++);
	}

	@Test
	public void styleMixin() throws IOException {
		Builder builder = new Builder(projectDir("styles"));
		BuilderProject project = builder.getProject();
		File buildDir = project.getSiteDir();

		builder.build();

		File styleFile = new File(buildDir, "style/page.css");
		assertTrue(styleFile.exists());

		String style = Strings.load(styleFile);
		assertTrue(style.contains("background-color: #001122;"));
		assertTrue(style.contains("color: white;"));
		assertTrue(style.contains("width: 50%;"));
		assertTrue(style.contains("height: 80px;"));
	}

	@Test
	public void scripts() throws IOException {
		Builder builder = new Builder(projectDir("scripts"));
		BuilderProject project = builder.getProject();
		File buildDir = project.getSiteDir();

		builder.build();

		File pageFile = new File(buildDir, "index.htm");
		assertTrue(pageFile.exists());
		assertTrue(new File(buildDir, "style/theme-reset.css").exists());
		assertTrue(new File(buildDir, "script/js-lib.js").exists());
		assertTrue(new File(buildDir, "script/js.format.RichText.js").exists());
		assertTrue(new File(buildDir, "script/js.widget.Description.js").exists());
		assertTrue(new File(buildDir, "script/js.wood.IndexPage.js").exists());

		DocumentBuilder documentBuilder = new DocumentBuilderImpl();
		Document doc = documentBuilder.loadHTML(pageFile);
		EList scripts = doc.findByTag("script");
		assertEquals(5, scripts.size());

		int index = 0;
		assertScript("script/js-lib.js", scripts, index++);
		assertScript("script/gen.js.controller.MainController.js", scripts, index++);
		assertScript("script/js.wood.IndexPage.js", scripts, index++);
		assertScript("script/js.format.RichText.js", scripts, index++);
		assertScript("script/js.widget.Description.js", scripts, index++);

		String script = Strings.load(new File(buildDir, "script/js.widget.Description.js"));
		assertTrue(script.contains("this.setCaption(\"caption\");"));
		assertTrue(script.contains("this.setText(\"Description.\");"));
	}

	@Test
	public void thirdPartyScripts() throws Exception {
		Builder builder = new Builder(projectDir("scripts"));
		BuilderProject project = builder.getProject();
		File buildDir = project.getSiteDir();

		builder.build();

		File pageFile = new File(buildDir, "geo-map.htm");
		assertTrue(pageFile.exists());
		assertTrue(new File(buildDir, "style/theme-reset.css").exists());
		assertTrue(new File(buildDir, "script/js-lib.js").exists());
		assertTrue(new File(buildDir, "script/js.wood.GeoMap.js").exists());
		assertTrue(new File(buildDir, "script/google-maps-api.js").exists());

		DocumentBuilder documentBuilder = new DocumentBuilderImpl();
		Document doc = documentBuilder.loadHTML(pageFile);
		doc.dump();
		EList scripts = doc.findByTag("script");
		assertEquals(4, scripts.size());

		int index = 0;
		assertScript("http://maps.google.com/maps/api/js?sensor=false", scripts, index++);
		assertScript("script/js-lib.js", scripts, index++);
		assertScript("script/js.wood.GeoMap.js", scripts, index++);
		assertScript("script/google-maps-api.js", scripts, index++);
	}

	@Test
	public void images() throws IOException {
		Builder builder = new Builder(projectDir("images"));
		BuilderProject project = builder.getProject();
		File buildDir = project.getSiteDir();

		builder.build();

		String[] layoutImages = new String[] { "asset_logo.png", //
				"compo_logo.png", //
				"lib-compo_logo.jpg", //
				"widget_next-page.png", //
				"widget_prev-page.png" };
		String[] styleImages = new String[] { "compo_background.jpg", //
				"lib-compo_background.jpg", //
				"template_background.jpg", //
				"theme_background.jpg", //
				"widget_background.jpg" };

		for (String image : layoutImages) {
			assertTrue("Media file not found: " + image, new File(buildDir, "media/" + image).exists());
		}
		for (String image : styleImages) {
			assertTrue("Media file not found: " + image, new File(buildDir, "media/" + image).exists());
		}

		String layout = Strings.load(new File(buildDir, "compo.htm"));
		for (String image : layoutImages) {
			assertTrue(layout.contains(image));
		}

		assertStyleImage("../media/compo_background.jpg", buildDir, "compo.css");
		assertStyleImage("../media/lib-compo_background.jpg", buildDir, "lib-compo.css");
		assertStyleImage("../media/template_background.jpg", buildDir, "template.css");
		assertStyleImage("../media/theme_background.jpg", buildDir, "theme-style.css");
		assertStyleImage("../media/widget_background.jpg", buildDir, "widget.css");
	}

	private static void assertStyleImage(String expected, File buildDir, String styleFile) throws IOException {
		assertTrue(Strings.load(new File(buildDir, "style/" + styleFile)).contains(expected));
	}

	@Test
	public void expression() throws IOException {
		Builder builder = new Builder(projectDir("project"));
		BuilderProject project = builder.getProject();
		resetProjectLocales(project);
		builder.build();

		File style = new File(project.getSiteDir(), "style/template-page.css");
		assertTrue(style.exists());
		assertTrue(Strings.load(style).contains("min-height: 82.0px;"));
	}

	@Test
	public void expression_RootContext() throws IOException {
		Builder builder = new Builder(projectDir("root-project"));
		BuilderProject project = builder.getProject();
		resetProjectLocales(project);
		builder.build();

		File style = new File(project.getSiteDir(), "style/template-page.css");
		assertTrue(style.exists());
		assertTrue(Strings.load(style).contains("min-height: 82.0px;"));
	}

	@Test
	public void sitePathTrailingSeparator() {
		BuilderProject project = project("project");
		assertTrue(project.getSitePath().endsWith("/"));
	}

	@Test
	public void sitePathTrailingSeparator_RootContext() {
		BuilderProject project = project("root-project");
		assertTrue(project.getSitePath().endsWith("/"));
	}

	@Test
	public void isExcluded() {
		BuilderProject project = project("project");
		assertTrue(project.isExcluded(new CompoPath(project, "page/about")));
		assertTrue(project.isExcluded(new DirPath(project, "res/page/about")));
		assertTrue(project.isExcluded(new FilePath(project, "res/page/about/about.htm")));
		assertFalse(project.isExcluded(new CompoPath(project, "page/index")));
	}

	@Test
	public void isExcluded_RootContext() {
		BuilderProject project = project("root-project");
		assertTrue(project.isExcluded(new CompoPath(project, "page/about")));
		assertTrue(project.isExcluded(new DirPath(project, "res/page/about")));
		assertTrue(project.isExcluded(new FilePath(project, "res/page/about/about.htm")));
		assertFalse(project.isExcluded(new CompoPath(project, "page/index")));
	}

	@Test
	public void buildNumber() throws Exception {
		Builder builder = new Builder(projectDir("project"), new File(projectDir("project"), "build/site"), 1);
		builder.setLocale(new Locale("en"));
		BuilderProject project = builder.getProject();
		resetProjectLocales(project);

		builder.buildPage(new CompoPath(project, "page/index"));

		File buildDir = project.getSiteDir();
		assertFile(buildDir, "index-001.htm");
		assertFile(buildDir, "media/template-page_logo-001.jpg");
		assertFile(buildDir, "script/js-lib-001.js");
		assertFile(buildDir, "script/hc.page.Index-001.js");
		assertFile(buildDir, "style/theme-reset-001.css");
		assertFile(buildDir, "style/page-index-001.css");

		String page = Strings.load(new File(buildDir, "index-001.htm"));
		assertTrue(page.contains("media/template-page_logo-001.jpg"));
		assertTrue(page.contains("script/js-lib-001.js"));
		assertTrue(page.contains("script/hc.page.Index-001.js"));
		assertTrue(page.contains("style/theme-reset-001.css"));
		assertTrue(page.contains("style/page-index-001.css"));
	}

	@Test
	public void buildNumber_RootContext() throws Exception {
		Builder builder = new Builder(projectDir("root-project"), new File(projectDir("project"), "build/site"), 1);
		builder.setLocale(new Locale("en"));
		BuilderProject project = builder.getProject();
		resetProjectLocales(project);

		builder.buildPage(new CompoPath(project, "page/index"));

		File buildDir = project.getSiteDir();
		assertFile(buildDir, "index-001.htm");
		assertFile(buildDir, "media/template-page_logo-001.jpg");
		assertFile(buildDir, "script/js-lib-001.js");
		assertFile(buildDir, "script/hc.page.Index-001.js");
		assertFile(buildDir, "style/theme-reset-001.css");
		assertFile(buildDir, "style/page-index-001.css");

		String page = Strings.load(new File(buildDir, "index-001.htm"));
		assertTrue(page.contains("media/template-page_logo-001.jpg"));
		assertTrue(page.contains("script/js-lib-001.js"));
		assertTrue(page.contains("script/hc.page.Index-001.js"));
		assertTrue(page.contains("style/theme-reset-001.css"));
		assertTrue(page.contains("style/page-index-001.css"));
	}

	private static void assertFile(File buildDir, String path) {
		assertTrue(new File(buildDir, path).exists());
	}

	// ------------------------------------------------------
	// PageDocument

	@Test
	public void pageDocumentSetters() throws IOException {
		DocumentBuilder builder = new DocumentBuilderImpl();
		Document metas = builder.parseXML("" + //
				"<metas>" + //
				"   <meta http-equiv='X-UA-Compatible' content='IE=9; IE=8; IE=7; IE=EDGE' />" + //
				"</metas>");

		BuilderProject project = project("scripts");
		project.scanBuildFiles();

		CompoPath compoPath = new CompoPath(project, "index");
		Component compo = new Component(compoPath, nullReferenceHandler());
		compo.scan(false);

		PageDocument page = new PageDocument(compo);
		page.setTitle("title");
		page.setAuthor("author");
		page.setDescription("description");
		page.setMetas(metas.getRoot().getChildren());
		page.addStyle("style/index.css");
		page.addScript("script/index.js", false);

		String doc = stringify(page.getDocument());
		assertTrue(doc.contains("<META content=\"author\" name=\"Author\"></META>"));
		assertTrue(doc.contains("<TITLE>title</TITLE>"));
		assertTrue(doc.contains("<META content=\"description\" name=\"Description\"></META>"));
		assertTrue(doc.contains("<META content=\"IE=9; IE=8; IE=7; IE=EDGE\" http-equiv=\"X-UA-Compatible\"></META>"));
		assertTrue(doc.contains("<LINK href=\"style/index.css\" rel=\"stylesheet\" type=\"text/css\"></LINK>"));
		assertTrue(doc.contains("<SCRIPT src=\"script/index.js\" type=\"text/javascript\"></SCRIPT>"));
	}

	@Test
	public void pageDocumentIncludeAnalyticsScript() throws IOException {
		BuilderProject project = project("scripts");
		project.scanBuildFiles();

		CompoPath compoPath = new CompoPath(project, "index");
		Component compo = new Component(compoPath, nullReferenceHandler());
		compo.scan(false);

		PageDocument page = new PageDocument(compo);
		page.addSDKScript(project.getFile("lib/sdk/analytics.js"), "UI-12345678");
		assertTrue(stringify(page.getDocument()).contains("UI-12345678"));

		page = new PageDocument(compo);
		page.addSDKScript(project.getFile("lib/sdk/analytics.js"), null);
		assertFalse(stringify(page.getDocument()).contains("UI-12345678"));
	}

	private static File projectDir(String dirName) {
		return new File("src/test/resources/" + dirName);
	}

	private static void assertStyle(String expected, EList styles, int index) {
		assertEquals(expected, styles.item(index).getAttr("href"));
	}

	private static void assertAnchor(String expected, EList anchors, int index) {
		assertEquals(expected, anchors.item(index).getText());
	}

	private static void assertImage(String expected, EList images, int index) {
		assertEquals(expected, images.item(index).getAttr("src"));
	}

	private static void assertScript(String expected, EList scripts, int index) {
		assertEquals(expected, scripts.item(index).getAttr("src"));
	}

	private static IReferenceHandler nullReferenceHandler() {
		return new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourceFile) throws IOException {
				return "null";
			}

			@Override
			public String toString() {
				return "null reference handler";
			}
		};
	}

	private static void resetProjectLocales(Project project) {
		List<Locale> locales = new ArrayList<Locale>();
		locales.add(new Locale("en"));
		Classes.setFieldValue(Classes.getFieldValue(project, Project.class, "descriptor"), "locales", locales);
	}

	private static String stringify(Document document) throws IOException {
		StringWriter writer = new StringWriter();
		document.serialize(writer);
		return writer.toString();
	}

	private static BuilderProject project(String projectDir) {
		try {
			BuilderProject project = new BuilderProject(new File("src/test/resources/" + projectDir));
			if (project.getSiteDir().exists()) {
				Files.removeFilesHierarchy(project.getSiteDir());
			}
			return project;
		} catch (IOException e) {
			e.printStackTrace();
			fail("Fail to create project instance.");
		}

		throw new IllegalStateException();
	}
}
