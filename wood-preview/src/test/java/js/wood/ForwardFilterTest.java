package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;

import javax.servlet.FilterChain;
import javax.servlet.FilterConfig;
import javax.servlet.RequestDispatcher;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServletRequest;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ForwardFilterTest {
	@Mock
	private ServletContext servletContext;
	@Mock
	private HttpServletRequest request;
	@Mock
	private ServletResponse response;
	@Mock
	private FilterChain chain;
	@Mock
	private Project project;

	@Test
	public void init() throws ServletException {
		FilterConfig config = mock(FilterConfig.class);
		when(config.getServletContext()).thenReturn(servletContext);

		when(servletContext.getContextPath()).thenReturn("/test-preview");
		when(servletContext.getInitParameter("PROJECT_DIR")).thenReturn(".");

		ForwardFilter filter = new ForwardFilter();
		filter.init(config);

		assertThat(filter.getServletContext(), equalTo(servletContext));
		assertThat(filter.getPreviewContextPath(), equalTo("/test-preview"));
		assertThat(filter.getBuildContextName(), equalTo("/test"));
		assertThat(filter.getProject(), notNullValue());
		assertThat(filter.getProject().getProjectRoot(), equalTo(new File(".")));
		assertThat(filter.getRequestPathMatcher(), notNullValue());
	}

	@Test
	public void init_UrlPatterns() throws ServletException {
		FilterConfig config = mock(FilterConfig.class);
		when(config.getServletContext()).thenReturn(servletContext);
		when(config.getInitParameter("URL_PATTERNS")).thenReturn("*.rmi");

		when(servletContext.getContextPath()).thenReturn("/test-preview");
		when(servletContext.getInitParameter("PROJECT_DIR")).thenReturn(".");

		ForwardFilter filter = new ForwardFilter();
		filter.init(config);

		Matchers requestPathMatcher = filter.getRequestPathMatcher();
		assertTrue(requestPathMatcher.match("site/Controller/test.rmi"));
	}

	@Test
	public void doFilter_Forward() throws IOException, ServletException {
		when(request.getRequestURI()).thenReturn("/test-preview/res/compo");

		ServletContext buildContext = mock(ServletContext.class);
		when(servletContext.getContext(null)).thenReturn(buildContext);

		RequestDispatcher dispatcher = mock(RequestDispatcher.class);
		when(buildContext.getRequestDispatcher("/res/compo")).thenReturn(dispatcher);

		ForwardFilter filter = new ForwardFilter(servletContext, "/test-preview", project);
		filter.doFilter(request, response, chain);
		verify(dispatcher, times(1)).forward(request, response);
	}

	@Test
	public void doFilter_Chain() throws IOException, ServletException {
		when(request.getRequestURI()).thenReturn("/test-preview/res/compo");
		
		ForwardFilter filter = new ForwardFilter(servletContext, "/test-preview", project);
		filter.getRequestPathMatcher().addPattern("*.rmi");
		
		filter.doFilter(request, response, chain);
		verify(chain, times(1)).doFilter(request, response);
	}

	@Test(expected = WoodException.class)
	public void doFilter_MissingBuildContext() throws IOException, ServletException {
		when(request.getRequestURI()).thenReturn("/test-preview/res/compo");
		ForwardFilter filter = new ForwardFilter(servletContext, "/test-preview", project);
		filter.doFilter(request, response, chain);
	}

	@Test
	public void forwardPath() {
		when(project.getProjectRoot()).thenReturn(new File("src/test/resources/forward-filter"));
		String forwardPath = ForwardFilter.forwardPath(project, "res/compos/dialogs/sixqs/site/controller/MainController/getCategoriesSelect.rmi");
		assertThat(forwardPath, equalTo("/sixqs/site/controller/MainController/getCategoriesSelect.rmi"));
	}
}
