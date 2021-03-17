package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintWriter;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Locale;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.rmi.BusinessException;
import js.util.Classes;
import js.util.Strings;
import js.wood.impl.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class PreviewServletTest {
	@Mock
	private Project project;
	@Mock
	private Factory factory;
	@Mock
	private VariablesCache variables;
	@Mock
	private IReferenceHandler referenceHandler;

	@Mock
	private ServletConfig servletConfig;
	@Mock
	private ServletContext servletContext;
	@Mock
	private HttpServletRequest httpRequest;
	@Mock
	private HttpServletResponse httpResponse;

	private PreviewServlet servlet;
	private StringWriter responseWriter = new StringWriter();
	private ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
	private String responseContentType;
	private int responseStatus;

	@Before
	public void beforeTest() throws IOException, ServletException {
		when(project.getFactory()).thenReturn(factory);
		when(project.getDefaultLocale()).thenReturn(Locale.ENGLISH);
		when(project.getThemeStyles()).thenReturn(mock(ThemeStyles.class));

		when(servletContext.getContextPath()).thenReturn("/test-preview");

		// when(httpRequest.getContextPath()).thenReturn("/test-preview");
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

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				responseStatus = invocation.getArgument(0);
				return null;
			}
		}).when(httpResponse).setStatus(anyInt());

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				responseContentType = invocation.getArgument(0);
				return null;
			}
		}).when(httpResponse).setContentType(anyString());

		servlet = new PreviewServlet(servletContext, project, variables);
	}

	@Test
	public void init() throws Exception {
		when(servletConfig.getServletContext()).thenReturn(servletContext);
		when(servletContext.getInitParameter("PROJECT_DIR")).thenReturn("/absolute/path/to/project");

		servlet.init(servletConfig);

		assertThat(servlet.getProject(), notNullValue());
		assertThat(servlet.getFactory(), notNullValue());
		assertThat(servlet.getVariables(), notNullValue());
	}

	@Test
	public void service_Component() throws ServletException, IOException {
		when(httpRequest.getServletContext()).thenReturn(servletContext);
		when(httpRequest.getRequestURI()).thenReturn("/test-preview/res/compo");

		FilePath layoutPreview = mock(FilePath.class);
		when(layoutPreview.exists()).thenReturn(true);

		CompoPath compoPath = mock(CompoPath.class);
		when(factory.createCompoPath("res/compo")).thenReturn(compoPath);
		when(compoPath.getFilePath("preview.htm")).thenReturn(layoutPreview);

		Component compo = mock(Component.class);
		when(factory.createComponent(any(FilePath.class), any(IReferenceHandler.class))).thenReturn(compo);

		DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
		when(compo.getLayout()).thenReturn(documentBuilder.parseXML("<body><h1>Test Compo</h1></body>").getRoot());

		servlet.service(httpRequest, httpResponse);
		assertThat(responseStatus, equalTo(200));
		assertThat(responseContentType, nullValue());

		Document document = documentBuilder.parseXML(responseWriter.toString());
		assertThat(document.getRoot().getTag(), equalTo("html"));
		assertThat(document.getByTag("H1").getText(), equalTo("Test Compo"));
	}

	@Test
	public void service_Style() throws ServletException, IOException {
		when(httpRequest.getRequestURI()).thenReturn("/test-preview/res/compo/compo.css");

		FilePath stylePath = mock(FilePath.class);
		when(factory.createFilePath("res/compo/compo.css")).thenReturn(stylePath);
		when(stylePath.exists()).thenReturn(true);
		when(stylePath.isStyle()).thenReturn(true);
		when(stylePath.getMimeType()).thenReturn("text/css;charset=UTF-8");
		when(stylePath.getReader()).thenReturn(new StringReader("body { width: 1200px; }"));
		when(stylePath.getParentDirPath()).thenReturn(mock(DirPath.class));

		servlet.service(httpRequest, httpResponse);
		assertThat(responseWriter.toString(), equalTo("body { width: 1200px; }"));
		assertThat(responseStatus, equalTo(200));
		assertThat(responseContentType, equalTo("text/css;charset=UTF-8"));
	}

	@Test
	public void service_Style_Variants() throws ServletException, IOException {
		when(httpRequest.getRequestURI()).thenReturn("/test-preview/res/compo/compo_w800.css");

		FilePath stylePath = mock(FilePath.class);
		when(factory.createFilePath("res/compo/compo_w800.css")).thenReturn(stylePath);
		when(stylePath.isStyle()).thenReturn(true);
		when(stylePath.hasVariants()).thenReturn(true);

		servlet.service(httpRequest, httpResponse);
		assertThat(responseContentType, nullValue());
		verify(stylePath, times(0)).copyTo(any(OutputStream.class));
	}

	@Test
	public void service_Script() throws ServletException, IOException {
		when(httpRequest.getRequestURI()).thenReturn("/test-preview/lib/js-lib.js");

		FilePath scriptPath = mock(FilePath.class);
		when(scriptPath.exists()).thenReturn(true);
		when(scriptPath.isScript()).thenReturn(true);
		when(scriptPath.getMimeType()).thenReturn("application/javascript;charset=UTF-8");
		when(scriptPath.getReader()).thenReturn(new StringReader("alert('test');"));
		when(factory.createFilePath("lib/js-lib.js")).thenReturn(scriptPath);

		servlet.service(httpRequest, httpResponse);
		assertThat(responseWriter.toString(), equalTo("alert('test');"));
		assertThat(responseStatus, equalTo(200));
		assertThat(responseContentType, equalTo("application/javascript;charset=UTF-8"));
	}

	@Test
	public void service_Media() throws ServletException, IOException {
		when(httpRequest.getRequestURI()).thenReturn("/test-preview/res/asset/logo.png");

		FilePath mediaPath = mock(FilePath.class);
		when(mediaPath.getMimeType()).thenReturn("image/png");
		when(factory.createFilePath("res/asset/logo.png")).thenReturn(mediaPath);

		doAnswer(new Answer<Void>() {
			@Override
			public Void answer(InvocationOnMock invocation) throws Throwable {
				OutputStream stream = invocation.getArgument(0);
				Strings.save("PNG", stream);
				return null;
			}
		}).when(mediaPath).copyTo(any(OutputStream.class));

		servlet.service(httpRequest, httpResponse);
		assertThat(responseStatus, equalTo(200));
		assertThat(responseContentType, equalTo("image/png"));
		assertThat(new String(responseStream.toByteArray()), equalTo("PNG"));
	}

	@Ignore
	@Test
	public void service_RMI() throws ServletException, IOException {
		when(httpRequest.getRequestURI()).thenReturn("/test-preview/com/kidscademy/Controller/page.rmi");

		RequestDispatcher dispatcher = mock(RequestDispatcher.class);
		ServletContext appcontext = mock(ServletContext.class);
		when(appcontext.getRequestDispatcher("/com/kidscademy/Controller/page.rmi")).thenReturn(dispatcher);
		when(servletContext.getContext("/test")).thenReturn(appcontext);

		servlet.service(httpRequest, httpResponse);
		verify(dispatcher, times(1)).forward(httpRequest, httpResponse);
	}

	@Test(expected = ServletException.class)
	public void service_RMI_MissingContext() throws ServletException, IOException {
		when(httpRequest.getRequestURI()).thenReturn("/test-preview/com/kidscademy/Controller/page.rmi");
		servlet.service(httpRequest, httpResponse);
	}

	@Test(expected = ServletException.class)
	public void service_BadExtension() throws ServletException, IOException {
		when(httpRequest.getRequestURI()).thenReturn("/test-preview/res/compo/compo.xxx");

		FilePath filePath = mock(FilePath.class);
		when(filePath.getMimeType()).thenThrow(WoodException.class);
		when(factory.createFilePath("res/compo/compo.xxx")).thenReturn(filePath);

		servlet.service(httpRequest, httpResponse);
	}

	@Test
	public void service_BusinessException() throws ServletException, IOException {
		when(httpRequest.getRequestURI()).thenThrow(new RuntimeException(new BusinessException()));

		servlet.service(httpRequest, httpResponse);
		assertThat(responseStatus, equalTo(400));
		assertThat(responseContentType, equalTo("application/json"));
	}

	@Test
	public void onResourceReference_Variable() {
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = mock(FilePath.class);
		when(variables.get(Locale.ENGLISH, reference, source, servlet)).thenReturn("Compo Title");
		assertThat(servlet.onResourceReference(reference, source), equalTo("Compo Title"));
	}

	@Test
	public void onResourceReference_Variable_NotDefined() {
		Reference reference = new Reference(ResourceType.STRING, "title");
		FilePath source = mock(FilePath.class);
		assertThat(servlet.onResourceReference(reference, source), equalTo("@string/title"));
	}

	@Test
	public void onResourceReference_Media() {
		Reference reference = new Reference(ResourceType.IMAGE, "res/asset/logo.png");
		FilePath source = mock(FilePath.class);

		DirPath mediaDir = mock(DirPath.class);
		when(mediaDir.value()).thenReturn("res/asset/");

		FilePath media = mock(FilePath.class);
		when(media.getParentDirPath()).thenReturn(mediaDir);
		when(media.getName()).thenReturn("logo.png");
		when(project.getMediaFile(Locale.ENGLISH, reference, source)).thenReturn(media);

		assertThat(servlet.onResourceReference(reference, source), equalTo("/test-preview/res/asset/logo.png"));
	}

	@Test(expected = WoodException.class)
	public void onResourceReference_Media_Missing() {
		Reference reference = new Reference(ResourceType.IMAGE, "res/asset/logo.png");
		FilePath source = mock(FilePath.class);
		servlet.onResourceReference(reference, source);
	}
}
