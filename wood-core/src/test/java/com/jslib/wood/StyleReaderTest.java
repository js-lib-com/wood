package com.jslib.wood;

import com.jslib.wood.impl.MediaQueries;
import com.jslib.wood.impl.MediaQueryDefinition;
import com.jslib.wood.impl.Variants;
import com.jslib.wood.util.FilesUtil;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class StyleReaderTest {
    @Mock
    private Project project;
    @Mock
    private FilePath styleFile;

    @Test
    public void GivenStyleFilesAndMediaQueries_WhenStyleConstructor_ThenInternalStateInitialized() throws IOException {
        // GIVEN
        class XFile extends File {
            private static final long serialVersionUID = -5975578621510948684L;

            public XFile(String pathname) {
                super(pathname);
            }

            @Override
            public boolean isFile() {
                return true;
            }
        }

        when(project.getProjectRoot()).thenReturn(new File("."));
        when(project.getMediaQueryDefinition("h600")).thenReturn(new MediaQueryDefinition("h600", "screen", "min-height: 600px"));
        when(project.getMediaQueryDefinition("w800")).thenReturn(new MediaQueryDefinition("w800", "screen", "min-width: 800px"));
        when(project.getMediaQueryDefinition("w1200")).thenReturn(new MediaQueryDefinition("w1200", "screen", "min-width: 1200px"));
        when(project.getMediaQueryDefinition("xsd")).thenReturn(new MediaQueryDefinition("xsd", "screen", "min-height: 560px"));
        when(project.getMediaQueryDefinition("smd")).thenReturn(new MediaQueryDefinition("smd", "screen", "min-height: 560px"));
        when(project.getMediaQueryDefinition("nod")).thenReturn(new MediaQueryDefinition("nod", "screen", "min-height: 560px"));
        when(project.getMediaQueryDefinition("mdd")).thenReturn(new MediaQueryDefinition("mdd", "screen", "min-height: 768px"));
        when(project.getMediaQueryDefinition("lgd")).thenReturn(new MediaQueryDefinition("lgd", "screen", "min-height: 992px"));
        when(project.getMediaQueryDefinition("landscape")).thenReturn(new MediaQueryDefinition("landscape", "screen", "orientation: landscape"));
        when(project.getMediaQueryDefinition("portrait")).thenReturn(new MediaQueryDefinition("portrait", "screen", "orientation: portrait"));

        File[] styleFiles = new File[]{ //
                new XFile("res/page/page_lgd.css"), //
                new XFile("res/page/page_nod.css"), //
                new XFile("res/page/page_mdd.css"), //
                new XFile("res/page/page_mdd_portrait.css"), //
                new XFile("res/page/page_mdd_landscape.css"), //
                new XFile("res/page/page_smd.css"), //
                new XFile("res/page/page_smd_portrait.css"), //
                new XFile("res/page/page_smd_landscape.css"), //
                new XFile("res/page/page_xsd.css"), //
                new XFile("res/page/page_xsd_portrait.css"), //
                new XFile("res/page/page_xsd_landscape.css"), //
                new XFile("res/page/page_w1200.css"), //
                new XFile("res/page/page_w800.css"), //
                new XFile("res/page/page_h600.css"), //
                new XFile("res/page/preview.css"), //
                new XFile("res/page/preview.htm"), //
                new XFile("res/page/page.htm") //
        };
        for (File styleFile : styleFiles) {
            FilePath stylePath = new FilePath(project, styleFile);
            when(project.createFilePath(styleFile)).thenReturn(stylePath);
        }

        File stylesDir = Mockito.mock(File.class);
        when(stylesDir.exists()).thenReturn(true);
        when(stylesDir.getPath()).thenReturn("res/page");
        when(stylesDir.listFiles()).thenReturn(styleFiles);
        FilePath parentDir = new FilePath(project, stylesDir);

        when(styleFile.getBasename()).thenReturn("page");
        when(styleFile.getParentDir()).thenReturn(parentDir);

        String source = "body { width: 960px; }";
        when(styleFile.getReader()).thenReturn(new StringReader(source));

        // WHEN
        StyleReader reader = new StyleReader(styleFile);
        reader.close();

        // THEN
        assertNotNull(reader.getReader());
        assertThat(reader.getState(), equalTo("BASE_CONTENT"));

        List<FilePath> variants = reader.getVariants();
        assertThat(variants, notNullValue());
        assertThat(variants.size(), equalTo(14));

        assertThat(variants, hasItem(key("res/page/page_lgd.css")));
        assertThat(variants, hasItem(key("res/page/page_nod.css")));

        assertThat(variants, hasItem(key("res/page/page_mdd.css")));
        assertThat(variants, hasItem(key("res/page/page_mdd_portrait.css")));
        assertThat(variants, hasItem(key("res/page/page_mdd_landscape.css")));

        assertThat(variants, hasItem(key("res/page/page_smd.css")));
        assertThat(variants, hasItem(key("res/page/page_smd_portrait.css")));
        assertThat(variants, hasItem(key("res/page/page_smd_landscape.css")));

        assertThat(variants, hasItem(key("res/page/page_xsd.css")));
        assertThat(variants, hasItem(key("res/page/page_xsd_portrait.css")));
        assertThat(variants, hasItem(key("res/page/page_xsd_landscape.css")));

        assertThat(variants, hasItem(key("res/page/page_w1200.css")));
        assertThat(variants, hasItem(key("res/page/page_w800.css")));
        assertThat(variants, hasItem(key("res/page/page_h600.css")));
    }

    private FilePath key(String path) {
        return new FilePath(project, path);
    }

    @Test
    public void GivenWellFormedBaseStyleFileAndMediaQueryStyleFiles_WhenCopyStyleFile_ThenMediaQueriesIncluded() throws IOException {
        // GIVEN
        fixtureForStyleCopyTest();
        String source = "body { width: 560px; }\r\n"; // well-formed style file ends with new line
        when(styleFile.getReader()).thenReturn(new StringReader(source));

        StyleReader reader = new StyleReader(styleFile);

        // WHEN
        StringWriter writer = new StringWriter();
        FilesUtil.copy(reader, writer);

        // THEN
        assertForStyleCopyTest(writer);
    }

    @Test
    public void GivenNotWellFormedBaseStyleFileAndMediaQueryStyleFiles_WhenCopyStyleFile_ThenMediaQueriesIncluded() throws IOException {
        // GIVEN
        fixtureForStyleCopyTest();
        String source = "body { width: 560px; }"; // not well-formed style file does not end with new line
        when(styleFile.getReader()).thenReturn(new StringReader(source));

        StyleReader reader = new StyleReader(styleFile);

        // WHEN
        StringWriter writer = new StringWriter();
        FilesUtil.copy(reader, writer);

        // THEN
        assertForStyleCopyTest(writer);
    }

    private void fixtureForStyleCopyTest() {
        class Mock {
            final FilePath file;

            Mock(String expression, String content) {
                MediaQueries mediaQueries = Mockito.mock(MediaQueries.class);
                when(mediaQueries.getMedia()).thenReturn("screen");
                when(mediaQueries.getExpression()).thenReturn(expression);

                Variants variants = Mockito.mock(Variants.class);
                when(variants.getMediaQueries()).thenReturn(mediaQueries);

                file = Mockito.mock(FilePath.class);
                when(file.getVariants()).thenReturn(variants);
                when(file.getReader()).thenReturn(new StringReader(content));

            }
        }

        Mock[] mocks = new Mock[]{ //
                new Mock("min-width: 680px", "body { width: 680px; }"), //
                new Mock("min-width: 960px", "body { width: 960px; }"), //
                new Mock("min-width: 1200px", "body { width: 1200px; }") //
        };

        FilePath parentDir = Mockito.mock(FilePath.class);
        List<FilePath> files = Arrays.stream(mocks).map(mock -> mock.file).collect(Collectors.toList());
        when(parentDir.filter(any())).thenReturn(files);

        when(styleFile.getParentDir()).thenReturn(parentDir);
    }

    private static void assertForStyleCopyTest(StringWriter writer) {
        String expected = "body { width: 560px; }" + System.lineSeparator() + //
                System.lineSeparator() + //
                "@media screen and min-width: 680px {" + System.lineSeparator() + //
                "body { width: 680px; }" + System.lineSeparator() + //
                "}" + System.lineSeparator() + //
                System.lineSeparator() + //
                "@media screen and min-width: 960px {" + System.lineSeparator() + //
                "body { width: 960px; }" + System.lineSeparator() + //
                "}" + System.lineSeparator() + //
                System.lineSeparator() + //
                "@media screen and min-width: 1200px {" + System.lineSeparator() + //
                "body { width: 1200px; }" + System.lineSeparator() + //
                "}" + System.lineSeparator();

        assertThat(writer.toString(), equalTo(expected));
    }
}
