package com.jslib.wood.preview;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.ENTRY_CREATE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_DELETE;
import static java.nio.file.StandardWatchEventKinds.ENTRY_MODIFY;

import java.io.IOException;
import java.nio.file.FileSystems;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.WatchEvent;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.WatchKey;
import java.nio.file.WatchService;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import com.jslib.api.log.Log;
import com.jslib.api.log.LogFactory;
import com.jslib.util.Strings;
import com.jslib.wood.WoodException;

import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;

/**
 * File system watcher for changes on project files. All change events are included: creation, modifications and deletions. This
 * class uses {@link EventsManager#pushEvent(js.lang.Event)} to actually send {@link FileSystemEvent} to all connected clients,
 * via {@link EventsServlet}.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class FileSystemWatcher implements ServletContextListener, Runnable {
	/** Java serialization version. */
	private static final Log log = LogFactory.getLog(FileSystemWatcher.class);

	private static final String EXCLUDE_DIRS_PARAM = "EXCLUDE_DIRS";

	private static final long TIMESTAMP_THRESHOLD = 500;

	/** File system watch service. */
	private final WatchService watchService;
	/** Keeps track of all registered directories. Used to register newly created directories and to unregister watch keys. */
	private final Map<WatchKey, Path> keyPaths = new HashMap<>();

	/** Watch service events loop. */
	private final Thread thread;
	/** Thread active flag. */
	private final AtomicBoolean running = new AtomicBoolean();

	/** Manager for client blocking events queues. Watch events are send to this manager for push to connected clients. */
	private final EventsManager eventsManager;

	private final List<Path> excludes = new ArrayList<>();

	/** Construct file system watcher. */
	public FileSystemWatcher() throws IOException {
		log.trace("FileSystemWatcher()");
		this.watchService = FileSystems.getDefault().newWatchService();
		this.thread = new Thread(this);
		this.eventsManager = EventsManager.instance();
	}

	/** Register all project directories and start watch service events loop. */
	@Override
	public void contextInitialized(ServletContextEvent contextEvent) {
		log.trace("contextInitialized(ServletContextEvent)");
		ServletContext servletContext = contextEvent.getServletContext();

		Path projectDir = Paths.get(servletContext.getInitParameter(PreviewServlet.PROJECT_DIR_PARAM));
		String excludeDirsParam = servletContext.getInitParameter(EXCLUDE_DIRS_PARAM);
		if (excludeDirsParam != null) {
			for (String excludeDir : Strings.split(excludeDirsParam, ',')) {
				excludes.add(projectDir.resolve(excludeDir));
			}
		}

		try {
			Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {
				@Override
				public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
					if (isExcluded(dir)) {
						return FileVisitResult.SKIP_SUBTREE;
					}
					register(dir);
					return FileVisitResult.CONTINUE;
				}
			});
		} catch (IOException e) {
			log.error(e);
		}

		running.set(true);
		thread.start();
	}

	private boolean isExcluded(Path dir) {
		return excludes.contains(dir) || dir.getFileName().toString().startsWith(".");
	}

	/** Stop watch service events loop and unregister directories. */
	@Override
	public void contextDestroyed(ServletContextEvent sce) {
		log.trace("contextDestroyed(ServletContextEvent)");
		running.set(false);
		thread.interrupt();

		for (WatchKey key : keyPaths.keySet()) {
			key.cancel();
		}
		try {
			watchService.close();
			log.debug("File system watcher closed.");
		} catch (IOException e) {
			log.error(e);
		}
	}

	/** Watch service events handling. Takes care to add newly created directories. */
	@Override
	public void run() {
		log.trace("run()");

		long lastEventTimestamp = 0;
		while (running.get()) {
			WatchKey key = null;
			try {
				key = watchService.take();
			} catch (InterruptedException expected) {
				continue;
			}

			Path dir = keyPaths.get(key);
			if (dir != null) {
				for (WatchEvent<?> watchEvent : key.pollEvents()) {
					Kind<?> kind = watchEvent.kind();
					WatchEvent<Path> event = cast(watchEvent);

					Path path = dir.resolve(event.context());
					if (Files.isDirectory(path, NOFOLLOW_LINKS)) {
						if (isExcluded(path)) {
							continue;
						}
						if (kind == ENTRY_CREATE) {
							register(path);
						}
					}

					long currentTimestamp = System.currentTimeMillis();
					if (currentTimestamp - lastEventTimestamp > TIMESTAMP_THRESHOLD) {
						eventsManager.pushEvent(new FileSystemEvent(kind.name(), event.context().toString()));
						lastEventTimestamp = currentTimestamp;
					}
				}
			}

			key.reset();
		}

		log.debug("Exit watcher thread.");
	}

	/**
	 * Register directory to watcher service, for all supported events. Also takes care to cache watch key to {@link #keyPaths}.
	 * 
	 * @param dir directory path.
	 * @throws IOException if watcher registration fails.
	 */
	void register(Path dir) {
		try {
			WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
			log.debug("Register watcher for directory |%s|.", dir);
			keyPaths.put(key, dir);
		} catch (IOException e) {
			throw new WoodException(e);
		}
	}

	/** Cast generic watch event to specialized one with warnings suppressed. */
	@SuppressWarnings("unchecked")
	private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
		return (WatchEvent<T>) event;
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	FileSystemWatcher(WatchService watchService, Thread thread, EventsManager eventsManager) {
		this.watchService = watchService;
		this.thread = thread;
		this.eventsManager = eventsManager;
	}

	WatchService getWatchService() {
		return watchService;
	}

	Map<WatchKey, Path> getKeyPaths() {
		return keyPaths;
	}

	Thread getThread() {
		return thread;
	}

	AtomicBoolean getRunning() {
		return running;
	}

	void setRunning(boolean running) {
		this.running.set(running);
	}

	EventsManager getEventsManager() {
		return eventsManager;
	}
}
