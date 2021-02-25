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
import js.wood.impl.FileType;
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
 * Directory path has no mutable state and is thread safe.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class DirPath extends Path {
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

	public List<FilePath> filter(Predicate<FilePath> predicate) {
		List<FilePath> files = new ArrayList<>();
		for (File file : listFiles()) {
			if (file.getName().charAt(0) == '.') {
				continue;
			}
			if (file.isDirectory()) {
				continue;
			}

			FilePath filePath = new FilePath(project, file);
			if (predicate.test(filePath)) {
				files.add(filePath);
			}
		}
		return files;
	}

	public FilePath findFirst(Predicate<FilePath> predicate) {
		for (File file : listFiles()) {
			if (file.getName().charAt(0) == '.') {
				continue;
			}
			if (file.isDirectory()) {
				continue;
			}

			FilePath filePath = new FilePath(project, file);
			if (predicate.test(filePath)) {
				return filePath;
			}
		}
		return null;
	}

	private File[] listFiles() {
		if(!exists()) {
			return new File[0];
		}
		return file.listFiles();
	}
	
	/**
	 * Get a new iterable instance on this directory child files. Note that child sub-directories are not included. Also hidden
	 * files are excluded; a hidden file is one that starts with dot, e.g. <code>.gitignore</code>.
	 * <p>
	 * This method is designed to work with for-each Java loop as in snippet below.
	 * 
	 * <pre>
	 * for (FilePath file : dir.files()) {
	 * 	// handle file path
	 * }
	 * </pre>
	 * 
	 * @return file iteratble instance.
	 */
	public Iterable<FilePath> files() {
		if (!exists()) {
			return Collections.emptyList();
		}
		return new FilesIterable(file);
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
		files(null, handler);
	}

	/**
	 * Iterate over direct children, both sub-directories and files, but only files of specified type. This method behaves the
	 * same as {@link #files(FilesHandler)} but list only files of specified type. If {@link FilesHandler#accept(FilePath)} is
	 * implemented its results uses AND logic with file type filtering. Also hidden directories and files are ignored. Iteration
	 * order is not guaranteed.
	 * <p>
	 * If attempt to invoke it on a not existing directory, this method does nothing.
	 * <p>
	 * Files handler anonymous instance does not need to override all handler methods as in sample below. For child files
	 * processing only, one can override just {@link FilesHandler#onFile(FilePath)}.
	 * 
	 * <pre>
	 * dir.files(FileType.LAYOUT, new FilesHandler() {
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
	 * @param fileType files type filter,
	 * @param handler files handler.
	 */
	public void files(FileType fileType, FilesHandler handler) {
		if (!exists()) {
			return;
		}

		try {
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
				// if file type is null accept all files
				if (fileType == null || FileType.forFile(file) == fileType) {
					handler.onFile(filePath);
				}
			}
		} catch (Exception e) {
			throw (e instanceof WoodException) ? (WoodException) e : new WoodException(e);
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

	// ------------------------------------------------------
	// Internal utility classes.

	/**
	 * Iterable over files from a directory.
	 * 
	 * @author Iulian Rotaru
	 * @since 1.0
	 */
	private class FilesIterable implements Iterable<FilePath> {
		/** Files iterator. */
		private final Iterator<FilePath> iterator;

		/**
		 * Construct file iterable instance for files from requested directory.
		 * 
		 * @param dir directory to iterate.
		 */
		public FilesIterable(File dir) {
			File[] files = dir.listFiles();
			if (files == null) {
				iterator = Collections.emptyIterator();
			} else {
				iterator = new FilesIterator(files);
			}
		}

		/**
		 * Retrieve files iterator instance.
		 * 
		 * @return files iterator instance.
		 */
		@Override
		public Iterator<FilePath> iterator() {
			return iterator;
		}
	}

	/**
	 * Iterator over array of files in natural, incremental order. Please note that this iterator is flat, that is, it does not
	 * enter sub-directories. In fact directories are ignored.
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
				if (files[index].isFile()) {
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
