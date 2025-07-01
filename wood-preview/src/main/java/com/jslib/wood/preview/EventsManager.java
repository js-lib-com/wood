package com.jslib.wood.preview;

import com.jslib.wood.lang.Event;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import static com.jslib.wood.util.StringsUtil.format;

/**
 * For every connected client there is a blocking events queue; {@link EventsServlet} read it and push events to client. This
 * class provides methods to acquire and release client blocking queue - see {@link #acquireQueue(Integer)} and
 * {@link #releaseQueue(Integer)}. Both method uses an integer ID to identify the client.
 * <p>
 * There is also a method to push events to all queues - see {@link #pushEvent(Event)}. If there are no connected clients event
 * is lost.
 * <p>
 * Since events manager is designed to work in a multithreaded environment all its methods are synchronized.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
public class EventsManager {
    private static final Logger log = LoggerFactory.getLogger(EventsManager.class);

    private static final class InstanceHolder {
        static final EventsManager instance = new EventsManager();
    }

    public static EventsManager instance() {
        return InstanceHolder.instance;
    }

    /**
     * Clients blocking event queues. This dictionary is live, it is updated on every new client connection creation and close.
     * Dictionary key is the client ID.
     */
    private final Map<Integer, BlockingQueue<Event>> queues = new HashMap<>();

    /**
     * Create a new blocking event queue for client identified by given ID.
     *
     * @param clientID client ID.
     * @return client blocking event queue.
     * @throws IllegalStateException if client already has an events queue.
     */
    public synchronized BlockingQueue<Event> acquireQueue(Integer clientID) {
        if (queues.get(clientID) != null) {
            throw new IllegalStateException(format("Events queue for client |%d| already created.", clientID));
        }
        BlockingQueue<Event> queue = new LinkedBlockingDeque<>();
        queues.put(clientID, queue);
        log.debug("Acquired events queue {}.", clientID);
        return queue;
    }

    /**
     * Release client events queue.
     *
     * @param clientID client ID.
     * @throws IllegalStateException if there is no events queue for requested client.
     */
    public synchronized void releaseQueue(Integer clientID) {
        if (queues.get(clientID) == null) {
            throw new IllegalStateException(format("Missing queue for client |%d|.", clientID));
        }
        queues.remove(clientID);
        log.debug("Released events queue {}.", clientID);
    }

    /**
     * Push event to registered events queues. If there are no clients connected this method is NOP and given event is lost.
     * Also, event is lost if events queue is full.
     *
     * @param event event to push to connected clients.
     */
    public synchronized void pushEvent(Event event) {
        for (Map.Entry<Integer, BlockingQueue<Event>> entry : queues.entrySet()) {
            if (!entry.getValue().offer(event)) {
                log.error("Events queue {} is full. Event {} lost.", entry.getKey(), event);
            }
        }
    }

    // --------------------------------------------------------------------------------------------
    // Test support

    Map<Integer, BlockingQueue<Event>> getQueues() {
        return queues;
    }
}
