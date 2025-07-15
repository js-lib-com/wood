package com.jslib.wood;

/**
 * Immutable at-meta reference. There are three families of at-meta references: variables, resource files - mostly media files,
 * and layout parameters references. At-meta reference has a type and a name - uniquely identifying the reference in its scope.
 * At-meta reference scope is the source file defining the reference plus global assets scope.
 * <p>
 * Regarding resource files, at-meta reference is a sort of abstract addressing. It does not identify precisely the file;
 * extension and variants are not included into resource reference name. Also, resource file can be stored private into component
 * directory or global into assets. Anyway, resource file references support subdirectories, for example
 * <code>@image/icon/logo</code>. There are methods to test and retrieve the file path, see {@link #hasPath()} and
 * {@link #getPath()}.
 * <p>
 * At-meta reference syntax is described below.
 *
 * <pre>
 * reference = MARK type SEP ?(path SEP) name
 * path      = 1*CH           ; optional path, for resource files only
 * name      = 1*CH           ; reference name, unique in scope
 * ; type is defined by {@link Reference.Type}, to lower case
 *
 * ; terminal symbols definition
 * MARK = "@"                 ; reference mark
 * SEP  = "/"                 ; reference name separator
 * CH   = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 *
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * Here are sample usage cases. Note that references are text replaced and where source file syntax requires quotes (") they
 * should be explicitly used.
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
 *      background-image: url("@image/page-bg");
 *      . . .
 *  }
 *
 *  alert("@string/exception");
 *  this.section.innerHTML = "@text/message";
 *  this.logo.src = "@image/logo";
 *  this.audioPlayer.src = "@audio/beep";
 * </pre>
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
public class Reference {
    /**
     * Mark for reference start.
     */
    public static final char MARK = '@';

    /**
     * Reference name separator.
     */
    public static final char SEPARATOR = '/';

    private final FilePath sourceFile;

    /**
     * Reference type.
     */
    private final Type type;

    private final String value;

    /**
     * Optional path, for resource files only. Default to null.
     */
    private final String path;

    /**
     * Reference name, unique in scope. Reference name scope is the component directory in which it is used plus global project
     * assets.
     */
    private final String name;

    /**
     * Create immutable reference instance for given type and name.
     *
     * @param sourceFile source file, for exception handling and debugging,
     * @param type       reference type,
     * @param name       reference name, uniquely identifying this reference in its scope.
     */
    public Reference(FilePath sourceFile, Reference.Type type, String name) {
        assert type != null : "Reference type argument is null";
        assert name != null && !name.isEmpty() : "Reference name argument is null or empty";
        this.sourceFile = sourceFile;
        this.type = type;
        this.value = name;

        int pathSeparator = name.lastIndexOf(SEPARATOR);
        if (pathSeparator == -1) {
            this.path = null;
            this.name = name;
        } else {
            if (isVariable()) {
                throw new WoodException("Invalid reference %s:%s syntax on file %s; variable reference with path", type, name, sourceFile);
            }
            this.path = name.substring(0, pathSeparator).replace('\\', '/');
            this.name = name.substring(pathSeparator + 1);
        }
    }

    /**
     * Test constructor.
     *
     * @param type reference type,
     * @param name unique name identifying reference in its scope.
     */
    public Reference(Reference.Type type, String name) {
        this(null, type, name);
    }

    public FilePath getSourceFile() {
        return sourceFile;
    }

    public Reference.Type getType() {
        return type;
    }

    public String getName() {
        return name;
    }

    /**
     * Test if this reference contains path.
     *
     * @return true if reference contains path.
     */
    public boolean hasPath() {
        return path != null;
    }

    /**
     * Return this reference path, possible null.
     *
     * @return this reference path, possible null.
     */
    public String getPath() {
        return path;
    }

    public String getValue() {
        return value;
    }

    /**
     * Test if this reference type is a variable. Current implementation consider as variable next reference types:
     * {@link Type#STRING}, {@link Type#TEXT}, {@link Type#STYLE}, {@link Type#LINK} and {@link Type#TIP}.
     *
     * @return true if this reference type is a variable.
     */
    public boolean isVariable() {
        return type.isVariable();
    }

    public boolean isProject() {
        return type == Type.PROJECT;
    }

    /**
     * Test if this reference type is a media file. Current implementation consider as media file next reference types:
     * {@link Type#IMAGE}, {@link Type#AUDIO} and {@link Type#VIDEO}.
     *
     * @return true if reference type is media file.
     */
    public boolean isMediaFile() {
        return type == Type.IMAGE || type == Type.AUDIO || type == Type.VIDEO;
    }

    /**
     * Test if this reference type is a font family file.
     *
     * @return true if this reference type is a font family file.
     */
    public boolean isFontFile() {
        return type == Type.FONT;
    }

    public boolean isLayoutFile() {
        return type == Type.LAYOUT;
    }

    public boolean isStyleFile() {
        return type == Type.STYLE;
    }

    /**
     * Test if this reference type is a generic file.
     *
     * @return true if this reference type is a generic file.
     */
    public boolean isGenericFile() {
        return type == Type.FILE;
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        result = prime * result + ((path == null) ? 0 : path.hashCode());
        result = prime * result + ((type == null) ? 0 : type.hashCode());
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
        return type == other.type;
    }

    @Override
    public String toString() {
        StringBuilder builder = new StringBuilder();
        builder.append('@');
        builder.append(type.name().toLowerCase());
        builder.append(SEPARATOR);
        if (path != null) {
            builder.append(path);
            builder.append(SEPARATOR);
        }
        builder.append(this.name);
        return builder.toString();
    }

    /**
     * Types enumeration for at-meta references.
     *
     * @author Iulian Rotaru
     */
    public enum Type {
        // VARIABLES

        /**
         * Plain string values mainly for multi-languages support.
         */
        STRING,

        /**
         * Same as string but allows for HTML format.
         */
        TEXT,

        /**
         * Links references to local or third part resources, mainly for <code>href</code> attribute.
         */
        LINK,

        /**
         * Tool-tip value, usually for elements supporting <code>title</code> attribute.
         */
        TIP,

        // DESCRIPTORS

        PROJECT,

        // RESOURCE FILES

        /**
         * Image file stored on server and linked via URLs from source files, be it layout, style or script.
         */
        IMAGE,

        /**
         * The same as {@link #IMAGE} but with audio content.
         */
        AUDIO,

        /**
         * The same as {@link #IMAGE} but with video content.
         */
        VIDEO,

        /**
         * Font family file loaded from server and declared by <code>@font-face</code> style rule.
         */
        FONT,

        /**
         * Generic file, for example license text file.
         */
        FILE,

        // LAYOUT PARAMETERS

        /**
         * Layout parameter declared by child component and with value defined by parent.
         */
        PARAM,

        // SITE FILES

        /**
         * HTML layout file, replace.
         */
        LAYOUT,

        /**
         * CSS style file usable, for example, by dynamic style loading. EXPERIMENTAL
         */
        STYLE,

        /**
         * Unknown reference type.
         */
        UNKNOWN;

        /**
         * Create reference type enumeration from type value, not case-sensitive. Returns {@link #UNKNOWN} if given reference
         * type parameter does not denote an enumeration constant.
         *
         * @param type value of reference type.
         * @return reference type enumeration, possible {@link #UNKNOWN}.
         */
        public static Type getValueOf(String type) {
            try {
                return Type.valueOf(type.toUpperCase());
            } catch (Exception ignored) {
            }
            return UNKNOWN;
        }

        // WARN: keep variable names in sync with reference type constants
        private static final String[] variables = new String[]{"string", "text", "link", "tip"};

        public static String[] variables() {
            return variables;
        }

        private Boolean variable;

        public boolean isVariable() {
            if (variable == null) {
                variable = this == Type.STRING || this == Type.TEXT || this == Type.LINK || this == Type.TIP;
            }
            return variable;
        }

        /**
         * Test if character is valid for reference name for this particular reference type. Current reference implementation
         * accept only US-ASCII alphanumeric characters and dash (-), reference <code>mark</code> and <code>separator</code> -
         * see {@link #MARK}, {@link #SEPARATOR}. Note that <code>separator</code> is accepted only if this reference type is
         * not variable.
         *
         * @param c character to test.
         * @return true if requested character is valid.
         */
        public boolean isChar(int c) {
            return Character.isLetterOrDigit(c) || c == '-' || c == MARK || (c == SEPARATOR && !isVariable());
        }
    }
}
