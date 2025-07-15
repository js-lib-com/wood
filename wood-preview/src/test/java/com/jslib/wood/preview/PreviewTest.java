package com.jslib.wood.preview;

import com.jslib.wood.*;
import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.DocumentBuilder;
import com.jslib.wood.util.StringsUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.StringWriter;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class PreviewTest {
    @Mock
    private Project project;
    @Mock
    private Component compo;
    @Mock
    private ThemeStyles theme;

    private Preview preview;

    @Before
    public void beforeTest() {
        preview = new Preview(project, compo, "test", true);
    }

    @Test
    public void GivenComponent_WhenPreviewSerialize_ThenCompoDocument() throws IOException, SAXException {
        // GIVEN

        // project fixture
        when(project.getDefaultLanguage()).thenReturn("en");
        when(project.getAuthors()).thenReturn(Collections.singletonList("Iulian Rotaru"));
        when(project.getThemeStyles()).thenReturn(theme);

        List<IMetaDescriptor> projectMetas = metas("og:url", "http://kids-cademy.com");
        when(project.getMetaDescriptors()).thenReturn(projectMetas);

        List<ILinkDescriptor> projectStyles = styles("https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css");
        when(project.getLinkDescriptors()).thenReturn(projectStyles);

        List<IScriptDescriptor> projectScripts = scripts("lib/js-lib.js");
        when(project.getScriptDescriptors()).thenReturn(projectScripts);

        FilePath themeReset = Mockito.mock(FilePath.class);
        when(themeReset.value()).thenReturn("theme/reset.css");
        when(theme.getDefaultStyles()).thenReturn(themeReset);

        FilePath themeFx = Mockito.mock(FilePath.class);
        when(themeFx.value()).thenReturn("theme/fx.css");
        when(theme.getAnimations()).thenReturn(themeFx);

        FilePath themeStyle = Mockito.mock(FilePath.class);
        when(themeStyle.value()).thenReturn("theme/form.css");
        when(theme.getStyles()).thenReturn(Collections.singletonList(themeStyle));

        // component fixture
        when(compo.getTitle()).thenReturn("Test compo");
        when(compo.getDescription()).thenReturn("Test compo description.");

        List<IMetaDescriptor> compoMetas = metas("og:title", "Test compo");
        when(compo.getMetaDescriptors()).thenReturn(compoMetas);

        List<ILinkDescriptor> compoStyles = styles("http://fonts.googleapis.com/css?family=Lato");
        when(compo.getLinkDescriptors()).thenReturn(compoStyles);

        List<IScriptDescriptor> compoScripts = scripts("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js");
        when(compo.getScriptDescriptors()).thenReturn(compoScripts);

        FilePath compoStyleFile = Mockito.mock(FilePath.class);
        when(compoStyleFile.value()).thenReturn("compo/compo.css");
        when(compo.getStyleFiles()).thenReturn(Collections.singletonList(compoStyleFile));

        String layout = "<body>" + //
                "	<h1>Test Compo</h1>" + //
                "</body>";
        DocumentBuilder documentBuilder = DocumentBuilder.getInstance();
        when(compo.getLayout()).thenReturn(documentBuilder.parseHTML(layout).getRoot());

        // WHEN
        Writer writer = new StringWriter();
        preview.serialize(writer);

        // THEN
        Document document = documentBuilder.parseXML(writer.toString());
        assertThat(str(document.stringify()), equalTo(str(StringsUtil.loadResource("/preview-test-document"))));
    }

    // --------------------------------------------------------------------------------------------

    private static List<IScriptDescriptor> scripts(String... sources) {
        List<IScriptDescriptor> scripts = new ArrayList<>();
        for (String source : sources) {
            IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
            when(script.getSource()).thenReturn(source);
            scripts.add(script);
        }
        return scripts;
    }

    private static List<IMetaDescriptor> metas(String property, String content) {
        IMetaDescriptor meta = Mockito.mock(IMetaDescriptor.class);
        when(meta.getProperty()).thenReturn(property);
        when(meta.getContent()).thenReturn(content);
        return Collections.singletonList(meta);
    }

    private static List<ILinkDescriptor> styles(String... sources) {
        List<ILinkDescriptor> styles = new ArrayList<>();
        for (String source : sources) {
            ILinkDescriptor style = Mockito.mock(ILinkDescriptor.class);
            when(style.getRelationship()).thenReturn("stylesheet");
            when(style.getHref()).thenReturn(source);
            styles.add(style);
        }
        return styles;
    }

    /**
     * There is a peculiarity when W3C element set text content that always use LF instead of CRLF. To allow for
     * serialized document comparison we need to remove CR from all compared strings.
     *
     * @param string string to remove CR from.
     * @return given string with CR removed.
     */
    private static String str(String string) {
        if (string == null) {
            return null;
        }
        return string.replaceAll("\r", "");
    }
}
