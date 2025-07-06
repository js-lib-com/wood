package com.jslib.wood;

import com.jslib.wood.impl.FilesHandler;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.ArrayList;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DirPathTest {
    @Mock
    private Project project;

    @Before
    public void beforeTest() {
        when(project.getProjectRoot()).thenReturn(new File("."));
    }

    @Test
    public void GivenValidPath_WhenConstructor_ThenInternalStateInitialized() {
        assertDirPath("res/", "res/", "res");
        assertDirPath("res/path/compo/", "res/path/compo/", "compo");
        assertDirPath("res/compo/video-player/", "res/compo/video-player/", "video-player");
        assertDirPath("lib/js-lib/", "lib/js-lib/", "js-lib");
        assertDirPath("script/js/wood/test/", "script/js/wood/test/", "test");
        assertDirPath("gen/js/wood/test/", "gen/js/wood/test/", "test");
    }

    private void assertDirPath(String pathValue, String value, String name) {
        FilePath dirPath = new FilePath(project, pathValue);
        assertThat(dirPath.value(), equalTo(value));
        assertThat(dirPath.toString(), equalTo(value));
        assertThat(dirPath.getName(), equalTo(name));
        assertFalse(dirPath.isProjectRoot());
        assertFalse(dirPath.isComponent());
    }

    @Test
    public void GivenValidDotPath_WhenConstructor_ThenInitializeAsProjectRoot() {
        // GIVEN
        String path = ".";

        // WHEN
        FilePath dirPath = new FilePath(project, path);

        // THEN
        assertThat(dirPath.value(), equalTo("."));
        assertThat(dirPath.toString(), equalTo("."));
        assertThat(dirPath.getName(), equalTo("."));
        assertTrue(dirPath.isProjectRoot());
        assertFalse(dirPath.isComponent());
    }

    @Test(expected = WoodException.class)
    public void GivenValidEmptyPath_WhenConstructor_ThenWoodException() {
        new FilePath(project, "");
    }

    @Test(expected = AssertionError.class)
    public void GivenNullPath_WhenConstructor_ThenAssertionError() {
        new FilePath(project, (String) null);
    }

    @Test(expected = WoodException.class)
    public void GivenPathWithUnderscore_WhenConstruct_ThenWoodException() {
        new FilePath(project, "res/page_en");
    }

    @Test
    public void GivenDirPathWithoutTrailingSeparator_WhenGetFilePath_ThenNotNullValue() {
        // GIVEN
        when(project.createFilePath("res/page/strings.xml")).thenReturn(mock(FilePath.class));
        FilePath dirPath = new FilePath(project, "res/page");

        // WHEN
        FilePath filePath = dirPath.getFilePath("strings.xml");

        // THEN
        assertThat(filePath, notNullValue());
    }

    @Test
    public void GivenDirPathWithoutTrailingSeparator_WhenGetSubdirPath_ThenNotNullValue() {
        // GIVEN
        when(project.createFilePath("res/page/media/")).thenReturn(mock(FilePath.class));
        FilePath dirPath = new FilePath(project, "res/page");

        // WHEN
        FilePath subdirPath = dirPath.getSubDirectoryPath("media");

        // THEN
        assertThat(subdirPath, notNullValue());
    }

    @Test
    public void GivenMediaSubDirWithTrailingSeparator_WhenGetSubdirPath_ThenTrailingSeparatorPreserved() {
        // GIVEN
        FilePath subdirPath = mock(FilePath.class);
        when(subdirPath.value()).thenReturn("res/page/media/");
        when(project.createFilePath("res/page/media/")).thenReturn(subdirPath);

        // WHEN
        FilePath dirPath = new FilePath(project, "res/page/");
        subdirPath = dirPath.getSubDirectoryPath("media/");

        // THEN
        assertThat(subdirPath, notNullValue());
        assertThat(subdirPath.value(), equalTo("res/page/media/"));
    }

    @Test
    public void GivenLongPath_WhenGetPathSegments_ThenNotEmptyStringsList() {
        // GIVEN
        FilePath dir = new FilePath(project, "script/js/tools/wood/tests/");

        // WHEN
        List<String> segments = dir.getPathSegments();

        // THEN
        assertThat(segments, notNullValue());
        assertThat(segments, hasSize(5));
        assertThat(segments, hasItems("script", "js", "tools", "wood", "tests"));
    }

    @Test
    public void GivenSingleSegmentPath_WhenGetPathSegments_ThenListWithOneItem() {
        // GIVEN
        FilePath dir = new FilePath(project, "script/");

        // WHEN
        List<String> segments = dir.getPathSegments();

        // THEN
        assertThat(segments, notNullValue());
        assertThat(segments, hasSize(1));
        assertThat(segments, hasItems("script"));
    }

    @Test
    public void GivenDirWithFiles_WhenLoopForEach_ThenCollectAllFiles() {
        // GIVEN
        File[] sources = new File[]{new SourceFile("compo.htm"), new SourceFile("compo.css")};
        initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        List<String> files = new ArrayList<>();
        for (FilePath file : dirPath) {
            files.add(file.value());
        }

        // THEN
        assertThat(files, hasSize(2));
        assertThat(files, hasItems("compo.htm", "compo.css"));
    }

    /**
     * FilePath iterator() method returns empty iterator if underlying listFiles() returns null. Note that it silently
     * ignore this error condition considering it as unexpected. This may be subject to rethink.
     */
    @Test
    public void GivenNullListFile_WhenIterableForLoop_ThenEmptyFilesButNotException() {
        // GIVEN
        FilePath dirPath = new FilePath(project, directory(null));

        // WHEN
        final List<String> files = new ArrayList<>();
        for (FilePath file : dirPath) {
            files.add(file.value());
        }

        // THEN
        assertThat(files, empty());
    }

    @Test
    public void GivenEmptyDir_WhenIterableForLoop_ThenEmptyFiles() {
        // GIVEN
        FilePath dirPath = new FilePath(project, directory(new File[]{}));

        // WHEN
        final List<String> files = new ArrayList<>();
        for (FilePath file : dirPath) {
            files.add(file.value());
        }

        // THEN
        assertThat(files, empty());
    }

    /**
     * FilePath iterator() method returns empty iterator if file does not exist.
     */
    @Test
    public void GivenMissingDir_WhenIterableForLoop_ThenEmptyFiles() {
        // GIVEN
        File dir = mock(File.class);
        when(dir.getPath()).thenReturn("res/compo");
        when(dir.exists()).thenReturn(false);
        FilePath dirPath = new FilePath(project, dir);

        // WHEN
        List<String> files = new ArrayList<>();
        for (FilePath file : dirPath) {
            files.add(file.value());
        }

        // THEN
        assertThat(files, empty());
    }

    @Test(expected = UnsupportedOperationException.class)
    public void GivenValidDir_WhenIteratorRemove_ThenUnsupportedOperationException() {
        // GIVEN
        FilePath dirPath = new FilePath(project, directory(new File[]{}));

        // WHEN
        dirPath.iterator().remove();

        // THEN
    }

    @Test
    public void GivenDirWithFiles_WhenFilterByStyle_ThenFoundStyle() {
        // GIVEN
        File[] sources = new File[]{new SourceFile("compo.htm"), new SourceFile("compo.css")};
        initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        List<FilePath> files = dirPath.filter(FilePath::isStyle);

        // THEN
        assertThat(files, hasSize(1));
        assertThat(files.get(0).value(), equalTo("compo.css"));
    }

    @Test
    public void GivenDirWithFiles_WhenFindFirstByStyle_ThenFoundStyle() {
        // GIVEN
        File[] sources = new File[]{new SourceFile("compo.htm"), new SourceFile("compo.css")};
        initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        FilePath file = dirPath.findFirst(FilePath::isStyle);

        // THEN
        assertThat(file, notNullValue());
        assertThat(file.value(), equalTo("compo.css"));
    }

    @Test
    public void GivenDirWithFiles_WhenFindFirstByNotExisting_ThenNull() {
        // GIVEN
        File[] sources = new File[]{new SourceFile("compo.htm"), new SourceFile("compo.css")};
        initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        FilePath file = dirPath.findFirst(FilePath::isScript);

        // THEN
        assertThat(file, nullValue());
    }

    @Test
    public void GivenDirWithFiles_WhenFilesHandler_ThenAllFilesProcessed() {
        // GIVEN
        File[] sources = new File[]{new SourceFile("compo.htm"), new SourceFile("compo.css")};
        FilePath[] paths = initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        dirPath.files(new FilesHandler() {
            private int index = 0;

            @Override
            public void onFile(FilePath filePath) {
                // THEN
                assertThat(filePath, equalTo(paths[index++]));
            }
        });
    }

    @Test
    public void GivenRootDir_WhenFilesHandler_ThenProcessProjectDescriptor() {
        // GIVEN
        File[] sources = new File[]{new SourceFile("project.xml")};
        FilePath[] paths = initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        dirPath.files(new FilesHandler() {
            private int index = 0;

            @Override
            public void onFile(FilePath filePath) {
                // THEN
                assertThat(filePath, equalTo(paths[index++]));
            }
        });
    }

    @Test
    public void GivenDirWithFiles_WhenFilesHandlerAccept_ThenProcessOnlyAccepted() {
        // GIVEN
        File[] sources = new File[]{new SourceFile("compo.htm"), new SourceFile("compo.css")};
        initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        final List<String> files = new ArrayList<>();
        dirPath.files(new FilesHandler() {
            @Override
            public boolean accept(FilePath file) {
                return file.isLayout();
            }

            @Override
            public void onFile(FilePath file) {
                files.add(file.value());
            }
        });

        // THEN
        assertThat(files, hasSize(1));
        assertThat(files, hasItems("compo.htm"));
    }

    @Test
    public void GivenDirWithHiddenFile_WhenFilesHandler_ThenDoNotProcessHiddenFile() {
        // GIVEN
        File[] sources = new File[]{new SourceFile(".gitignore"), new SourceFile("compo.htm"), new SourceFile("compo.css")};
        FilePath[] paths = initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        dirPath.files(new FilesHandler() {
            private int index = 1;

            @Override
            public void onFile(FilePath filePath) {
                // THEN
                assertThat(index, lessThan(3));
                assertThat(filePath, equalTo(paths[index++]));
            }
        });
    }

    @Test
    public void GivenDirWithSubdir_WhenFilesHandler_ThenSubdirProcessed() {
        // GIVEN
        File[] sources = new File[]{new SourceDirectory("media")};
        FilePath subdir = new FilePath(project, "media/");
        when(project.createFilePath(sources[0])).thenReturn(subdir);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        dirPath.files(new FilesHandler() {
            @Override
            public void onDirectory(FilePath dir) {
                // THEN
                assertThat(dir, equalTo(subdir));
            }
        });
    }

    @Test
    public void GivenDirWithSubdirWithTrailingSeparator_WhenFilesHandler_ThenTrailingSeparatorPreserved() {
        // GIVEN
        File[] sources = new File[]{new SourceDirectory("media/")};
        FilePath[] paths = initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        dirPath.files(new FilesHandler() {
            @Override
            public void onDirectory(FilePath dir) {
                // THEN
                assertThat(dir, equalTo(paths[0]));
            }
        });
    }

    @Test
    public void GivenDirWithSubdirAndFiles_WhenFilesHandle_ThenProcessSubdirAndFiles() {
        // GIVEN
        File[] sources = new File[]{new SourceDirectory("media"), new SourceFile("compo.htm"), new SourceFile("compo.css")};
        initSources(sources);
        FilePath dirPath = new FilePath(project, directory(sources));

        // WHEN
        final List<String> result = new ArrayList<>();
        dirPath.files(new FilesHandler() {
            @Override
            public void onDirectory(FilePath dir) {
                // THEN
                assertThat(dir.value(), equalTo("media/"));
            }

            @Override
            public void onFile(FilePath file) {
                result.add(file.value());
            }
        });

        // THEN
        assertThat(result, hasSize(2));
        assertThat(result, hasItems("compo.htm", "compo.css"));
    }

    @Test
    public void GivenNotExistingDir_WhenFilesHandler_ThenHandlerNotInvoked() {
        // GIVEN
        File dir = mock(File.class);
        when(dir.getPath()).thenReturn("res/compo");
        when(dir.exists()).thenReturn(false);
        FilePath dirPath = new FilePath(project, dir);

        // WHEN
        dirPath.files(new FilesHandler() {
            @Override
            public void onDirectory(FilePath dir) {
                // THEN
                fail();
            }

            @Override
            public boolean accept(FilePath file) {
                // THEN
                fail();
                return true;
            }

            @Override
            public void onFile(FilePath file) {
                // THEN
                fail();
            }
        });
    }

    @Test(expected = WoodException.class)
    public void GivenNullListFiles_WhenFilesHandler_ThenWoodException() {
        // GIVEN
        FilePath dirPath = new FilePath(project, directory(null));

        // WHEN
        dirPath.files(new FilesHandler() {});

        // THEN
    }

    @Test
    public void GivenDirPath_WhenAccept_ThenAlwaysFalse() {
        assertFalse(FilePath.accept("res/"));
        assertFalse(FilePath.accept("res"));
        assertFalse(FilePath.accept("res/template/page/"));
        assertFalse(FilePath.accept("res/template/page"));
        assertFalse(FilePath.accept("res_de"));
    }

    // --------------------------------------------------------------------------------------------

    @SuppressWarnings("all")
    private static class SourceFile extends File {
        private static final long serialVersionUID = -5975578621510948684L;

        public SourceFile(String pathname) {
            super(pathname);
        }

        @Override
        public boolean isFile() {
            return true;
        }
    }

    @SuppressWarnings("all")
    private static class SourceDirectory extends File {
        private static final long serialVersionUID = 962174848140145344L;

        public SourceDirectory(String pathname) {
            super(pathname);
        }

        @Override
        public boolean isDirectory() {
            return true;
        }
    }

    private File directory(File[] files) {
        File dir = Mockito.mock(File.class);
        when(dir.getPath()).thenReturn(".");
        when(dir.exists()).thenReturn(true);
        when(dir.listFiles()).thenReturn(files);
        return dir;
    }

    private FilePath[] initSources(File[] files) {
        List<FilePath> paths = new ArrayList<>();
        for (File file : files) {
            if (file instanceof SourceDirectory) {
                FilePath path = new FilePath(project, file);
                when(project.createFilePath(file)).thenReturn(path);
                paths.add(path);
            } else if (file instanceof SourceFile) {
                FilePath path = new FilePath(project, file);
                when(project.createFilePath(file)).thenReturn(path);
                paths.add(path);
            }
        }
        return paths.toArray(new FilePath[]{});
    }
}
