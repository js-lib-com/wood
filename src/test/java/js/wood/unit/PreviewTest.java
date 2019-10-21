package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertThat;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.*;
import static org.hamcrest.Matchers.*;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
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
import js.wood.Preview;
import js.wood.PreviewServlet;
import js.wood.Project;
import js.wood.Reference;
import js.wood.ReferenceHandler;
import js.wood.ResourceType;
import js.wood.Variables;
import js.wood.WoodException;

@RunWith(MockitoJUnitRunner.class)
public class PreviewTest extends WoodTestCase implements ReferenceHandler {
	private Project project;
	private StringWriter responseWriter = new StringWriter();
	private ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

	// ------------------------------------------------------
	// Preview

	@Test
	public void previewSimpleLayout() throws IOException {
		Document doc = exercisePreview("components", "res/simple/layout");
		assertEmptyHead(doc);
		assertEquals("Components / Layout", doc.getByTag("title").getText());
		assertEquals("Simple Layout", doc.getByTag("h1").getText());
	}

	@Test
	public void previewSimpleTemplate() throws IOException {
		Document doc = exercisePreview("components", "res/simple/template/compo");
		assertEmptyHead(doc);
		assertEquals("Components / Compo", doc.getByTag("title").getText());

		EList headings = doc.findByTag("h1");
		assertEquals(2, headings.size());
		assertEquals("Template", headings.item(0).getText());
		assertEquals("Content", headings.item(1).getText());
	}

	@Test
	public void previewVariableTemplate() throws IOException {
		Document doc = exercisePreview("components", "res/variable-template/compo");
		assertEmptyHead(doc);
		assertEquals("Components / Compo", doc.getByTag("title").getText());

		EList inputs = doc.findByTag("input");
		assertEquals(3, inputs.size());
		assertEquals("user-name", inputs.item(0).getAttr("name"));
		assertEquals("address", inputs.item(1).getAttr("name"));
		assertEquals("password", inputs.item(2).getAttr("name"));
	}

	@Test
	public void previewTemplatesHierarchy() throws IOException {
		Document doc = exercisePreview("components", "res/templates-hierarchy/compo");
		assertEmptyHead(doc);
		assertEquals("Components / Compo", doc.getByTag("title").getText());

		EList headings = doc.findByTag("h1");
		assertEquals(3, headings.size());
		assertEquals("Grand Parent", headings.item(0).getText());
		assertEquals("Parent", headings.item(1).getText());
		assertEquals("Child", headings.item(2).getText());
	}

	@Test
	public void previewSimpleWidget() throws IOException {
		Document doc = exercisePreview("components", "res/simple/widget/compo");
		assertEmptyHead(doc);
		assertEquals("Components / Compo", doc.getByTag("title").getText());

		EList headings = doc.findByTag("h1");
		assertEquals(2, headings.size());
		assertEquals("Component", headings.item(0).getText());
		assertEquals("Widget", headings.item(1).getText());
	}

	@Test
	public void previewWidgetsTree() throws IOException {
		Document doc = exercisePreview("components", "res/widgets-tree/compo");
		assertEmptyHead(doc);
		assertEquals("Components / Compo", doc.getByTag("title").getText());

		EList headings = doc.findByTag("h1");
		assertEquals(5, headings.size());
		assertEquals("Component", headings.item(0).getText());
		assertEquals("Child One", headings.item(1).getText());
		assertEquals("Nephew One", headings.item(2).getText());
		assertEquals("Child Two", headings.item(3).getText());
		assertEquals("Nephew Two", headings.item(4).getText());
	}

	@Test
	public void previewStrings() throws IOException {
		Document doc = exercisePreview("strings", "compo");
		assertEquals("Component Legend", doc.getByTag("h1").getText());
		assertEquals("component tooltip", doc.getByTag("p").getAttr("title"));
	}

