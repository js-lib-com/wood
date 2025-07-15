package com.jslib.wood.preview;

import com.jslib.wood.json.Json;
import com.jslib.wood.lang.Event;
import jakarta.servlet.*;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.net.SocketException;
import java.util.concurrent.BlockingQueue;
import java.util.concurrent.TimeUnit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.fail;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class EventsServletTest {
    @Mock
    private HttpServletRequest httpRequest;
    @Mock
    private HttpServletResponse httpResponse;

    @Mock
    private Json json;
    @Mock
    private EventsManager eventsManager;
    @Mock
    private BlockingQueue<Event> queue;

    private EventsServlet servlet;

    private final ByteArrayOutputStream responseStream = new ByteArrayOutputStream();

    @Before
    public void beforeTest() throws IOException {
        when(eventsManager.acquireQueue(anyInt())).thenReturn(queue);

        when(httpResponse.getOutputStream()).thenReturn(new ServletOutputStream() {
            @Override
            public void write(int b) {
                responseStream.write(b);
            }

            @Override
            public boolean isReady() {
                return true;
            }

            @Override
            public void setWriteListener(WriteListener writeListener) {
            }
        });

        servlet = new EventsServlet(json, eventsManager);
    }

    @Test
    public void GivenServletContextAndConfig_WhenServletInit_ThenServletNameSavedOnContextAttribute() throws ServletException {
        // GIVEN
        ServletContext context = mock(ServletContext.class);
        ServletConfig config = mock((ServletConfig.class));
        when(config.getServletContext()).thenReturn(context);

        // WHEN
        servlet.init(config);

        // THEN
        verify(context, times(1)).setAttribute(EventsServlet.class.getName(), true);
    }

    @Test
    public void GivenFileSystemEventOnQueue_WhenServletService_ThenSendEventJson() throws IOException, InterruptedException {
        // GIVEN
        // queue returns first a file system event then a shutdown event to break servlet loop
        when(queue.poll(30000, TimeUnit.MILLISECONDS)).thenReturn(new FileSystemEvent("page.htm", "ENTRY_MODIFY")).thenReturn(new ShutdownEvent());
        when(json.stringify(any(FileSystemEvent.class))).thenReturn("{\"file\":\"page.htm\",\"action\":\"ENTRY_MODIFY\"}");

        // WHEN
        servlet.service(httpRequest, httpResponse);

        // THEN
        String expected = "event:FileSystemEvent\r\n" + //
                "data:{\"file\":\"page.htm\",\"action\":\"ENTRY_MODIFY\"}\r\n" + //
                "\r\n";
        assertThat(responseStream.toString(), equalTo(expected));
    }

    @Test
    public void GivenNoEventOnQueue_WhenServletService_ThenSendKeepAlive() throws IOException, InterruptedException {
        // GIVEN
        // queue returns first null signaling no event in the queue, then a shutdown event to break servlet loop
        when(queue.poll(30000, TimeUnit.MILLISECONDS)).thenReturn(null).thenReturn(new ShutdownEvent());
        when(json.stringify(any(KeepAliveEvent.class))).thenReturn("{}");

        // WHEN
        servlet.service(httpRequest, httpResponse);

        // THEN
        String expected = "event:KeepAliveEvent\r\n" + //
                "data:{}\r\n" + //
                "\r\n";
        assertThat(responseStream.toString(), equalTo(expected));
    }

    @Test
    public void GivenQueueInterruptedException_WhenServletService_ThenSendKeepAlive() throws IOException, InterruptedException {
        // GIVEN
        // queue first throw an interrupted exception then a shutdown event to break servlet loop
        when(queue.poll(30000, TimeUnit.MILLISECONDS)).thenThrow(InterruptedException.class).thenReturn(new ShutdownEvent());
        when(json.stringify(any(KeepAliveEvent.class))).thenReturn("{}");

        // WHEN
        servlet.service(httpRequest, httpResponse);

        // THEN
        String expected = "event:KeepAliveEvent\r\n" + //
                "data:{}\r\n" + //
                "\r\n";
        assertThat(responseStream.toString(), equalTo(expected));
    }

    @Test
    public void GivenRuntimeException_WhenServletService_ThenServletGracefullyClose() throws IOException, InterruptedException {
        // GIVEN
        when(queue.poll(30000, TimeUnit.MILLISECONDS)).thenThrow(RuntimeException.class);

        // WHEN
        try {
            servlet.service(httpRequest, httpResponse);
            fail("Expected runtime exception.");
        } catch (RuntimeException ignored) {
        }

        // THEN
        verify(eventsManager, times(1)).releaseQueue(anyInt());
    }

    @Test
    public void GivenClientClose_WhenServletService_ThenServletOutputStreamClosed() throws IOException {
        // GIVEN
        ServletOutputStream stream = mock(ServletOutputStream.class);
        // simulate client close by throwing socket exception
        doThrow(SocketException.class).when(stream).write(any(byte[].class));
        when(httpResponse.getOutputStream()).thenReturn(stream);

        // WHEN
        servlet.service(httpRequest, httpResponse);

        // THEN
        verify(stream, times(1)).write(any(byte[].class));
        verify(stream, times(0)).flush();
    }
}
