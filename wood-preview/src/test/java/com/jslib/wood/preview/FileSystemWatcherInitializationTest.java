package com.jslib.wood.preview;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.nio.file.WatchService;

import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemWatcherInitializationTest {
    @Mock
    private ServletContext servletContext;
    @Mock
    private ServletContextEvent contextEvent;
    @Mock
    private Thread thread;
    @Mock
    private EventsManager eventsManager;

    private FileSystemWatcher watcher;

    @Before
    public void beforeTest() throws IOException {
        when(servletContext.getInitParameter("PROJECT_DIR")).thenReturn("src/test/resources/project");
        when(contextEvent.getServletContext()).thenReturn(servletContext);

        WatchService watchService = FileSystems.getDefault().newWatchService();
        watcher = new FileSystemWatcher(watchService, thread, eventsManager);
    }

    @Test
    public void GivenServletContextEvent_WhenContextInitialized_ThenThreadStarted() {
        // GIVEN

        // WHEN
        watcher.contextInitialized(contextEvent);

        // THEN
        assertTrue(watcher.getRunning().get());
        verify(thread, times(1)).start();
    }

    @Test
    public void GivenExcludeDirInitParameter_WhenContextInitialized_ThenThreadStarted() {
        // GIVEN
        when(servletContext.getInitParameter("EXCLUDE_DIRS")).thenReturn("subdir1, subdir2");

        // WHEN
        watcher.contextInitialized(contextEvent);

        // THEN
        assertTrue(watcher.isExcluded(Paths.get("src/test/resources/project/subdir1")));
        assertTrue(watcher.isExcluded(Paths.get("src/test/resources/project/subdir2")));
    }
}
