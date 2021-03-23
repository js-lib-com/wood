package js.wood.impl;

import java.util.Locale;

/**
 * WOOD operators. Operators are used by layout files to declare component relations.
 * 
 * @author Iulian Rotaru
 */
public enum Operator {
	/** Define editable area into templates. */
	EDITABLE,
	/** Declare template path from an inheritance relation. */
	TEMPLATE,
	/** Inject template content into editable area. */
	CONTENT,
	/** Insert widget layout into calling scope. */
	COMPO,
	/** Used in conjunction with {@link #COMPO} and {@link #TEMPLATE} to define layout parameters list. */
	PARAM;

	public String value() {
		return name().toLowerCase(Locale.getDefault());
	}
}
