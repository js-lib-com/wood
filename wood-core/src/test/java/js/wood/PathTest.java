package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class PathTest {
	@Mock
	private Project project;

	@Before
	public void beforeTest() throws Exception {
		when(project.getProjectRoot()).thenReturn(new File("."));
	}

	@Test
	public void constructor_FilePath() {
		Path path = new TestPath(project, "res/compo/compo.htm");
		assertThat(path.value(), equalTo("res/compo/compo.htm"));
		assertFalse(path.exists());
		assertTrue(path.getProject() == project);
		assertThat(path.hashCode(), not(equalTo(0)));
		assertThat(path.toString(), equalTo("res/compo/compo.htm"));
	}

	@Test
	public void constructor_DirectoryPath() {
		Path path = new TestPath(project, "res/compo/");
		assertThat(path.value(), equalTo("res/compo/"));
		assertFalse(path.exists());
		assertTrue(path.getProject() == project);
		assertThat(path.hashCode(), not(equalTo(0)));
		assertThat(path.toString(), equalTo("res/compo/"));
	}

	@Test
	public void constructor_RootPath() {
		Path path = new TestPath(project);
		assertThat(path.value(), equalTo(""));
		assertTrue(path.exists());
		assertTrue(path.getProject() == project);
		assertThat(path.hashCode(), not(equalTo(0)));
		assertThat(path.toString(), equalTo(""));
	}

	@Test
	public void constructor_File() {
		Path path = new TestPath(project, new File("res/compo/"));
		assertThat(path.value(), equalTo("res/compo"));
		assertFalse(path.exists());
		assertTrue(path.getProject() == project);
		assertThat(path.hashCode(), not(equalTo(0)));
		assertThat(path.toString(), equalTo("res/compo"));
	}

	@Test
	public void constructor_InvalidPath() {
		Path path = new TestPath(project, "http://server/path");
		assertThat(path.value(), equalTo("http://server/path"));
	}

	@Test
	public void equals() {
		Path path1 = new TestPath(project, "res/path1.htm");
		Path path1bis = new TestPath(project, "res/path1.htm");
		Path path2 = new TestPath(project, "res/path2.htm");
		assertThat(path1.equals(path1), equalTo(true));
		assertThat(path1.equals(path1bis), equalTo(true));
		assertThat(path1.equals(path2), equalTo(false));
		assertThat(path1.equals(null), equalTo(false));
	}

	@Test
	public void equals_NullValue() {
		Path path1 = new TestPath(project);
		Path path2 = new TestPath(project, "res/path.htm");
		assertFalse(path1.equals(path2));
	}

	private static class TestPath extends Path {
		public TestPath(Project project) {
			super(project);
		}

		public TestPath(Project project, String value) {
			super(project, value);
		}

		public TestPath(Project project, File file) {
			super(project, file);
		}
	}
}
