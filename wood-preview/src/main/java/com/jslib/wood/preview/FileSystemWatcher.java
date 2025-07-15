package com.jslib.wood.preview;

import com.jslib.wood.WoodException;
import com.jslib.wood.util.StringsUtil;
import jakarta.servlet.ServletContext;
import jakarta.servlet.ServletContextEvent;
import jakarta.servlet.ServletContextListener;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.nio.file.*;
import java.nio.file.WatchEvent.Kind;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicBoolean;

import static java.nio.file.LinkOption.NOFOLLOW_LINKS;
import static java.nio.file.StandardWatchEventKinds.*;

/**
 * File system watcher for changes on project files. All change events are included: creation, modifications and deletions. This
 * class uses {@link EventsManager#pushEvent(com.jslib.wood.lang.Event)} to actually send {@link FileSystemEvent} to all connected clients,
 * via {@link EventsServlet}.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
public class FileSystemWatcher implements ServletContextListener, Runnable {
    private static final Logger log = LoggerFactory.getLogger(FileSystemWatcher.class);

    private static final String EXCLUDE_DIRS_PARAM = "EXCLUDE_DIRS";

    private static final long TIMESTAMP_THRESHOLD = 500;

    /**
     * File system watch service.
     */
    private final WatchService watchService;
    /**
     * Keeps track of all registered directories. Used to register newly created directories and to unregister watch keys.
     */
    private final Map<WatchKey, Path> keyPaths;

    /**
     * Watch service events loop.
     */
    private final Thread thread;
    /**
     * Thread active flag.
     */
    private final AtomicBoolean running;

    /**
     * Manager for client blocking events queues. Watch events are send to this manager for push to connected clients.
     */
    private final EventsManager eventsManager;

    private final List<Path> excludes;

    /**
     * Construct file system watcher.
     */
    public FileSystemWatcher() throws IOException {
        log.trace("FileSystemWatcher()");
        this.watchService = FileSystems.getDefault().newWatchService();
        this.keyPaths = new HashMap<>();
        this.thread = new Thread(this);
        this.running = new AtomicBoolean();
        this.eventsManager = EventsManager.instance();
        this.excludes = new ArrayList<>();
    }

    /**
     * Register all project directories and start watch service events loop.
     */
    @Override
    public void contextInitialized(ServletContextEvent contextEvent) {
        log.trace("contextInitialized(ServletContextEvent contextEvent)");
        ServletContext servletContext = contextEvent.getServletContext();

        String projectDirParam = servletContext.getInitParameter(PreviewServlet.PROJECT_DIR_PARAM);
        if(projectDirParam == null) {
            log.warn("Missing servlet init parameter {}; abort file system watcher", PreviewServlet.PROJECT_DIR_PARAM);
            return;
        }
        Path projectDir = Paths.get(projectDirParam);
        if(!Files.isDirectory(projectDir)) {
            log.warn("Servlet init parameter {} is not an existing directory; abort file system watcher", projectDirParam);
            return;
        }

        String excludeDirsParam = servletContext.getInitParameter(EXCLUDE_DIRS_PARAM);
        if (excludeDirsParam != null) {
            for (String excludeDir : StringsUtil.split(excludeDirsParam, ',')) {
                excludes.add(projectDir.resolve(excludeDir));
            }
        }

        try {
            Files.walkFileTree(projectDir, new SimpleFileVisitor<Path>() {
                @Override
                public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) {
                    if (isExcluded(dir)) {
                        return FileVisitResult.SKIP_SUBTREE;
                    }
                    register(dir);
                    return FileVisitResult.CONTINUE;
                }
            });
        } catch (IOException e) {
            log.error("Fail to initialize watch service: {}: {}", e.getClass(), e.getMessage(), e);
        }

        running.set(true);
        thread.start();
    }

    boolean isExcluded(Path dir) {
        return excludes.contains(dir) || dir.getFileName().toString().startsWith(".");
    }

    /**
     * Stop watch service events loop and unregister directories.
     */
    @Override
    public void contextDestroyed(ServletContextEvent servletContextEvent) {
        log.trace("contextDestroyed(ServletContextEvent servletContextEvent)");
        running.set(false);
        thread.interrupt();

        for (WatchKey key : keyPaths.keySet()) {
            key.cancel();
        }
        try {
            watchService.close();
            log.debug("File system watcher closed");
        } catch (IOException e) {
            log.error("Fail to close watch service: {}: {}", e.getClass(), e.getMessage(), e);
        }
    }

    /**
     * Watch service events handling. Takes care to add newly created directories.
     */
    @Override
    public void run() {
        log.trace("run()");

        long lastEventTimestamp = 0;
        while (running.get()) {
            WatchKey key;
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

        log.debug("Exit watcher thread");
    }

    /**
     * Register directory to watcher service, for all supported events. Also takes care to cache watch key to {@link #keyPaths}.
     *
     * @param dir directory path.
     */
    void register(Path dir) {
        log.trace("register(Path dir)");
        try {
            WatchKey key = dir.register(watchService, ENTRY_CREATE, ENTRY_DELETE, ENTRY_MODIFY);
            log.debug("Register watcher for directory {}", dir);
            keyPaths.put(key, dir);
        } catch (IOException e) {
            throw new WoodException(e);
        }
    }

    /**
     * Cast generic watch event to specialized one with warnings suppressed.
     */
    @SuppressWarnings("unchecked")
    private static <T> WatchEvent<T> cast(WatchEvent<?> event) {
        return (WatchEvent<T>) event;
    }

    // --------------------------------------------------------------------------------------------
    // Test support

    FileSystemWatcher(WatchService watchService, Thread thread, EventsManager eventsManager) {
        log.trace("FileSystemWatcher(WatchService watchService, Thread thread, EventsManager eventsManager)");
        this.watchService = watchService;
        this.keyPaths = new HashMap<>();
        this.thread = thread;
        this.running = new AtomicBoolean();
        this.eventsManager = eventsManager;
        this.excludes = new ArrayList<>();
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