	@Test
	public void previewStyles() throws Throwable {
		Document doc = exercisePreview("styles", "res/index");
		EList styles = doc.findByTag("link");
		assertEquals(7, styles.size());

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
		assertEquals(6, scripts.size());

		File projectDir = new File("fixture/scripts");
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
		assertEquals(4, scripts.size());

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
		assertEquals(7, images.size());

		int index = 0;
		assertImage("template-logo.png", images, index++);
		assertImage("compo-logo.png", images, index++);
		assertImage(null, images, index++);
		assertImage("https://www.paypalobjects.com/en_US/i/btn/btn_donate_SM.gif", images, index++);
		assertImage("widget-prev-page.png", images, index++);
		assertImage("widget-next-page.png", images, index++);
		assertImage("compo-logo.png", images, index++);
	}

	@Test
	public void previewProjectIndexPage() throws IOException {
		assertIndexPage(exercisePreview("project", "page/index"));
	}

	// ------------------------------------------------------
	// Preview helpers

	@Override
	public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException {
		if (reference.isVariable()) {
			Variables variables = new Variables(project, sourceFile.getDirPath());
			if (project.getAssetsDir().exists()) {
				invoke(variables, "load", project.getAssetsDir());
			}
			return variables.get(new Locale("en"), reference, sourceFile, this);
		}

		assert reference.isMedia();
		return Strings.concat(Strings.join(sourceFile.getDirPath().getPathSegments(), '-'), '-', reference.getName(), ".png");
	}

	private Document exercisePreview(String projectDirName, String pathValue) throws IOException {
		project = project(projectDirName);

		CompoPath path = new CompoPath(project, pathValue);
		project.previewScriptFiles();

		Component component = new Component(path.getLayoutPath(), this);
		component.scan(true);
		Preview preview = new Preview(component);
		StringWriter writer = new StringWriter();
		preview.serialize(writer);
		DocumentBuilder builder = new DocumentBuilderImpl();
		return builder.parseHTML(writer.toString());
	}

	private static void assertEmptyHead(Document doc) {
		EList styles = doc.findByTag("link");
		assertEquals(2, styles.size());
		assertStyle("/components-preview/res/theme/reset.css", styles, 0);
		assertStyle("/components-preview/res/theme/fx.css", styles, 1);

		assertTrue(doc.findByTag("script").isEmpty());
	}

	// ------------------------------------------------------
	// PreviewServlet

	@Test
	public void servletContextInitialization() throws Exception {
		HttpServletResponse httpResponse = exerciseServlet("styles", "/test/res/page/page.css");
		assertNotNull(project.getProjectDir());
		ArgumentCaptor<String> contentTypeCaptor=ArgumentCaptor.forClass(String.class);
		verify(httpResponse,times(1)).setContentType(contentTypeCaptor.capture());
		assertThat(contentTypeCaptor.getValue(), equalTo("text/css;charset=UTF-8"));
	}

	@Test
	public void servletBadContextInitialization() {
		try {
			exerciseServlet("fake-project", "/test/res/page/page.css");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
			assertTrue(e.getMessage().contains("is missing or is not a directory"));
		}
	}

	@Test
	public void servletLayout() throws Exception {
		exerciseServlet("project", "/test/page/index");
		DocumentBuilder builder = new DocumentBuilderImpl();
		assertIndexPage(builder.parseHTML(responseWriter.toString()));
	}

	@Test
	public void servletCompoPreview() throws Exception {
		exerciseServlet("styles", "/test/compo");
		DocumentBuilder builder = new DocumentBuilderImpl();
		Document doc = builder.parseHTML(responseWriter.toString());

		assertEquals("Styles / Preview", doc.getByTag("title").getText());

		EList styles = doc.findByTag("link");
		assertEquals(6, styles.size());

		int index = 0;
		assertStyle("/styles-preview/res/theme/reset.css", styles, index++);
		assertStyle("/styles-preview/res/theme/fx.css", styles, index++);

		// site theme styles order, beside reset and fx is not guaranteed
		assertNotNull(doc.getByXPath("//LINK[@href='/styles-preview/res/theme/form.css']"));
		assertNotNull(doc.getByXPath("//LINK[@href='/styles-preview/res/theme/styles.css']"));
		index += 2; // skip form.css and styles.css

		assertStyle("/styles-preview/res/compo/compo.css", styles, index++);
		assertStyle("/styles-preview/res/compo/preview.css", styles, index++);
	}

