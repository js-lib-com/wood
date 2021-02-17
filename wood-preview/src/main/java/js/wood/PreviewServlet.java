package js.wood;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.RequestDispatcher;
import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.json.Json;
import js.lang.BugError;
import js.log.Log;
import js.log.LogContext;
import js.log.LogFactory;
import js.rmi.BusinessException;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;

/**
 * Preview servlet allows access from browser to project components and related files. This allows to use browser for components
 * preview by simple refreshing loaded page. A file is returned as it is but component layout is aggregated and preview HTML
 * code is generated on the fly; uses {@link Preview} for that.
 * <p>
 * In order to work preview servlet should be properly declared into web.xml file:
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
 *  &lt;servlet&gt;
 *      &lt;servlet-name&gt;development&lt;/servlet-name&gt;
 *      &lt;servlet-class&gt;js.wood.PreviewServlet&lt;/servlet-class&gt;
 *      &lt;load-on-startup&gt;1&lt;/load-on-startup&gt;
 *  &lt;/servlet&gt;
 *  
 *  &lt;servlet-mapping&gt;
 *      &lt;servlet-name&gt;development&lt;/servlet-name&gt;
 *      &lt;url-pattern&gt;/&lt;/url-pattern&gt;
 *  &lt;/servlet-mapping&gt;
 * </pre>
 * 
 * Preview WAR archive is actually empty; it contains only <code>web.xml</code> descriptor, that is, no Java classes nor static
 * content.
 * 
 * @author Iulian Rotaru
 */
public final class PreviewServlet extends HttpServlet implements IReferenceHandler {
	/** Java serialization ID, */
	private static final long serialVersionUID = -2971057399517075118L;

	/** Class logger. */
	private static final Log log = LogFactory.getLog(PreviewServlet.class);

	/** Logger diagnostic context stores contextual information regarding current request. */
	private static final LogContext logContext = LogFactory.getLogContext();
	/** Diagnostic context name for context path, aka application. */
	private static final String LOG_CONTEXT_APP = "app";
	/** Diagnostic context name for remote host, aka IP address. */
	private static final String LOG_CONTEXT_IP = "ip";
	/** Diagnostic context name for current request ID. */
	private static final String LOG_CONTEXT_ID = "id";

	private static final AtomicInteger requestID = new AtomicInteger();

	/** Servlet context init parameter for project directory. */
	private static final String PROJECT_DIR_PARAM = "PROJECT_DIR";

	/**
	 * Preview layout is a special layout used for component unit test. It is returned instead of component; preview layout uses
	 * component layout as widget.
	 */
	private static final String LAYOUT_PREVIEW = "preview.htm";

	/** Project instance initialized from Servlet context parameter on Servlet initialization. */
	private PreviewProject project;

	/** Variables cache initialized before every component preview processing. */
	private VariablesCache variables;

	/**
	 * Servlet instance initialization. This hook is invoked by Servlet container when first create preview Servlet instance.
	 * Since this Servlet is declared <code>load-on-startup</code> this initialization occurs at application deployment.
	 * <p>
	 * For every deployed preview context/application, Servlet container creates exactly one instance of this preview Servlet
	 * and invoke this method once. This method takes care to create {@link #project} instance with proper root directory loaded
	 * from initialization parameters.
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext context = config.getServletContext();
		project = new PreviewProject(new File(context.getInitParameter(PROJECT_DIR_PARAM)));
		variables = new VariablesCache(project);
	}

	/**
	 * Servlet service routine just delegates {@link #doService(HttpServletRequest, HttpServletResponse)} and print exception
	 * stack.
	 * 
	 * @param httpRequest HTTP request,
	 * @param httpResponse HTTP response.
	 */
	@Override
	protected void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws ServletException, IOException {
		// push context path and remote address of the requested processed by this thread to logger diagnostic context
		logContext.put(LOG_CONTEXT_APP, httpRequest.getContextPath().isEmpty() ? "root" : httpRequest.getContextPath().substring(1));
		logContext.put(LOG_CONTEXT_IP, httpRequest.getRemoteHost());
		logContext.put(LOG_CONTEXT_ID, Integer.toString(requestID.getAndIncrement(), Character.MAX_RADIX));

		long start = System.currentTimeMillis();

		try {
			doService(httpRequest, httpResponse);
		} catch (BusinessException e) {
		} catch (Throwable e) {
			if (e.getCause() instanceof BusinessException) {
				sendThrowable(httpResponse, HttpServletResponse.SC_BAD_REQUEST, e.getCause());
				return;
			}
			log.dump("Fatal preview exception: ", e);
			throw new ServletException(e);
		} finally {
			log.trace("%s %s processed in %d msec.", httpRequest.getMethod(), httpRequest.getRequestURL(), System.currentTimeMillis() - start);
			logContext.clear();
		}
	}

