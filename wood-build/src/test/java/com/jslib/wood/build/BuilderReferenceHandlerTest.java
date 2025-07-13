package com.jslib.wood.build;

import com.jslib.wood.FilePath;
import com.jslib.wood.Reference;
import com.jslib.wood.Variables;
import com.jslib.wood.WoodException;
import com.jslib.wood.impl.FileType;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.IOException;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class BuilderReferenceHandlerTest {
    @Mock
    private BuilderProject project;
    @Mock
    private BuildFS buildFS;
    @Mock
    private Variables dirVariables;
    @Mock
    private Variables assetVariables;

    private Builder builder;

    @Before
    public void beforeTest() {
        when(project.getVariables(null)).thenReturn(dirVariables);
        when(project.getAssetVariables()).thenReturn(assetVariables);

        builder = new Builder(project, buildFS);
        builder.setLanguage("en");
    }

    @Test
    public void GivenDirVariables_WhenOnResourceReference_ThenValue() throws IOException {
        // GIVEN
        Reference reference = new Reference(Reference.Type.STRING, "title");
        FilePath source = mock(FilePath.class);
        when(dirVariables.get("en", reference, source, builder)).thenReturn("Page Title");

        // WHEN
        String value = builder.onResourceReference(reference, source);

        // THEN
        assertThat(value, notNullValue());
        assertThat(value, equalTo("Page Title"));
    }

    @Test
    public void GivenAssetVariables_WhenOnResourceReference_ThenValue() throws IOException {
        // GIVEN
        Reference reference = new Reference(Reference.Type.STRING, "title");
        FilePath source = mock(FilePath.class);
        when(assetVariables.get("en", reference, source, builder)).thenReturn("Project Title");

        // WHEN
        String value = builder.onResourceReference(reference, source);

        // THEN
        assertThat(value, notNullValue());
        assertThat(value, equalTo("Project Title"));
    }

    @Test(expected = WoodException.class)
    public void GivenMissingVariables_WhenOnResourceReference_ThenWoodException() throws IOException {
        // GIVEN
        Reference reference = new Reference(Reference.Type.STRING, "title");
        FilePath source = mock(FilePath.class);

        // WHEN
        builder.onResourceReference(reference, source);

        // THEN
    }

    @Test
    public void GivenLayoutMedia_WhenOnResourceReference_ThenMediaPath() throws IOException {
        // GIVEN
        Reference reference = new Reference(Reference.Type.IMAGE, "icon");
        FilePath source = mock(FilePath.class);
        when(source.getType()).thenReturn(FileType.LAYOUT);

        FilePath mediaFile = mock(FilePath.class);
        when(project.getResourceFile("en", reference, source)).thenReturn(mediaFile);
        when(buildFS.writePageMedia(null, mediaFile)).thenReturn("../media/icon.png");

        // WHEN
        String value = builder.onResourceReference(reference, source);

        // THEN
        assertThat(value, notNullValue());
        assertThat(value, equalTo("../media/icon.png"));
    }

    @Test
    public void GivenStyleMedia_WhenOnResourceReference_ThenMediaPath() throws IOException {
        // GIVEN
        Reference reference = new Reference(Reference.Type.IMAGE, "icon");
        FilePath source = mock(FilePath.class);
        when(source.getType()).thenReturn(FileType.STYLE);

        FilePath mediaFile = mock(FilePath.class);
        when(project.getResourceFile("en", reference, source)).thenReturn(mediaFile);
        when(buildFS.writeStyleMedia(mediaFile)).thenReturn("../media/icon.png");

        // WHEN
        String value = builder.onResourceReference(reference, source);

        // THEN
        assertThat(value, notNullValue());
        assertThat(value, equalTo("../media/icon.png"));
    }

    @Test
    public void GivenBadSourceForMedia_WhenOnResourceReference_ThenNullPath() throws IOException {
        // GIVEN
        Reference reference = new Reference(Reference.Type.IMAGE, "icon");
        FilePath source = mock(FilePath.class);
        when(source.getType()).thenReturn(FileType.XML);

        FilePath mediaFile = mock(FilePath.class);
        when(project.getResourceFile("en", reference, source)).thenReturn(mediaFile);

        // WHEN
        String value = builder.onResourceReference(reference, source);

        // THEN
        assertThat(value, nullValue());
    }

    @Test
    public void GivenScriptMedia_WhenOnResourceReference_ThenMediaPath() throws IOException {
        // GIVEN
        Reference reference = new Reference(Reference.Type.IMAGE, "icon");
        FilePath source = mock(FilePath.class);
        when(source.getType()).thenReturn(FileType.SCRIPT);

        FilePath mediaFile = mock(FilePath.class);
        when(project.getResourceFile("en", reference, source)).thenReturn(mediaFile);
        when(buildFS.writeScriptMedia(mediaFile)).thenReturn("../media/icon.png");

        // WHEN
        String value = builder.onResourceReference(reference, source);

        // THEN
        assertThat(value, notNullValue());
        assertThat(value, equalTo("../media/icon.png"));
    }

    @Test(expected = WoodException.class)
    public void GivenMissingMedia_WhenOnResourceReference_ThenWoodException() throws IOException {
        // GIVEN
        Reference reference = new Reference(Reference.Type.IMAGE, "icon");
        FilePath source = mock(FilePath.class);

        // WHEN
        builder.onResourceReference(reference, source);

        // THEN
    }
}
