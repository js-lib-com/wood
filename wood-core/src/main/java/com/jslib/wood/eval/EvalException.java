package com.jslib.wood.eval;

import com.jslib.util.Strings;

/**
 * Thrown when evaluation logic fails for whatever reason.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class EvalException extends Exception
{
  /**
   * Java serialization version.
   */
  private static final long serialVersionUID = 4788259545507001393L;

  /**
   * Construct exception instance with given message.
   * 
   * @param message exception message.
   */
  public EvalException(String message)
  {
    super(message);
  }

  public EvalException(String message, Object... args)
  {
    super(Strings.format(message, args));
  }
}