	/**
	 * Process requests from browser for component layout and all dependent files.
	 * 
	 * @param httpRequest HTTP request,
	 * @param httpResponse HTTP response.
	 * @throws Exception exceptions from preview process are thrown to caller.
	 */
	private void doService(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws Exception {
		httpResponse.setCharacterEncoding("UTF-8");

		final String contextPath = httpRequest.getContextPath();
		// request path is request URI without context; it does not starts with a path separator
		String requestPath = httpRequest.getRequestURI().substring(httpRequest.getContextPath().length() + 1);
		log.debug("Request |%s| on context |%s|.", requestPath, contextPath);

		if (needForward(requestPath)) {
			// by convention preview context has suffix -preview
			int suffixSeparatorPosition = contextPath.lastIndexOf('-');
			String contextName = contextPath.substring(0, suffixSeparatorPosition);

			ServletContext context = getServletContext().getContext(contextName);
			if (context == null) {
				throw new BugError("Application context |%s| is not deployed or preview context is not configured with crossContext='true'", contextName);
			}

			RequestDispatcher dispatcher = context.getRequestDispatcher(forwardPath(project, requestPath));
			dispatcher.forward(httpRequest, httpResponse);
			return;
		}

		if (CompoPath.accept(requestPath)) {
			CompoPath compoPath = new CompoPath(project, requestPath);
			FilePath layoutPath = compoPath.getLayoutPath();

			// if component has preview layout uses it instead of component layout
			// preview layout should use component as widget
			FilePath layoutPreview = compoPath.getFilePath(LAYOUT_PREVIEW);
			if (layoutPreview.exists()) {
				layoutPath = layoutPreview;
			}

			// update variables cache every time a component is requested
			variables.update();

			// create component with support for preview script
			Component component = new Component(layoutPath, this);
			component.scan();
			Preview preview = new Preview(project, component);
			preview.serialize(httpResponse.getWriter());
			return;
		}

		FilePath filePath = project.getFile(requestPath);
		if (filePath.isStyle()) {
			if (!filePath.hasVariants()) {
				httpResponse.setContentType(TEXT_CSS);
				Reader reader = new SourceReader(new StyleReader(filePath), filePath, this);
				Files.copy(reader, httpResponse.getWriter());
			}
			return;
		}

		if (filePath.isScript()) {
			httpResponse.setContentType(APPLICATION_JAVASCRIPT);
			Reader reader = new SourceReader(filePath, this);
			Files.copy(reader, httpResponse.getWriter());
			return;
		}

		// all other files are just sent back to browser
		File file = filePath.toFile();
		httpResponse.setContentType(forFile(file));
		Files.copy(file, httpResponse.getOutputStream());
	}

	/**
	 * Handler for resource references takes care of variables injection and media file processing.
	 * 
	 * @param reference resource references,
	 * @param source source containing the resource references.
	 * @return resource variable values or media file source.
	 */
	@Override
	public String onResourceReference(IReference reference, FilePath source) {
		Locale previewLocale = new Locale("en");
		if (reference.isVariable()) {
			String value = variables.get(previewLocale, reference, source, this);
			return value != null ? value : reference.toString();
		}

		// discover media file and returns its absolute URL path
		FilePath mediaFile = project.getMediaFile(previewLocale, reference, source);
		if (mediaFile == null) {
			throw new WoodException("Missing media file for reference |%s| from source |%s|.", reference, source);
		}

		StringBuilder builder = new StringBuilder();
		builder.append(Path.SEPARATOR);
		builder.append(project.getPreviewName());
		builder.append(Path.SEPARATOR);
		builder.append(mediaFile.getParentDirPath().value());
		builder.append(mediaFile.getName());
		return builder.toString();
	}

	/**
	 * Send throwable back to preview browser.
	 * 
	 * @param httpResponse HTTP response,
	 * @param responseCode HTTP response code,
	 * @param throwable throwable instance.
	 * @throws IOException if sending throwable fails.
	 */
	private void sendThrowable(HttpServletResponse httpResponse, int responseCode, Throwable throwable) throws IOException {
		httpResponse.setStatus(responseCode);
		httpResponse.setContentType(APPLICATION_JSON);

		Json json = Classes.loadService(Json.class);
		StringWriter buffer = new StringWriter();
		json.stringify(buffer, throwable);

		byte[] bytes = buffer.toString().getBytes("UTF-8");
		httpResponse.setContentLength(bytes.length);
		httpResponse.getOutputStream().write(bytes);
		httpResponse.getOutputStream().flush();
	}

	private static boolean needForward(String requestPath) {
		if (requestPath.endsWith(".rmi")) {
			return true;
		}
		if (requestPath.contains("/rest/")) {
			return true;
		}
		if (requestPath.endsWith(".xsp") || requestPath.contains("/captcha/image")) {
			return true;
		}
		return false;
	}

	private static final String TEXT_HTML = "text/html;charset=UTF-8";
	private static final String TEXT_XML = "text/xml;charset=UTF-8";
	private static final String TEXT_CSS = "text/css;charset=UTF-8";
	private static final String APPLICATION_JAVASCRIPT = "application/javascript;charset=UTF-8";
	private static final String APPLICATION_JSON = "application/json";
	private static final String IMAGE_PNG = "image/png";
	private static final String IMAGE_JPEG = "image/jpeg";
	private static final String IMAGE_GIF = "image/gif";
	private static final String IMAGE_TIFF = "image/tiff";
	private static final String IMAGE_SVG = "image/svg+xml";

	/** Content type for widespread file extensions. */
	private static final Map<String, String> FILE_TYPES = new HashMap<>();
	static {
		FILE_TYPES.put("html", TEXT_HTML);
		FILE_TYPES.put("htm", TEXT_HTML);
		FILE_TYPES.put("xml", TEXT_XML);
		FILE_TYPES.put("css", TEXT_CSS);
		FILE_TYPES.put("js", APPLICATION_JAVASCRIPT);
		FILE_TYPES.put("json", APPLICATION_JSON);
		FILE_TYPES.put("png", IMAGE_PNG);
		FILE_TYPES.put("jpg", IMAGE_JPEG);
		FILE_TYPES.put("jpeg", IMAGE_JPEG);
		FILE_TYPES.put("gif", IMAGE_GIF);
		FILE_TYPES.put("tiff", IMAGE_TIFF);
		FILE_TYPES.put("svg", IMAGE_SVG);
	}

	private static String forFile(File file) {
		String contentType = FILE_TYPES.get(Files.getExtension(file));
		if (contentType == null) {
			log.debug("Unknown content type for |%s|. Replace with default |%s|.", file, TEXT_HTML);
			contentType = TEXT_HTML;
		}
		return contentType;
	}

	private static String forwardPath(PreviewProject project, String requestPath) {
		// assume we are into preview for component '/res/compos/dialogs/alert'
		// current loaded content is the last part of the path, i.e. 'alert'
		// from a component script there is a RMI request for sixqs.site.controller.MainController#getCategoriesSelect

		// in this case browser generated URL is like
		// http://localhost/site2/compos/dialog/sixqs/site/controller/MainController/getCategoriesSelect.rmi
		// note that from prefix path 'alert' is missing since is current loaded content

		// in order to discover RMI class path need to remove prefix path, after extension remove
		// for that remove from request path the paths components that are directories into project resources
		// in above example remove 'compos/dialogs/'

		// compos/dialogs/sixqs/site/controller/MainController/getCategoriesSelect.rmi
		List<String> pathParts = Strings.split(requestPath, '/');
		// [ compos, dialogs, sixqs, site, controller, MainController, getCategoriesSelect.rmi]

		// remove path parts that are directories into project resources
		File resourcesPath = project.getProjectRoot();
		for (;;) {
			resourcesPath = new File(resourcesPath, pathParts.get(0));
			if (!resourcesPath.isDirectory()) {
				break;
			}
			pathParts.remove(0);
		}

		// [ sixqs, site, controller, MainController, getCategoriesSelect.rmi]
		StringBuilder builder = new StringBuilder();
		for (String pathPart : pathParts) {
			builder.append('/');
			builder.append(pathPart);
		}
		// /sixqs/site/controller/MainController/getCategoriesSelect.rmi
		return builder.toString();
	}
}