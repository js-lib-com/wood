package com.jslib.wood.util;

import java.io.*;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.zip.ZipInputStream;
import java.util.zip.ZipOutputStream;

import static com.jslib.wood.util.StringsUtil.format;

/**
 * Functions for files, byte and character streams copy and file path manipulation. This class supplies methods for byte
 * and character streams transfer and files copy. If not otherwise specified, in order to simplify caller logic, streams
 * methods close streams before returning, including on error. Also for files copy, target file is created if it does not
 * exist; if target file creation fails, perhaps because of missing rights or target exists and is a directory, file
 * methods throw {@link FileNotFoundException}. Please note that in all cases target content is overwritten.
 * <p>
 * Finally, there are method working with temporary files as target. These methods return newly created temporary file
 * and is caller responsibility to remove it when is no longer necessary. This library does not keep record of created
 * temporary file and there is no attempt to remove then, not even at virtual machine exit.
 *
 * @author Iulian Rotaru
 */
public class FilesUtil {
    /**
     * The size of buffer used by copy operations.
     */
    private static final int BUFFER_SIZE = 4 * 1024;

    /**
     * Prevent default constructor synthesis but allow sub-classing.
     */
    protected FilesUtil() {
    }

    /**
     * Copy source file to target. Copy destination should be a file and this method throws access denied if attempt to
     * write to a directory. Source file should exist but target is created by this method, but if not already exist.
     *
     * @param source file to read from, should exist,
     * @param target file to write to.
     * @return the number of bytes transferred.
     * @throws FileNotFoundException if source file does not exist or target file does not exist and cannot be created.
     * @throws IOException           if copy operation fails, including if <code>target</code> is a directory.
     */
    public static long copy(File source, File target) throws FileNotFoundException, IOException {
        return copy(new FileInputStream(source), new FileOutputStream(target));
    }

    /**
     * Copy source file bytes to requested output stream. Note that output stream is closed after transfer completes,
     * including on error.
     *
     * @param file         source file,
     * @param outputStream destination output stream.
     * @return the number of bytes processed.
     * @throws FileNotFoundException    if <code>file</code> does not exist.
     * @throws IOException              bytes processing fails.
     * @throws IllegalArgumentException if input file or output stream is null.
     */
    public static long copy(File file, OutputStream outputStream) throws IOException {
        assert file != null : "Input file argument is null";
        return copy(new FileInputStream(file), outputStream);
    }

    /**
     * Copy characters from a reader to a given writer then close both character streams.
     *
     * @param reader character stream to read from,
     * @param writer character stream to write to.
     * @return the number of characters processed.
     * @throws IOException              if read or write operation fails.
     * @throws IllegalArgumentException if reader or writer is null.
     */
    public static int copy(Reader reader, Writer writer) throws IOException {
        assert reader != null : "Reader argument is null";
        assert writer != null : "Writer argument is null";

        if (!(reader instanceof BufferedReader)) {
            reader = new BufferedReader(reader);
        }
        if (!(writer instanceof BufferedWriter)) {
            writer = new BufferedWriter(writer);
        }

        int charsCount = 0;
        try {
            char[] buffer = new char[BUFFER_SIZE];
            for (; ; ) {
                int readChars = reader.read(buffer);
                if (readChars == -1) {
                    break;
                }
                charsCount += readChars;
                writer.write(buffer, 0, readChars);
            }
        } finally {
            close(reader);
            close(writer);
        }
        return charsCount;
    }

    /**
     * Copy bytes from input to given output stream then close both byte streams. Please be aware this method closes both
     * input and output streams. This is especially important if work with ZIP streams; trying to get/put next ZIP entry
     * after this method completes will fail with <em>stream closed</em> exception.
     *
     * @param inputStream  bytes input stream,
     * @param outputStream bytes output stream.
     * @return the number of bytes processed.
     * @throws IOException              if reading or writing fails.
     * @throws IllegalArgumentException if input or output stream is null or ZIP stream.
     */
    public static long copy(InputStream inputStream, OutputStream outputStream) throws IOException, IllegalArgumentException {
        assert inputStream != null : "Input stream argument is null";
        assert outputStream != null : "Output stream argument is null";
        assert !(inputStream instanceof ZipInputStream) : "Input stream argument is ZIP.";
        assert !(outputStream instanceof ZipOutputStream) : "Output stream argument is ZIP.";

        if (!(inputStream instanceof BufferedInputStream)) {
            inputStream = new BufferedInputStream(inputStream);
        }
        if (!(outputStream instanceof BufferedOutputStream)) {
            outputStream = new BufferedOutputStream(outputStream);
        }

        long bytes = 0;
        try {
            byte[] buffer = new byte[BUFFER_SIZE];
            int length;
            while ((length = inputStream.read(buffer)) != -1) {
                bytes += length;
                outputStream.write(buffer, 0, length);
            }
        } finally {
            close(inputStream);
            close(outputStream);
        }
        return bytes;
    }

