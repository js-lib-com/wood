package js.wood;

import js.util.Params;
import js.wood.impl.ResourceType;

/**
 * Immutable resource reference. Simply put, a reference is a pointer from a source file to a resource, be it variable or media
 * file. It has a resource type and a name - uniquely identifying the resource in its scope. Resource reference scope is the
 * source file defining the reference plus global assets scope.
 * <p>
 * Regarding media files, resource reference is a sort of abstract addressing. It does not identify precisely the file;
 * extension and variants are not included into media reference name. Also media file can be stored private into component
 * directory or global into assets. Anyway, media file references support sub-directories, for example
 * <code>@image/icon/logo</code>. There are methods to test and retrieve the file path, see {@link #hasPath()} and
 * {@link #getPath()}.
 * <p>
 * Reference syntax is described below. This syntax is the same, no mater the source file type where reference is used: layout,
 * style, script or descriptors.
 * 
 * <pre>
 * reference = MARK resource-type SEP ?(path SEP) name
 * path      = 1*CH           ; optional path, for media files only
 * name      = 1*CH           ; resource name
 * ; resource-type is defined by {@link ResourceType}, to lower case
 * 
 * ; terminal symbols definition
 * MARK = "@"                 ; reference mark
 * SEP  = "/"                 ; reference name separator
 * CH   = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * As stated variables and media files can be referenced from any source file: layout, style, script or descriptors. Here are
 * sample usage cases. Note that references are text replaced and where source file syntax requires quotes (") they should be
 * explicitly used.
 * 
 * <pre>
 *  &lt;body&gt;
 *      &lt;h1&gt;@string/title&lt;/h1&gt;
 *      &lt;img src="@image/logo" /&gt;
 *      &lt;p&gt;@text/message&lt;/p&gt;
 *      . . .
 *  &lt;/body&gt;
 *  
 *  body {
 *      width: {@literal @}dimen/page-width;
 *      background-image: url("@image/page-bg");
 *      background-color: {@literal @}color/page-color;
 *      . . .
 *  }
 * 
 *  js.ua.System.alert("@string/exception");
 *  this.setRichText("@text/message");
 *  this.logo.setSrc("@image/logo");
 *  this.audioPlayer.play("@audio/beep");
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class Reference {
	/** Mark for reference start. */
	public static final char MARK = '@';

	/** Reference name separator. */
	public static final char SEPARATOR = '/';

	private final FilePath sourceFile;

	/** Referenced resource type. If type is {@link ResourceType#UNKNOWN} this resource reference is invalid. */
	private final ResourceType resourceType;

	/** Optional path, for media files only. */
	private final String path;

	/**
	 * Resource name, unique in scope. Resource reference name scope is the component directory in which it is used plus global
	 * project assets.
	 */
	private final String name;

	/**
	 * Create immutable reference instance from given type and name.
	 * 
	 * @param sourceFile source file, for exception handling and debugging,
	 * @param resourceType referenced resource type,
	 * @param resourceName name uniquely identifying referred resource, in its scope.
	 * @throws IllegalArgumentException if a parameter is null.
	 */
	public Reference(FilePath sourceFile, ResourceType resourceType, String resourceName) throws IllegalArgumentException {
		Params.notNull(resourceType, "Resource type");
		Params.notNullOrEmpty(resourceName, "Resource name");
		this.sourceFile = sourceFile;
		this.resourceType = resourceType;

		int pathSeparator = resourceName.lastIndexOf(SEPARATOR);
		if (pathSeparator == -1) {
			this.path = null;
			this.name = resourceName;
		} else {
			if (isVariable()) {
				throw new WoodException("Invalid reference |%s| syntax on file |%s|. Variable reference with path.", resourceName, sourceFile);
			}
			this.path = resourceName.substring(0, pathSeparator);
			this.name = resourceName.substring(pathSeparator + 1);
		}
	}

	/**
	 * Test contructor.
	 * 
	 * @param resourceType referenced resource type,
	 * @param name name uniquely identifying resource in its scope.
	 */
	public Reference(ResourceType resourceType, String name) throws IllegalArgumentException {
		this(null, resourceType, name);
	}

	public FilePath getSourceFile() {
		return sourceFile;
	}

	/**
	 * Get resource type.
	 * 
	 * @return resource type.
	 * @see #resourceType
	 */
	public ResourceType getResourceType() {
		return resourceType;
	}

	/**
	 * Get resource name.
	 * 
	 * @return resource name.
	 * @see #name
	 */
	public String getName() {
		return name;
	}

	/**
	 * Test if this reference contains path.
	 * 
	 * @return true if reference contains path.
	 * @see #path
	 */
	public boolean hasPath() {
		return path != null;
	}

	/**
	 * Return this reference path, possible null.
	 * 
	 * @return reference path.
	 * @see #path
	 */
	public String getPath() {
		return path;
	}

	/**
	 * Test if referenced resource is a variable. This predicate delegates {@link ResourceType#isVariable()}.
	 * 
	 * @return true if resource is a variable.
	 * @see ResourceType#isVariable()
	 */
	public boolean isVariable() {
		return resourceType.isVariable();
	}

	/**
	 * Test if referenced resource is a media file. This predicate delegates {@link ResourceType#isMedia()}.
	 * 
	 * @return true if resource is media file.
	 * @see ResourceType#isMedia()
	 */
	public boolean isMediaFile() {
		return resourceType.isMediaFile();
	}

	/**
	 * Test if referenced resource is a font family file.
	 * 
	 * @return true if resource is a font family file.
	 */
	public boolean isFontFile() {
		return resourceType == ResourceType.FONT;
	}

	/**
	 * Test if referenced resource is a generic file.
	 * 
	 * @return true if resource is a generic file.
	 */
	public boolean isGenericFile() {
		return resourceType == ResourceType.FONT;
	}

	/**
	 * Test if this reference is valid, that is, referenced resource type is known.
	 * 
	 * @return true if this reference is valid.
	 * @see #resourceType
	 */
	public boolean isValid() {
		return resourceType != ResourceType.UNKNOWN;
	}

	/**
	 * Test if character is valid for resource reference. Current reference implementation accept only US-ASCII alphanumeric
	 * characters and dash (-), reference mark and separator - see {@link #MARK}, {@link #SEPARATOR}.
	 * 
	 * @param c character to test.
	 * @return true if requested character is valid.
	 */
	public static boolean isChar(int c) {
		return Character.isLetterOrDigit(c) || c == '-' || c == MARK || c == SEPARATOR;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((name == null) ? 0 : name.hashCode());
		result = prime * result + ((path == null) ? 0 : path.hashCode());
		result = prime * result + ((resourceType == null) ? 0 : resourceType.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		Reference other = (Reference) obj;
		if (name == null) {
			if (other.name != null)
				return false;
		} else if (!name.equals(other.name))
			return false;
		if (path == null) {
			if (other.path != null)
				return false;
		} else if (!path.equals(other.path))
			return false;
		if (resourceType != other.resourceType)
			return false;
		return true;
	}

	/**
	 * Get reference instance string representation.
	 * 
	 * @return string representation.
	 * @see #string
	 */
	@Override
	public String toString() {
		StringBuilder builder = new StringBuilder();
		builder.append('@');
		builder.append(resourceType.name().toLowerCase());
		builder.append(SEPARATOR);
		if (this.path != null) {
			builder.append(this.path);
			builder.append(SEPARATOR);
		}
		builder.append(this.name);
		return builder.toString();
	}
}
