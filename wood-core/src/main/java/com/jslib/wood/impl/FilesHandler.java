package com.jslib.wood.impl;

import com.jslib.wood.FilePath;

/**
 * Abstract handler for directory files iteration.
 * <p>
 * Here is an example for listing all files, direct child, from a directory.
 * 
 * <pre>
 * dir.files(new FilesHandler()
 * {
 *   public void onFile(FilePath file) throws Exception
 *   {
 *     // handle the file
 *   }
 * });
 * </pre>
 * <p>
 * Note that above snippet traverses only direct child files. To scan entire files hierarchy one may use a recursive
 * approach.
 * 
 * <pre>
 * public void scan(FilePath dir)
 * {
 *   dir.files(new FilesHandler()
 *   {
 *     public void onDirectory(DirPath dir) throws Exception
 *     {
 *       scan(dir);
 *     }
 * 
 *     public void onFile(FilePath file) throws Exception
 *     {
 *       // handle the file
 *     }
 *   });
 * }
 * 
 * </pre>
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public abstract class FilesHandler
{
  /**
   * Subdirectory handler. Invoked when a subdirectory is found.
   * 
   * @param dir subdirectory.
   */
  public void onDirectory(FilePath dir)
  {
  }

  /**
   * If this predicate returns true {@link #onFile(FilePath)} is invoked, otherwise file is skipped. Default
   * implementation always returns true.
   * 
   * @param file file path.
   * @return true if file is accepted.
   */
  public boolean accept(FilePath file)
  {
    return true;
  }

  /**
   * Handler invoked for files from directory if {@link #accept(FilePath)} predicate grant it.
   * 
   * @param file file path.
   */
  public void onFile(FilePath file)
  {
  }
}