    /**
     * Close given <code>closeable</code> if not null but ignoring IO exception generated by failing close operation.
     *
     * @param closeable closeable to close.
     */
    public static void close(Closeable closeable) {
        if (closeable == null) {
            return;
        }
        try {
            closeable.close();
        } catch (IOException ignored) {
        }
    }

    /**
     * Get file extension as lower case or empty string. Returned extension does not contain dot separator, that is,
     * <code>htm</code> not <code>.htm</code>. Returns null if <code>file</code> parameter is null.
     *
     * @param file file to return extension of.
     * @return file extension or empty string or null if <code>file</code> parameter is null.
     */
    public static String getExtension(File file) {
        return file != null ? getExtension(file.getAbsolutePath()) : null;
    }

    /**
     * The extension separator character.
     */
    public static final char EXTENSION_SEPARATOR = '.';
    /**
     * The Unix separator character.
     */
    private static final char UNIX_SEPARATOR = '/';
    /**
     * The Windows separator character.
     */
    private static final char WINDOWS_SEPARATOR = '\\';

    /**
     * Get the lower case extension of the file denoted by given path or empty string if not extension. Returned extension
     * does not contain dot separator, that is, <code>htm</code> not <code>.htm</code>. Returns null if given
     * <code>path</code> parameter is null.
     *
     * @param path the path of the file to return extension of.
     * @return file extension, as lower case, or empty string if no extension.
     */
    public static String getExtension(String path) {
        if (path == null) {
            return null;
        }

        // search for both Unix and Windows path separators because this logic is common for files and URLs

        int extensionPos = path.lastIndexOf(EXTENSION_SEPARATOR);
        int lastUnixPos = path.lastIndexOf(UNIX_SEPARATOR);
        int lastWindowsPos = path.lastIndexOf(WINDOWS_SEPARATOR);
        int lastSeparatorPos = Math.max(lastUnixPos, lastWindowsPos);

        // do not consider extension separator before last path separator, e.g. /etc/rc.d/file
        int i = (lastSeparatorPos > extensionPos ? -1 : extensionPos);
        return i == -1 ? "" : path.substring(i + 1).toLowerCase();
    }

    /**
     * Replace extension on given file path and return resulting path. Is legal for new extension parameter to start with
     * dot extension separator, but is not mandatory.
     *
     * @param path         file path to replace extension,
     * @param newExtension newly extension, with optional dot separator prefix.
     * @return newly created file path.
     * @throws IllegalArgumentException if path or new extension parameter is null.
     */
    public static String replaceExtension(String path, String newExtension) throws IllegalArgumentException {
        assert path != null : "Path argument is null";
        assert newExtension != null : "New extension argument is null";

        if (newExtension.charAt(0) == '.') {
            newExtension = newExtension.substring(1);
        }

        int extensionDotIndex = path.lastIndexOf('.') + 1;
        if (extensionDotIndex == 0) {
            extensionDotIndex = path.length();
        }
        StringBuilder sb = new StringBuilder(path.length());
        sb.append(path.substring(0, extensionDotIndex));
        sb.append(newExtension);
        return sb.toString();
    }

    /**
     * Replace extension on given file and return resulting file.
     *
     * @param file         file to replace extension,
     * @param newExtension newly extension.
     * @return newly created file.
     * @throws IllegalArgumentException if file parameter is null.
     */
    public static File replaceExtension(File file, String newExtension) throws IllegalArgumentException {
        assert file != null : "File argument is null";
        return new File(replaceExtension(file.getPath(), newExtension));
    }

    /**
     * Get file path components. Return a list of path components in their natural order. List first item is path root
     * stored as an empty string; if file argument is empty returned list contains only root. If file argument is null
     * returns empty list.
     *
     * @param file file to retrieve path components.
     * @return file path components.
     */
    public static List<String> getPathComponents(File file) {
        if (file == null) {
            return Collections.emptyList();
        }
        List<String> pathComponents = new ArrayList<>();
        do {
            pathComponents.add(0, file.getName());
            file = file.getParentFile();
        }
        while (file != null);
        return pathComponents;
    }

