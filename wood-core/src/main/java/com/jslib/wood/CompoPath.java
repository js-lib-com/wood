package com.jslib.wood;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Component path is the {@link FilePath} to the project directory where component files reside. Being a file path is
 * relative to the project root; it is also a sequence of path segments, but it has no trailing separator. Last path segment is
 * usually known as component name.
 * <p>
 * Component paths are used on layout files to declare inheritance and composition. Here is a sample usage for component paths.
 * <code>wood:template</code> operator declare a reference to and editable element from a template whereas
 * <code>wood:compo</code> operator is a reference to a reusable component.
 *
 * <pre>
 * &lt;div wood:template="template/page#body"&gt;
 *   . . .
 *   &lt;div wood:compo="compo/three-no"&gt;&lt;/div&gt;
 * &lt;/div&gt;
 * </pre>
 * <p>
 * Below is the component path syntax. Component path is a sequence of path segments, the last segment being called component
 * name. Both paths segment and component names uses US-ASCII alphanumeric characters and dash (-). Note that underscore (_) is
 * not supported since is used for variants separator.
 *
 * <pre>
 * compo-path   = 1*path-segment compo-name [SEP]
 * path-segment = name SEP
 * compo-name   = name
 * name         = 1*CH
 *
 * ; terminal symbols definition
 * SEP = "/"                 ; file separator
 * CH  = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 *
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * A component always has a layout file. It is an HTML file with basename equal to parent directory name and this path instance
 * points to component directory. In this case component directory contains, beside layout, couple related files: styles,
 * scripts, media files, variables and descriptor.
 * <p>
 * Anyway, there are simplified components that have only layout. For example a select with options reused in multiple forms. In
 * this case component directory can miss and this component path points to layout file itself; need only to add layout
 * extension. This simplified component is named <code>widget</code>.
 * <p>
 * In ASCII diagram we have a sample project file system with couple components and relation between project directories
 * structure and components path. Component path <code>res/template/page</code> points to directory
 * <code>${project-root}/res/template/page</code> whereas component path <code>res/template/select</code> points to file
 * <code>${project-root}/res/template/select.htm</code>.
 *
 * <pre>
 * project /
 *         ~
 *         / lib /
 *         |     / paging /              --> lib/paging
 *         ~
 *         / res /
 *         |     / template /
 *         |     |          / page /     --> res/template/page
 *         |     |          / select.htm --> res/template/select - widget
 *         ~     ~
 *         |     / page /
 *         |     |      / index /        --> res/page/index
 * </pre>
 * <p>
 * This class provides convenient method to retrieve layout file path. In above diagram it
 * returns <code>res/template/page/page.htm</code> for component path <code>res/template/page</code>. For widget
 * <code>res/template/select</code> returned layout file path is <code>res/template/select.htm</code>.
 * <p>
 * Note that for a full component this component path points to an existing directory. By contrast, widget path is abstract,
 * with no direct node associated on project file system.
 * <p>
 * Component path has no mutable state and is thread safe.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
public class CompoPath extends FilePath {
    /**
     * Component path pattern.
     */
    protected static final Pattern PATTERN = Pattern.compile("^(" + //
                    "(?:[a-z0-9-]+/)*" + // path segments
                    "[a-z0-9-]+" + // component name
                    ")/?$", //
            Pattern.CASE_INSENSITIVE);

    /**
     * Create component path instance with normalized path value. This constructor is designed to be called with verified path
     * value and does not attempt to test its validity or if directory actually exist. Attempting to use this constructor with
     * invalid path value will conclude in not predictable behavior.
     *
     * @param project WOOD project context,
     * @param value   component path value.
     */
    public CompoPath(Project project, String value) {
        super(project, value(value));
    }

    /**
     * Check component path syntax and return normalized value. In this context normalization means adding trailing path
     * separator. Syntax is matched against {@link #PATTERN component path pattern}.
     *
     * @param compoPath component path value.
     * @return normalized path value.
     * @throws WoodException if component path parameter has bad syntax.
     */
    private static String value(String compoPath) throws WoodException {
        assert compoPath != null && !compoPath.isEmpty() : "Component path argument is null or empty";
        Matcher matcher = PATTERN.matcher(compoPath);
        if (!matcher.find()) {
            throw new WoodException("Invalid component path %s", compoPath);
        }
        return matcher.group(1) + FilePath.SEPARATOR_CHAR;
    }

    /**
     * Test constructor.
     *
     * @param project WOOD project context,
     * @param file    component file.
     */
    CompoPath(Project project, File file) {
        super(project, file);
    }

    /**
     * Get file path for this component layout. Usually layout is an HTML file with basename equal to parent directory name and
     * this path instance points to component directory. In this case component directory contains, beside layout, couple
     * related files: styles, scripts, media files, variables and descriptor.
     * <p>
     * Anyway, there are simplified component that have only layout. For example a select with options reused in multiple forms.
     * In this case component directory can miss and this path points to layout file itself; need only to add layout extension.
     * This simplified component is named <code>inline component</code>.
     * <p>
     * To detect if this component is inline check if this path is an existing directory. If so, we have a full component,
     * otherwise this component is inline.
     *
     * @return component layout.
     */
    public FilePath getLayoutPath() {
        if (file.isDirectory()) {
            return getFilePath(getName() + CT.DOT_LAYOUT_EXT);
        }
        return project.createFilePath(value().substring(0, value().length() - 1) + CT.DOT_LAYOUT_EXT);
    }

    /**
     * Test if path value is acceptable for component path instance creation.
     *
     * @param path path value.
     * @return true if path value match component pattern.
     */
    public static boolean accept(String path) {
        Matcher matcher = PATTERN.matcher(path);
        return matcher.find();
    }
}
