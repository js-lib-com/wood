package com.jslib.wood.preview;

import com.jslib.wood.*;
import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.DocumentBuilder;
import com.jslib.wood.util.StringsUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;

import java.io.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class PreviewServletTest {
    @Mock
    private Project project;
    @Mock
    private VariablesCache variables;

    @Mock
    private ServletConfig servletConfig;
    @Mock
    private ServletContext servletContext;
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;

    private PreviewServlet servlet;
    private final StringWriter responseWriter = new StringWriter();
    private final ByteArrayOutputStream responseStream = new ByteArrayOutputStream();
    private String responseContentType;
    private int responseStatus;

    @Before
    public void beforeTest() throws IOException {
        when(project.getDefaultLanguage()).thenReturn("en");
        when(project.getThemeStyles()).thenReturn(mock(ThemeStyles.class));

        when(servletContext.getContextPath()).thenReturn("/test-preview");

        // when(httpRequest.getContextPath()).thenReturn("/test-preview");
        when(httpResponse.getWriter()).thenReturn(new PrintWriter(responseWriter));

        when(httpResponse.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public void write(int b) {
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

        doAnswer((Answer<Void>) invocation -> {
            responseStatus = invocation.getArgument(0);
            return null;
        }).when(httpResponse).setStatus(anyInt());

        doAnswer((Answer<Void>) invocation -> {
            responseContentType = invocation.getArgument(0);
            return null;
        }).when(httpResponse).setContentType(anyString());

        servlet = new PreviewServlet(servletContext, project, variables);
    }

    @Test
    public void init() throws Exception {
        when(servletConfig.getServletContext()).thenReturn(servletContext);
        when(servletContext.getInitParameter("PROJECT_DIR")).thenReturn("src/test/resources/project");

        servlet.init(servletConfig);

        assertThat(servlet.getProject(), notNullValue());
        assertThat(servlet.getVariables(), notNullValue());
    }

    @Test
    public void service_Component() throws ServletException, SAXException {
        when(httpRequest.getServletContext()).thenReturn(servletContext);
        when(httpRequest.getRequestURI()).thenReturn("/test-preview/res/compo");

        FilePath layoutPath = mock(FilePath.class);
        when(layoutPath.exists()).thenReturn(true);

        CompoPath compoPath = mock(CompoPath.class);
        when(project.createCompoPath("res/compo")).thenReturn(compoPath);
        when(compoPath.getLayoutPath()).thenReturn(layoutPath);
        when(compoPath.getFilePath("preview.htm")).thenReturn(layoutPath);

        Component compo = mock(Component.class);
        when(project.createComponent(any(FilePath.class), any(IReferenceHandler.class))).thenReturn(compo);

        DocumentBuilder documentBuilder = DocumentBuilder.getInstance();
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
        when(project.createFilePath("res/compo/compo.css")).thenReturn(stylePath);
        when(stylePath.exists()).thenReturn(true);
        when(stylePath.isStyle()).thenReturn(true);
        when(stylePath.getMimeType()).thenReturn("text/css;charset=UTF-8");
        when(stylePath.getReader()).thenReturn(new StringReader("body { width: 1200px; }"));
        when(stylePath.getParentDir()).thenReturn(mock(FilePath.class));

        servlet.service(httpRequest, httpResponse);
        assertThat(responseWriter.toString(), equalTo("body { width: 1200px; }"));
        assertThat(responseStatus, equalTo(200));
        assertThat(responseContentType, equalTo("text/css;charset=UTF-8"));
    }

    @Test
    public void service_Style_Variants() throws ServletException, IOException {
        when(httpRequest.getRequestURI()).thenReturn("/test-preview/res/compo/compo_w800.css");

        FilePath stylePath = mock(FilePath.class);
        when(project.createFilePath("res/compo/compo_w800.css")).thenReturn(stylePath);
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
        when(project.createFilePath("lib/js-lib.js")).thenReturn(scriptPath);

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
        when(project.createFilePath("res/asset/logo.png")).thenReturn(mediaPath);

        doAnswer((Answer<Void>) invocation -> {
            OutputStream stream = invocation.getArgument(0);
            StringsUtil.save("PNG", stream);
            return null;
        }).when(mediaPath).copyTo(any(OutputStream.class));

        servlet.service(httpRequest, httpResponse);
        assertThat(responseStatus, equalTo(200));
        assertThat(responseContentType, equalTo("image/png"));
        assertThat(responseStream.toString(), equalTo("PNG"));
    }

    @Test(expected = ServletException.class)
    public void service_RMI_MissingContext() throws ServletException {
        when(httpRequest.getRequestURI()).thenReturn("/test-preview/com/kidscademy/Controller/page.rmi");
        servlet.service(httpRequest, httpResponse);
    }

    @Test(expected = ServletException.class)
    public void service_BadExtension() throws ServletException, IOException {
        when(httpRequest.getRequestURI()).thenReturn("/test-preview/res/compo/compo.xxx");

        FilePath filePath = mock(FilePath.class);
        when(filePath.getMimeType()).thenThrow(WoodException.class);
        when(project.createFilePath("res/compo/compo.xxx")).thenReturn(filePath);

        servlet.service(httpRequest, httpResponse);
    }

    @Test
    public void onResourceReference_Variable() {
        Reference reference = new Reference(Reference.Type.STRING, "title");
        FilePath source = mock(FilePath.class);
        when(variables.get("en", reference, source, servlet)).thenReturn("Compo Title");
        assertThat(servlet.onResourceReference(reference, source), equalTo("Compo Title"));
    }

    @Test
    public void onResourceReference_Variable_NotDefined() {
        Reference reference = new Reference(Reference.Type.STRING, "title");
        FilePath source = mock(FilePath.class);
        assertThat(servlet.onResourceReference(reference, source), equalTo("@string/title"));
    }

    @Test
    public void onResourceReference_Media() {
        Reference reference = new Reference(Reference.Type.IMAGE, "res/asset/logo.png");
        FilePath source = mock(FilePath.class);

        FilePath mediaDir = mock(FilePath.class);
        when(mediaDir.value()).thenReturn("res/asset/");

        FilePath media = mock(FilePath.class);
        when(media.getParentDir()).thenReturn(mediaDir);
        when(media.getName()).thenReturn("logo.png");
        when(project.getResourceFile("en", reference, source)).thenReturn(media);

        assertThat(servlet.onResourceReference(reference, source), equalTo("/test-preview/res/asset/logo.png"));
    }

    @Test(expected = WoodException.class)
    public void onResourceReference_Media_Missing() {
        Reference reference = new Reference(Reference.Type.IMAGE, "res/asset/logo.png");
        FilePath source = mock(FilePath.class);
        servlet.onResourceReference(reference, source);
    }
}
