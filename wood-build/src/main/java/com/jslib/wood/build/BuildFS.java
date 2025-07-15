package com.jslib.wood.build;

import com.jslib.wood.*;
import com.jslib.wood.dom.Document;
import com.jslib.wood.util.FilesUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.*;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.util.ArrayList;
import java.util.List;

import static com.jslib.wood.util.StringsUtil.format;

/**
 * Build file system is the directory structure for site files. Different build file systems are supported but a default
 * implementation exists, see {@link DefaultBuildFS}.
 * <p>
 * BuildFS is the target of the building process. It supplies specialized write methods whereas actual directory and file names
 * are solved by subclasses, concrete implementations. All write methods take care to avoid multiple processing of the same
 * file. Also append build number to target file name, if {@link #buildNumber} is non-zero.
 * <p>
 * If project is multi-language, BuildFS is language sensitive. There is optional {@link #setLanguage(String)} that is used, for
 * multi-language build, to store current processing language; when compute paths insert the language too. Language is BCP
 * encoded: language is always lower case and country, if present, upper case separated by hyphen.
 * <p>
 * Build file system implementations are not thread safe. Do not use BuildFS instances into a concurrent context.
 *
 * @author Iulian Rotaru
 * @since 1.0
 */
public abstract class BuildFS {
    private static final Logger log = LoggerFactory.getLogger(BuildFS.class);

    /**
     * Project reference.
     */
    protected final File buildDir;

    /**
     * Build number or 0 if not set. Specialized write methods take care to append build number to target file name.
     */
    private final int buildNumber;

    /**
     * Processed files cache to avoid multiple processing of the same file.
     */
    private final List<File> processedFiles;

    /**
     * Current processing language for multi-language build. Language is inserted into directory paths and URL absolute paths.
     * For projects without multi-language support this field is always null.
     */
    private String language;

    /**
     * Protected constructor.
     *
     * @param buildDir    builder project directory,
     * @param buildNumber optional build number, 0 if not used.
     * @throws IllegalArgumentException if project parameter is null or build number is negative.
     */
    protected BuildFS(File buildDir, int buildNumber) {
        log.trace("BuildFS(File buildDir, int buildNumber)");
        assert buildDir != null : "Build directory argument is null";
        assert buildNumber >= 0 : "Build number argument is negative";

        this.buildDir = buildDir;
        this.buildNumber = buildNumber;
        this.processedFiles = new ArrayList<>();
    }

    /**
     * Set current processing language for multi-language build.
     *
     * @param language current processing language.
     * @see #language
     */
    public void setLanguage(String language) {
        assert language != null && !language.isEmpty() : "Language argument is null or empty";
        this.language = language;
    }

    /**
     * Serialize page document to pages directory. Target file name is derived from page component name argument; uses
     * {@link #formatPageName(String)} to format it. Stores target file into {@link #processedFiles} in order to avoid multiple
     * processing. Also takes care to append {@link #buildNumber}, if set.
     *
     * @param page     page component,
     * @param document page document.
     * @throws IOException if write fails.
     */
    public void writePage(Component page, Document document) throws IOException {
        File targetFile = new File(getPageDir(page), insertBuildNumber(formatPageName(page.getLayoutFileName())));
        if (!processedFiles.contains(targetFile)) {
            document.serialize(new OutputStreamWriter(Files.newOutputStream(targetFile.toPath()), StandardCharsets.UTF_8), true);
            processedFiles.add(targetFile);
        }
    }

    public String getPageLayout(FilePath layoutFile) {
        File targetFile = new File(getPageDir(null), insertBuildNumber(formatPageName(layoutFile.getName() + CT.DOT_LAYOUT_EXT)));
        return FilesUtil.getRelativePath(getPageDir(null), targetFile, true);
    }

