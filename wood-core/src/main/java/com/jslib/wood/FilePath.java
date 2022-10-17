package com.jslib.wood;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.io.UnsupportedEncodingException;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;
import java.util.function.Predicate;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.jslib.lang.BugError;
import com.jslib.util.Files;
import com.jslib.util.Params;
import com.jslib.util.Strings;
import com.jslib.wood.impl.FileType;
import com.jslib.wood.impl.FilesHandler;
import com.jslib.wood.impl.Variants;

/**
 * File path identify project files, usually component files like layout, styles and scripts. A file path is a standard Java
 * file with couple syntax constrains: it always uses slash (/) for file separator, directory path is optional and file name
 * supports variants. Variants qualify file path so that one can create group of files with the same semantic content but
 * differently presented, e.g. string variables for multi-locale support.
 * <p>
 * Currently supported variants are locale and media queries for style files. See {@link Variants} class.
 * <p>
 * Characters used by path segments and file base name are US-ASCII alphanumeric characters, dash and dot. Dot is allowed for
 * file names with version, for example <code>js-lib-1.2.3.js</code>. Last dot is for extension. Underscore is reserved for
 * variants separator and is not valid in names.
 * 
 * <pre>
 * file-path    = *path-segment base-name *variant DOT extension 
 * path-segment = 1*CH F-SEP
 * base-name    = 1*CH
 * variant      = V-SEP 1*(ALPHA / DIGIT / "-")
 * extension    = 1*(ALPHA / DIGIT)
 * 
 * ; terminal symbols definition
 * F-SEP = "/"                         ; file separator is always slash
 * V-SEP = "_"                         ; variant separator is always underscore
 * DOT   = "."                         ; dot as file extension separator
 * CH    = ALPHA / DIGIT / "-" / "."   ; character is US-ASCII alphanumeric, dash and dot
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * File path has no mutable state and is thread safe.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class FilePath implements Iterable<FilePath> {
	/** Path separator is always slash. Note that this path separator is also used by URL path related logic. */
	public static final String SEPARATOR = "/";
	/** Convenient path separator represented as a character. */
	public static final char SEPARATOR_CHAR = '/';

	private static final Pattern DIRECTORY_PATTERN = Pattern.compile("^" + //
			"((?:[a-z0-9-]+/)*)" + // (1) optional parent directory path; if exists it has trailing path separator
			"(?:([a-z0-9-\\.]+)/?)" + // (2) directory name is the last path segment
			"$", //
			Pattern.CASE_INSENSITIVE);

	private static final Pattern FILE_PATTERN = Pattern.compile("^" + //
			"((?:[a-z0-9-]+/)*)" + // (1) optional parent directory path; if exists it has trailing path separator
			"([a-z0-9-\\.]+?)" + // (2) file base name does not include variants or extension
			"(?:_([a-z0-9][a-z0-9-_]*))?" + // (3) optional file variants
			"(?:\\.([a-z0-9]+))" + // (4) file extension
			"$", //
			Pattern.CASE_INSENSITIVE);

	/** Parent project reference. All paths are always relative to this parent project. */
	protected final Project project;

	/** Path value relative to project root. If this path is a directory it always ends with path separator. */
	private final String value;

	private final String parentPath;

	private final String parentName;

	/** Path segments relative to project root. */
	private final List<String> pathSegments;

	/** File base name is the file name without variants and extension. Leading file separator is not included. */
	private final String basename;

	/** File name including extension, if the case, but no trailing file separator nor variants. */
	private final String fileName;

	/** Optional variants, empty if file path has none. */
	private final Variants variants;

	/** File type. */
	private final FileType fileType;

	/** Wrapped Java file include project root. */
	protected File file;

	/**
	 * Create immutable path instance from a given path value.
	 * 
	 * @param project project reference,
	 * @param value file path value.
	 */
	public FilePath(Project project, String value) {
		Params.notNull(project, "Project");
		Params.notNullOrEmpty(value, "Path value");

		this.project = project;
		this.pathSegments = new ArrayList<>(Strings.split(value, FilePath.SEPARATOR_CHAR));

		Matcher matcher = FILE_PATTERN.matcher(value);
		boolean directory = false;
		if (!matcher.find()) {
			directory = true;
			matcher = DIRECTORY_PATTERN.matcher(value);
			if (!matcher.find()) {
				throw new WoodException("Invalid file path |%s|.", value);
			}
		}

		if (directory && !value.equals(".") && !value.endsWith(SEPARATOR)) {
			value += SEPARATOR;
		}
		this.value = value;

		this.parentPath = matcher.group(1);
		this.parentName = this.parentPath.isEmpty() ? "" : pathSegments.get(pathSegments.size() - 2);
		this.basename = matcher.group(2);

		if (!directory) {
			this.variants = new Variants(this, matcher.group(3));
			String extension = matcher.group(4);
			this.fileName = Strings.concat(this.basename, '.', extension);
			this.fileType = FileType.forExtension(extension);
		} else {
			this.variants = new Variants(this, null);
			this.fileName = this.basename;
			this.fileType = FileType.NONE;
		}

		this.file = value.equals(".") ? project.getProjectRoot() : new File(project.getProjectRoot(), value);
	}

	/**
	 * Get parent project in which this path is declared.
	 * 
	 * @return parent project.
	 * @see #project
	 */
	public Project getProject() {
		return project;
	}

	/**
	 * Get this path value. Note that returned value is always relative to project root and never starts with slash separator.
	 * 
	 * @return path value.
	 * @see #value
	 */
	public String value() {
		return value;
	}

	/**
	 * Test if this path denotes an existing entity. This predicate simply check that wrapped Java {@link #file} exists.
	 * 
	 * @return true if entity exists.
	 */
	public boolean exists() {
		return file.exists();
	}

	/**
	 * Get file type.
	 * 
	 * @return file type.
	 * @see #fileType
	 */
	public FileType getType() {
		return fileType;
	}

	/**
	 * Guess MIME type for this file path. Note that returned value is not <code>Content-Type</code>, therefore it does not
	 * contain characters set.
	 * 
	 * @return MIME type.
	 * @throws IOException
	 */
	public String getMimeType() throws IOException {
		if (fileType == FileType.SCRIPT) {
			// Java NIO returns "text/plain"
			return "application/javascript";
		}
		return java.nio.file.Files.probeContentType(file.toPath());
	}

	/**
	 * Get this file name with extension but not variants.
	 * 
	 * @return file name.
	 * @see #fileName
	 */
	public String getName() {
		return value.isEmpty() ? "" : fileName;
	}

	/**
	 * Test if this file path has variants.
	 * 
	 * @return true if this file path has variants.
	 */
	public boolean hasVariants() {
		return !variants.isEmpty();
	}

	/**
	 * Get this file variants, possible empty.
	 * 
	 * @return this file variants.
	 * @see #variants
	 */
	public Variants getVariants() {
		return variants;
	}

	/**
	 * Get this file parent directory or null if file is in project root.
	 * 
	 * @return parent directory, possible null.
	 * @see #parentDir
	 */
	public FilePath getParentDir() {
		return parentPath.isEmpty() ? null : new FilePath(project, parentPath);
	}

	/**
	 * Get base name, that is, the file name without extension and variants.
	 * 
	 * @return file base name.
	 * @see #basename
	 */
	public String getBasename() {
		return basename;
	}

	/**
	 * Test if this file base name equals given name. Base name is file name without variants and extension.
	 * 
	 * @param name name to compare with.
	 * @return true if this file base name equals given name.
	 * @see #basename
	 */
	public boolean hasBaseName(String name) {
		return basename.equals(name);
	}

	/**
	 * Test if this directory path is the project root.
	 * 
	 * @return true if this directory path is the project root.
	 */
	public boolean isProjectRoot() {
		return value.equals(".");
	}

	/**
	 * Test if this file path denotes an existing directory.
	 * 
	 * @return true if this file path is an existing directory.
	 */
	public boolean isDirectory() {
		return file.isDirectory();
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
	 * Test if this file is a component descriptor. A component descriptor is a XML file that has base name the same as
	 * component name; by convention component has the same name as its directory.
	 * 
	 * @return true if this file is a component descriptor.
	 */
	public boolean isComponentDescriptor() {
		return fileType == FileType.XML && basename.equals(parentName);
	}

	/**
	 * Test if this file is resource variables definition. A variables definition file has XML extension but not the same base
	 * name as parent directory. Note that descriptors and variables have both XML extension and differ only by base name.
	 * 
	 * @return true if this file is resource variables.
	 */
	public boolean isVariables() {
		return fileType == FileType.XML && !basename.equals(parentName);
	}

	/**
	 * Test if this file is a component layout file.
	 * 
	 * @return true if this file is a layout file.
	 */
	public boolean isLayout() {
		return fileType == FileType.LAYOUT;
	}

	/**
	 * Test if this file designates a style sheet, be it component or theme styles.
	 * 
	 * @return true if this file is style sheet.
	 */
	public boolean isStyle() {
		return fileType == FileType.STYLE;
	}

	/**
	 * Test if this file is a script file.
	 * 
	 * @return true if this file is a script file.
	 */
	public boolean isScript() {
		return fileType == FileType.SCRIPT;
	}

	/**
	 * Test if this file is script preview file. Preview script is part of component sources used solely for preview; it is not
	 * part of component distribution.
	 * 
	 * @return true if this file is script preview.
	 */
	public boolean isPreviewScript() {
		return fileName.equalsIgnoreCase(CT.PREVIEW_SCRIPT);
	}

	/**
	 * Test if this file is a media file.
	 * 
	 * @return true if this file is media file.
	 */
	public boolean isMedia() {
		return fileType == FileType.MEDIA;
	}

	public boolean isXML(String... roots) {
		if (fileType != FileType.XML) {
			return false;
		}
		if (roots.length == 0) {
			// if there are no 'roots' arguments accept all XML files
			return true;
		}
		try (BufferedReader reader = new BufferedReader(getReader())) {
			String line = reader.readLine();
			if (line.startsWith("<?")) {
				line = reader.readLine();
			}
			for (String root : roots) {
				if (line.startsWith(Strings.concat('<', root, '>'))) {
					return true;
				}
			}
			return false;
		} catch (IOException ignore) {
			return false;
		}
	}

	public boolean isManifest() {
		return CT.MANIFEST_FILE.equals(value);
	}

	public boolean isSynthetic() {
		return fileType == FileType.VAR;
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
	 * Get the path of a child file with a given name.
	 * 
	 * @param fileName the name of child file.
	 * @return path of child file.
	 */
	public FilePath getFilePath(String fileName) {
		return createFilePath(fileName);
	}

	/**
	 * Get the path of named child directory.
	 * 
	 * @param path sub-directory path.
	 * @return path of child directory.
	 */
	public FilePath getSubdirPath(String path) {
		if (!path.endsWith(FilePath.SEPARATOR)) {
			path += FilePath.SEPARATOR_CHAR;
		}
		return createFilePath(path);
	}

	private FilePath createFilePath(String name) {
		return project.createFilePath(value + name);
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
				handler.onDirectory(project.createFilePath(file));
				continue;
			}

			FilePath filePath = project.createFilePath(file);
			if (!handler.accept(filePath)) {
				continue;
			}

			handler.onFile(filePath);
		}
	}

	/**
	 * Clone this file path but forces the type to style. Returned file differs only by its type.
	 * 
	 * @return style file.
	 */
	public FilePath cloneTo(FileType type) {
		return new FilePath(project, Files.replaceExtension(value(), type.extension()));
	}

	public void copyTo(FilePath target) throws IOException {
		Files.copy(file, target.file);
	}

	public void copyTo(Writer writer) throws IOException {
		Files.copy(getReader(), writer);
	}

	public void copyTo(OutputStream outputStream) throws IOException {
		Files.copy(file, outputStream);
	}

	public String load() throws IOException {
		return Strings.load(getReader());
	}

	public Properties properties() {
		Properties properties = new Properties();
		try (Reader reader = getReader()) {
			properties.load(reader);
		} catch (IOException e) {
			throw new WoodException(e);
		}
		return properties;
	}

	/**
	 * Get a reader usable for this file content. Returns a new created reader instance; it is caller responsibility to close
	 * it.
	 * 
	 * @return newly created reader.
	 * @throws WoodException if this file does not exist on file system.
	 */
	public Reader getReader() {
		try {
			// uses InputStreamReader with explicit encoding to avoid using system default encoding used by FileReader
			return new InputStreamReader(new FileInputStream(file), "UTF-8");
		} catch (UnsupportedEncodingException e) {
			// fatal condition: JVM without UTF-8 support
			throw new BugError(e);
		} catch (FileNotFoundException e) {
			throw new WoodException(e);
		}
	}

	/**
	 * Access to underlying Java file.
	 * 
	 * @return underlying file.
	 */
	public File toFile() {
		return file;
	}

	/**
	 * Instance string representation returns the same value as {@link #value()}.
	 * 
	 * @return path string representation.
	 */
	@Override
	public String toString() {
		return value;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((value == null) ? 0 : value.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (!getClass().isAssignableFrom(obj.getClass()))
			return false;
		FilePath other = (FilePath) obj;
		if (value == null) {
			if (other.value != null)
				return false;
		} else if (!value.equals(other.value))
			return false;
		return true;
	}

	/**
	 * Test if path value is acceptable for file path instance creation.
	 * 
	 * @param path path value.
	 * @return true if path value match file pattern.
	 */
	public static boolean accept(String path) {
		Matcher matcher = FILE_PATTERN.matcher(path);
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
			return project.createFilePath(files[index]);
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

	// --------------------------------------------------------------------------------------------
	// Test support

	/**
	 * Test constructor.
	 * 
	 * @param project WOOD project context,
	 * @param file underlying Java file.
	 */
	FilePath(Project project, File file) {
		this(project, file.getPath().replace('\\', '/'));
		this.file = file;
	}
}
