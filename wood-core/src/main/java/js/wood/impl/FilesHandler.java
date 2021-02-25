package js.wood.impl;

import js.wood.DirPath;
import js.wood.FilePath;

/**
 * Abstract handler for directory files iteration. This class is designed to be used in conjunction with
 * {@link DirPath#files(FilesHandler)} class. Create an anonymous subclass and overwrite methods of interest, see
 * samples below.
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
 * Note that above snippet traverses only direct child files. To scan entire files hierarchy one may use an recursive
 * approach.
 * 
 * <pre>
 * public void scan(DirPath dir)
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
   * Sub-directory handler. Invoked when a sub-directory is found.
   * 
   * @param dir sub-directory.
   */
  public void onDirectory(DirPath dir)
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
