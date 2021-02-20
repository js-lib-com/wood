package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.List;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Test;
import org.mockito.ArgumentCaptor;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.EList;
import js.dom.w3c.DocumentBuilderImpl;
import js.util.Classes;
import js.wood.FilePath;
import js.wood.PreviewProject;
import js.wood.PreviewServlet;
import js.wood.Reference;
import js.wood.VariablesCache;
import js.wood.WoodException;
import js.wood.impl.ResourceType;

public class PreviewServletTest {
	private PreviewProject project;
	private StringWriter responseWriter = new StringWriter();
	private ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

	@Test
	public void contextInitialization() throws Exception {
		HttpServletResponse httpResponse = exercise("styles", "/test/res/page/page.css");
		assertNotNull(project.getProjectRoot());

		ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
		verify(httpResponse, times(1)).setContentType(contentTypeCaptor.capture());
		assertThat(contentTypeCaptor.getValue(), equalTo("text/css;charset=UTF-8"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void badContextInitialization() throws Exception {
		exercise("fake-project", "/test/res/page/page.css");
	}

	@Test
	public void layout() throws Exception {
		exercise("project", "/test/res/page/index");
		DocumentBuilder builder = new DocumentBuilderImpl();
		assertIndexPage(builder.parseHTML(responseWriter.toString()));
	}

	@Test
	public void compoPreview() throws Exception {
		exercise("styles", "/test/res/compo");
		DocumentBuilder builder = new DocumentBuilderImpl();
		Document doc = builder.parseHTML(responseWriter.toString());

		assertThat(doc.getByTag("title").getText(), equalTo("Styles / Preview"));

		EList styles = doc.findByTag("link");
		assertThat(styles.size(), equalTo(6));

		int index = 0;
		assertThat(styles.item(index++).getAttr("href"), equalTo("/test/res/theme/reset.css"));
		assertThat(styles.item(index++).getAttr("href"), equalTo("/test/res/theme/fx.css"));

		// site theme styles order, beside reset and fx is not guaranteed
		assertNotNull(doc.getByXPath("//LINK[@href='/test/res/theme/form.css']"));
		assertNotNull(doc.getByXPath("//LINK[@href='/test/res/theme/styles.css']"));
		index += 2; // skip form.css and styles.css

		assertThat(styles.item(index++).getAttr("href"), equalTo("/test/res/compo/compo.css"));
		assertThat(styles.item(index++).getAttr("href"), equalTo("/test/res/compo/preview.css"));
	}

	@Test
	public void style() throws Throwable {
		HttpServletResponse httpResponse = exercise("styles", "/test/res/page/page.css");
		String style = responseWriter.toString();

		ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
		verify(httpResponse, times(1)).setContentType(contentTypeCaptor.capture());
		assertThat(contentTypeCaptor.getValue(), equalTo("text/css;charset=UTF-8"));

		assertTrue(style.startsWith("body {"));
	}

	@Test
	public void styleReset() throws Throwable {
		HttpServletResponse httpResponse = exercise("styles", "/test/res/theme/reset.css");
		String style = responseWriter.toString();

		ArgumentCaptor<String> contentTypeCaptor = ArgumentCaptor.forClass(String.class);
		verify(httpResponse, times(1)).setContentType(contentTypeCaptor.capture());
		assertThat(contentTypeCaptor.getValue(), equalTo("text/css;charset=UTF-8"));

		assertTrue(style.contains("h1,h2,h3,h4,h5,h6 {"));
		assertTrue(style.contains("list-style-type: none;"));
		assertTrue(style.contains("quotes: none;"));
		assertTrue(style.contains("text-decoration: none;"));
		assertTrue(style.contains("border-collapse: collapse;"));
	}

	@Test
	public void script() throws Throwable {
		exercise("strings", "/test/script/js/wood/Compo.js");
		String script = responseWriter.toString();

		assertTrue(script.startsWith("$package(\"js.wood\");"));
		assertTrue(script.contains("this.setCaption('Hello');"));
		assertTrue(script.contains("this.setName('Component Name');"));
		assertTrue(script.contains("this.setDescription('This is <strong>script</strong> <em>description</em>.');"));
	}

	@Test
	public void image() throws Throwable {
		exercise("images", "/test/res/compo/logo.png");
		byte[] image = responseStream.toByteArray();
		assertThat(image[1], equalTo((byte) 'P'));
		assertThat(image[2], equalTo((byte) 'N'));
		assertThat(image[3], equalTo((byte) 'G'));
	}

	@Test
	public void imageVariant() throws Throwable {
		exercise("images-variant", "/test/res/asset/logo_ro.png");
		byte[] image = responseStream.toByteArray();
		assertThat(image[1], equalTo((byte) 'P'));
		assertThat(image[2], equalTo((byte) 'N'));
		assertThat(image[3], equalTo((byte) 'G'));
	}

	@Test(expected = ServletException.class)
	public void missingImage() throws Exception {
		exercise("images", "/test/res/compo/fake.png");
	}

	@Test
	public void resourceReference() {
		project = new PreviewProject(new File("src/test/resources/project"));
		VariablesCache variables = new VariablesCache(project);

		PreviewServlet servlet = new PreviewServlet();
		Classes.setFieldValue(servlet, "project", project);
		Classes.setFieldValue(servlet, "variables", variables);

		FilePath source = new FilePath(project, "res/page/index/index.htm");
		Reference reference = new Reference(source, ResourceType.STRING, "title");
		assertThat(servlet.onResourceReference(reference, source), equalTo("Index Page"));

		reference = new Reference(source, ResourceType.IMAGE, "logo");
		source = new FilePath(project, "res/template/page/page.htm");
		assertThat(servlet.onResourceReference(reference, source), equalTo("/test/res/template/page/logo.jpg"));
	}

	@Test
	public void badVariableOnResourceReference() {
		project = new PreviewProject(new File("src/test/resources/project"));
		VariablesCache variables = new VariablesCache(project);

		PreviewServlet servlet = new PreviewServlet();
		Classes.setFieldValue(servlet, "project", project);
		Classes.setFieldValue(servlet, "variables", variables);

		FilePath source = new FilePath(project, "res/page/index/index.htm");
		Reference reference = new Reference(source, ResourceType.STRING, "fake-variable");
		assertThat(servlet.onResourceReference(reference, source), equalTo(reference.toString()));
	}

	@Test(expected = WoodException.class)
	public void servletBadMediaOnResourceReference() {
		project = new PreviewProject(new File("src/test/resources/project"));
		PreviewServlet servlet = new PreviewServlet();
		Classes.setFieldValue(servlet, "project", project);

		FilePath source = new FilePath(project, "res/template/page/page.htm");
		Reference reference = new Reference(source, ResourceType.IMAGE, "fake-media");
		servlet.onResourceReference(reference, source);
	}

	private HttpServletResponse exercise(String projectDirName, String requestURI, String... headers) throws Exception {
		ServletContext context = mock(ServletContext.class);
		when(context.getInitParameter("PROJECT_DIR")).thenReturn("src/test/resources/" + projectDirName);

		ServletConfig config = mock(ServletConfig.class);
		when(config.getServletContext()).thenReturn(context);

		PreviewServlet servlet = new PreviewServlet();
		servlet.init(config);

		project = servlet.getProject();

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

		servlet.getVariables().update();
		servlet.service(httpRequest, httpResponse);
		return httpResponse;
	}

	private static void assertIndexPage(Document doc) {
		assertThat(doc.getByTag("title").getText(), equalTo("Test Project / Index"));

		EList links = doc.findByTag("link");
		assertThat(links.size(), equalTo(12));

		int index = 0;
		assertThat(links.item(index++).getAttr("href"), equalTo("http://fonts.googleapis.com/css?family=Roboto"));
		assertThat(links.item(index++).getAttr("href"), equalTo("http://fonts.googleapis.com/css?family=Great+Vibes"));
		assertThat(links.item(index++).getAttr("href"), equalTo("/test/res/theme/reset.css"));
		assertThat(links.item(index++).getAttr("href"), equalTo("/test/res/theme/fx.css"));

		// site styles order, beside reset and fx is not guaranteed
		assertNotNull(doc.getByXPath("//LINK[@href='/test/res/theme/form.css']"));
		assertNotNull(doc.getByXPath("//LINK[@href='/test/res/theme/style.css']"));

		index += 2; // skip form.css and styles.css
		assertThat(links.item(index++).getAttr("href"), equalTo("/test/res/template/dialog/dialog.css"));
		assertThat(links.item(index++).getAttr("href"), equalTo("/test/lib/paging/paging.css"));
		assertThat(links.item(index++).getAttr("href"), equalTo("/test/lib/list-view/list-view.css"));
		assertThat(links.item(index++).getAttr("href"), equalTo("/test/res/template/page/page.css"));
		assertThat(links.item(index++).getAttr("href"), equalTo("/test/res/template/sidebar-page/sidebar-page.css"));
		assertThat(links.item(index++).getAttr("href"), equalTo("/test/res/page/index/index.css"));

		EList elist = doc.findByTag("script");
		List<String> scripts = new ArrayList<>();
		// first is analytics inline code
		for (int i = 0; i < elist.size(); ++i) {
			scripts.add(elist.item(i).getAttr("src"));
		}
		assertThat(scripts, hasSize(10));

		assertTrue(scripts.indexOf("/test/script/hc/page/Index.js") > scripts.indexOf("/test/lib/js-lib/js-lib.js"));
		assertTrue(scripts.indexOf("/test/script/hc/view/DiscographyView.js") > scripts.indexOf("/test/lib/js-lib/js-lib.js"));
		assertTrue(scripts.indexOf("/test/script/hc/view/DiscographyView.js") > scripts.indexOf("/test/script/hc/view/VideoPlayer.js"));
		assertTrue(scripts.indexOf("/test/script/hc/view/VideoPlayer.js") > scripts.indexOf("/test/lib/js-lib/js-lib.js"));
		assertTrue(scripts.indexOf("/test/script/hc/view/VideoPlayer.js") > scripts.indexOf("/test/script/js.compo.Dialog.js"));
		assertTrue(scripts.indexOf("/test/script/js/compo/Dialog.js") > scripts.indexOf("/test/lib/js-lib/js-lib.js"));

		assertTrue(scripts.contains("/test/lib/js-lib/js-lib.js"));
		assertTrue(scripts.contains("/test/script/hc/page/Index.js"));
		assertTrue(scripts.contains("/test/gen/js/controller/MainController.js"));
		assertTrue(scripts.contains("/test/script/js/hood/MainMenu.js"));
		assertTrue(scripts.contains("/test/script/hc/view/VideoPlayer.js"));
		assertTrue(scripts.contains("/test/lib/list-view/list-view.js"));
		assertTrue(scripts.contains("/test/lib/paging/paging.js"));
		assertTrue(scripts.contains("/test/script/js/compo/Dialog.js"));
		assertTrue(scripts.contains("/test/script/js/hood/TopMenu.js"));
		assertTrue(scripts.contains("/test/script/hc/view/DiscographyView.js"));
	}
}
