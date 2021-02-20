package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.Element;
import js.dom.w3c.DocumentBuilderImpl;
import js.util.Classes;
import js.util.Strings;
import js.wood.CompoPath;
import js.wood.Component;
import js.wood.FilePath;
import js.wood.IReferenceHandler;
import js.wood.Preview;
import js.wood.PreviewProject;
import js.wood.Reference;
import js.wood.Variables;

@RunWith(MockitoJUnitRunner.class)
public class PreviewTest implements IReferenceHandler {
	private PreviewProject project;

	@Test
	public void previewSimpleLayout() throws IOException {
		Document doc = exercisePreview("components", "res/simple/layout");
		assertEmptyHead(doc);
		assertThat(doc.getByTag("title").getText(), equalTo("Components / Layout"));
		assertThat(doc.getByTag("h1").getText(), equalTo("Simple Layout"));
	}

	@Test
	public void previewSimpleTemplate() throws IOException {
		Document doc = exercisePreview("components", "res/simple/template/compo");
		assertEmptyHead(doc);
		assertThat(doc.getByTag("title").getText(), equalTo("Components / Compo"));

		EList headings = doc.findByTag("h1");
		assertThat(headings.size(), equalTo(2));
		assertThat(headings.item(0).getText(), equalTo("Template"));
		assertThat(headings.item(1).getText(), equalTo("Content"));
	}

	@Test
	public void previewVariableTemplate() throws IOException {
		Document doc = exercisePreview("components", "res/variable-template/compo");
		assertEmptyHead(doc);
		assertThat(doc.getByTag("title").getText(), equalTo("Components / Compo"));

		EList inputs = doc.findByTag("input");
		assertThat(inputs.size(), equalTo(3));
		assertThat(inputs.item(0).getAttr("name"), equalTo("user-name"));
		assertThat(inputs.item(1).getAttr("name"), equalTo("address"));
		assertThat(inputs.item(2).getAttr("name"), equalTo("password"));
	}

	@Test
	public void previewTemplatesHierarchy() throws IOException {
		Document doc = exercisePreview("components", "res/templates-hierarchy/compo");
		assertEmptyHead(doc);
		assertThat(doc.getByTag("title").getText(), equalTo("Components / Compo"));

		EList headings = doc.findByTag("h1");
		assertThat(headings.size(), equalTo(3));
		assertThat(headings.item(0).getText(), equalTo("Grand Parent"));
		assertThat(headings.item(1).getText(), equalTo("Parent"));
		assertThat(headings.item(2).getText(), equalTo("Child"));
	}

	@Test
	public void previewSimpleWidget() throws IOException {
		Document doc = exercisePreview("components", "res/simple/widget/compo");
		assertEmptyHead(doc);
		assertThat(doc.getByTag("title").getText(), equalTo("Components / Compo"));

		EList headings = doc.findByTag("h1");
		assertThat(headings.size(), equalTo(2));
		assertThat(headings.item(0).getText(), equalTo("Component"));
		assertThat(headings.item(1).getText(), equalTo("Widget"));
	}

	@Test
	public void previewWidgetsTree() throws IOException {
		Document doc = exercisePreview("components", "res/widgets-tree/compo");
		assertEmptyHead(doc);
		assertThat(doc.getByTag("title").getText(), equalTo("Components / Compo"));

		EList headings = doc.findByTag("h1");
		assertThat(headings.size(), equalTo(5));
		assertThat(headings.item(0).getText(), equalTo("Component"));
		assertThat(headings.item(1).getText(), equalTo("Child One"));
		assertThat(headings.item(2).getText(), equalTo("Nephew One"));
		assertThat(headings.item(3).getText(), equalTo("Child Two"));
		assertThat(headings.item(4).getText(), equalTo("Nephew Two"));
	}

	@Test
	public void previewStrings() throws IOException {
		Document doc = exercisePreview("strings", "res/compo");
		assertThat(doc.getByTag("h1").getText(), equalTo("Component Legend"));
		assertThat(doc.getByTag("p").getAttr("title"), equalTo("component tooltip"));
	}

