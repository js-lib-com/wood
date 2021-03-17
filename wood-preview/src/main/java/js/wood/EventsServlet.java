package js.wood;

import java.io.IOException;
import java.io.OutputStream;
import java.io.UnsupportedEncodingException;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import javax.servlet.ServletConfig;
import javax.servlet.ServletContext;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import js.json.Json;
import js.lang.Event;
import js.lang.KeepAliveEvent;
import js.log.Log;
import js.log.LogFactory;
import js.util.Classes;

/**
 * Servlet for server sent events, aka push events. This servlet implements W3C - Server Sent Events, server side. It has a
 * reference to {@link EventsManager} from which it acquires an events blocking queue for every connected client.
 * <p>
 * A new instance is created for every connected client. Instance stay blocked on events queue; when a new event is added to
 * queues servlet instance become active and sent the event to the client. When client closes its connection there is socket
 * error and servlet instance is destroyed.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class EventsServlet extends HttpServlet {
	/** Java serialization version. */
	private static final long serialVersionUID = 6319917762096267440L;
	/** Class logger. */
	private static final Log log = LogFactory.getLog(EventsServlet.class);

	/** Keep alive period, in milliseconds. */
	private static final int KEEP_ALIVE_PERIOD = 30 * 1000;

	/** Event client connection has an auto-generated incremental ID. */
	private static final AtomicInteger ID_SEED = new AtomicInteger();

	/** Event is pushed to client using JSON format. */
	private final Json json;
	/** Events manager has an events blocking queue for every connected client. */
	private final EventsManager eventsManager;

	/** Construct events servlet instance. */
	public EventsServlet() {
		log.trace("EventsServlet()");
		this.json = Classes.loadService(Json.class);
		this.eventsManager = EventsManager.instance();
	}

	/**
	 * Servlet instance initialization. This hook is invoked by servlet container on instance creation. Since this servlet is
	 * declared <code>load-on-startup</code> this initialization occurs at application deployment.
	 * <p>
	 * Current implementation just create boolean attribute with this servlet class name, so that {@link PreviewServlet} is able
	 * to detect if events servlet is configured and configure preview control script accordingly.
	 * 
	 * @param config servlet configuration.
	 */
	@Override
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		ServletContext servletContext = config.getServletContext();
		log.trace("Initialize servlet |%s#%s|.", servletContext.getServletContextName(), config.getServletName());
		servletContext.setAttribute(EventsServlet.class.getName(), true);
	}

	/**
	 * Retrieve a blocking events queue from {@link #eventsManager} and wait for events in a loop. When new event is present
	 * send it to client. Terminate this method when client closes connection.
	 */
	@Override
	protected void service(HttpServletRequest request, HttpServletResponse response) throws ServletException, IOException {
		log.trace("service(Request,Response)");

		response.setStatus(HttpServletResponse.SC_OK);
		response.setHeader("Cache-Control", "no-cache");
		response.setHeader("Content-Type", "text/event-stream; charset=UTF-8");

		OutputStream stream = response.getOutputStream();

		Integer id = ID_SEED.incrementAndGet();
		log.debug("Open event stream |%d| from |%s|. ", id, request.getRemoteAddr());
		BlockingQueue<Event> queue = eventsManager.acquireQueue(id);

		try {
			for (;;) {
				Event event = null;
				try {
					event = queue.poll(KEEP_ALIVE_PERIOD, TimeUnit.MILLISECONDS);
				} catch (InterruptedException unused) {
				}
				if (event == null) {
					event = new KeepAliveEvent();
				}
				if (event instanceof ShutdownEvent) {
					log.debug("Got shutdown event. Break events loop.");
					break;
				}
				log.debug("Sending event |%s| on stream |%d|.", event, id);

				try {
					// event: counterCRLF
					stream.write(bytes("event:"));
					// event field is the simple class name of the push event instance
					stream.write(bytes(event.getClass().getSimpleName()));
					stream.write(bytes("\r\n"));

					// data: { json }CRLF
					stream.write(bytes("data:"));
					stream.write(bytes(json.stringify(event)));
					stream.write(bytes("\r\n"));

					// empty line for event end mark
					stream.write(bytes("\r\n"));
					stream.flush();
				} catch (SocketException e) {
					log.debug("Socket exception: %s", e.getMessage());
					log.debug("Client closes event stream. Break server-sent events loop.");
					break;
				}
			}
		} finally {
			eventsManager.releaseQueue(id);
			log.debug("Close event stream |%d| with |%s|.", id, request.getRemoteAddr());
		}
	}

	/** Convenient way to retrieve string bytes. */
	private static byte[] bytes(String string) throws UnsupportedEncodingException {
		return string.getBytes("UTF-8");
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	EventsServlet(Json json, EventsManager eventsManager) {
		this.json = json;
		this.eventsManager = eventsManager;
	}

	Json getJson() {
		return json;
	}

	EventsManager getEventsManager() {
		return eventsManager;
	}
}
