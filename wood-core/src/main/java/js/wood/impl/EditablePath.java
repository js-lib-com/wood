package js.wood.impl;

import java.util.regex.Matcher;

import js.wood.CompoPath;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.WoodException;

/**
 * The path to an editable element from a component. It has component path and editable name, separated by hash (#). An editable
 * path is used by components to declare OOP inheritance. It is the path to editable element from template, for which component
 * define content.
 * <p>
 * In sample code, <code>wood:template</code> attribute declares a bound to an editable element named <code>body</code> from
 * <code>template/page</code> component.
 * 
 * <pre>
 * &lt;div wood:template="template/page#body"&gt;
 *   // content for template editable element
 * &lt;/div&gt;
 * </pre>
 * <p>
 * An editable element is part of a template layout. Is legal to have more editable elements in a template; for this reason
 * editable path should identify both template component and named editable.
 * 
 * <pre>
 * editable-path = compo-path SEP editable-name
 * editable-name = 1*CH
 * 
 * ; terminal symbols definition
 * SEP = "#"                 ; editable separator is always hash
 * CH  = ALPHA / DIGIT / "-" ; character is US-ASCII alphanumeric and dash
 * 
 * ; compo-path nonterminal symbol is described in {@link CompoPath component path}
 * ; ALPHA and DIGIT are described by RFC5234, Appendix B.1
 * </pre>
 * <p>
 * Editable path has no mutable state and is thread safe.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class EditablePath extends CompoPath {
	/** Separator for editable name. */
	private static final char EDITABLE_SEPARATOR = '#';

	/** Editable name as defined into template. */
	private final String editableName;

	/** Cached value for object string representation. */
	private final String string;

	/**
	 * Construct editable path instance for value loaded from layout file. This constructor extracts editable name from given
	 * path and store it; with what is left delegates {@link CompoPath#CompoPath(Project, FilePath)}. Since editable path is
	 * loaded from layout files it is not reliable and uses component constructor that check component path integrity.
	 * 
	 * @param project WOOD project context,
	 * @param editablePath editable path value.
	 * @throws WoodException if <code>editablePath</code> parameter is invalid or its component path missing.
	 */
	public EditablePath(Project project, String editablePath) throws WoodException {
		super(project, compoPath(editablePath));
		editableName = editablePath.substring(editablePath.lastIndexOf(EDITABLE_SEPARATOR) + 1);

		StringBuilder builder = new StringBuilder(value.substring(0, value.length() - 1));
		builder.append(EDITABLE_SEPARATOR);
		builder.append(editableName);
		string = builder.toString();
	}

	/**
	 * Get the component part from an editable path.
	 * 
	 * @param editablePath editable path.
	 * @return component path.
	 * @throws WoodException if editable name is missing.
	 */
	private static String compoPath(String editablePath) throws WoodException {
		int index = editablePath.lastIndexOf(EDITABLE_SEPARATOR);
		if (index == -1) {
			throw new WoodException("Missing editable name from editable path |%s|.", editablePath);
		}
		return editablePath.substring(0, index);
	}

	/**
	 * Get this editable name.
	 * 
	 * @return editable name.
	 * @see #editableName
	 */
	public String getEditableName() {
		return editableName;
	}

	/**
	 * Get editable path string representation.
	 * 
	 * @return editable string representation.
	 */
	@Override
	public String toString() {
		return string;
	}

	/**
	 * Test if path value is acceptable for editable path instance creation.
	 * 
	 * @param path path value.
	 * @return true if path value match editable pattern.
	 */
	public static boolean accept(String path) {
		int index = path.lastIndexOf(EDITABLE_SEPARATOR);
		if (index == -1) {
			return false;
		}
		Matcher matcher = PATTERN.matcher(path.substring(0, index));
		return matcher.find();
	}
}
