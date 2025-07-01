package com.jslib.wood;

import com.jslib.wood.impl.FileType;
import com.jslib.wood.impl.MediaQueryDefinition;
import com.jslib.wood.impl.Variants;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class FilePathTest {
    @Mock
    private Project project;

    @Test
    public void GivenValidPath_WhenFilePatternMatch_ThenGroupsFound() {
        Pattern pattern = FilePath.FILE_PATTERN;
        assertThat(pattern, notNullValue());

        assertFilePattern(pattern, ".wood.properties", "", ".wood", null, "properties");
        assertFilePattern(pattern, "project.xml", "", "project", null, "xml");
        assertFilePattern(pattern, "res/path/compo/compo.htm", "res/path/compo/", "compo", null, "htm");
        assertFilePattern(pattern, "res/path/compo/compo_port.htm", "res/path/compo/", "compo", "port", "htm");
        assertFilePattern(pattern, "res/path/second-compo/second-compo.css", "res/path/second-compo/", "second-compo", null, "css");
        assertFilePattern(pattern, "res/path/second-compo/second-compo_w800.css", "res/path/second-compo/", "second-compo", "w800", "css");
        assertFilePattern(pattern, "lib/js-lib.js", "lib/", "js-lib", null, "js");
        assertFilePattern(pattern, "script/js/format/RichText.js", "script/js/format/", "RichText", null, "js");
        assertFilePattern(pattern, "gen/js/widget/Paging.js", "gen/js/widget/", "Paging", null, "js");
        assertFilePattern(pattern, "res/path/compo/background_port_ro.png", "res/path/compo/", "background", "port_ro", "png");
        assertFilePattern(pattern, "res/3pty-scripts/3pty-scripts.htm", "res/3pty-scripts/", "3pty-scripts", null, "htm");
    }

    private static void assertFilePattern(Pattern pattern, String value, String... groups) {
        Matcher m = pattern.matcher(value);
        assertThat(m.find(), equalTo(true));
        assertThat(groups[0], equalTo(m.group(1)));
        assertThat(groups[1], equalTo(m.group(2)));
        assertThat(groups[2], equalTo(m.group(3)));
        assertThat(groups[3], equalTo(m.group(4)));
    }

    @Test
    public void GivenValidPath_WhenDirectoryPatternMatch_ThenGroupsFound() {
        Pattern pattern = FilePath.DIRECTORY_PATTERN;
        assertThat(pattern, notNullValue());

        assertDirectoryPattern(pattern, "res", "", "res");
        assertDirectoryPattern(pattern, "res/", "", "res");
        assertDirectoryPattern(pattern, "res/path/compo", "res/path/", "compo");
        assertDirectoryPattern(pattern, "res/path/compo/", "res/path/", "compo");
    }

    private static void assertDirectoryPattern(Pattern pattern, String value, String... groups) {
        Matcher m = pattern.matcher(value);
        assertThat(m.find(), equalTo(true));
        assertThat(groups[0], equalTo(m.group(1)));
        assertThat(groups[1], equalTo(m.group(2)));
    }

    @Test
    public void GivenValidPath_WhenConstructor_ThenInternalStateInit() {
        assertFilePath("res/", null, "res", "res", FileType.NONE, null);
        assertFilePath("res/", null, "res", "res", FileType.NONE, null);
        assertFilePath("res/compo/discography/", "res/compo/", "discography", "discography", FileType.NONE, null);
        assertFilePath("res/compo/discography/", "res/compo/", "discography", "discography", FileType.NONE, null);

        assertFilePath("res/compo/discography/discography_ro.css", "res/compo/discography/", "discography", "discography.css", FileType.STYLE, "ro");
        assertFilePath("res/compo/discography/strings.xml", "res/compo/discography/", "strings", "strings.xml", FileType.XML, null);
        assertFilePath("res/compo/discography/logo_de.png", "res/compo/discography/", "logo", "logo.png", FileType.MEDIA, "de");
        assertFilePath("lib/js-lib-1.2.3.js", "lib/", "js-lib-1.2.3", "js-lib-1.2.3.js", FileType.SCRIPT, null);
        assertFilePath("script/js/compo/Dialog.js", "script/js/compo/", "Dialog", "Dialog.js", FileType.SCRIPT, null);
    }

    private void assertFilePath(String value, String parent, String basename, String fileName, FileType fileType, String language) {
        FilePath p = new FilePath(project, value);
        assertThat(p.value(), equalTo(value));
        if (parent != null) {
            assertThat(p.getParentDir().value(), equalTo(parent));
        }
        assertThat(p.getBasename(), equalTo(basename));
        assertThat(p.getName(), equalTo(fileName));
        assertThat(p.getType(), equalTo(fileType));
        assertThat(p.getVariants(), notNullValue());
        if (language != null) {
            assertThat(p.getVariants().getLanguage(), equalTo(language));
        } else {
            assertThat(p.getVariants().getLanguage(), nullValue());
        }
    }

    @Test(expected = WoodException.class)
    public void GivenInvalidPath_WhenConstructor_ThenException() {
        new FilePath(project, "http://server/path");
    }

    @Test
    public void GivenValidDirectory_WhenGetBaseName_ThenExpectedValue() {
        // given
        FilePath path = new FilePath(project, "res/page/about/");

        // when
        String basename = path.getBasename();

        // then
        assertThat(basename, equalTo("about"));
    }

    @Test
    public void GivenValidFile_WhenGetBaseName_ThenExpectedValue() {
        // given
        FilePath path = new FilePath(project, "res/compo/compo.css");

        // when
        String basename = path.getBasename();

        // then
        assertThat(basename, equalTo("compo"));
    }

    @Test
    public void GivenValidFileWithVariant_WhenGetBaseName_ThenExpectedValue() {
        // given
        FilePath path = new FilePath(project, "res/compo/strings_de.xml");

        // when
        String basename = path.getBasename();

        // then
        assertThat(basename, equalTo("strings"));
    }

    @Test
    public void GivenLayoutFileWithVariants_WhenCloneToStyle_ThenPreserveState() {
        // given
        when(project.getMediaQueryDefinition("w800")).thenReturn(new MediaQueryDefinition("w800", "x", 0));
        FilePath layoutFile = new FilePath(project, "res/compo/compo_w800.htm");

        // when
        FilePath styleFile = layoutFile.cloneTo(FileType.STYLE);

        // then
        assertThat(layoutFile.isLayout(), equalTo(true));
        assertThat(styleFile.isStyle(), equalTo(true));
        assertThat(layoutFile.getBasename(), equalTo(styleFile.getBasename()));
        assertThat(layoutFile.getParentDir(), equalTo(styleFile.getParentDir()));
        assertThat(layoutFile.getVariants().getLanguage(), equalTo(styleFile.getVariants().getLanguage()));
        assertThat(layoutFile.getVariants().getMediaQueries(), equalTo(styleFile.getVariants().getMediaQueries()));
    }

    @Test
    public void GivenFilePath_WhenGetParentDir_ThenExpectedValue() {
        // given
        FilePath path = new FilePath(project, "res/asset/background.jpg");

        // when
        FilePath parentDir = path.getParentDir();

        // then
        assertThat(parentDir.value(), equalTo("res/asset/"));
    }

    @Test
    public void GivenDirectoryPath_WhenGetParentDir_ThenExpectedValue() {
        // given
        FilePath path = new FilePath(project, "res/asset/icons");

        // when
        FilePath parentDir = path.getParentDir();

        // then
        assertThat(parentDir.value(), equalTo("res/asset/"));
    }

    @Test
    public void GivenFilePath_WhenExpectedValue_ThenTrue() {
        assertTrue(new FilePath(project, "res/asset/background.jpg").isMedia());
        assertTrue(new FilePath(project, "res/compo/compo.htm").isLayout());
        assertTrue(new FilePath(project, "res/compo/compo.css").isStyle());
        assertTrue(new FilePath(project, "res/compo/preview.js").isScript());
        assertTrue(new FilePath(project, "res/compo/preview.js").isPreviewScript());
        assertTrue(new FilePath(project, "res/compo/compo.xml").isComponentDescriptor());
        assertTrue(new FilePath(project, "res/compo/strings.xml").isVariables());
    }

    @Test
    public void GivenFilePath_WhenNotExpectedValue_ThenFalse() {
        assertFalse(new FilePath(project, "res/compo/compo.css").isMedia());
        assertFalse(new FilePath(project, "res/compo/compo.css").isLayout());
        assertFalse(new FilePath(project, "res/compo/preview.js").isStyle());
        assertFalse(new FilePath(project, "res/compo/preview.css").isScript());
        assertFalse(new FilePath(project, "res/compo/compo.js").isPreviewScript());
        assertFalse(new FilePath(project, "res/compo/compo.htm").isComponentDescriptor());
        assertFalse(new FilePath(project, "res/compo/strings.xml").isComponentDescriptor());
        assertFalse(new FilePath(project, "res/compo/compo.xml").isVariables());
        assertFalse(new FilePath(project, "res/compo/compo.htm").isVariables());
    }

    @Test
    public void GivenFileWithVariant_WhenHasVariants_ThenTrue() {
        assertTrue(new FilePath(project, "res/compo/strings_de.xml").hasVariants());
    }

    @Test
    public void GivenFileWithoutVariant_WhenHasVariants_ThenFalse() {
        assertFalse(new FilePath(project, "res/compo/strings.xml").hasVariants());
    }

    @Test
    public void GivenProjectWithMediaQuery_WhenGetVariants_ThenNotNullValue() {
        when(project.getMediaQueryDefinition("w800")).thenReturn(new MediaQueryDefinition("w800", "min-width: 800px", 0));
        when(project.getMediaQueryDefinition("h800")).thenReturn(new MediaQueryDefinition("h800", "min-height: 800px", 1));
        when(project.getMediaQueryDefinition("lgd")).thenReturn(new MediaQueryDefinition("lgd", "min-width: 992px", 1));
        when(project.getMediaQueryDefinition("portrait")).thenReturn(new MediaQueryDefinition("portrait", "orientation: portrait", 1));

        FilePath path = new FilePath(project, "res/compo/strings_de.xml");
        assertThat(path.hasVariants(), equalTo(true));
        Variants variants = path.getVariants();
        assertThat(variants, notNullValue());
        assertThat(variants.getLanguage(), equalTo("de"));
        assertThat(variants.getMediaQueries().isEmpty(), is(true));

        path = new FilePath(project, "res/compo/compo_w800.css");
        variants = path.getVariants();
        assertThat(variants.getLanguage(), nullValue());
        assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
        assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px )"));

        path = new FilePath(project, "res/compo/compo_h800.css");
        variants = path.getVariants();
        assertThat(variants.getLanguage(), nullValue());
        assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
        assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-height: 800px )"));

        path = new FilePath(project, "res/compo/colors_ro_w800.xml");
        variants = path.getVariants();
        assertThat(variants.getLanguage(), equalTo("ro"));
        assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
        assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px )"));

        path = new FilePath(project, "res/compo/colors_w800_ro.xml");
        variants = path.getVariants();
        assertThat(variants.getLanguage(), equalTo("ro"));
        assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
        assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px )"));

        path = new FilePath(project, "res/compo/compo_lgd.css");
        variants = path.getVariants();
        assertThat(variants.getMediaQueries().getQueries(), hasSize(1));
        assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 992px )"));

        path = new FilePath(project, "res/compo/colors_w800_portrait_ro.xml");
        variants = path.getVariants();
        assertThat(variants.getLanguage(), equalTo("ro"));
        assertThat(variants.getMediaQueries().getQueries(), hasSize(2));
        assertThat(variants.getMediaQueries().getExpression(), equalTo("( min-width: 800px ) and ( orientation: portrait )"));
    }

    @Test(expected = WoodException.class)
    public void GivenProjectWithoutMediaQueryAndPathWithVariant_WhenConstructor_ThenException() {
        new FilePath(project, "res/compo/colors_q800.xml");
    }

    @Test
    public void GivenValidPath_WhenAccept_ThenTrue() {
        assertTrue(FilePath.accept("res/template/page/page.htm"));
        assertTrue(FilePath.accept("lib/js-lib.js"));
        assertTrue(FilePath.accept("res/compo/video-player/video-player.xml"));
        assertTrue(FilePath.accept("lib/js-lib-1.2.3.js"));
        assertTrue(FilePath.accept("template/page/page_ro.htm"));
        assertTrue(FilePath.accept("dir/template/page/page_w800_ro.htm"));
        assertTrue(FilePath.accept("project.xml"));
    }

    @Test
    public void GivenInvalidPath_WhenAccept_ThenFalse() {
        assertFalse(FilePath.accept("res/template/page/page.htm#body"));
        assertFalse(FilePath.accept("dir/template/page#body"));
    }

    @Test
    public void GivenFileWithExtension_WhenGetMimeType_ThenExpectedValue() throws IOException {
        assertThat(new FilePath(project, "file.htm").getMimeType(), equalTo("text/html"));
        assertThat(new FilePath(project, "file.html").getMimeType(), equalTo("text/html"));
        assertThat(new FilePath(project, "file.xhtml").getMimeType(), equalTo("application/xhtml+xml"));
        assertThat(new FilePath(project, "file.xml").getMimeType(), equalTo("text/xml"));
        assertThat(new FilePath(project, "file.css").getMimeType(), equalTo("text/css"));
        assertThat(new FilePath(project, "file.js").getMimeType(), equalTo("application/javascript"));
        assertThat(new FilePath(project, "file.json").getMimeType(), equalTo("application/json"));
        assertThat(new FilePath(project, "file.png").getMimeType(), equalTo("image/png"));
        assertThat(new FilePath(project, "file.jpg").getMimeType(), equalTo("image/jpeg"));
        assertThat(new FilePath(project, "file.jpeg").getMimeType(), equalTo("image/jpeg"));
        assertThat(new FilePath(project, "file.gif").getMimeType(), equalTo("image/gif"));
        assertThat(new FilePath(project, "file.tiff").getMimeType(), equalTo("image/tiff"));
        assertThat(new FilePath(project, "file.svg").getMimeType(), equalTo("image/svg+xml"));
    }
}
