package com.jslib.wood.build;

import com.jslib.wood.Component;
import com.jslib.wood.FilePath;
import com.jslib.wood.util.FilesUtil;
import org.junit.Before;
import org.junit.Test;

import java.io.File;
import java.io.IOException;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

public class DefaultBuildFsTest {
    private BuilderProject project;
    private File buildDir;
    private BuildFS buildFS;

    @Before
    public void beforeTest() {
        File projectRoot = new File("src/test/resources/build-fs");
        buildDir = new File(projectRoot, "build");
        if (buildDir.exists()) {
            try {
                FilesUtil.removeFilesHierarchy(buildDir);
            } catch (IOException ignore) {
            }
        }

        project = new BuilderProject(projectRoot);
        buildFS = new DefaultBuildFS(buildDir, 0);
    }

    @Test
    public void GivenPageCompo_WhenGetPageDir_ThenLayoutDir() {
        // GIVEN
        Component page = mock(Component.class);

        // WHEN
        File dir = buildFS.getPageDir(page);

        // THEN
        assertThat(dir, equalTo(buildDir));
    }

    @Test
    public void GivenPageCompoWithResourceGroup_WhenGetPageDir_ThenResourcesGroupDir() {
        // GIVEN
        Component page = mock(Component.class);
        when(page.getResourcesGroup()).thenReturn("admin");

        // WHEN
        File dir = buildFS.getPageDir(page);

        // THEN
        assertThat(dir, equalTo(new File(buildDir, "admin")));
    }

    @SuppressWarnings("ConstantConditions")
    @Test
    public void GivenNullComponent_WhenGetPageDir_ThenLayoutDir() {
        // GIVEN
        Component page = null;

        // WHEN
        File dir = buildFS.getPageDir(page);

        // THEN
        assertThat(dir, equalTo(buildDir));
    }

    @Test
    public void Given_WhenGetStyleDir_ThenStyleDir() {
        // GIVEN

        // WHEN
        File dir = buildFS.getStyleDir();

        // THEN
        assertThat(dir, equalTo(new File(buildDir, "style")));
    }

    @Test
    public void Given_WhenGetFontDir_ThenFontDir() {
        // GIVEN

        // WHEN
        File dir = buildFS.getFontDir();

        // THEN
        assertThat(dir, equalTo(new File(buildDir, "style")));
    }

    @Test
    public void Given_WhenGetScriptDir_ThenScriptDir() {
        // GIVEN

        // WHEN
        File dir = buildFS.getScriptDir();

        // THEN
        assertThat(dir, equalTo(new File(buildDir, "script")));
    }

    @Test
    public void Given_WhenGetFilesDir_ThenFilesDir() {
        // GIVEN

        // WHEN
        File dir = buildFS.getFilesDir();

        // THEN
        assertThat(dir, equalTo(new File(buildDir, "files")));
    }

    @Test
    public void Given_WhenGetMediaDir_ThenMediaDir() {
        // GIVEN

        // WHEN
        File dir = buildFS.getMediaDir();

        // THEN
        assertThat(dir, equalTo(new File(buildDir, "media")));
    }

    @Test
    public void GivenPageName_WhenFormatPageName_ThenLayoutFilePath() {
        // GIVEN
        String pageName = "index.htm";

        // WHEN
        String path = buildFS.formatPageName(pageName);

        // THEN
        assertThat(path, equalTo("index.htm"));
    }

    @Test
    public void GivenStyleFile_WhenFormatStyleName_ThenFormatedFileName() {
        assertThat(buildFS.formatStyleName(path("res/page/index/index.css")), equalTo("res-page_index.css"));
        assertThat(buildFS.formatStyleName(path("res/theme/style.css")), equalTo("res-theme_style.css"));
        assertThat(buildFS.formatStyleName(path("lib/video-player/style.css")), equalTo("lib-video-player_style.css"));

        // this condition is not really licit since style file cannot reside into source directory root
        // but is allowed by file path syntax and need to ensure styleName cope with it
        assertThat(buildFS.formatStyleName(path("res/style.css")), equalTo("res_style.css"));
        assertThat(buildFS.formatStyleName(path("lib/style.css")), equalTo("lib_style.css"));
    }

    @Test
    public void GivenScriptFile_WhenFormatScriptName_ThenFormatedFileName() {
        assertThat(buildFS.formatScriptName(path("script/hc/page/Index.js")), equalTo("script.hc.page.Index.js"));
        assertThat(buildFS.formatScriptName(path("lib/paging.js")), equalTo("lib.paging.js"));
        assertThat(buildFS.formatScriptName(path("lib/js-lib/js-lib.js")), equalTo("lib.js-lib.js"));
        assertThat(buildFS.formatScriptName(path("gen/js/wood/Controller.js")), equalTo("gen.js.wood.Controller.js"));
    }

    @Test
    public void GivenMediaFile_WhenFormatMediaName_ThenFormatedFileName() {
        assertThat(buildFS.formatMediaName(path("res/page/index/background.png")), equalTo("res-page-index_background.png"));
        assertThat(buildFS.formatMediaName(path("res/page/index/index.png")), equalTo("res-page-index_index.png"));
        assertThat(buildFS.formatMediaName(path("res/page/index/icon/logo.png")), equalTo("res-page-index-icon_logo.png"));
        assertThat(buildFS.formatMediaName(path("res/page/index/icon-logo.png")), equalTo("res-page-index_icon-logo.png"));
        assertThat(buildFS.formatMediaName(path("res/theme/background.png")), equalTo("res-theme_background.png"));
        assertThat(buildFS.formatMediaName(path("res/asset/background.png")), equalTo("res-asset_background.png"));
        assertThat(buildFS.formatMediaName(path("script/js/wood/player/background.png")), equalTo("script-js-wood-player_background.png"));
        assertThat(buildFS.formatMediaName(path("lib/paging/background.png")), equalTo("lib-paging_background.png"));
    }

    @Test
    public void GivenResourceGroup_WhenWritePageMedia_ThenRelativePath() throws IOException {
        // GIVEN
        Component component = mock(Component.class);
        when(component.getResourcesGroup()).thenReturn("admin");

        FilePath resourceFile = mock(FilePath.class);
        FilePath parentDir = mock(FilePath.class);
        when(parentDir.getPathSegments()).thenReturn(Collections.singletonList("asset"));
        when(resourceFile.getParentDir()).thenReturn(parentDir);
        when(resourceFile.getName()).thenReturn("icon.png");

        // WHEN
        String path = buildFS.writePageMedia(component, resourceFile);

        // THEN
        assertThat(path, notNullValue());
        assertThat(path, equalTo("../media/asset_icon.png"));
    }

    private FilePath path(String path) {
        return new FilePath(project, path);
    }
}
