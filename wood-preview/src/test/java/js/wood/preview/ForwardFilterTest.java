package js.wood.preview;

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

import jakarta.servlet.FilterChain;
import jakarta.servlet.FilterConfig;
import jakarta.servlet.RequestDispatcher;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.ServletResponse;
import jakarta.servlet.http.HttpServletRequest;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.WoodException;

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

	private File projectRoot;

	@Before
	public void beforeTest() {
		projectRoot = new File("src/test/resources/forward-filter");
	}

	@Test
	public void GivenInitParameter_WhenInit_ThenSetInternalState() throws ServletException {
		// given
		FilterConfig config = mock(FilterConfig.class);
		when(config.getServletContext()).thenReturn(servletContext);

		when(servletContext.getContextPath()).thenReturn("/test-preview");
		when(servletContext.getInitParameter("PROJECT_DIR")).thenReturn(".");

		// when
		ForwardFilter filter = new ForwardFilter();
		filter.init(config);

		// then
		assertThat(filter.getServletContext(), equalTo(servletContext));
		assertThat(filter.getPreviewContextPath(), equalTo("/test-preview"));
		assertThat(filter.getBuildContextName(), equalTo("/test"));
		assertThat(filter.getProjectRoot(), notNullValue());
		assertThat(filter.getProjectRoot(), equalTo(new File(".")));
		assertThat(filter.getRequestPathMatcher(), notNullValue());
	}

	@Test
	public void GivenUrlPatternsInitParameter_WhenInit_ThenRequestPathMatches() throws ServletException {
		// given
		FilterConfig config = mock(FilterConfig.class);
		when(config.getServletContext()).thenReturn(servletContext);
		when(config.getInitParameter("URL_PATTERNS")).thenReturn("*.rmi");

		when(servletContext.getContextPath()).thenReturn("/test-preview");
		when(servletContext.getInitParameter("PROJECT_DIR")).thenReturn(".");

		// when
		ForwardFilter filter = new ForwardFilter();
		filter.init(config);

		// then
		Matchers requestPathMatcher = filter.getRequestPathMatcher();
		assertTrue(requestPathMatcher.match("site/Controller/test.rmi"));
	}

	@Test
	public void GivenRequestUriMatchPattern_WhenDoFilter_ThenForwardToBuildContext() throws IOException, ServletException {
		// given
		when(request.getRequestURI()).thenReturn("/test-preview/res/compos/dialogs/sixqs/site/controller/MainController/getCategoriesSelect.rmi");

		ServletContext buildContext = mock(ServletContext.class);
		when(servletContext.getContext(null)).thenReturn(buildContext);

		RequestDispatcher dispatcher = mock(RequestDispatcher.class);
		when(buildContext.getRequestDispatcher("/sixqs/site/controller/MainController/getCategoriesSelect.rmi")).thenReturn(dispatcher);

		ForwardFilter filter = new ForwardFilter(servletContext, "/test-preview", projectRoot);
		filter.getRequestPathMatcher().addPattern("*.rmi");

		// when
		filter.doFilter(request, response, chain);

		// then
		verify(dispatcher, times(1)).forward(request, response);
	}

	@Test
	public void GivenRequestUriDoNotMatchPattern_WhenDoFilter_ThenChainToPreviewContext() throws IOException, ServletException {
		// given
		when(request.getRequestURI()).thenReturn("/test-preview/res/compo");

		ForwardFilter filter = new ForwardFilter(servletContext, "/test-preview", projectRoot);
		filter.getRequestPathMatcher().addPattern("*.rmi");

		// when
		filter.doFilter(request, response, chain);

		// then
		verify(chain, times(1)).doFilter(request, response);
	}

	@Test(expected = WoodException.class)
	public void GivenMissingBuildContext_WhenDoFilter_ThenWoodException() throws IOException, ServletException {
		// given
		when(request.getRequestURI()).thenReturn("/test-preview/res/compos/dialogs/sixqs/site/controller/MainController/getCategoriesSelect.rmi");
		ForwardFilter filter = new ForwardFilter(servletContext, "/test-preview", projectRoot);
		filter.getRequestPathMatcher().addPattern("*.rmi");

		// when
		filter.doFilter(request, response, chain);

		// then
	}

	@Test
	public void GivenRequestUriForBuildContext_WhenForwardPath_ThenReturnRmiMethodPath() {
		// given
		String requestURI = "res/compos/dialogs/sixqs/site/controller/MainController/getCategoriesSelect.rmi";
		
		// when
		String forwardPath = ForwardFilter.forwardPath(projectRoot, requestURI);
		
		// then
		assertThat(forwardPath, equalTo("/sixqs/site/controller/MainController/getCategoriesSelect.rmi"));
	}
}
