package js.wood;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.util.Files;
import js.util.Params;
import js.util.Strings;
import js.wood.impl.FilesHandler;

/**
 * Directory path relative to project root. Directory path is always relative to project root and this class provides methods to
 * retrieve and iterate child files.
 * <p>
 * A directory path is a sequence of path segments separated by file separator. First segment is known as
 * <code>source directory</code>. File separator is always slash (/) and trailing file separator is mandatory. Since directory
 * path is relative to project root leading file separator is not accepted. Characters for name and path segment are those
 * accepted by {@link Path} class: US-ASCII alphanumeric and dash (-). Note that underscore (_) is not supported since is used
 * for variants separator.
 * 
 * <pre>
 * dir-path     = 1*(path-segment SEP) 
 * path-segment = 1*CH
 * 
 * ; terminal symbols definition
 * SEP = "/"                 ; file separator is always forward slash
 * CH  = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * Directory path implements {@link Iterable<FilePath>} interface that allows to use it in for-each Java loop, see code snippet.
 * 
 * <pre>
 * for (FilePath file : dir) {
 * 	// handle file path
 * }
 * </pre>
 * <p>
 * Directory path instance has no mutable state and is thread safe.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class DirPath extends Path implements Iterable<FilePath> {
	/** Pattern for directory path. */
	private static final Pattern PATTERN = Pattern.compile("^(?:[a-z0-9-]+/)+?$", Pattern.CASE_INSENSITIVE);

	/** Directory path segments. */
	private final List<String> pathSegments;

	/**
	 * Create root directory.
	 * 
	 * @param project WOOD project context.
	 * @throws IllegalArgumentException if project is null.
	 */
	public DirPath(Project project) {
		super(project);
		pathSegments = Collections.emptyList();
	}

	/**
	 * Create an immutable directory path instance. Directory path value should be relative to project root. See {@link DirPath}
	 * class description for directory path syntax.
	 * 
	 * @param project WOOD project context,
	 * @param value directory path value, relative to project root.
	 * @throws WoodException if <code>path</code> syntax is not valid.
	 * @throws IllegalArgumentException if project is null or path value is null or empty.
	 */
	public DirPath(Project project, String value) throws WoodException {
		super(project, value);
		Matcher matcher = PATTERN.matcher(value);
		if (!matcher.find()) {
			throw new WoodException("Directory path parameter |%s| is invalid.", value);
		}
		pathSegments = new ArrayList<>(Strings.split(value, Path.SEPARATOR));
	}

	/**
	 * Test constructor. Initialize directory path instance from Java directory.
	 * 
	 * @param project WOOD project context.
	 * @param dir Java directory.
	 */
	DirPath(Project project, File dir) {
		super(project, dir);
		pathSegments = new ArrayList<>(Strings.split(dir.getPath(), Path.SEPARATOR));
	}

	/**
	 * Get this directory path segments. Returned list is unmodifiable.
	 * 
	 * @return directory path segments.
	 * @see #pathSegments
	 */
	public List<String> getPathSegments() {
		return Collections.unmodifiableList(pathSegments);
	}

	/**
	 * Get directory name. Returned value has not leading or trailing file separator.
	 * 
	 * @return this directory name.
	 */
	public String getName() {
		return file.getName();
	}

	/**
	 * Get the path of a child file with a given name.
	 * 
	 * @param fileName the name of child file.
	 * @return path of child file.
	 */
	public FilePath getFilePath(String fileName) {
		return new FilePath(project, value + fileName);
	}

	/**
	 * Get the path of named child directory.
	 * 
	 * @param path sub-directory path.
	 * @return path of child directory.
	 */
	public DirPath getSubdirPath(String path) {
		String dir = value + path;
		if (!dir.endsWith(Path.SEPARATOR)) {
			dir += Path.SEPARATOR;
		}
		return new DirPath(project, dir);
	}

	/**
	 * Directory path implements {@link Iterable<FilePath>} interface that allows to use it in for-each Java loop, see code
	 * snippet.
	 * 
	 * <pre>
	 * for (FilePath file : dir) {
	 * 	// handle file path
	 * }
	 * </pre>
	 * 
	 * Returned iterator instance handle only direct child files. Note that child sub-directories are not included. Also hidden
	 * files are excluded; a hidden file is one that starts with dot, e.g. <code>.gitignore</code>.
	 * <p>
	 * There is no guarantees about a particular iteration order.
	 * 
	 * @return file iterator instance.
	 * @see FilesIterator
	 */
	@Override
	public Iterator<FilePath> iterator() {
		if (!exists()) {
			return Collections.emptyIterator();
		}
		File[] files = file.listFiles();
		return files != null ? new FilesIterator(files) : Collections.emptyIterator();
	}

	/**
	 * Collect child files that are accepted by given predicate. Returned files list can be empty if there is no match.
	 * 
	 * @param predicate predicate to test child files.
	 * @return found child files list, possible empty.
	 */
	public List<FilePath> filter(Predicate<FilePath> predicate) {
		List<FilePath> files = new ArrayList<>();
		for (FilePath file : this) {
			if (predicate.test(file)) {
				files.add(file);
			}
		}
		return files;
	}

	/**
	 * Get first child file that satisfy given predicate or null if not found. Use this finder to locate files with unique
	 * properties. Since files iteration is not guaranteed, if there are multiple files accepted by predicate, there is no
	 * guarantee about which file is returned.
	 * 
	 * @param predicate predicate to test child file.
	 * @return child file accepted by predicate or null.
	 */
	public FilePath findFirst(Predicate<FilePath> predicate) {
		for (FilePath file : this) {
			if (predicate.test(file)) {
				return file;
			}
		}
		return null;
	}

	/**
	 * Iterate over all this directory direct children, both files and sub-directories. Please note that iteration order is not
	 * guaranteed. Hidden directories and files are excluded; a hidden directory or file has a name that starts with a dot.
	 * <p>
	 * Files handler instance does not need to override all handler methods as in sample below. In fact is common case to
	 * override only {@link FilesHandler#onFile(FilePath)}.
	 * 
	 * <pre>
	 * dir.files(new FilesHandler() {
	 * 	&#064;Override
	 * 	public void onDirectory(DirPath dir) throws Exception {
	 * 		// recursive scanning of sub-directories
	 * 	}
	 * 
	 * 	&#064;Override
	 * 	public boolean accept(FilePath file) {
	 * 		// test file attributes and decide if acceptable
	 * 	}
	 * 
	 * 	&#064;Override
	 * 	public void onFile(FilePath file) throws Exception {
	 * 		// handle file
	 * 	}
	 * });
	 * </pre>
	 * 
	 * @param handler files handler.
	 */
	public void files(FilesHandler handler) {
		if (!exists()) {
			return;
		}

		File[] files = file.listFiles();
		if (files == null) {
			throw new WoodException("Cannot list files from directory |%s|.", file);
		}
		
		for (File file : file.listFiles()) {
			// ignores hidden files and directories
			if (file.getName().charAt(0) == '.') {
				continue;
			}

			if (file.isDirectory()) {
				// takes care to create DirPath relative to project
				String dir = Files.getRelativePath(project.getProjectRoot(), file, true);
				if (!dir.endsWith(Path.SEPARATOR)) {
					dir += Path.SEPARATOR;
				}
				handler.onDirectory(new DirPath(project, dir));
				continue;
			}

			FilePath filePath = new FilePath(project, Files.getRelativePath(project.getProjectRoot(), file, true));
			if (!handler.accept(filePath)) {
				continue;
			}

			handler.onFile(filePath);
		}
	}

	/**
	 * Test if this directory path is excluded from build process.
	 * 
	 * @return true if this directory path is excluded from build.
	 */
	public boolean isExcluded() {
		return project.getExcludes().contains(this);
	}

	/**
	 * Test if this directory path is a HTML component.
	 * 
	 * @return true if this directory path is a HTML component.
	 */
	public boolean isComponent() {
		File layoutFile = new File(file, getName() + CT.DOT_LAYOUT_EXT);
		return layoutFile.exists();
	}

	/**
	 * Test if this directory path is the project root.
	 * 
	 * @return true if this directory path is the project root.
	 */
	public boolean isRoot() {
		return value.equals(".");
	}

	/**
	 * Test if path value is acceptable for directory path instance creation.
	 * 
	 * @param path path value.
	 * @return true if path value match directory pattern.
	 */
	public static boolean accept(String path) {
		Matcher matcher = PATTERN.matcher(path);
		return matcher.find();
	}

	// --------------------------------------------------------------------------------------------

	/**
	 * Iterator over array of files in natural, incremental order. Please note that this iterator is flat, that is, it does not
	 * enter sub-directories. In fact directories are ignored. Hidden files are also ignored; a hidden file has a name that
	 * starts with a dot, e.g. <code>.gitignore</code>.
	 * <p>
	 * Be aware that this implementation mandates standard iterator pattern, see sample code. Trying to call {@link #next()}
	 * without {@link #hasNext()} will render not predictable behavior.
	 * 
	 * <pre>
	 * FileIterator it = new FilesIterator(dir.listFiles());
	 * while (it.hasNext()) {
	 * 	it.next();
	 * }
	 * </pre>
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private class FilesIterator implements Iterator<FilePath> {
		/** Directory files. */
		private final File[] files;

		/** Current processing index updated by {@link #hasNext()}. */
		private int index;

		/**
		 * Construct iterator instance for given files list, preparing {@link #index} for {@link #hasNext()} invocation.
		 * 
		 * @param files files list.
		 */
		public FilesIterator(File[] files) {
			Params.notNull(files, "Files list");
			this.files = files;
			this.index = -1;
		}

		/**
		 * Test if there are more files to iterate and update {@link #index} for current file handling via {@link #next()}.
		 * 
		 * @return true if there are more files to iterate.
		 */
		@Override
		public boolean hasNext() {
			for (++index; index < files.length; ++index) {
				if (files[index].isFile() && files[index].getName().charAt(0) != '.') {
					return true;
				}
			}
			return false;
		}

		/**
		 * Uses {@link #index} to retrieve current file and returns it wrapped by {@link FilePath} instance.
		 * <p>
		 * Implementation note: this method uses {@link #index} to retrieve current file but does not increment it. This method
		 * relies on {@link #hasNext()} for index increment.
		 * 
		 * @return current file.
		 */
		@Override
		public FilePath next() {
			return new FilePath(project, Files.getRelativePath(project.getProjectRoot(), files[index], true));
		}

		/**
		 * Remove operation is not supported.
		 * 
		 * @throws UnsupportedOperationException remove not supported.
		 */
		@Override
		public void remove() {
			throw new UnsupportedOperationException();
		}
	}
}
