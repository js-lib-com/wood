package js.wood;

import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.WatchEvent;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import javax.servlet.ServletContext;
import javax.servlet.ServletContextEvent;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;

import js.lang.Event;

@RunWith(MockitoJUnitRunner.class)
public class FileSystemWatcherTest {
	@Mock
	private WatchService watchService;
	@Mock
	private Thread thread;
	@Mock
	private EventsManager eventsManager;

	private FileSystemWatcher watcher;

	@Before
	public void beforeTest() throws IOException {
		watcher = new FileSystemWatcher(watchService, thread, eventsManager);
	}

	@Test
	public void constructor() throws IOException {
		watcher = new FileSystemWatcher();
		assertThat(watcher.getWatchService(), notNullValue());
		assertThat(watcher.getKeyPaths(), notNullValue());
		assertThat(watcher.getWatchService(), notNullValue());
		assertThat(watcher.getThread(), notNullValue());
		assertFalse(watcher.getRunning().get());
		assertThat(watcher.getEventsManager(), notNullValue());
	}

	@Test
	public void contextInitialized() {
		ServletContext context = mock(ServletContext.class);
		when(context.getInitParameter("PROJECT_DIR")).thenReturn("/path/to/project/");

		ServletContextEvent contextEvent = mock(ServletContextEvent.class);
		when(contextEvent.getServletContext()).thenReturn(context);

		watcher.contextInitialized(contextEvent);
		assertTrue(watcher.getRunning().get());
		verify(thread, times(1)).start();
	}

	@Test
	public void contextDestroyed() throws IOException {
		WatchKey watchKey = mock(WatchKey.class);
		watcher.getKeyPaths().put(watchKey, mock(Path.class));

		watcher.contextDestroyed(mock(ServletContextEvent.class));
		assertFalse(watcher.getRunning().get());
		verify(thread, times(1)).interrupt();
		verify(watchKey, times(1)).cancel();
		verify(watcher.getWatchService(), times(1)).close();
	}

	/** IOException from WatchService close should not propagate. */
	@Test
	public void contextDestroyed_CloseException() throws IOException {
		doThrow(IOException.class).when(watchService).close();
		watcher.contextDestroyed(mock(ServletContextEvent.class));
	}

	@Test
	public void walkFileTree() {
		File childDir = mock(File.class);
		when(childDir.isDirectory()).thenReturn(true);
		File childFile = mock(File.class);

		File dir = mock(File.class);
		when(dir.isDirectory()).thenReturn(true);
		when(dir.listFiles()).thenReturn(new File[] { childDir, childFile });

		List<File> files = new ArrayList<>();
		FileSystemWatcher.walkFileTree(dir, Collections.emptyList(), file -> files.add(file));

		assertThat(files, hasSize(2));
		assertThat(files, contains(dir, childDir));
	}

	@SuppressWarnings("unchecked")
	@Test
	public void run() throws InterruptedException {
		WatchKey watchKey = mock(WatchKey.class);

		Path dir = mock(Path.class);
		watcher.getKeyPaths().put(watchKey, dir);

		// reset running flag after first take() to force running loop end
		when(watchService.take()).thenAnswer(new Answer<WatchKey>() {
			@Override
			public WatchKey answer(InvocationOnMock invocation) throws Throwable {
				watcher.setRunning(false);
				return watchKey;
			}
		});

		WatchEvent.Kind<Path> kind = mock(WatchEvent.Kind.class);
		when(kind.name()).thenReturn("ENTRY_MODIFY");

		WatchEvent<Path> watchEvent = mock(WatchEvent.class);
		when(watchEvent.kind()).thenReturn(kind);
		when(watchEvent.context()).thenReturn(dir);
		when(watchKey.pollEvents()).thenReturn(Arrays.asList(watchEvent));

		watcher.setRunning(true);
		watcher.run();

		verify(eventsManager, times(1)).pushEvent(any(Event.class));
	}

	@Test
	public void run_TakeInterrupted() throws InterruptedException {
		// reset running flag after first take() to force running loop end
		when(watchService.take()).thenAnswer(new Answer<WatchKey>() {
			@Override
			public WatchKey answer(InvocationOnMock invocation) throws Throwable {
				watcher.setRunning(false);
				throw new InterruptedException();
			}
		});

		watcher.setRunning(true);
		watcher.run();
		verify(watchService, times(1)).take();
	}

	@Test
	public void register() throws IOException {
		WatchKey watchKey = mock(WatchKey.class);

		Path dir = mock(Path.class);
		when(dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)).thenReturn(watchKey);

		watcher.register(dir);
		assertThat(watcher.getKeyPaths().get(watchKey), equalTo(dir));
	}

	@Test(expected = WoodException.class)
	public void register_IOException() throws IOException {
		Path dir = mock(Path.class);
		when(dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY)).thenThrow(IOException.class);
		watcher.register(dir);
	}
}
