package js.wood;

import static org.mockito.Mockito.mock;

import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

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
		Project.IFilePathVisitor visitor = new BuilderProject.FilePathVisitor(project, variables, pages);
		FilePath file = mock(FilePath.class);

		// when
		visitor.visitFile(file);

		// then
	}
}
