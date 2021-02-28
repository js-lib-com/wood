package js.wood;

import java.io.File;

import js.util.Params;

/**
 * Path designates project entities like components, source and resource files and directories, descriptors, etc. It is an
 * immutable sequence of path segments and is always relative to {@link Project#getRootDir()}.
 * <p>
 * Path syntax depends on concrete implementation but resemble Java file: it has a sequence of parts, separated by slash (/).
 * Note that Path always uses slash (/) for separator, no matter JVM platform. Also names from path uses US-ASCII alphanumeric
 * characters and dash (-). Note that underscore (_) is not allowed since it is used as variants separator.
 * 
 * <pre>
 * path = part *(SEP part) 
 * part = 1*CH
 * 
 * ; terminal symbols definition
 * SEP = "/"                 ; file separator
 * CH  = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * Path has no mutable state and is thread safe.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public abstract class Path {
	/** Path separator is always slash. Note that this path separator is also used by URL path related logic. */
	public static final String SEPARATOR = "/";
	/** Convenient path separator represented as a character. */
	public static final char SEPARATOR_CHAR = '/';

	/** Parent project reference. All paths are always relative to this parent project. */
	protected final Project project;

	/** Path value relative to project root. */
	protected final String value;

	/** Wrapped Java file include project root. */
	protected final File file;

	/**
	 * Root path.
	 * 
	 * @param project parent project.
	 * @throws IllegalArgumentException if project is null.
	 */
	protected Path(Project project) {
		Params.notNull(project, "Project");
		this.project = project;
		this.value = ".";
		this.file = project.getProjectRoot();
	}

	/**
	 * Create path instance and initialize value and wrapped Java file.
	 * 
	 * @param project parent project,
	 * @param value path value relative to project.
	 * @throws IllegalArgumentException if project is null or path value is null or empty.
	 */
	protected Path(Project project, String value) {
		Params.notNull(project, "Project");
		Params.notNullOrEmpty(value, "Path value");
		this.project = project;
		this.value = value;
		this.file = new File(project.getProjectRoot(), value);
	}

	/**
	 * Test constructor.
	 * 
	 * @param project parent project,
	 * @param file Java file relative to project root.
	 */
	Path(Project project, File file) {
		this.project = project;
		String value = file.getPath().replace('\\', SEPARATOR_CHAR);
		if (file.isDirectory()) {
			value += SEPARATOR_CHAR;
		}
		this.value = value;
		this.file = file;
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
		result = prime * result + value.hashCode();
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		Path other = (Path) obj;
		return value.equals(other.value);
	}
}
