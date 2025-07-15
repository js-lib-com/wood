package com.jslib.wood.preview;

import com.jslib.wood.WoodException;
import com.jslib.wood.util.StringsUtil;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.File;
import java.io.IOException;
import java.util.List;

/**
 * Forward filter for server services. For a given WOOD project there are two contexts deployed on web application container:
 * build and preview contexts. First is the complete web application able to run outside development environment. Beside web
 * user interface it can have server side logic for server services. On the other hand, preview context delivers to browser
 * files directly from project file system, taking care to create component preview on the fly. Preview context does not
 * implement server side logic but forward server services requests to build context.
 * <p>
 * This filter is just for that: intercept server services requests on preview context and forward them to build context.
 * Forward filter is necessary only if web application implements server services. Anyway, if is used need to be declared on
 * preview context <code>web.xml</code>.
 *
 * <pre>
 * 	&lt;filter&gt;
 * 		&lt;filter-name&gt;rmi-forward&lt;/filter-name&gt;
 * 		&lt;filter-class&gt;js.wood.ForwardFilter&lt;/filter-class&gt;
 * 		&lt;init-param&gt;
 * 			&lt;param-name&gt;URL_PATTERNS&lt;/param-name&gt;
 * 			&lt;param-value&gt;*.rmi,*.xsp,*{@literal /}captcha/image*,*{@literal /}rest/*&lt;/param-value&gt;
 * 		&lt;/init-param&gt;
 * 	&lt;/filter&gt;
 * 	&lt;filter-mapping&gt;
 * 		&lt;filter-name&gt;rmi-forward&lt;/filter-name&gt;
 * 		&lt;url-pattern&gt;/*&lt;/url-pattern&gt;
 * 	&lt;/filter-mapping&gt;
 * </pre>
 * <p>
 * Forward filter need to know URL patterns for server services requests. For that it can use standard
 * <code>filter-mapping</code>. Unfortunately filter mapping syntax is pretty limited, e.g. URL requests containing certain path
 * segments. To overcome this limitation one can configure filter mapping to accept all and add optional filter
 * <code>init-param</code>, as in code snippet above.
 * <p>
 * By convention preview context path is the same as build context path plus <code>-preview</code> suffix. For example, if build
 * context is named <code>site</code>, preview context path should be <code>site-preview</code>.
 * <p>
 * Finally, by default web application containers disable cross context requests dispatching. To enable it, need to add
 * <code>crossContext="true"</code> to preview context configuration.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
public class ForwardFilter implements Filter {
    private static final Logger log = LoggerFactory.getLogger(ForwardFilter.class);

    /**
     * Optional filter initialization parameter for URL patterns.
     *
     * <pre>
     * &lt;init-param&gt;
     * 		&lt;param-name&gt;URL_PATTERNS&lt;/param-name&gt;
     * 		&lt;param-value&gt;*.rmi,*.xsp,*{@literal /}captcha/image*,*{@literal /}rest/*&lt;/param-value&gt;
     * 	&lt;/init-param&gt;
     * </pre>
     */
    private static final String URL_PATTERNS = "URL_PATTERNS";

    /**
     * Cache Servlet context reference since it is not changed on this class life cycle.
     */
    private ServletContext servletContext;

    /**
     * Cache for preview context path used to extract request path from request URI. Starts with path separator, e.g.
     * <code>/test-preview</code>.
     */
    private String previewContextPath;

    /**
     * Name of the related build context that run server services. This context name is used by forward filter to load request
     * dispatcher and execute forward.
     * <p>
     * By convention preview context path is the same as build context path plus <code>-preview</code> suffix. This way forward
     * filter is able to infer build context name knowing preview context.
     * <p>
     * Starts with path separator, e.g. <code>/test</code>.
     */
    private String buildContextName;

    /**
     * Project root directory initialized from Servlet context parameter on this filter initialization - see
     * {@link #init(FilterConfig)}.
     */
    private File projectRoot;

    /**
     * Optional matcher for request paths accepted by this filter. Patterns are loaded from filter initialization parameter, see
     * {@link #URL_PATTERNS}. If filter initialization parameter is not configured this matcher is empty and accept all. In this
     * case <code>filter-mapping</code> is used.
     */
    private final Matchers requestPathMatcher;

    /**
     * Default constructor mandated by Servlet container.
     */
    public ForwardFilter() {
        log.trace("ForwardFilter()");
        this.requestPathMatcher = new Matchers();
    }

    /**
     * Filter instance initialization. This hook is invoked by Servlet container on before {@link PreviewServlet} instance
     * creation.
     */
    @Override
    public void init(FilterConfig config) {
        log.trace("init(FilterConfig config)");
        servletContext = config.getServletContext();
        log.trace("Initialize filter {}#{}", servletContext.getServletContextName(), config.getFilterName());
        previewContextPath = servletContext.getContextPath();

        // by convention preview context has suffix -preview
        int suffixSeparatorPosition = previewContextPath.lastIndexOf('-');
        buildContextName = previewContextPath.substring(0, suffixSeparatorPosition);
        projectRoot = new File(servletContext.getInitParameter(PreviewServlet.PROJECT_DIR_PARAM));

        String urlPatterns = config.getInitParameter(URL_PATTERNS);
        if (urlPatterns != null) {
            log.debug("Forward filter URL patterns {}", urlPatterns);
            requestPathMatcher.addPattern(urlPatterns.split(","));
        }
    }

    /**
     * Redirect to build context if request path is accepted by {@link #requestPathMatcher}. Otherwise, allow request to reach
     * {@link PreviewServlet}.
     */
    @Override
    public void doFilter(ServletRequest request, ServletResponse response, FilterChain chain) throws IOException, ServletException {
        log.trace("doFilter(ServletRequest request, ServletResponse response, FilterChain chain)");
        HttpServletRequest httpRequest = (HttpServletRequest) request;
        String requestPath = httpRequest.getRequestURI().substring(previewContextPath.length() + 1);

        if (!requestPathMatcher.match(requestPath)) {
            chain.doFilter(request, response);
            return;
        }

        ServletContext buildContext = servletContext.getContext(buildContextName);
        if (buildContext == null) {
            throw new WoodException("Build context %s is not deployed or preview context is not configured with crossContext='true'", buildContextName);
        }

        RequestDispatcher dispatcher = buildContext.getRequestDispatcher(forwardPath(projectRoot, requestPath));
        dispatcher.forward(request, response);
    }

    @Override
    public void destroy() {
        log.trace("destroy()");
    }

    /**
     * Heuristic to determine forward request path used to invoke server services from build context.
     *
     * @param projectRoot project root directory,
     * @param requestPath preview request path.
     * @return forward path.
     */
    static String forwardPath(File projectRoot, String requestPath) {
        // assume we are into preview for component '/res/compos/dialogs/alert'
        // current loaded content is the last part of the path, i.e. 'alert'
        // from a component script there is an RMI request for eon.site.controller.MainController#getCategoriesSelect

        // in this case browser generated URL is like
        // http://localhost/site-preview/res/compos/dialog/eon/site/controller/MainController/getCategoriesSelect.rmi
        // note that from prefix path 'alert' is missing since is current loaded content

        // in order to discover RMI class path need to remove prefix path, after extension remove
        // for that remove from request path the paths components that are directories into project resources
        // in above example remove 'compos/dialogs/'

        // res/compos/dialogs/eon/site/controller/MainController/getCategoriesSelect.rmi
        List<String> pathParts = StringsUtil.split(requestPath, '/');
        // [ res, compos, dialogs, eon, site, controller, MainController, getCategoriesSelect.rmi]

        // remove path parts that are directories into project resources
        File resourcesPath = projectRoot;
        for (; ; ) {
            resourcesPath = new File(resourcesPath, pathParts.get(0));
            if (!resourcesPath.isDirectory()) {
                break;
            }
            pathParts.remove(0);
        }

        // [ eon, site, controller, MainController, getCategoriesSelect.rmi]
        StringBuilder builder = new StringBuilder();
        for (String pathPart : pathParts) {
            builder.append('/');
            builder.append(pathPart);
        }
        // /eon/site/controller/MainController/getCategoriesSelect.rmi
        return builder.toString();
    }

    // --------------------------------------------------------------------------------------------
    // Test support

    ForwardFilter(ServletContext servletContext, String previewContextPath, File projectRoot) {
        log.trace("ForwardFilter(ServletContext servletContext, String previewContextPath, File projectRoot)");
        this.requestPathMatcher = new Matchers();
        this.servletContext = servletContext;
        this.previewContextPath = previewContextPath;
        this.projectRoot = projectRoot;
    }

    ServletContext getServletContext() {
        return servletContext;
    }

    String getPreviewContextPath() {
        return previewContextPath;
    }

    String getBuildContextName() {
        return buildContextName;
    }

    File getProjectRoot() {
        return projectRoot;
    }

    Matchers getRequestPathMatcher() {
        return requestPathMatcher;
    }
}
