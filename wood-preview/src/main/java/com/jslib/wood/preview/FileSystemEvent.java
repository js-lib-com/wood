package com.jslib.wood.preview;

import com.jslib.wood.lang.Event;

/**
 * File system event pushed by {@link EventsServlet} when {@link FileSystemWatcher} discover a change on a file from the
 * project.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class FileSystemEvent implements Event {
	private final String action;
	private final String file;

	public FileSystemEvent(String action, String file) {
		this.action = action;
		this.file = file;
	}

	@Override
	public String toString() {
		return "FileSystemEvent [action=" + action + ", file=" + file + "]";
	}
}
