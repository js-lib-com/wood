package com.jslib.wood.build;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.wood.CompoPath;
import com.jslib.wood.FilePath;
import com.jslib.wood.Project;
import com.jslib.wood.Variables;

@RunWith(MockitoJUnitRunner.class)
public class ProjectScanTest {
	@Mock
	private BuilderProject project;
	@Mock
	private Map<FilePath, Variables> variables;
	@Mock
	private List<CompoPath> pages;

	@Test
	public void Given_WhenVisitFile_Then() throws Exception {
		// given
		Project.IFilePathVisitor visitor = new BuilderProject.FilePathVisitor(variables, pages);
		FilePath file = mock(FilePath.class);

		// when
		visitor.visitFile(project, file);

		// then
	}
}