    /**
     * Write favicon to media directory. Target file name is the file name of the source favicon parameter. Stores target file
     * into {@link #processedFiles} in order to avoid multiple processing.
     * <p>
     * Returns favicon URL path, relative to site page location. Returned URL path is ready to be inserted into page document.
     *
     * @param favicon favicon file.
     * @return URL path of favicon file.
     * @throws IOException if favicon file write fails.
     */
    public String writeFavicon(Component page, FilePath favicon) throws IOException {
        File targetFile = new File(getMediaDir(), favicon.getName());
        if (!processedFiles.contains(targetFile)) {
            favicon.copyTo(Files.newOutputStream(targetFile.toPath()));
            processedFiles.add(targetFile);
        }
        return FilesUtil.getRelativePath(getPageDir(page), targetFile, true);
    }

    public String writePwaManifest(SourceReader manifestReader) throws IOException {
        File targetFile = new File(pwaDir(), manifestReader.getSourceFile().getName());
        if (!processedFiles.contains(targetFile)) {
            copy(manifestReader, targetFile);
            processedFiles.add(targetFile);
        }
        return FilesUtil.getRelativePath(pwaDir(), targetFile, true);
    }

    public void writePwaWorker(SourceReader workerReader) throws IOException {
        File targetFile = new File(pwaDir(), workerReader.getSourceFile().getName());
        if (!processedFiles.contains(targetFile)) {
            copy(workerReader, targetFile);
            processedFiles.add(targetFile);
        }
    }

    private static void copy(SourceReader sourceReader, File targetFile) throws IOException {
        char[] buffer = new char[1024];
        try (Writer writer = new BufferedWriter(new FileWriter(targetFile))) {
            int length;
            while ((length = sourceReader.read(buffer, 0, 1024)) != -1) {
                writer.write(buffer, 0, length);
            }
        }
    }

    /**
     * Write media file referenced from site page. Target file name is the media file name formated by
     * {@link #formatMediaName(FilePath)}. Stores target file into {@link #processedFiles} in order to avoid multiple
     * processing. Also takes care to append {@link #buildNumber}, if set.
     * <p>
     * Returns media file URL path, relative to site page location. Returned URL path is ready to be inserted into page
     * document.
     *
     * @param mediaFile media file.
     * @return media file URL path.
     * @throws IOException if media file write fails.
     */
    public String writePageMedia(Component page, FilePath mediaFile) throws IOException {
        return writeFile(getPageDir(page), getMediaDir(), mediaFile);
    }

    /**
     * Write media file referenced from style. Target file name is the media file name formated by
     * {@link #formatMediaName(FilePath)}. Stores target file into {@link #processedFiles} in order to avoid multiple
     * processing. Also takes care to append {@link #buildNumber}, if set.
     * <p>
     * Returns media file URL path, relative to style file location. Returned URL path is ready to be inserted into page
     * document.
     *
     * @param mediaFile media file.
     * @return media file URL path.
     * @throws IOException if media file write fails.
     */
    public String writeStyleMedia(FilePath mediaFile) throws IOException {
        return writeFile(getStyleDir(), getMediaDir(), mediaFile);
    }

    public String writeScriptMedia(FilePath mediaFile) throws IOException {
        return writeFile(getScriptDir(), getMediaDir(), mediaFile);
    }

    public String writeManifestMedia(FilePath mediaFile) throws IOException {
        return writeFile(pwaDir(), getMediaDir(), mediaFile);
    }

    public String writeFontFile(FilePath fontFile) throws IOException {
        return writeFile(getStyleDir(), getFontDir(), fontFile);
    }

    public String writePageFile(Component page, FilePath genericFile) throws IOException {
        return writeFile(getPageDir(page), getFilesDir(), genericFile);
    }

    public String writeScriptFile(FilePath genericFile) throws IOException {
        return writeFile(getScriptDir(), getFilesDir(), genericFile);
    }