	@Test
	public void servletStyle() throws Throwable {
		HttpServletResponse httpResponse = exerciseServlet("styles", "/test/res/page/page.css");
		String style = responseWriter.toString();

		ArgumentCaptor<String> contentTypeCaptor=ArgumentCaptor.forClass(String.class);
		verify(httpResponse,times(1)).setContentType(contentTypeCaptor.capture());
		assertThat(contentTypeCaptor.getValue(), equalTo("text/css;charset=UTF-8"));

		assertTrue(style.startsWith("body {"));
		assertTrue(style.contains("background-color: #001122;"));
		assertTrue(style.contains("color: white;"));
		assertTrue(style.contains("width: 50%;"));
		assertTrue(style.contains("height: 80px;"));
	}

	@Test
	public void servletStyleReset() throws Throwable {
		HttpServletResponse httpResponse = exerciseServlet("styles", "/test/res/theme/reset.css");
		String style = responseWriter.toString();

		ArgumentCaptor<String> contentTypeCaptor=ArgumentCaptor.forClass(String.class);
		verify(httpResponse,times(1)).setContentType(contentTypeCaptor.capture());
		assertThat(contentTypeCaptor.getValue(), equalTo("text/css;charset=UTF-8"));

		assertTrue(style.contains("h1,h2,h3,h4,h5,h6 {"));
		assertTrue(style.contains("list-style-type: none;"));
		assertTrue(style.contains("quotes: none;"));
		assertTrue(style.contains("text-decoration: none;"));
		assertTrue(style.contains("border-collapse: collapse;"));
	}

	@Test
	public void servletScript() throws Throwable {
		exerciseServlet("strings", "/test/script/js/wood/Compo.js");
		String script = responseWriter.toString();

		assertTrue(script.startsWith("$package(\"js.wood\");"));
		assertTrue(script.contains("this.setCaption('Hello');"));
		assertTrue(script.contains("this.setName('Component Name');"));
		assertTrue(script.contains("this.setDescription('This is <strong>script</strong> <em>description</em>.');"));
	}

	@Test
	public void servletImage() throws Throwable {
		exerciseServlet("images", "/test/res/compo/logo.png");
		byte[] image = responseStream.toByteArray();
		assertEquals('P', image[1]);
		assertEquals('N', image[2]);
		assertEquals('G', image[3]);
	}

	@Test
	public void servletImageVariant() throws Throwable {
		exerciseServlet("images-variant", "/test/res/asset/logo.png");
		byte[] image = responseStream.toByteArray();
		assertEquals('P', image[1]);
		assertEquals('N', image[2]);
		assertEquals('G', image[3]);
	}

	@Test
	public void servletMissingImage() throws Exception {
		boolean exception = false;
		try {
			exerciseServlet("images", "/test/res/compo/fake.png");
		} catch (ServletException e) {
			if (e.getCause().getMessage().contains("Attempt to use not existing file path")) {
				exception = true;
			}
		}
		assertTrue(exception);
	}

	@Test
	public void servletOnResourceReference() {
		project = project("project");
		PreviewServlet servlet = new PreviewServlet();
		field(servlet, "project", project);

		FilePath source = new FilePath(project, "res/page/index/index.htm");
		Reference reference = new Reference(source, ResourceType.STRING, "title");
		assertEquals("Index Page", servlet.onResourceReference(reference, source));

		reference = new Reference(source, ResourceType.IMAGE, "logo");
		source = new FilePath(project, "res/template/page/page.htm");
		assertEquals("/project-preview/res/template/page/logo.jpg", servlet.onResourceReference(reference, source));
	}

