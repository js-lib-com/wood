package com.jslib.wood.preview;

import com.jslib.wood.json.Json;
import com.jslib.wood.lang.Event;
import jakarta.servlet.ServletOutputStream;
import jakarta.servlet.WriteListener;
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
import static org.hamcrest.Matchers.notNullValue;
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
    public void constructor() {
        servlet = new EventsServlet();
        assertThat(servlet.getJson(), notNullValue());
        assertThat(servlet.getEventsManager(), notNullValue());
    }

    @Test
    public void service_FileSystem() throws IOException, InterruptedException {
        when(queue.poll(30000, TimeUnit.MILLISECONDS)).thenReturn(new FileSystemEvent("page.htm", "ENTRY_MODIFY")).thenReturn(new ShutdownEvent());
        when(json.stringify(any(FileSystemEvent.class))).thenReturn("{\"file\":\"page.htm\",\"action\":\"ENTRY_MODIFY\"}");

        servlet.service(httpRequest, httpResponse);

        String expected = "event:FileSystemEvent\r\n" + //
                "data:{\"file\":\"page.htm\",\"action\":\"ENTRY_MODIFY\"}\r\n" + //
                "\r\n";
        assertThat(responseStream.toString(), equalTo(expected));
    }

    @Test
    public void service_KeepAlive() throws IOException, InterruptedException {
        when(queue.poll(30000, TimeUnit.MILLISECONDS)).thenReturn(null).thenReturn(new ShutdownEvent());
        when(json.stringify(any(KeepAliveEvent.class))).thenReturn("{}");

        servlet.service(httpRequest, httpResponse);

        String expected = "event:KeepAliveEvent\r\n" + //
                "data:{}\r\n" + //
                "\r\n";
        assertThat(responseStream.toString(), equalTo(expected));
    }

    @Test
    public void service_Interrupted() throws IOException, InterruptedException {
        when(queue.poll(30000, TimeUnit.MILLISECONDS)).thenThrow(InterruptedException.class).thenReturn(new ShutdownEvent());
        when(json.stringify(any(KeepAliveEvent.class))).thenReturn("{}");

        servlet.service(httpRequest, httpResponse);

        String expected = "event:KeepAliveEvent\r\n" + //
                "data:{}\r\n" + //
                "\r\n";
        assertThat(responseStream.toString(), equalTo(expected));
    }

    @Test
    public void service_ClientClose() throws IOException {
        ServletOutputStream stream = mock(ServletOutputStream.class);
        doThrow(SocketException.class).when(stream).write(any(byte[].class));
        when(httpResponse.getOutputStream()).thenReturn(stream);

        servlet.service(httpRequest, httpResponse);
        verify(stream, times(1)).write(any(byte[].class));
        verify(stream, times(0)).flush();
    }
}