	@Test
	public void previewStyles() throws Throwable {
		Document doc = exercisePreview("styles", "res/index");
		EList styles = doc.findByTag("link");
		assertThat(styles.size(), equalTo(7));

		int index = 0;
		assertStyle("/styles-preview/res/theme/reset.css", styles, index++);
		assertStyle("/styles-preview/res/theme/fx.css", styles, index++);

		// site styles order, beside reset and fx is not guaranteed
		assertNotNull(doc.getByXPath("//LINK[@href='/styles-preview/res/theme/form.css']"));
		assertNotNull(doc.getByXPath("//LINK[@href='/styles-preview/res/theme/styles.css']"));

		index += 2; // skip form.css and styles.css
		assertStyle("/styles-preview/res/compo/compo.css", styles, index++);
		assertStyle("/styles-preview/res/page/page.css", styles, index++);
		assertStyle("/styles-preview/res/index/index.css", styles, index++);
	}

	@Test
	public void previewScripts() throws IOException {
		Document doc = exercisePreview("scripts", "res/index");
		EList scripts = doc.findByTag("script");
		assertThat(scripts.size(), equalTo(6));

		File projectDir = new File("src/test/resources/scripts");
		for (Element script : scripts) {
			File file = new File(projectDir, script.getAttr("src"));
			assertNotNull(file);
		}

		int index = 0;
		assertScript("/scripts-preview/lib/js-lib/js-lib.js", scripts, index++);
		assertScript("/scripts-preview/gen/js/controller/MainController.js", scripts, index++);
		assertScript("/scripts-preview/script/js/wood/IndexPage.js", scripts, index++);
		assertScript("/scripts-preview/script/js/format/RichText.js", scripts, index++);
		assertScript("/scripts-preview/script/js/widget/Description.js", scripts, index++);
		assertScript("/scripts-preview/res/index/preview.js", scripts, index++);
	}

	@Test
	public void previewThirdPartyScripts() throws IOException {
		Document doc = exercisePreview("scripts", "res/geo-map");
		EList scripts = doc.findByTag("script");
		assertThat(scripts.size(), equalTo(4));

		int index = 0;
		assertScript("http://maps.google.com/maps/api/js?sensor=false", scripts, index++);
		assertScript("/scripts-preview/lib/js-lib/js-lib.js", scripts, index++);
		assertScript("/scripts-preview/script/js/wood/GeoMap.js", scripts, index++);
		assertScript("/scripts-preview/lib/google-maps-api.js", scripts, index++);
	}

	@Test
	public void previewImages() throws IOException {
		Document doc = exercisePreview("images", "res/compo");
		EList images = doc.findByTag("img");
		assertThat(images.size(), equalTo(7));

		int index = 0;
		assertImage("res-template-logo.png", images, index++);
		assertImage("res-compo-logo.png", images, index++);
		assertImage(null, images, index++);
		assertImage("https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif", images, index++);
		assertImage("res-widget-prev-page.png", images, index++);
		assertImage("res-widget-next-page.png", images, index++);
		assertImage("lib-compo-logo.png", images, index++);
	}

	@Test
	public void previewProjectIndexPage() throws IOException {
		assertIndexPage(exercisePreview("project", "res/page/index"));
	}

	// ------------------------------------------------------
	// Preview helpers

	@Override
	public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException {
		if (reference.isVariable()) {
			Variables variables = new Variables(sourceFile.getParentDirPath());
			if (project.getAssetsDir().exists()) {
				try {
					Classes.invoke(variables, "load", project.getAssetsDir());
				} catch (Exception e) {
					e.printStackTrace();
				}
			}
			return variables.get(new Locale("en"), reference, sourceFile, this);
		}

		return Strings.concat(Strings.join(sourceFile.getParentDirPath().getPathSegments(), '-'), '-', reference.getName(), ".png");
	}

	private Document exercisePreview(String projectDirName, String pathValue) throws IOException {
		project = new PreviewProject(new File("src/test/resources/" + projectDirName));

		CompoPath path = new CompoPath(project, pathValue);
		Component component = new Component(path.getLayoutPath(), this);
		component.scan();

		Preview preview = new Preview(Strings.concat('/', projectDirName, "-preview"), project, component);
		StringWriter writer = new StringWriter();
		preview.serialize(writer);

		DocumentBuilder builder = new DocumentBuilderImpl();
		return builder.parseHTML(writer.toString());
	}