	@Test
	public void servletBadVariableOnResourceReference() {
		project = project("project");
		PreviewServlet servlet = new PreviewServlet();
		field(servlet, "project", project);
		FilePath source = new FilePath(project, "res/page/index/index.htm");
		Reference reference = new Reference(source, ResourceType.STRING, "fake-variable");

		try {
			servlet.onResourceReference(reference, source);
			fail("Missing variables value should rise exception.");
		} catch (WoodException e) {
			assertTrue(e.getMessage().equals("Missing variables |@string/fake-variable| referenced from |res/page/index/index.htm|."));
		}
	}

	@Test
	public void servletBadMediaOnResourceReference() {
		project = project("project");
		PreviewServlet servlet = new PreviewServlet();
		field(servlet, "project", project);

		FilePath source = new FilePath(project, "res/template/page/page.htm");
		Reference reference = new Reference(source, ResourceType.IMAGE, "fake-media");

		try {
			servlet.onResourceReference(reference, source);
			fail("Missing media file should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("Missing media file"));
		}
	}

	private HttpServletResponse exerciseServlet(String projectDirName, String requestURI, String... headers) throws Exception {
		ServletContext context = mock(ServletContext.class);
		when(context.getInitParameter("PROJECT_DIR")).thenReturn(path(projectDirName));
		
		ServletConfig config = mock(ServletConfig.class);
		when(config.getServletContext()).thenReturn(context);

		PreviewServlet servlet = new PreviewServlet();
		servlet.init(config);

		project = Classes.getFieldValue(servlet, "project");

		HttpServletRequest httpRequest = mock(HttpServletRequest.class);
		when(httpRequest.getContextPath()).thenReturn("/test");
		when(httpRequest.getRequestURI()).thenReturn(requestURI);
		for (int i = 0; i < headers.length; i += 2) {
			// TestCase.assertTrue(headers.length > i + 1);
			// httpRequest.headers.put(headers[i], headers[i + 1]);
		}

		HttpServletResponse httpResponse = mock(HttpServletResponse.class);
		when(httpResponse.getWriter()).thenReturn(new PrintWriter(responseWriter));
		when(httpResponse.getOutputStream()).thenReturn(new ServletOutputStream() {
			@Override
			public void write(int b) throws IOException {
				responseStream.write(b);
			}

			@Override
			public boolean isReady() {
				return true;
			}

			@Override
			public void setWriteListener(WriteListener writeListener) {
			}
		});

		invoke(field(servlet, "variablesCache"), "update", project);
		servlet.service(httpRequest, httpResponse);
		return httpResponse;
	}

	// ------------------------------------------------------
	// Commons helpers

	private static void assertIndexPage(Document doc) {
		assertEquals("Index Page", doc.getByTag("title").getText());

		EList styles = doc.findByTag("link");
		assertEquals(12, styles.size());

		int index = 0;
		assertStyle("http://fonts.googleapis.com/css?family=Roboto", styles, index++);
		assertStyle("http://fonts.googleapis.com/css?family=Great+Vibes", styles, index++);
		assertStyle("/project-preview/res/theme/reset.css", styles, index++);
		assertStyle("/project-preview/res/theme/fx.css", styles, index++);

		// site styles order, beside reset and fx is not guaranteed
		assertNotNull(doc.getByXPath("//LINK[@href='/project-preview/res/theme/form.css']"));
		assertNotNull(doc.getByXPath("//LINK[@href='/project-preview/res/theme/style.css']"));

		index += 2; // skip form.css and styles.css
		assertStyle("/project-preview/res/template/dialog/dialog.css", styles, index++);
		assertStyle("/project-preview/lib/paging/paging.css", styles, index++);
		assertStyle("/project-preview/lib/list-view/list-view.css", styles, index++);
		assertStyle("/project-preview/res/template/page/page.css", styles, index++);
		assertStyle("/project-preview/res/template/sidebar-page/sidebar-page.css", styles, index++);
		assertStyle("/project-preview/res/page/index/index.css", styles, index++);

		EList elist = doc.findByTag("script");
		List<String> scripts = new ArrayList<>();
		// first is analytics inline code
		for (int i = 0; i < elist.size(); ++i) {
			scripts.add(elist.item(i).getAttr("src"));
		}
		assertEquals(10, scripts.size());

		doc.dump();

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
}
