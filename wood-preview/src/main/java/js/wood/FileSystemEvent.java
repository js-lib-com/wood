package js.wood;

import js.lang.Event;

/**
 * File system event pushed by {@link EventsServlet} when {@link FileSystemWatcher} discover a change on a file from the
 * project.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class FileSystemEvent implements Event {
	private final String file;
	private final String action;

	public FileSystemEvent(String file, String action) {
		this.file = file;
		this.action = action;
	}

	@Override
	public String toString() {
		return "FileSystemEvent [file=" + file + ", action=" + action + "]";
	}
}