	private static void assertEmptyHead(Document doc) {
		EList styles = doc.findByTag("link");
		assertThat(styles.size(), equalTo(2));
		assertStyle("/components-preview/res/theme/reset.css", styles, 0);
		assertStyle("/components-preview/res/theme/fx.css", styles, 1);

		assertTrue(doc.findByTag("script").isEmpty());
	}

	private static void assertIndexPage(Document doc) {
		assertThat(doc.getByTag("title").getText(), equalTo("Test Project / Index"));

		EList links = doc.findByTag("link");
		assertThat(links.size(), equalTo(12));

		int index = 0;
		assertStyle("http://fonts.googleapis.com/css?family=Roboto", links, index++);
		assertStyle("http://fonts.googleapis.com/css?family=Great+Vibes", links, index++);
		assertStyle("/project-preview/res/theme/reset.css", links, index++);
		assertStyle("/project-preview/res/theme/fx.css", links, index++);

		// site styles order, beside reset and fx is not guaranteed
		assertNotNull(doc.getByXPath("//LINK[@href='/project-preview/res/theme/form.css']"));
		assertNotNull(doc.getByXPath("//LINK[@href='/project-preview/res/theme/style.css']"));

		index += 2; // skip form.css and styles.css
		assertStyle("/project-preview/res/template/dialog/dialog.css", links, index++);
		assertStyle("/project-preview/lib/paging/paging.css", links, index++);
		assertStyle("/project-preview/lib/list-view/list-view.css", links, index++);
		assertStyle("/project-preview/res/template/page/page.css", links, index++);
		assertStyle("/project-preview/res/template/sidebar-page/sidebar-page.css", links, index++);
		assertStyle("/project-preview/res/page/index/index.css", links, index++);

		EList elist = doc.findByTag("script");
		List<String> scripts = new ArrayList<>();
		// first is analytics inline code
		for (int i = 0; i < elist.size(); ++i) {
			scripts.add(elist.item(i).getAttr("src"));
		}
		assertThat(scripts, hasSize(10));

		assertTrue(scripts.indexOf("/project-preview/script/hc/page/Index.js") > scripts.indexOf("/project/lib/js-lib/js-lib.js"));
		assertTrue(scripts.indexOf("/project-preview/script/hc/view/DiscographyView.js") > scripts.indexOf("/project/lib/js-lib/js-lib.js"));
		assertTrue(scripts.indexOf("/project-preview/script/hc/view/DiscographyView.js") > scripts.indexOf("/project/script/hc/view/VideoPlayer.js"));
		assertTrue(scripts.indexOf("/project-preview/script/hc/view/VideoPlayer.js") > scripts.indexOf("/project/lib/js-lib/js-lib.js"));
		assertTrue(scripts.indexOf("/project-preview/script/hc/view/VideoPlayer.js") > scripts.indexOf("/project/script/js.compo.Dialog.js"));
		assertTrue(scripts.indexOf("/project-preview/script/js/compo/Dialog.js") > scripts.indexOf("/project/lib/js-lib/js-lib.js"));

		assertTrue(scripts.contains("/project-preview/lib/js-lib/js-lib.js"));
		assertTrue(scripts.contains("/project-preview/script/hc/page/Index.js"));
		assertTrue(scripts.contains("/project-preview/gen/js/controller/MainController.js"));
		assertTrue(scripts.contains("/project-preview/script/js/hood/MainMenu.js"));
		assertTrue(scripts.contains("/project-preview/script/hc/view/VideoPlayer.js"));
		assertTrue(scripts.contains("/project-preview/lib/list-view/list-view.js"));
		assertTrue(scripts.contains("/project-preview/lib/paging/paging.js"));
		assertTrue(scripts.contains("/project-preview/script/js/compo/Dialog.js"));
		assertTrue(scripts.contains("/project-preview/script/js/hood/TopMenu.js"));
		assertTrue(scripts.contains("/project-preview/script/hc/view/DiscographyView.js"));
	}

	private static void assertStyle(String expected, EList styles, int index) {
		assertThat(styles.item(index).getAttr("href"), equalTo(expected));
	}

	private static void assertScript(String expected, EList scripts, int index) {
		assertThat(scripts.item(index).getAttr("src"), equalTo(expected));
	}

	private static void assertImage(String expected, EList images, int index) {
		assertThat(images.item(index).getAttr("src"), equalTo(expected));
	}
}
