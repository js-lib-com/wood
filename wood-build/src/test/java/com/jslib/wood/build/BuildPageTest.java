package com.jslib.wood.build;

import com.jslib.wood.*;
import com.jslib.wood.dom.Document;
import com.jslib.wood.dom.DocumentBuilder;
import com.jslib.wood.util.StringsUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import java.io.IOException;
import java.io.Reader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BuildPageTest {
    @Mock
    private BuilderProject project;
    @Mock
    private Component page;
    @Mock
    private BuildFS buildFS;

    @Mock
    private FilePath pwaManifest;
    @Mock
    private FilePath pwaLoader;
    @Mock
    private FilePath favicon;
    @Mock
    private ThemeStyles theme;

    private Builder builder;

    @Before
    public void beforeTest() {
        when(project.getAuthors()).thenReturn(Collections.singletonList("Iulian Rotaru"));
        when(project.getPwaManifest()).thenReturn(pwaManifest);
        when(project.getPwaLoader()).thenReturn(pwaLoader);
        when(project.getFavicon()).thenReturn(favicon);
        when(project.getThemeStyles()).thenReturn(theme);

        //when(page.getProject()).thenReturn(project);
        when(page.getTitle()).thenReturn("Test Page");
        when(page.getDescription()).thenReturn("Test page description.");

        builder = new Builder(project, buildFS);
    }

    @Test
    public void GivenProjectAndPageComponent_WhenBuildPage_ThenExpectedHtmlGenerated() throws IOException, SAXException {
        // GIVEN

        // project fixture

        when(pwaManifest.exists()).thenReturn(true);
        SourceReader reader = mock(SourceReader.class);
        when(pwaManifest.getReader()).thenReturn(reader);
        when(buildFS.writePwaManifest(any(SourceReader.class))).thenReturn("manifest.json");

        when(favicon.exists()).thenReturn(true);
        when(buildFS.writeFavicon(any(Component.class), eq(favicon))).thenReturn("favicon.ico");

        List<IMetaDescriptor> projectMetas = metas("og:url", "http://kids-cademy.com");
        when(project.getMetaDescriptors()).thenReturn(projectMetas);

        List<ILinkDescriptor> projectStyles = styles("https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css");
        when(project.getLinkDescriptors()).thenReturn(projectStyles);

        List<IScriptDescriptor> projectScripts = scripts("lib/js-lib.js");
        when(project.getScriptDescriptors()).thenReturn(projectScripts);
        when(buildFS.writeScript(any(Component.class), any(SourceReader.class))).thenReturn("script/js-lib.js");

        FilePath themeReset = Mockito.mock(FilePath.class);
        when(theme.getDefaultStyles()).thenReturn(themeReset);
        when(buildFS.writeStyle(any(Component.class), eq(themeReset), any(IReferenceHandler.class))).thenReturn("styles/reset.css");

        FilePath themeFx = Mockito.mock(FilePath.class);
        when(theme.getAnimations()).thenReturn(themeFx);
        when(buildFS.writeStyle(any(Component.class), eq(themeFx), any(IReferenceHandler.class))).thenReturn("styles/fx.css");

        FilePath themeStyle = Mockito.mock(FilePath.class);
        when(theme.getStyles()).thenReturn(Collections.singletonList(themeStyle));
        when(buildFS.writeStyle(any(Component.class), eq(themeStyle), any(IReferenceHandler.class))).thenReturn("styles/form.css");

        // page fixture

        List<IMetaDescriptor> pageMetas = metas("og:title", "Test Page");
        when(page.getMetaDescriptors()).thenReturn(pageMetas);

        List<ILinkDescriptor> pageStyles = styles("http://fonts.googleapis.com/css?family=Lato");
        when(page.getLinkDescriptors()).thenReturn(pageStyles);

        List<IScriptDescriptor> pageScripts = scripts("https://stackpath.bootstrapcdn.com/bootstrap/4.4.1/js/bootstrap.min.js");
        when(page.getScriptDescriptors()).thenReturn(pageScripts);

        FilePath pageStyleFile = Mockito.mock(FilePath.class);
        when(page.getStyleFiles()).thenReturn(Collections.singletonList(pageStyleFile));
        when(buildFS.writeStyle(any(Component.class), eq(pageStyleFile), any(IReferenceHandler.class))).thenReturn("styles/page.css");

        String layout = "<body>" + //
                "	<h1>Test Page</h1>" + //
                "</body>";
        DocumentBuilder documentBuilder = DocumentBuilder.getInstance();
        when(page.getLayout()).thenReturn(documentBuilder.parseHTML(layout).getRoot());

        // WHEN
        builder.setLanguage("en");
        builder.buildPage(page);

        // THEN
        ArgumentCaptor<Document> documentArgument = ArgumentCaptor.forClass(Document.class);
        verify(buildFS).writePage(any(), documentArgument.capture());
        Document document = documentArgument.getValue();
        assertThat(document.stringify(), equalTo(StringsUtil.loadResource("/expected-build-page-test")));
    }

    // --------------------------------------------------------------------------------------------

    private List<IScriptDescriptor> scripts(String... sources) {
        List<IScriptDescriptor> scripts = new ArrayList<>();
        for (String source : sources) {
            IScriptDescriptor script = Mockito.mock(IScriptDescriptor.class);
            when(script.getSource()).thenReturn(source);
            if (FilePath.accept(source)) {
                FilePath path = Mockito.mock(FilePath.class);
                when(path.exists()).thenReturn(true);
                Reader reader = Mockito.mock(Reader.class);
                when(path.getReader()).thenReturn(reader);
                when(project.createFilePath(source)).thenReturn(path);
            }
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
}
