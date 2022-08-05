package com.jslib.wood.preview;

import com.jslib.lang.Event;

/**
 * Shutdown event used to force {@link EventsServlet} closing from server logic. When invoke
 * {@link EventsManager#pushEvent(Event)} with this event client connection is closed.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class ShutdownEvent implements Event {

}