    private String writeFile(File sourceDir, File targetDir, FilePath file) throws IOException {
        File targetFile = new File(targetDir, insertBuildNumber(formatMediaName(file)));
        if (!processedFiles.contains(targetFile)) {
            file.copyTo(Files.newOutputStream(targetFile.toPath()));
            processedFiles.add(targetFile);
        }
        return FilesUtil.getRelativePath(sourceDir, targetFile, true);
    }

    /**
     * Write style file using external references' handler. References handler is used for resources processing. Returns URL path
     * of the written style file, relative to page location, ready to be inserted into page document.
     * <p>
     * Stores target file into {@link #processedFiles} in order to avoid multiple processing. Also takes care to append
     * {@link #buildNumber}, if set.
     *
     * @param styleFile        style file,
     * @param referenceHandler resource references handler.
     * @return URL path relative to page location.
     * @throws IOException if write operation fails.
     */
    public String writeStyle(Component page, FilePath styleFile, IReferenceHandler referenceHandler) throws IOException {
        String fileName = insertBuildNumber(formatStyleName(styleFile));
        File targetFile = new File(getStyleDir(), fileName);
        if (!processedFiles.contains(targetFile)) {
            FilesUtil.copy(new SourceReader(new StyleReader(styleFile), styleFile, referenceHandler), new OutputStreamWriter(Files.newOutputStream(targetFile.toPath()), StandardCharsets.UTF_8));
            processedFiles.add(targetFile);
        }
        return FilesUtil.getRelativePath(getPageDir(page), targetFile, true);
    }

    public String writeShadowStyle(Component page, FilePath styleFile) throws IOException {
        return writeFile(getPageDir(page), getStyleDir(), styleFile);
    }

    /**
     * Write script file using external references' handler. References handler is used for resources processing. Returns URL
     * path of the written script file, relative to page location, ready to be inserted into page document. Stores target file
     * into {@link #processedFiles} in order to avoid multiple processing. Also takes care to append {@link #buildNumber}, if
     * set.
     *
     * @param scriptFile       script file,
     * @param referenceHandler resource references handler.
     * @return URL path relative to page location.
     * @throws IOException if write operation fails.
     */
    public String writeScript(Component page, FilePath scriptFile, IReferenceHandler referenceHandler) throws IOException {
        File targetFile = getScriptFile(insertBuildNumber(formatScriptName(scriptFile)));
        if (!processedFiles.contains(targetFile)) {
            FilesUtil.copy(new SourceReader(scriptFile, referenceHandler), new OutputStreamWriter(Files.newOutputStream(targetFile.toPath()), StandardCharsets.UTF_8));
            processedFiles.add(targetFile);
        }
        return FilesUtil.getRelativePath(getPageDir(page), targetFile, true);
    }

    public String writeScript(Component page, SourceReader sourceReader) throws IOException {
        File targetFile = getScriptFile(insertBuildNumber(formatScriptName(sourceReader.getSourceFile())));
        if (!processedFiles.contains(targetFile)) {
            FilesUtil.copy(sourceReader, new OutputStreamWriter(Files.newOutputStream(targetFile.toPath()), StandardCharsets.UTF_8));
            processedFiles.add(targetFile);
        }
        return FilesUtil.getRelativePath(getPageDir(page), targetFile, true);
    }

    private File getScriptFile(String scriptFileName) throws IOException {
        File file = new File(getScriptDir(), scriptFileName);
        if (!file.getParentFile().isDirectory() && !file.getParentFile().mkdirs()) {
            throw new IOException(format("Fail to create script directory %s", file.getParentFile()));
        }
        return file;
    }

    // ------------------------------------------------------
    // Private helper methods

