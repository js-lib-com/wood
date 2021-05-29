package js.wood.build;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.CompoPath;
import js.wood.FilePath;
import js.wood.Project;
import js.wood.Variables;
import js.wood.build.BuilderProject;

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
