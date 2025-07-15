package com.jslib.wood.preview;

import com.jslib.wood.WoodException;
import com.jslib.wood.lang.Event;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemWatcherTest {
    @Mock
    private FileSystem fileSystem;
    @Mock
    private FileSystemProvider provider;
    @Mock
    private BasicFileAttributes attributes;

    @Mock
    private WatchService watchService;
    @Mock
    private Thread thread;
    @Mock
    private EventsManager eventsManager;

    private FileSystemWatcher watcher;

    @Before
    public void beforeTest() {
        when(fileSystem.provider()).thenReturn(provider);
        watcher = new FileSystemWatcher(watchService, thread, eventsManager);
    }

    @Test
    public void Given_WhenConstructor_ThenStateInitializedAndThreadNotRunning() throws IOException {
        // GIVEN

        // WHEN
        watcher = new FileSystemWatcher();

        // THEN
        assertThat(watcher.getWatchService(), notNullValue());
        assertThat(watcher.getKeyPaths(), notNullValue());
        assertThat(watcher.getWatchService(), notNullValue());
        assertThat(watcher.getThread(), notNullValue());
        assertFalse(watcher.getRunning().get());
        assertThat(watcher.getEventsManager(), notNullValue());
    }

    @Test
    public void GivenNullProjectDirInitParameter_WhenContextInitialized_ThenThreadNotStarted() {
        // GIVEN
        ServletContext context = mock(ServletContext.class);
        ServletContextEvent contextEvent = mock(ServletContextEvent.class);
        when(contextEvent.getServletContext()).thenReturn(context);

        // WHEN
        watcher.contextInitialized(contextEvent);

        // THEN
        assertFalse(watcher.getRunning().get());
        verify(thread, times(0)).start();
    }

    @Test
    public void Given_WhenContextDestroyed_ThenThreadStopped() {
        // GIVEN

        // WHEN
        watcher.contextDestroyed(mock(ServletContextEvent.class));

        // THEN
        assertFalse(watcher.getRunning().get());
        verify(thread, times(1)).interrupt();
    }

    @Test
    public void GivenWatchKey_WhenContextDestroyed_ThenWatchKeyCancel() throws IOException {
        // GIVEN
        WatchKey watchKey = mock(WatchKey.class);
        watcher.getKeyPaths().put(watchKey, mock(Path.class));

        // WHEN
        watcher.contextDestroyed(mock(ServletContextEvent.class));

        // THEN
        verify(watchKey, times(1)).cancel();
        verify(watcher.getWatchService(), times(1)).close();
    }

    /**
     * IOException from WatchService close should not propagate.
     */
    @Test
    public void GivenWatchServiceIoException_WhenContextDestroyed_ThenExceptionLogged() throws IOException {
        // GIVEN
        doThrow(IOException.class).when(watchService).close();

        // WHEN
        watcher.contextDestroyed(mock(ServletContextEvent.class));

        // THEN
    }

    @SuppressWarnings("unchecked")
    @Test
    public void GivenWatchKeyFromWatchService_WhenThreadRun_ThenPushFileSystemEvent() throws InterruptedException, IOException {
        // GIVEN
        WatchKey watchKey = mock(WatchKey.class);

        Path dir = mock(Path.class);
        Path path = mock(Path.class);
        when(dir.resolve(dir)).thenReturn(path);
        when(path.getFileSystem()).thenReturn(fileSystem);
        lenient().when(provider.readAttributes(path, BasicFileAttributes.class, NOFOLLOW_LINKS)).thenReturn(attributes);
        watcher.getKeyPaths().put(watchKey, dir);

        // reset running flag after first take() to force running loop end
        when(watchService.take()).thenAnswer((Answer<WatchKey>) invocation -> {
            watcher.setRunning(false);
            return watchKey;
        });

        WatchEvent.Kind<Path> kind = mock(WatchEvent.Kind.class);
        when(kind.name()).thenReturn("ENTRY_MODIFY");

        WatchEvent<Path> watchEvent = mock(WatchEvent.class);
        when(watchEvent.kind()).thenReturn(kind);
        when(watchEvent.context()).thenReturn(dir);
        when(watchKey.pollEvents()).thenReturn(Collections.singletonList(watchEvent));

        // WHEN
        watcher.setRunning(true);
        watcher.run();

        // THEN
        verify(eventsManager, times(1)).pushEvent(any(Event.class));
    }

    @Test
    public void GivenWatchServiceTakeInterruptionAndNotRunning_WhenThreadRun_ThenBreakThreadLoop() throws InterruptedException {
        // GIVEN
        // reset running flag after first take() to force running loop end
        when(watchService.take()).thenAnswer((Answer<WatchKey>) invocation -> {
            watcher.setRunning(false);
            throw new InterruptedException();
        });

        // WHEN
        watcher.setRunning(true);
        watcher.run();

        // THEN
        verify(watchService, times(1)).take();
    }

    @Test
    public void GivenWatchKeyOnDirPathRegistration_WhenWatcherRegister_ThenKeyPathsUpdated() throws IOException {
        // GIVEN
        WatchKey watchKey = mock(WatchKey.class);
        Path dir = mock(Path.class);
        when(dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)).thenReturn(watchKey);

        // WHEN
        watcher.register(dir);

        // THEN
        assertThat(watcher.getKeyPaths().get(watchKey), equalTo(dir));
    }

    @Test(expected = WoodException.class)
    public void GivenDirPathRegisterIoException_WhenWatcherRegister_ThenWoodException() throws IOException {
        // GIVEN
        Path dir = mock(Path.class);
        when(dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)).thenThrow(IOException.class);

        // WHEN
        watcher.register(dir);

        // THEN
    }
}
