package com.jslib.wood.build;

import com.jslib.wood.*;
import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.DocumentBuilder;
import com.jslib.wood.util.FilesUtil;
import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.mockito.stubbing.Answer;
import org.xml.sax.SAXException;

import java.io.*;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BuildFsTest {
    private DocumentBuilder documentBuilder;

    @Mock
    private Component compo;
    @Mock
    private IReferenceHandler referenceHandler;

    private File buildDir;
    private BuildFS buildFS;

    @Before
    public void beforeTest() {
        documentBuilder = DocumentBuilder.getInstance();

        File projectRoot = new File("src/test/resources/build-fs");
        buildDir = new File(projectRoot, "site");
        buildFS = new TestBuildFS(buildDir, 0);
    }

    @After
    public void afterTest() throws IOException {
        if (buildDir.exists()) {
            FilesUtil.removeFilesHierarchy(buildDir);
        }
    }

    @Test
    public void GivenPageCompo_WhenWritePage_ThenCreateFile() throws IOException, SAXException {
        // GIVEN
        Component compo = compo();
        PageDocument page = new PageDocument(compo);

        // WHEN
        buildFS.writePage(compo, page.getDocument());

        // THEN
        assertTrue(buildFile("htm/index.htm").exists());
    }

    @Test
    public void GivenPageCompo_WhenWritePageTwice_ThenSecondWriteIsIgnored() throws IOException, SAXException {
        // GIVEN
        Component compo = compo();
        PageDocument page = new PageDocument(compo);

        buildFS.writePage(compo, page.getDocument());
        assertTrue(buildFile("htm/index.htm").exists());
        assertTrue(buildFile("htm/index.htm").delete());

        // WHEN
        // second attempt to write page in the same build FS instance is ignored
        buildFS.writePage(compo, page.getDocument());

        // THEN
        // because second write page is NOOP the file is not actually created
        assertFalse(buildFile("htm/index.htm").exists());
    }

    @Test
    public void GivenPageCompoAndLanguage_WhenWritePage_ThenCreateFileOnLanguageDir() throws IOException, SAXException {
        // GIVEN
        Component compo = compo();
        PageDocument page = new PageDocument(compo);
        buildFS.setLanguage("ro");

        // WHEN
        buildFS.writePage(compo, page.getDocument());

        // THEN
        assertTrue(buildFile("ro/htm/index.htm").exists());
    }

    @Test
    public void GivenPageCompoAndBuildNumber_WhenWritePage_ThenFileCreatedWithBuildNumber() throws IOException, SAXException {
        // GIVEN
        Component compo = compo();
        PageDocument page = new PageDocument(compo);
        BuildFS buildFS = new TestBuildFS(buildDir, 4);

        // WHEN
        buildFS.writePage(compo, page.getDocument());

        // THEN
        assertTrue(buildFile("htm/index-004.htm").exists());
    }

    @Test
    public void GivenPwaManifestFile_WhenWritePwaManifest_ThenFileCreated() throws IOException {
        // GIVEN
        FilePath manifest = file("manifest.json");

        SourceReader reader = mock(SourceReader.class);
        when(reader.getSourceFile()).thenReturn(manifest);
        when(reader.read(any(char[].class), eq(0), eq(1024))).thenReturn(-1);

        // WHEN
        String path = buildFS.writePwaManifest(reader);

        // THEN
        assertTrue(buildFile("manifest.json").exists());
        assertThat(path, equalTo("manifest.json"));
    }

    @Test
    public void GivenPwaWorkerFile_WhenWritePwaWorker_ThenFileCreated() throws IOException {
        // GIVEN
        FilePath manifest = file("worker.js");

        SourceReader reader = mock(SourceReader.class);
        when(reader.getSourceFile()).thenReturn(manifest);
        when(reader.read(any(char[].class), eq(0), eq(1024))).thenReturn(-1);

        // WHEN
        buildFS.writePwaWorker(reader);

        // THEN
        assertTrue(buildFile("worker.js").exists());
    }

    @Test
    public void GivenFaviconFile_WhenWriteFavicon_ThenFileCreated() throws IOException {
        // GIVEN
        FilePath favicon = file("favicon.ico");
        assertFalse(buildFile("favicon.ico").exists());

        // WHEN
        String path = buildFS.writeFavicon(null, favicon);

        // THEN
        assertTrue(buildFile("img/favicon.ico").exists());
        assertThat(path, equalTo("../img/favicon.ico"));
    }

    @Test
    public void GivenMediaFile_WhenWritePageMedia_ThenFileCreated() throws IOException {
        // GIVEN
        FilePath mediaFile = file("background.jpg");

        // WHEN
        String path = buildFS.writePageMedia(null, mediaFile);

        // THEN
        assertTrue(buildFile("img/background.jpg").exists());
        assertThat(path, equalTo("../img/background.jpg"));
    }

    @Test
    public void GivenMediaFileAndLanguage_WhenWritePageMedia_ThenFileCreatedOnLanguageDir() throws IOException {
        // GIVEN
        FilePath mediaFile = file("background.jpg");
        buildFS.setLanguage("ro");

        // WHEN
        String path = buildFS.writePageMedia(null, mediaFile);

        // THEN
        assertTrue(buildFile("ro/img/background.jpg").exists());
        assertThat(path, equalTo("../img/background.jpg"));
    }

    @Test
    public void GivenMediaFileAndBuildNumber_WhenWritePageMedia_ThenFileCreatedWithBuildNumber() throws IOException {
        // GIVEN
        FilePath mediaFile = file("background.jpg");
        BuildFS buildFS = new TestBuildFS(buildDir, 4);

        // WHEN
        String path = buildFS.writePageMedia(null, mediaFile);

        // THEN
        assertTrue(buildFile("img/background-004.jpg").exists());
        assertThat(path, equalTo("../img/background-004.jpg"));
    }

    @Test(expected = IOException.class)
    public void GivenMediaFile_WhenFailToWritePageMedia_ThenIOException() throws IOException {
        // GIVEN
        FilePath mediaFile = file("background.jpg");

        // need to ensure stream is closed otherwise file delete on @After hook will fail
        doAnswer((Answer<Void>) invocation -> {
            OutputStream stream = invocation.getArgument(0);
            stream.close();
            throw new IOException();
        }).when(mediaFile).copyTo(any(OutputStream.class));

        // WHEN
        buildFS.writePageMedia(null, mediaFile);

        // THEN
    }

    @Test
    public void GivenMediaFile_WhenWriteStyleMedia_ThenFileCreated() throws IOException {
        // GIVEN
        FilePath mediaFile = file("background.jpg");

        // WHEN
        String path = buildFS.writeStyleMedia(mediaFile);

        // THEN
        assertThat(path, equalTo("../img/background.jpg"));
        assertTrue(buildFile("img/background.jpg").exists());
    }

    @Test
    public void GivenMediaFileAndLanguage_WhenWriteStyleMedia_ThenFileCreatedOnLanguageDir() throws IOException {
        // GIVEN
        FilePath mediaFile = file("background.jpg");
        buildFS.setLanguage("ro");

        // WHEN
        String path = buildFS.writeStyleMedia(mediaFile);

        // THEN
        assertTrue(buildFile("ro/img/background.jpg").exists());
        assertThat(path, equalTo("../img/background.jpg"));
    }

    @Test
    public void GivenMediaFileAndBuildNumber_WhenWriteStyleMedia_ThenFileCreatedWithBuildNumber() throws IOException {
        // GIVEN
        FilePath mediaFile = file("background.jpg");
        BuildFS buildFS = new TestBuildFS(buildDir, 4);

        // WHEN
        String path = buildFS.writeStyleMedia(mediaFile);

        // THEN
        assertThat(path, equalTo("../img/background-004.jpg"));
        assertTrue(buildFile("img/background-004.jpg").exists());
    }

    @Test(expected = IOException.class)
    public void GivenMediaFile_WhenFailToWriteStyleMedia_ThenIOException() throws IOException {
        // GIVEN
        FilePath mediaFile = file("background.jpg");

        // need to ensure stream is closed otherwise file delete on @After hook will fail
        doAnswer((Answer<Void>) invocation -> {
            OutputStream stream = invocation.getArgument(0);
            stream.close();
            throw new IOException();
        }).when(mediaFile).copyTo(any(OutputStream.class));

        // WHEN
        buildFS.writeStyleMedia(mediaFile);

        // THEN
    }

    @Test
    public void GiveStyleFile_WhenWriteStyle_ThenFileCreated() throws IOException {
        // GIVEN
        FilePath sourceDir = mock(FilePath.class);
        when(sourceDir.filter(any())).thenReturn(Collections.emptyList());

        FilePath styleFile = file("style.css");
        when(styleFile.getParentDir()).thenReturn(sourceDir);

        // WHEN
        String path = buildFS.writeStyle(null, styleFile, referenceHandler);

        // THEN
        assertTrue(buildFile("css/style.css").exists());
        assertThat(path, equalTo("../css/style.css"));
    }

    @Test
    public void GiveStyleFileAndLanguage_WhenWriteStyle_ThenFileCreatedOnLanguageDir() throws IOException {
        // GIVEN
        FilePath sourceDir = mock(FilePath.class);
        when(sourceDir.filter(any())).thenReturn(Collections.emptyList());

        FilePath styleFile = file("style.css");
        when(styleFile.getParentDir()).thenReturn(sourceDir);

        buildFS.setLanguage("ro");

        // WHEN
        String path = buildFS.writeStyle(null, styleFile, referenceHandler);

        // THEN
        assertTrue(buildFile("ro/css/style.css").exists());
        assertThat(path, equalTo("../css/style.css"));
    }

    @Test
    public void GiveStyleFileAndBuildNumber_WhenWriteStyle_ThenFileCreatedWithBuildNumber() throws IOException {
        // GIVEN
        FilePath sourceDir = mock(FilePath.class);
        when(sourceDir.filter(any())).thenReturn(Collections.emptyList());

        FilePath styleFile = file("style.css");
        when(styleFile.getParentDir()).thenReturn(sourceDir);

        BuildFS buildFS = new TestBuildFS(buildDir, 4);

        // WHEN
        String path = buildFS.writeStyle(null, styleFile, referenceHandler);

        // THEN
        assertTrue(buildFile("css/style-004.css").exists());
        assertThat(path, equalTo("../css/style-004.css"));
    }

    @Test(expected = IOException.class)
    public void GiveStyleFile_WhenFailToWriteStyle_ThenIOException() throws IOException {
        // GIVEN
        FilePath sourceDir = mock(FilePath.class);
        when(sourceDir.filter(any())).thenReturn(Collections.emptyList());

        FilePath styleFile = file("style.css");
        when(styleFile.getParentDir()).thenReturn(sourceDir);
        when(styleFile.getReader()).thenReturn(new ExceptionalReaderTest());

        // WHEN
        buildFS.writeStyle(null, styleFile, referenceHandler);

        // THEN
    }

    @Test
    public void GivenScriptFile_WhenWriteScript_ThenFileCreated() throws IOException {
        // GIVEN
        FilePath scriptFile = file("Index.js");

        // WHEN
        String path = buildFS.writeScript(null, scriptFile, referenceHandler);

        // THEN
        assertTrue(buildFile("js/Index.js").exists());
        assertThat(path, equalTo("../js/Index.js"));
    }

    @Test
    public void GivenScriptFileAndLanguage_WhenWriteScript_ThenFileCreatedOnLanguageDir() throws IOException {
        // GIVEN
        FilePath scriptFile = file("Index.js");
        buildFS.setLanguage("ro");

        // WHEN
        String path = buildFS.writeScript(null, scriptFile, referenceHandler);

        // THEN
        assertTrue(buildFile("ro/js/Index.js").exists());
        assertThat(path, equalTo("../js/Index.js"));
    }

    @Test
    public void GivenScriptFileAndBuildNumber_WhenWriteScript_ThenFileCreatedWithBuildNumber() throws IOException {
        // GIVEN
        FilePath scriptFile = file("Index.js");
        BuildFS buildFS = new TestBuildFS(buildDir, 4);

        // WHEN
        String path = buildFS.writeScript(null, scriptFile, referenceHandler);

        // THEN
        assertTrue(buildFile("js/Index-004.js").exists());
        assertThat(path, equalTo("../js/Index-004.js"));
    }

    @Test(expected = IOException.class)
    public void GivenScriptFile_WhenFailToWriteScript_ThenIOException() throws IOException {
        // GIVEN
        FilePath scriptFile = file("Index.js");
        when(scriptFile.getReader()).thenReturn(new ExceptionalReaderTest());

        // WHEN
        buildFS.writeScript(null, scriptFile, referenceHandler);

        // THEN
    }

    @Test
    public void GivenDirName_WhenCreateDirectory_ThenDirCreated() {
        // GIVEN
        String dirName = "images";

        // WHEN
        File dir = buildFS.createDirectory(dirName);

        // THEN
        assertThat(dir, isDir("src/test/resources/build-fs/site/images"));
    }

    @Test
    public void GivenDirNameAndLanguage_WhenCreateDirectory_ThenSubdirCreatedIntoLanguageDir() {
        // GIVEN
        String dirName = "images";
        buildFS.setLanguage("ro");

        // WHEN
        File dir = buildFS.createDirectory(dirName);

        // THEN
        assertThat(dir, isDir("src/test/resources/build-fs/site/ro/images"));
    }

    @SuppressWarnings("ConstantConditions")
    @Test(expected = AssertionError.class)
    public void GivenNullLanguage_WhenSetLanguage_ThenAssertionError() {
        // GIVEN
        String language = null;

        // WHEN
        buildFS.setLanguage(language);

        // THEN
    }

    @Test(expected = WoodException.class)
    public void GivenFileWithMissingExtensionAndBuildNumber_WhenWriteFile_ThenWoodException() throws IOException {
        // GIVEN
        FilePath mediaFile = file("style");
        BuildFS buildFS = new TestBuildFS(buildDir, 4);

        // WHEN
        buildFS.writeStyleMedia(mediaFile);

        // THEN
    }

    // --------------------------------------------------------------------------------------------

    private File buildFile(String path) {
        return new File(buildDir, path);
    }

    private Component compo() throws SAXException {
        Document doc = documentBuilder.parseXML("<body></body>");
        when(compo.getLayout()).thenReturn(doc.getRoot());
        when(compo.getLayoutFileName()).thenReturn("index.htm");
        return compo;
    }

    private static FilePath file(String fileName) throws IOException {
        FilePath file = mock(FilePath.class);
        when(file.exists()).thenReturn(true);
        when(file.getName()).thenReturn(fileName);
        when(file.getReader()).thenReturn(new StringReader("CONTENT"));

        // need to ensure stream is closed otherwise file delete on @After hook will fail
        doAnswer((Answer<Void>) invocation -> {
            OutputStream stream = invocation.getArgument(0);
            stream.close();
            return null;
        }).when(file).copyTo(any(OutputStream.class));

        return file;
    }

    private static Matcher<File> isDir(String path) {
        return new BaseMatcher<File>() {
            @Override
            public boolean matches(Object item) {
                return item.equals(new File(path.replace('/', File.separatorChar))) && ((File) item).isDirectory();
            }

            @Override
            public void describeTo(Description description) {
                description.appendText(path);
            }
        };
    }

    private static class TestBuildFS extends BuildFS {
        public TestBuildFS(File buildDir, int buildNumber) {
            super(buildDir, buildNumber);
        }

        @Override
        protected File getPageDir(Component compo) {
            return createDirectory("htm");
        }

        @Override
        protected File getStyleDir() {
            return createDirectory("css");
        }

        @Override
        protected File getScriptDir() {
            return createDirectory("js");
        }

        @Override
        protected File getMediaDir() {
            return createDirectory("img");
        }

        @Override
        protected File pwaDir() {
            return buildDir;
        }

        @Override
        protected File getFontDir() {
            return createDirectory("font");
        }

        @Override
        protected File getFilesDir() {
            return createDirectory("files");
        }

        @Override
        protected String formatPageName(String pageName) {
            return pageName;
        }

        @Override
        protected String formatStyleName(FilePath styleFile) {
            return styleFile.getName();
        }

        @Override
        protected String formatScriptName(FilePath scriptFile) {
            return scriptFile.getName();
        }

        @Override
        protected String formatMediaName(FilePath mediaFile) {
            return mediaFile.getName();
        }
    }

    private static class ExceptionalReaderTest extends Reader {
        @Override
        public int read(char[] cbuf, int off, int len) throws IOException {
            throw new IOException();
        }

        @Override
        public void close() throws IOException {
            throw new IOException();
        }
    }
}