    /**
     * Insert build number into file name. {@link #buildNumber Build number} should be already set before invoking this method.
     * If {@link #buildNumber} is not set, that is, its value is 0 this method returns original file name.
     * <p>
     * Build number is added to file base name separated by dash. Number is padded with 0 to three digits. Extension is
     * preserved.
     *
     * @param fileName file name.
     * @return numbered file name.
     * @throws WoodException if file does not have extension.
     */
    private String insertBuildNumber(String fileName) {
        if (buildNumber == 0) {
            return fileName;
        }
        int extensionSeparatorIndex = fileName.lastIndexOf('.');
        if (extensionSeparatorIndex == -1) {
            throw new WoodException("Invalid file name %s; missing extension", fileName);
        }
        String baseName = fileName.substring(0, extensionSeparatorIndex);
        String extension = fileName.substring(extensionSeparatorIndex + 1);
        return String.format("%s-%03d.%s", baseName, buildNumber, extension);
    }

    // ------------------------------------------------------
    // Interface to be implemented by concrete build file system.

    /**
     * Get the directory that stores site pages. Optional <code>page</code> parameter can be used to create alternative pages
     * directories based on component properties, most likely {@link Component#getResourcesGroup()}.
     *
     * @param page optional page component, null if not provided.
     * @return pages directory.
     */
    protected abstract File getPageDir(Component page);

    /**
     * Get the directory that stores site styles.
     *
     * @return styles directory.
     */
    protected abstract File getStyleDir();

    /**
     * Get the directory that stores site scripts.
     *
     * @return scripts directory.
     */
    protected abstract File getScriptDir();

    /**
     * Get the directory that stores site media files, all types in the same place.
     *
     * @return media files directory.
     */
    protected abstract File getMediaDir();

    /**
     * Site directory storing PWA related files: manifest file and service worker script.
     *
     * @return PWA directory.
     */
    protected abstract File pwaDir();

    /**
     * Get the directory that stores font files, if build file system implementation decides to keep them separated from style
     * files.
     *
     * @return font files directory.
     */
    protected abstract File getFontDir();

    /**
     * Get the directory that stores generic files like license or other legal stuff but not limited to.
     *
     * @return generic files' directory.
     */
    protected abstract File getFilesDir();

    /**
     * Format the page file name. Implementation is free to pre-process page name in every way, including returning original
     * name unchanged.
     *
     * @param pageName page name.
     * @return formatted page name.
     */
    protected abstract String formatPageName(String pageName);

    /**
     * Format the style file name. Implementation is free to pre-process file name in every way, including returning original
     * name unchanged.
     *
     * @param styleFile style file.
     * @return formatted style name.
     */
    protected abstract String formatStyleName(FilePath styleFile);

    /**
     * Format the script file name. Implementation is free to pre-process file name in every way, including returning original
     * name unchanged.
     *
     * @param scriptFile script file.
     * @return formatted style name.
     */
    protected abstract String formatScriptName(FilePath scriptFile);

    /**
     * Format the media file name. Implementation is free to pre-process file name in every way, including returning original
     * name unchanged.
     *
     * @param mediaFile media file.
     * @return formatted style name.
     */
    protected abstract String formatMediaName(FilePath mediaFile);

    // ------------------------------------------------------
    // Helper methods common to all build file system implementations

    /**
     * Create named directory into site build. This factory method takes into account current processing language, see
     * {@link #language}. If project is multi-language, this method prefixes directory name with current language; if project is
     * single language, language subdirectory is not created.
     * <p>
     * Both language directory name and <code>lang</code> attribute has the same format; hence language encoded into request
     * path has also the same format.
     * <p>
     * Create directory, if it does not already exist.
     *
     * @param dirName name of directory to create.
     * @return created directory.
     */
    protected File createDirectory(String dirName) {
        // clone build directory since need to keep it unchanged
        File dir = new File(buildDir.getPath());

        // if project is multi-language create a subdirectory with name equal with current language
        if (language != null) {
            dir = new File(dir, language);
        }

        if (!".".equals(dirName)) {
            dir = new File(dir, dirName);
        }
        if (!dir.exists()) {
            if (!dir.mkdirs()) {
                log.error("Fail to create directory {}", dir);
            }
        }
        return dir;
    }
}