    /**
     * Get file access path relative to a base directory. This is the relative path that allows to access given file
     * starting from the base directory. But be warned: both base directory and file should reside on the same root. Also,
     * file argument can be a directory too but base directory must be a directory indeed.
     * <p>
     * Resulting relative path uses system separator unless optional <em>forceURLPath</em> is supplied and is false; in
     * this case always use <em>/</em>, no matter platform running JVM.
     * <p>
     * Known limitations: this method always assume that both base directory and file are on the same root; failing to
     * satisfy this condition render not predictable results.
     *
     * @param baseDir      base directory,
     * @param file         file or directory to compute relative path,
     * @param forceURLPath flag to force resulting path as URL path, i.e. always uses '/' for path components separator.
     * @return file access path regarding base directory.
     * @throws IllegalArgumentException if any argument is null or empty.
     */
    public static String getRelativePath(File baseDir, File file, boolean... forceURLPath) {
        assert baseDir != null && !baseDir.getName().isEmpty() : "Base directory argument is null or empty";
        assert file != null && !file.getName().isEmpty() : "File argument is null or empty";

        List<String> baseDirPathComponents = getPathComponents(baseDir.getAbsoluteFile());
        if (!baseDirPathComponents.isEmpty()) {
            int lastIndex = baseDirPathComponents.size() - 1;
            if (baseDirPathComponents.get(lastIndex).equals(".")) {
                baseDirPathComponents.remove(lastIndex);
            } else if (baseDirPathComponents.get(lastIndex).equals("..")) {
                if (baseDirPathComponents.size() < 2) {
                    throw new IllegalStateException("Invalid base directory for relative path. It ends with '..' but has no parent directory.");
                }
                baseDirPathComponents.remove(baseDirPathComponents.size() - 1);
                baseDirPathComponents.remove(baseDirPathComponents.size() - 1);
            }
        }
        List<String> filePathComponents = getPathComponents(file.getAbsoluteFile());
        List<String> relativePath = new ArrayList<>();

        int i = 0;
        for (; i < baseDirPathComponents.size(); i++) {
            if (i == filePathComponents.size()) break;
            if (!baseDirPathComponents.get(i).equals(filePathComponents.get(i))) break;
        }
        for (int j = i; j < baseDirPathComponents.size(); j++) {
            relativePath.add("..");
        }
        for (; i < filePathComponents.size(); i++) {
            relativePath.add(filePathComponents.get(i));
        }

        return StringsUtil.join(relativePath, forceURLPath.length > 0 && forceURLPath[0] ? '/' : File.separatorChar);
    }

    /**
     * Remove ALL files and directories from a given base directory. This method remove ALL files and directory tree,
     * child of given <code>baseDir</code> but directory itself is not removed. As a result <code>baseDir</code> becomes
     * empty, that is, no children. If exception occur base directory state is not defined, that is, some files may be
     * removed and other may still be there.
     * <p>
     * For caller convenience this method returns given base directory. This allows to chain this method with
     * {@link File#delete()} method, like <code>Files.removeFilesHierarchy(dir).delete();</code>.
     *
     * @param baseDir existing, not null, base directory to clean-up.
     * @return base directory argument for method chaining, mainly for {@link File#delete()}.
     * @throws IllegalArgumentException if base directory argument is null or is not an existing directory.
     * @throws IOException              if remove operation fails.
     */
    public static File removeFilesHierarchy(File baseDir) throws IOException {
        assert baseDir != null : "Base directory argument is null";
        assert baseDir.isDirectory() : "Base directory argument is not a directory";
        removeDirectory(baseDir);
        return baseDir;
    }

    /**
     * Utility method invoked recursively to remove directory files. This method traverses <code>directory</code> files
     * and remove them, one by one. If a child file is happen to be a directory this method invoked itself with child
     * directory as parameter. After child directory is clean-up iteration continue removing child directory itself. Note
     * that given <code>directory</code> is not removed.
     * <p>
     * On remove exception <code>directory</code> state is not defined, that is, some files may be removed while others
     * may not.
     *
     * @param directory directory to remove files from.
     * @throws IOException if remove operation fails.
     */
    private static void removeDirectory(File directory) throws IOException {
        // File.listFiles() may return null is file is not a directory condition already tested before entering this method
        for (File file : directory.listFiles()) {
            if (file.isDirectory()) {
                removeDirectory(file);
            }
            if (!file.delete()) {
                throw new IOException(format("Fail to delete %s |%s|.", file.isDirectory() ? "empty directory" : "file", file));
            }
        }
    }
}
