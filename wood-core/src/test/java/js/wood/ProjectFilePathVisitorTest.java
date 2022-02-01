package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectFilePathVisitorTest {
	@Mock
	private Project project;
	@Mock
	private ICustomElementsRegistry customElements;
	@Mock
	private FilePath file;

	private Map<String, List<IScriptDescriptor>> scriptDependencies = new HashMap<>();

	private Project.IFilePathVisitor visitor;

	@Before
	public void beforeTest() {
		when(file.isComponentDescriptor()).thenReturn(true);

		visitor = new Project.FilePathVisitor(customElements, scriptDependencies);
	}

	@Test
	public void GivenFileIsComponentDescriptor_ThenCollectTagCompoPath() throws Exception {
		// given
		FilePath parentDir = mock(FilePath.class);

		// when
		visitor.visitFile(project, parentDir);

		// then
	}

	@Test
	public void GivenScriptWithDependency_ThenCollectDependency() throws Exception {
		// given
		when(file.isComponentDescriptor()).thenReturn(true);

		String document = "" + //
				"<compo>" + //
				"	<script src='lib/geo-map'>" + //
				"		<dependency src='lib/js-lib/js-lib.js'></dependency>" + //
				"	</script>" + //
				"</compo>";
		when(file.getReader()).thenReturn(new StringReader(document));

		// when
		visitor.visitFile(project, file);

		// then
		assertThat(scriptDependencies.keySet(), hasSize(1));
		assertTrue(scriptDependencies.containsKey("lib/geo-map"));

		List<IScriptDescriptor> dependencies = scriptDependencies.get("lib/geo-map");
		assertThat(dependencies, notNullValue());
		assertThat(dependencies.get(0), notNullValue());
		assertThat(dependencies.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
	}

	@Test
	public void GivenRegisteredScriptWithDependency_ThenCollectDependency() throws Exception {
		// given
		scriptDependencies.put("lib/geo-map", new ArrayList<>());
		when(file.isComponentDescriptor()).thenReturn(true);

		String document = "" + //
				"<compo>" + //
				"	<script src='lib/geo-map'>" + //
				"		<dependency src='lib/js-lib/js-lib.js'></dependency>" + //
				"	</script>" + //
				"</compo>";
		when(file.getReader()).thenReturn(new StringReader(document));

		// when
		visitor.visitFile(project, file);

		// then
		assertThat(scriptDependencies.keySet(), hasSize(1));
		assertTrue(scriptDependencies.containsKey("lib/geo-map"));

		List<IScriptDescriptor> dependencies = scriptDependencies.get("lib/geo-map");
		assertThat(dependencies, notNullValue());
		assertThat(dependencies.get(0), notNullValue());
		assertThat(dependencies.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
	}

	@Test
	public void GivenFileNotDescriptor_ThenEmptyTagCompoPaths() throws Exception {
		// given
		when(file.isComponentDescriptor()).thenReturn(false);

		// when
		visitor.visitFile(project, file);

		// then
	}

	@Test
	public void GivenDescriptorNotCompo_ThenEmptyTagCompoPaths() throws Exception {
		// given
		when(file.getReader()).thenReturn(new StringReader("<template></template>"));

		// when
		visitor.visitFile(project, file);

		// then
	}

	@Test
	public void GivenFileWithoutParent_ThenEmptyTagCompoPaths() throws Exception {
		// given
		when(file.getParentDir()).thenReturn(null);
		when(file.getReader()).thenReturn(new StringReader("<compo></compo>"));

		// when
		visitor.visitFile(project, file);

		// then
	}
}
