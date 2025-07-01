package com.jslib.wood.lang;

/**
 * Generic event interface. This interface is a mark interface. It is an unsolicited message generated as response to some state
 * changes. For example, it is used by server sent events logic: client opens an event stream and block waiting for objects
 * implementing this interface. Another example may be push notification system where a notification is an event.
 * 
 * @author Iulian Rotaru
 */
public interface Event
{
}
