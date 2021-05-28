package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectScanTest {
	private Map<String, String> compoPaths = new HashMap<>();
	private Map<String, List<IScriptDescriptor>> scriptDependencies = new HashMap<>();

	@Test
	public void GivenFileIsComponentDescriptor_WhenVisitFile_ThenCollectCompoPath() throws Exception {
		// given
		Project.IFilePathVisitor visitor = new Project.FilePathVisitor(compoPaths, scriptDependencies);
		FilePath file = mock(FilePath.class);
		when(file.isComponentDescriptor()).thenReturn(true);
		when(file.getParentDir()).thenReturn(mock(FilePath.class));

		String document = "" + //
				"<compo>" + //
				"</compo>";
		when(file.getReader()).thenReturn(new StringReader(document));

		// when
		visitor.visitFile(file);

		// then
		assertThat(compoPaths.keySet(), hasSize(1));
	}
}
