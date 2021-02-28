package js.wood.impl;

import java.util.Locale;

/**
 * WOOD operators. Operators are used by layout files to declare component relations.
 * 
 * @author Iulian Rotaru
 */
public enum Operator
{
  /** Define editable area into templates. */
  EDITABLE,
  /** Inject template content into editable area. */
  TEMPLATE,
  /** Insert widget layout into calling scope. */
  COMPO,
  /** Used in conjunction with {@link #WIDGET} and {@link #TEMPLATE} to define layout parameters list. */
  PARAM;

  public String value()
  {
    return name().toLowerCase(Locale.getDefault());
  }
}
