package com.jslib.wood.preview;

import com.jslib.wood.*;
import com.jslib.wood.util.FilesUtil;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.slf4j.MDC;

import java.io.File;
import java.io.Reader;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Preview Servlet allows access from browser to project components and resource files. This allows to use browser for
 * components preview by simple refreshing loaded page. A file is returned as it is but component layout is aggregated and
 * preview HTML code is generated on the fly; uses {@link Preview} class for that.
 * <p>
 * In order to work, Preview Servlet should be properly declared into web.xml file:
 *
 * <pre>
 *  &lt;context-param&gt;
 *      &lt;param-name&gt;PROJECT_DIR&lt;/param-name&gt;
 *      &lt;param-value&gt;path/to/project&lt;/param-value&gt;
 *  &lt;/context-param&gt;
 *
 *  &lt;listener&gt;
 *      &lt;listener-class&gt;js.wood.PreviewServlet&lt;/listener-class&gt;
 *  &lt;/listener&gt;
 *
 *  &lt;Servlet&gt;
 *      &lt;Servlet-name&gt;development&lt;/Servlet-name&gt;
 *      &lt;Servlet-class&gt;js.wood.PreviewServlet&lt;/Servlet-class&gt;
 *      &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 *  &lt;/Servlet&gt;
 *
 *  &lt;Servlet-mapping&gt;
 *      &lt;Servlet-name&gt;development&lt;/Servlet-name&gt;
 *      &lt;url-pattern&gt;/&lt;/url-pattern&gt;
 *  &lt;/Servlet-mapping&gt;
 * </pre>
 * <p>
 * Preview WAR archive is actually empty; it contains only <code>web.xml</code> descriptor, that is, no Java classes nor static
 * content.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
public final class PreviewServlet extends HttpServlet implements IReferenceHandler {
    /**
     * Java serialization ID,
     */
    private static final long serialVersionUID = -2971057399517075118L;

    /**
     * Class logger.
     */
    private static final Logger log = LoggerFactory.getLogger(PreviewServlet.class);

    /**
     * Diagnostic context name for context path, aka application.
     */
    private static final String LOG_CONTEXT_APP = "app";
    /**
     * Diagnostic context name for remote host, aka IP address.
     */
    private static final String LOG_CONTEXT_IP = "ip";
    /**
     * Diagnostic context name for current request ID.
     */
    private static final String LOG_CONTEXT_ID = "id";

    /**
     * Request ID seed, atomically incremented on every new request. For logging.
     */
    private static final AtomicInteger REQUEST_ID = new AtomicInteger();

    /**
     * Servlet context init parameter for project directory.
     */
    public static final String PROJECT_DIR_PARAM = "PROJECT_DIR";

    /**
     * Preview layout is a special layout used for component unit test. It is returned instead of component; preview layout uses
     * component layout as child component.
     */
    private static final String LAYOUT_PREVIEW = "preview.htm";

    /**
     * Cache Servlet context reference since it is not changed on this class life cycle.
     */
    private ServletContext servletContext;

    /**
     * Cache context path used by {@link Preview} to generate URL absolute paths.
     */
    private String contextPath;

    /**
     * Project instance initialized from Servlet context parameter on Servlet initialization - see {@link #init(ServletConfig)}.
     */
    private Project project;

    /**
     * Variables cache initialized before every component preview processing.
     */
    private VariablesCache variables;

    /**
     * Default constructor mandated by Servlet container.
     */
    public PreviewServlet() {
        super();
        log.trace("PreviewServlet()");
    }

    /**
     * Servlet instance initialization. This hook is invoked by Servlet container on Preview Servlet instance creation. Since
     * this Servlet is declared <code>load-on-startup</code> this initialization occurs at application deployment.
     * <p>
     * For every deployed preview context/application, Servlet container creates exactly one instance of this Preview Servlet
     * and invoke this method once. This method takes care to create {@link #project} instance with proper root directory loaded
     * from initialization parameters - see class description for <code>web.xml</code> sample.
     */
    @Override
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        servletContext = config.getServletContext();
        log.trace("Initialize servlet {}#{}.", servletContext.getServletContextName(), config.getServletName());
        contextPath = servletContext.getContextPath();

