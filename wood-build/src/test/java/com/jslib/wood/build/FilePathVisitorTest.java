package com.jslib.wood.build;

import com.jslib.wood.*;
import com.jslib.wood.impl.Variants;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.StringReader;
import java.util.List;
import java.util.Map;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@RunWith(MockitoJUnitRunner.class)
public class FilePathVisitorTest {
    @Mock
    private Project project;
    @Mock
    private FilePath file;
    @Mock
    private FilePath parentDir;
    @Mock
    private Map<FilePath, Variables> variables;
    @Mock
    private List<CompoPath> pages;

    private Project.IFilePathVisitor visitor;

    @Before
    public void beforeTest() {
        when(file.getParentDir()).thenReturn(parentDir);
        visitor = new BuilderProject.FilePathVisitor(variables, pages);
    }

    @Test
    public void GivenStringVariables_WhenVisitFile_ThenUpdateVariablesMap() throws Exception {
        // GIVEN
        when(file.isXml(Reference.Type.variables())).thenReturn(true);
        Variants variants = mock(Variants.class);
        when(file.getVariants()).thenReturn(variants);
        when(file.getReader()).thenReturn(new StringReader("<string></string>"));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        verify(variables, times(1)).put(any(), any());
    }

    @Test
    public void GivenPageDescriptor_WhenVisitFile_ThenUpdatePagesList() throws Exception {
        // GIVEN
        when(file.hasBaseName(any())).thenReturn(true);
        when(file.isXml("page")).thenReturn(true);

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        verify(pages, times(1)).add(any());
    }

    @Test
    public void GivenFileOnProjectRoot_WhenVisitFile_ThenNoUpdates() throws Exception {
        // GIVEN
        when(file.getParentDir()).thenReturn(null);

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        verify(pages, times(0)).add(any());
        verify(variables, times(0)).put(any(), any());
    }

    @Test
    public void GivenNoVariablesOrPageComponent_WhenVisitFile_ThenNoUpdates() throws Exception {
        // GIVEN

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        verify(pages, times(0)).add(any());
        verify(variables, times(0)).put(any(), any());
    }
}
