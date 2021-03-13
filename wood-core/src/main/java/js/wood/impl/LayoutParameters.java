package js.wood.impl;

import java.util.HashMap;
import java.util.Map;

import js.lang.Pair;
import js.util.Strings;
import js.wood.SourceReader;

/**
 * Template and child component layout parameters.Layout parameters are a mean to customize a reusable component. Parameters are
 * defined at the place where component is linked whereas parameters value is used inside that component.
 * <p>
 * Layout parameters are loaded from <code>wood:param</code> operator and used by {@link SourceReader} to inject values into
 * layout, on the fly. Into layout source file, parameters are declared using <code>@param/name</code> reference. Parameter
 * reference acts as a placeholder that is replaced with parameter value.
 * <p>
 * Below is an example child component layout with <code>caption</code> parameter. The number of parameter references is not
 * limited but all should be defined when declare the child component. Also a parameter references can be used multiple times in
 * a given layout. Because parameter references is text replaced with string value, it can appear into layout anywhere a string
 * is valid.
 * 
 * <pre>
 * &lt;div class="listview"&gt;
 *   &lt;h4 class="caption"&gt;@param/caption&lt;/h4&gt;
 *   ...
 * </pre>
 * <p>
 * When reference the child component, declares parameters value. All parameter references from layout should be resolved. If
 * there are more parameters, position into list is not relevant since parameters are passed by name.
 * 
 * <pre>
 * &lt;div wood:compo="compo/list-view" wood:param="caption:Info Links"&gt;&lt;/div&gt;
 * </pre>
 * 
 * <p>
 * Parameters definition syntax is similar to inline CSS style. It is a list of parameters separated by semicolon and a
 * parameter is a name / value pair separated by colon. Spaces are allowed around separators.
 * 
 * <pre>
 * parameters = parameter *(PARAM_SEP parameter) [PARAM_SEP]
 * parameter = name NAME_SEP value
 * PARAM_SEP = ';'
 * NAME_SEP = ':'
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class LayoutParameters {
	/** Parameters storage. */
	private final Map<String, String> parameters = new HashMap<String, String>();

	/**
	 * Reload layout parameters from parameters definition, possible null. If <code>parameters</code> argument is null this
	 * method does nothing. Parameters definition format is similar inline CSS style, see class description.
	 * <p>
	 * Note that current parameters, if any, are lost. This method perform clean up before parameters loading.
	 * 
	 * @param parametersDefintion layout parameters definition, null accepted.
	 */
	public void reload(String parametersDefintion) {
		if (parametersDefintion == null) {
			return;
		}
		parameters.clear();
		for (Pair pair : Strings.splitPairs(parametersDefintion, ';', ':')) {
			parameters.put(pair.first(), Strings.escapeXML(pair.second()));
		}
	}

	/**
	 * Get parameter value or null if named parameter does not exist.
	 * 
	 * @param name parameter name.
	 * @return parameter value, possible null.
	 */
	public String getValue(String name) {
		return parameters.get(name);
	}
}
