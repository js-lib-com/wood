package com.jslib.wood.preview;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.LinkedBlockingDeque;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.lang.BugError;
import com.jslib.lang.Event;

/**
 * For every connected client there is a blocking events queue; {@link EventsServlet} read it and push events to client. This
 * class provides methods to acquire and release client blocking queue - see {@link #acquireQueue(Integer)} and
 * {@link #releaseQueue(Integer)}. Both method uses an integer ID to identify the client.
 * <p>
 * There is also a method to push events to all queues - see {@link #pushEvent(Event)}. If there are no connected clients event
 * is lost.
 * <p>
 * Since events manager is designed to work in a multi-threaded environment all its methods are synchronized.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class EventsManager {
	/** Class logger. */
	private static final Log log = LogFactory.getLog(EventsManager.class);

	/** Mutex for singleton pattern implemented by {@link #instance()} method. */
	private static final Object mutex = new Object();
	/** Events manager instance, a single one per class loader. */
	private static EventsManager instance;

	/**
	 * Get events manager singleton. Instance is created on the fly at first invocation.
	 * 
	 * @return events manager singleton.
	 */
	public static EventsManager instance() {
		if (instance == null) {
			synchronized (mutex) {
				if (instance == null) {
					instance = new EventsManager();
				}
			}
		}
		return instance;
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
	 * @throws BugError if client already has an events queue.
	 */
	public synchronized BlockingQueue<Event> acquireQueue(Integer clientID) {
		if (queues.get(clientID) != null) {
			throw new BugError("Events queue for client |%d| already created.", clientID);
		}
		BlockingQueue<Event> queue = new LinkedBlockingDeque<>();
		queues.put(clientID, queue);
		log.debug("Acquired events queue |%d|.", clientID);
		return queue;
	}

	/**
	 * Release client events queue.
	 * 
	 * @param clientID client ID.
	 * @throws BugError if there is no events queue for requested client.
	 */
	public synchronized void releaseQueue(Integer clientID) {
		if (queues.get(clientID) == null) {
			throw new BugError("Missing queue for client |%d|.", clientID);
		}
		queues.remove(clientID);
		log.debug("Released events queue |%d|.", clientID);
	}

	/**
	 * Push event to registered events queues. If there are no clients connected this method is NOP and given event is lost.
	 * Also event is lost if events queue is full.
	 * 
	 * @param event event to push to connected clients.
	 */
	public synchronized void pushEvent(Event event) {
		for (Map.Entry<Integer, BlockingQueue<Event>> entry : queues.entrySet()) {
			if (!entry.getValue().offer(event)) {
				log.error("Events queue |%d| is full. Event |%s| lost.", entry.getKey(), event);
			}
		}
	}
	
	// --------------------------------------------------------------------------------------------
	// Test support

	Map<Integer, BlockingQueue<Event>> getQueues() {
		return queues;
	}
}
