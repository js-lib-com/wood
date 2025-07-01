package com.jslib.wood.preview;

import com.jslib.wood.lang.Event;

/**
 * Ensure connection is preserved alive. This event is sent periodically to ensure client is still alive and to keep
 * connections through routers with NAT idle connections timeout. TCP/IP does not require keep alive or dead connection
 * detection packets but there are routers that drop idle connections after some timeout.
 *
 * @author Iulian Rotaru
 */
public final class KeepAliveEvent implements Event {
    @Override
    public String toString() {
        return "KeepAliveEvent";
    }
}
