package com.jslib.wood.build;

import com.jslib.wood.*;
import com.jslib.wood.dom.Document;
import com.jslib.wood.impl.FileType;
import com.jslib.wood.impl.XmlnsOperatorsHandler;
import com.jslib.wood.util.StringsUtil;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class BuilderBuildTest {
    @Mock
    private BuilderProject project;
    @Mock
    private BuildFS buildFS;

    private Builder builder;
    private FilePath descriptorPath;
    private FilePath layoutPath;

    @Before
    public void beforeTest() {
        when(project.getLanguages()).thenReturn(Collections.singletonList("en"));

        when(project.getProjectRoot()).thenReturn(new File("test"));
        when(project.getOperatorsHandler()).thenReturn(new XmlnsOperatorsHandler());
        when(project.getPwaLoader()).thenReturn(mock(FilePath.class));
        when(project.getPwaManifest()).thenReturn(mock(FilePath.class));
        when(project.getPwaWorker()).thenReturn(mock(FilePath.class));
        when(project.getFavicon()).thenReturn(mock(FilePath.class));
        when(project.getThemeStyles()).thenReturn(mock(ThemeStyles.class));

        FilePath filePath = mock(FilePath.class);
        when(project.createFilePath(any(String.class))).thenReturn(filePath);

        layoutPath = mock(FilePath.class);
        when(layoutPath.exists()).thenReturn(true);
        when(layoutPath.isLayout()).thenReturn(true);
        when(layoutPath.getProject()).thenReturn(project);

        descriptorPath = mock(FilePath.class);
        when(descriptorPath.getProject()).thenReturn(project);

        when(layoutPath.cloneTo(FileType.XML)).thenReturn(descriptorPath);
        when(layoutPath.cloneTo(FileType.STYLE)).thenReturn(mock(FilePath.class));
        when(descriptorPath.cloneTo(FileType.SCRIPT)).thenReturn(mock(FilePath.class));

        CompoPath compoPath = mock(CompoPath.class);
        when(project.getPages()).thenReturn(Collections.singletonList(compoPath));
        when(layoutPath.getReader()).thenReturn(new StringReader("<body><h1>Test Page</h1></body>"));
        when(compoPath.getLayoutPath()).thenReturn(layoutPath);

        builder = new Builder(project, buildFS);
    }

    @Test
    public void GivenPageComponent_WhenBuild_ThenPageHtmlGenerated() throws IOException {
        // GIVEN

        // WHEN
        builder.build();

        // THEN
        ArgumentCaptor<Document> documentArgument = ArgumentCaptor.forClass(Document.class);
        verify(buildFS, times(1)).writePage(any(), documentArgument.capture());
        Document document = documentArgument.getValue();
        assertThat(document.stringify(), equalTo(StringsUtil.loadResource("/expected-builder-page-test")));
    }

    @Test
    public void GivenThemeStyles_WhenBuild_ThenStyleVariableIncluded() throws IOException {
        // GIVEN
        ThemeStyles themeStyles = mock(ThemeStyles.class);
        when(project.getThemeStyles()).thenReturn(themeStyles);

        FilePath variablesFile = mock(FilePath.class);
        when(themeStyles.getVariables()).thenReturn(variablesFile);
        when(buildFS.writeStyle(any(), eq(variablesFile), any())).thenReturn("/style/var.css");

        FilePath defaultFile = mock(FilePath.class);
        when(themeStyles.getDefaultStyles()).thenReturn(defaultFile);
        when(buildFS.writeStyle(any(), eq(defaultFile), any())).thenReturn("/style/default.css");

        FilePath animationFile = mock(FilePath.class);
        when(themeStyles.getAnimations()).thenReturn(animationFile);
        when(buildFS.writeStyle(any(), eq(animationFile), any())).thenReturn("/style/fx.css");

        // WHEN
        builder.build();

        // THEN
        ArgumentCaptor<Document> documentArgument = ArgumentCaptor.forClass(Document.class);
        verify(buildFS, times(1)).writePage(any(), documentArgument.capture());
        Document document = documentArgument.getValue();
        assertThat(document.stringify(), equalTo(StringsUtil.loadResource("/expected-builder-theme-test")));
    }

    @Test
    public void GivenPwaLoaderFile_WhenBuild_ThenEmbedPwaLoaderScript() throws IOException {
        // GIVEN
        FilePath pwaLoader = mock(FilePath.class);
        when(pwaLoader.exists()).thenReturn(true);
        when(project.getPwaLoader()).thenReturn(pwaLoader);

        IScriptDescriptor pwaDescriptor = mock(IScriptDescriptor.class);
        when(pwaDescriptor.getSource()).thenReturn("worker.js");
        when(pwaDescriptor.isEmbedded()).thenReturn(true);

        FilePath pwaLoaderFile = mock(FilePath.class);
        when(pwaLoaderFile.exists()).thenReturn(true);
        when(pwaLoaderFile.getReader()).thenReturn(new StringReader("navigator.serviceWorker.register('worker.js');"));
        when(project.createFilePath("worker.js")).thenReturn(pwaLoaderFile);
        when(project.createScriptDescriptor(pwaLoader, true)).thenReturn(pwaDescriptor);

        // WHEN
        builder.build();

        // THEN
        ArgumentCaptor<Document> documentArgument = ArgumentCaptor.forClass(Document.class);
        verify(buildFS, times(1)).writePage(any(), documentArgument.capture());
        Document document = documentArgument.getValue();
        assertThat(document.stringify(), equalTo(StringsUtil.loadResource("/expected-builder-pwa-test")));
    }

    @Test
    public void GivenMultiLanguageProject_WhenBuild_ThenSetLanguageOnBuildFilesystem() throws IOException {
        // GIVEN
        when(project.getLanguages()).thenReturn(Arrays.asList("en", "ro"));
        when(project.isMultiLanguage()).thenReturn(true);

        String html = "<body><h1>Test Page</h1></body>";
        // component is scanned twice, once per language
        when(layoutPath.getReader()).thenReturn(new StringReader(html)).thenReturn(new StringReader(html));

        // WHEN
        builder.build();

        // THEN
        verify(buildFS, times(1)).setLanguage("en");
        verify(buildFS, times(1)).setLanguage("ro");
    }

    @Test
    public void GivenPwaWorkerExists_WhenBuild_ThenWritePwaWorkerOnBuildFilesystem() throws IOException {
        // GIVEN
        FilePath pwaWorker = mock(FilePath.class);
        when(pwaWorker.exists()).thenReturn(true);
        when(pwaWorker.getReader()).thenReturn(new StringReader("var cacheName = '@project/title';"));
        when(project.getPwaWorker()).thenReturn(pwaWorker);

        // WHEN
        builder.build();

        // THEN
        verify(buildFS, times(1)).writePwaWorker(any());
    }

    @Test
    public void GivenPageComponentScripts_WhenBuild_Then() throws IOException {
        // GIVEN
        when(descriptorPath.exists()).thenReturn(true);
        when(descriptorPath.getReader()).thenReturn(new StringReader("<page><script src='lib/format.js'/></page>"));

        FilePath scriptFile = mock(FilePath.class);
        when(scriptFile.exists()).thenReturn(true);
        when(scriptFile.getReader()).thenReturn(new StringReader(""));
        when(project.createFilePath("lib/format.js")).thenReturn(scriptFile);
        when(buildFS.writeScript(any(), any())).thenReturn("script/lib_format.js");

        // WHEN
        builder.build();

        // THEN
    }

    @Test(expected = WoodException.class)
    public void GivenPageComponentMissingScripts_WhenBuild_ThenWoodException() throws IOException {
        // GIVEN
        when(descriptorPath.exists()).thenReturn(true);
        when(descriptorPath.getReader()).thenReturn(new StringReader("<page><script src='lib/format.js'/></page>"));

        FilePath scriptFile = mock(FilePath.class);
        when(scriptFile.exists()).thenReturn(false);
        when(project.createFilePath("lib/format.js")).thenReturn(scriptFile);

        // WHEN
        builder.build();

        // THEN
    }
}