        project = (Project) servletContext.getAttribute(Project.class.getName());
        if (project == null) {
            project = Project.create(new File(servletContext.getInitParameter(PROJECT_DIR_PARAM)));
            servletContext.setAttribute(Project.class.getName(), project);
        }
        variables = new VariablesCache(project);
    }

    /**
     * Servlet service routine just delegates {@link #doService(HttpServletRequest, HttpServletResponse)} and print exception
     * stack.
     *
     * @param httpRequest  HTTP request,
     * @param httpResponse HTTP response.
     */
    @Override
    protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException {
        // push context path and remote address of the requested processed by this thread to logger diagnostic context
        MDC.put(LOG_CONTEXT_APP, contextPath.isEmpty() ? "root" : contextPath.substring(1));
        MDC.put(LOG_CONTEXT_IP, httpRequest.getRemoteHost());
        MDC.put(LOG_CONTEXT_ID, Integer.toString(REQUEST_ID.getAndIncrement(), Character.MAX_RADIX));

        long start = System.currentTimeMillis();

        try {
            doService(httpRequest, httpResponse);
        } catch (Throwable e) {
            log.error("Fatal preview exception: ", e);
            throw new ServletException(e);
        } finally {
            log.trace("{} {} processed in {} msec.", httpRequest.getMethod(), httpRequest.getRequestURL(), System.currentTimeMillis() - start);
            MDC.clear();
        }
    }

    /**
     * Process requests from browser for component layout and all dependent files.
     *
     * @param httpRequest  HTTP request,
     * @param httpResponse HTTP response.
     * @throws Exception exceptions from preview process are thrown to caller.
     */
    private void doService(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
        httpResponse.setCharacterEncoding("UTF-8");
        httpResponse.setStatus(HttpServletResponse.SC_OK);

        // request path is request URI without context; it does not start with a path separator
        String requestPath = httpRequest.getRequestURI().substring(contextPath.length() + 1);
        log.debug("Request {} on context {}.", requestPath, contextPath);

        if (CompoPath.accept(requestPath)) {
            CompoPath compoPath = project.createCompoPath(requestPath);
            FilePath layoutPath = compoPath.getLayoutPath();
            if (!layoutPath.exists()) {
                throw new WoodException("Missing component layout |%s|.", layoutPath);
            }

            // if component has preview layout uses it instead of component layout
            // preview layout should use component as widget
            FilePath layoutPreview = compoPath.getFilePath(LAYOUT_PREVIEW);
            if (layoutPreview.exists()) {
                layoutPath = layoutPreview;
            }
            httpResponse.setContentType(layoutPath.getMimeType());

            // update variables cache every time a component is requested
            variables.update();

            // create component with support for preview script
            Component compo = project.createComponent(layoutPath, this);
            boolean controlScript = httpRequest.getServletContext().getAttribute(EventsServlet.class.getName()) != null;
            Preview preview = new Preview(project, compo, contextPath, controlScript);
            preview.serialize(httpResponse.getWriter());
            return;
        }

        FilePath filePath = project.createFilePath(requestPath);
        httpResponse.setContentType(filePath.getMimeType());

        if (filePath.isStyle()) {
            if (!filePath.hasVariants()) {
                Reader reader = new SourceReader(new StyleReader(filePath), filePath, this);
                FilesUtil.copy(reader, httpResponse.getWriter());
            }
            return;
        }

        if (filePath.isScript()) {
            Reader reader = new SourceReader(filePath, this);
            FilesUtil.copy(reader, httpResponse.getWriter());
            return;
        }

        // all other files are just sent back to browser
        filePath.copyTo(httpResponse.getOutputStream());
    }

    /**
     * Handler for resource references takes care of variables injection and media file processing.
     *
     * @param reference  resource references,
     * @param sourceFile source file containing the resource references.
     * @return resource variable values or media file source.
     */
    @Override
    public String onResourceReference(Reference reference, FilePath sourceFile) {
        String previewLanguage = "en";
        if (reference.isVariable()) {
            String value = variables.get(previewLanguage, reference, sourceFile, this);
            return value != null ? value : reference.toString();
        }
        if (reference.isProject()) {
            String value = project.getDescriptor().getValue(reference.getName());
            return value != null ? value : reference.toString();
        }

        // discover resource file and returns its absolute URL path
        FilePath resourceFile = project.getResourceFile(previewLanguage, reference, sourceFile);
        if (resourceFile == null) {
            throw new WoodException("Missing resource file for reference |%s| from source |%s|.", reference, sourceFile);
        }

        StringBuilder builder = new StringBuilder();
        builder.append(contextPath);
        builder.append(FilePath.SEPARATOR_CHAR);
        if (resourceFile.getParentDir() != null) {
            builder.append(resourceFile.getParentDir().value());
        }
        builder.append(resourceFile.getName());
        return builder.toString();
    }

    // --------------------------------------------------------------------------------------------
    // Test support

    /**
     * Test constructor.
     *
     * @param servletContext mock for Servlet context,
     * @param project        mock for WOOD project,
     * @param variables      mock for variables cache.
     */
    PreviewServlet(ServletContext servletContext, Project project, VariablesCache variables) {
        super();
        this.servletContext = servletContext;
        this.contextPath = servletContext.getContextPath();

        this.project = project;
        this.variables = variables;
    }

    Project getProject() {
        return project;
    }

    VariablesCache getVariables() {
        return variables;
    }
}