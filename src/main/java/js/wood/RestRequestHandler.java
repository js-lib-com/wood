package js.wood;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.lang.reflect.InvocationTargetException;
import java.util.HashMap;
import java.util.Map;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.container.AuthorizationException;
import js.container.ContainerSPI;
import js.container.ManagedClassSPI;
import js.container.ManagedMethodSPI;
import js.http.ContentType;
import js.http.Resource;
import js.http.encoder.ArgumentsReader;
import js.http.encoder.ServerEncoders;
import js.json.Json;
import js.lang.BugError;
import js.lang.SyntaxException;
import js.servlet.RequestContext;
import js.servlet.RequestPreprocessor;
import js.util.Classes;
import js.util.Types;

/**
 * Invoke method identified by REST path.
 * 
 * @author Iulian Rotaru
 */
public class RestRequestHandler {
	/**
	 * A request path starting with <code>rest</code> is considered REST request.
	 * 
	 * @param requestPath request path.
	 * @return true if request path identify a REST resource.
	 */
	public static boolean accept(String requestPath) {
		// TODO does not work if remove class qualified name contains <code>rest</code> package
		return requestPath.contains("/rest/");
	}

	private final ContainerSPI container;

	/** Remote method. */
	private final ManagedMethodSPI method;

	public RestRequestHandler(ContainerSPI container, Project project, String requestPath) {
		this.container = container;

		@SuppressWarnings("unchecked")
		Map<String, ManagedMethodSPI> restMethods = (Map<String, ManagedMethodSPI>) project.getAttribute("rest.methods");
		if (restMethods == null) {
			restMethods = new HashMap<>();
			project.setAttribute("rest.methods", restMethods);

			for (ManagedMethodSPI managedMethod : container.getManagedMethods()) {
				if (!managedMethod.isRemotelyAccessible()) {
					continue;
				}
				if (!Types.isKindOf(managedMethod.getReturnType(), Resource.class)) {
					restMethods.put(key(managedMethod), managedMethod);
				}
			}
		}
		method = restMethods.get(key(requestPath));
	}

	public void service(HttpServletRequest httpRequest, HttpServletResponse httpResponse) throws SyntaxException, IllegalArgumentException, InvocationTargetException, IOException, AuthorizationException {
		if (method == null) {
			return;
		}

		ServerEncoders encoders = ServerEncoders.getInstance();
		ArgumentsReader argumentsReader = encoders.getArgumentsReader(httpRequest, method.getParameterTypes());
		Object[] parameters = argumentsReader.read(httpRequest, method.getParameterTypes());

		Object instance = container.getInstance(method.getDeclaringClass());
		Object value = method.invoke(instance, parameters);

		httpResponse.setCharacterEncoding("UTF-8");
		if (method.isVoid()) {
			httpResponse.setStatus(HttpServletResponse.SC_NO_CONTENT);
			httpResponse.setContentLength(0);
			return;
		}

		ByteArrayOutputStream buffer = new ByteArrayOutputStream();
		Json json = Classes.loadService(Json.class);
		json.stringify(new OutputStreamWriter(buffer, "UTF-8"), value);

		httpResponse.setStatus(HttpServletResponse.SC_OK);
		httpResponse.setContentType(ContentType.APPLICATION_JSON.getValue());
		httpResponse.setContentLength(buffer.size());

		OutputStream outputStream = httpResponse.getOutputStream();
		outputStream.write(buffer.toByteArray());
		outputStream.flush();
		outputStream.close();
	}

	// --------------------------------------------------------------------------------------------
	// UTILITY METHODS

	/**
	 * Generate storage key for REST methods cache. This key is create from declaring class and managed method request paths and
	 * is used on cache initialization. It is paired with {@link #key(String)} created from request path on actual method
	 * invocation.
	 * <p>
	 * Here is storage key syntax that should be identical with retrieval key. Key has optional resource path and sub-resource
	 * path. Resource path is the declaring class request path, {@link ManagedClassSPI#getRequestPath()} and sub-resource path
	 * is managed method request path, {@link ManagedMethodSPI#getRequestPath()}.
	 * 
	 * <pre>
	 * key = ["/" resource ] "/" sub-resource
	 * resource = declaring class request path
	 * sub-resource = managed method request path
	 * </pre>
	 * 
	 * @param restMethod REST method.
	 * @return REST method key.
	 */
	private static String key(ManagedMethodSPI restMethod) {
		StringBuilder key = new StringBuilder();
		if (restMethod.getDeclaringClass().getRequestPath() != null) {
			key.append('/');
			key.append(restMethod.getDeclaringClass().getRequestPath());
		}
		key.append('/');
		key.append(restMethod.getRequestPath());
		return key.toString();
	}

	/**
	 * Generate retrieval key for REST methods cache. This key is used by request routing logic to locate REST method about to
	 * invoke. It is based on request path extracted from request URI, see {@link RequestPreprocessor} and
	 * {@link RequestContext#getRequestPath()} - and should be identical with storage key.
	 * <p>
	 * Retrieval key syntax is identical with storage key but is based on request path, that on its turn is extracted from
	 * request URI. In fact this method just trim query parameters and extension, if any.
	 * 
	 * <pre>
	 * request-path = ["/" resource] "/" sub-resource ["?" query-string]
	 * key = ["/" resource ] "/" sub-resource
	 * resource = managed class request-path
	 * sub-resource = managed method request-path
	 * </pre>
	 * 
	 * @param requestPath request path identify REST resource to retrieve.
	 * @return REST method key.
	 */
	// TOOD update apidoc
	private static String key(String requestPath) {
		int restIndex = requestPath.indexOf("/rest/");
		if (restIndex == -1) {
			throw new BugError("Missing rest path.");
		}
		// /rest/ is 6; skip 5 to include the second / so that key to start with /
		return requestPath.substring(restIndex + 5);
	}
}
