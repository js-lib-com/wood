package js.wood;

import java.io.File;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.wood.impl.EditablePath;

/**
 * Component path is used by layout files to declare editable elements and compos bounds. It is the mean to implement
 * inheritance and composition. Component path identifies a component under UI resources or library source directories. Project
 * directory segments are mapped to component path elements.
 * <p>
 * Here is a sample usage for component path. Template attribute declare a reference to and editable element from a template
 * whereas widget attribute is a reference to an widget. For full template reference description see {@link EditablePath}.
 * 
 * <pre>
 * &lt;div wood:template="template/page#body"&gt;
 *   . . .
 *   &lt;div wood:compo="compo/three-no"&gt;&lt;/div&gt;
 * &lt;/div&gt;
 * </pre>
 * <p>
 * Below is component path syntax. Component path starts with an optional source directory followed by a file separator then an
 * optional number of path segments and mandatory component name. If trailing path separator is missing takes care to add it.
 * Both path segment and component names uses US-ASCII alphanumeric characters and dash (-). If source directory is missing uses
 * UI resources.
 * 
 * <pre>
 * compo-path   = [source-dir SEP] *path-segment compo-name [SEP]
 * source-dir   = RES / LIB ; RES is default value
 * path-segment = name SEP
 * compo-name   = name
 * name         = 1*CH
 * 
 * ; terminal symbols definition
 * RES = "res"               ; UI resources
 * LIB = "lib"               ; third-party Java archives and UI components
 * SEP  = "/"                ; file separator 
 * CH  = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * As stated, component directory segments are mapped on component path elements:
 * <ul>
 * <li>first director is source directory, optional,
 * <li>last director is component name,
 * <li>the rest are component path segments, optional.
 * </ul>
 * <p>
 * In ASCII diagram we have a sample project file system with couple components and relation between project directories
 * structure and components path.
 * 
 * <pre>
 * project /                              
 *         ~                              
 *         / lib /                        
 *         |     / paging /          --> lib/paging
 *         ~ 
 *         / res /
 *         |     / template /
 *         |     |          / page / --> template/page
 *         ~     ~
 *         |     / page /
 *         |     |      / index /    --> page/index
 * </pre>
 * <p>
 * Component path has no mutable state and is thread safe.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class CompoPath extends DirPath {
	/** Component path pattern. */
	protected static final Pattern PATTERN = Pattern.compile("^(" + //
			"(?:[\\w-]+/)+" + // path segments
			"[\\w-]+" + // component name
			")/?$", //
			Pattern.CASE_INSENSITIVE);

	/** Simple component that has only layout file and no component directory. */
	private final boolean inlineCompoPath;

	/**
	 * Create component path instance with safe path value. This constructor is designed to be called with verified path value
	 * and does not attempt to test its validity or if directory actually exist. Attempting to use this constructor with invalid
	 * path value will conclude in not predictable behavior.
	 * 
	 * @param project project reference,
	 * @param compoPath component path value.
	 */
	public CompoPath(Project project, String compoPath) {
		super(project, value(compoPath));
		if (!exists()) {
			// by convention component path is the name of the directory where layout file is stored
			// for example for path/to/compo there is 'compo' directory and 'compo/compo.htm' layout file
			// in this case 'compo' directory should exist, condition tested by above exists() predicate

			// in the case of inline components there may be no enclosing directory
			// remember that inline component have only layout files and directory is optional
			// in this is the case test for layout file existence

			File inlineCompoFile = new File(file.getAbsoluteFile() + CT.DOT_LAYOUT_EXT);
			if (!inlineCompoFile.exists()) {
				throw new WoodException("Missing component path |%s|.", compoPath);
			}
			inlineCompoPath = true;
		} else {
			inlineCompoPath = false;
		}
	}

	/**
	 * Check component path syntax and return normalized value. In this context normalization means removing trailing path
	 * separator, if present. Syntax is matched against {@link #PATTERN component path pattern}.
	 * 
	 * @param compoPath component path value.
	 * @return normalized path value.
	 * @throws WoodException if component path parameter has bad syntax.
	 */
	private static String value(String compoPath) throws WoodException {
		Matcher matcher = PATTERN.matcher(compoPath);
		if (!matcher.find()) {
			throw new WoodException("Invalid component path |%s|.", compoPath);
		}
		return matcher.group(1);
	}

	/**
	 * Get this component name.
	 * 
	 * @return component name.
	 */
	public String getName() {
		return file.getName();
	}

	/**
	 * Get file path for this component layout.
	 * 
	 * @return component layout.
	 */
	public FilePath getLayoutPath() {
		if (inlineCompoPath) {
			return new FilePath(project, value.substring(0, value.length() - 1) + CT.DOT_LAYOUT_EXT);
		}
		FilePath layoutPath = getFilePath(getName() + CT.DOT_LAYOUT_EXT);
		return layoutPath;
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
