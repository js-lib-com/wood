package com.jslib.wood.preview;

import com.jslib.wood.json.Json;
import com.jslib.wood.lang.Event;
import jakarta.servlet.ServletConfig;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServlet;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

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
    private static final long serialVersionUID = 6319917762096267440L;
    private static final Logger log = LoggerFactory.getLogger(EventsServlet.class);

    /**
     * Keep alive period, in milliseconds.
     */
    private static final int KEEP_ALIVE_PERIOD = 30 * 1000;

    /**
     * Event client connection has an auto-generated incremental ID.
     */
    private static final AtomicInteger ID_SEED = new AtomicInteger();

    /**
     * Event is pushed to client using JSON format.
     */
    private final Json json;
    /**
     * Events manager has an events blocking queue for every connected client.
     */
    private final EventsManager eventsManager;

    @SuppressWarnings("unused")
    public EventsServlet() {
        log.trace("EventsServlet()");
        this.json = Json.getInstance();
        this.eventsManager = EventsManager.instance();
    }

    // TEST
    EventsServlet(Json json, EventsManager eventsManager) {
        log.trace("EventsServlet(Json json, EventsManager eventsManager)");
        this.json = json;
        this.eventsManager = eventsManager;
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
        log.trace("init(ServletConfig config)");
        ServletContext servletContext = config.getServletContext();
        log.trace("Initialize servlet {}#{}", servletContext.getServletContextName(), config.getServletName());
        // flag for preview servlet that events servlet is running
        servletContext.setAttribute(EventsServlet.class.getName(), true);
    }

    /**
     * Retrieve a blocking events queue from {@link #eventsManager} and wait for events in a loop. When new event is present
     * send it to client. Terminate this method when client closes connection.
     */
    @Override
    protected void service(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.trace("service(HttpServletRequest request, HttpServletResponse response)");

        response.setStatus(HttpServletResponse.SC_OK);
        response.setHeader("Cache-Control", "no-cache");
        response.setHeader("Content-Type", "text/event-stream; charset=UTF-8");

        OutputStream stream = response.getOutputStream();

        Integer id = ID_SEED.incrementAndGet();
        log.debug("Open event stream {} from {}", id, request.getRemoteAddr());
        BlockingQueue<Event> queue = eventsManager.acquireQueue(id);

        try {
            for (; ; ) {
                Event event = null;
                try {
                    event = queue.poll(KEEP_ALIVE_PERIOD, TimeUnit.MILLISECONDS);
                } catch (InterruptedException ignored) {
                }
                if (event == null) {
                    event = new KeepAliveEvent();
                }
                if (event instanceof ShutdownEvent) {
                    log.debug("Got shutdown event; break events loop");
                    break;
                }
                log.debug("Sending event {} on stream {}", event, id);

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
                } catch (IOException e) {
                    log.debug("IO / Socket exception: {}", e.getMessage());
                    log.debug("Client presumably closes event stream; break server-sent events loop");
                    break;
                } catch (Exception e) {
                    log.error("Error processing events stream: {}: {}", e.getClass(), e.getMessage(), e);
                }
            }
        } finally {
            eventsManager.releaseQueue(id);
            log.debug("Close event stream {} with {}", id, request.getRemoteAddr());
        }
    }

    /**
     * Convenient way to retrieve string bytes.
     */
    private static byte[] bytes(String string) {
        return string.getBytes(StandardCharsets.UTF_8);
    }
}
