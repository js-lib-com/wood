package js.wood;

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
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.lang.BugError;
import js.util.Files;
import js.util.Strings;
import js.wood.impl.FileType;
import js.wood.impl.Variants;

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
 * extension    = 2*3(ALPHA / DIGIT)
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
public class FilePath extends Path {
	// res/template/progress-bar/progress-bar_ro_desktop.htm
	// ---------- path --------|-|-- name --|-|variants|-|ext

	/** Pattern for file path accordingly syntax described by class description. */
	private static final Pattern PATTERN = Pattern.compile("^" + //
			"((?:[a-z]+)/(?:[a-z0-9-]+/)*)?" + // directory path
			"([a-z0-9-\\.]+)" + // base name is file name without variants or extension
			"(?:_([a-z0-9][a-z0-9-_]*))?" + // variants
			"\\.([a-z0-9]{2,5})" + // file extension
			"$", //
			Pattern.CASE_INSENSITIVE);

	/** This file parent directory. */
	private final DirPath parentDirPath;

	/** File base name is the file name without variants and extension. Leading file separator is not included. */
	private final String basename;

	/** File name including extension but no trailing file separator nor variants. */
	private final String name;

	/** Optional variants, empty if file path has none. */
	private final Variants variants;

	/** File type. */
	private final FileType fileType;

	/**
	 * Create immutable path instance from a given path value.
	 * 
	 * @param project project reference,
	 * @param filePath file path value.
	 */
	public FilePath(Project project, String filePath) {
		super(project, filePath);

		Matcher matcher = PATTERN.matcher(filePath);
		if (!matcher.find()) {
			throw new WoodException("Invalid file path |%s|.", filePath);
		}

		this.parentDirPath = matcher.group(1) != null ? new DirPath(project, matcher.group(1)) : project.getProjectDir();
		this.basename = matcher.group(2);
		String extension = matcher.group(4);
		this.name = Strings.concat(this.basename, '.', extension);
		this.variants = new Variants(this, matcher.group(3));
		this.fileType = FileType.forExtension(extension.toLowerCase());
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
	 * @see #name
	 */
	public String getName() {
		return name;
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
	 * Get this file parent directory.
	 * 
	 * @return parent directory.
	 * @see #parentDirPath
	 */
	public DirPath getParentDirPath() {
		return parentDirPath;
	}

	/**
	 * Get base name, that is, the file name without extension and variants.
	 * 
	 * @return file base name.
	 * @see #basename
	 */
	public String getBaseName() {
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
	 * Test if this file is a component descriptor. A component descriptor is a XML file that has base name the same as
	 * component name; by convention component has the same name as its directory.
	 * 
	 * @return true if this file is a component descriptor.
	 */
	public boolean isComponentDescriptor() {
		return fileType == FileType.XML && basename.equals(parentDirPath.getName());
	}

	/**
	 * Test if this file is resource variables definition. A variables definition file has XML extension but not the same base
	 * name as parent directory. Note that descriptors and variables have both XML extension and differ only by base name.
	 * 
	 * @return true if this file is resource variables.
	 */
	public boolean isVariables() {
		return fileType == FileType.XML && !basename.equals(parentDirPath.getName());
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
		return name.equalsIgnoreCase(CT.PREVIEW_SCRIPT);
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

	public void copyTo(OutputStream stream) throws IOException {
		Files.copy(file, stream);
	}

	public String load() throws IOException {
		return Strings.load(getReader());
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
	 * Test if path value is acceptable for file path instance creation.
	 * 
	 * @param path path value.
	 * @return true if path value match file pattern.
	 */
	public static boolean accept(String path) {
		Matcher matcher = PATTERN.matcher(path);
		return matcher.find();
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
		this(project, Files.getRelativePath(project.getProjectRoot(), file, true));
	}
}
