package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.not;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.wood.CompoPath;
import js.wood.Path;
import js.wood.Project;
import js.wood.WoodException;

public class PathTest {
	private Project project;

	@Before
	public void beforeTest() throws Exception {
		project = new Project(new File("src/test/resources/project"));
	}

	@Test
	public void constructor() {
		// uses TestPath to create Path instance that is abstract
		Path path = new TestPath(project, "res/compo/discography/discography.htm");
		assertThat(path.value(), equalTo("res/compo/discography/discography.htm"));
		assertTrue(path.exists());
		assertThat(path.toFile().getPath().replace('\\', '/'), equalTo("src/test/resources/project/res/compo/discography/discography.htm"));
		assertTrue(path.getProject() == project);
		assertThat(path.hashCode(), not(equalTo(0)));
		assertThat(path.toString(), equalTo("res/compo/discography/discography.htm"));
	}

	@Test
	public void constructor_InvalidPath() {
		Path path = new TestPath(project, "http://server/path");
		assertThat(path.value(), equalTo("http://server/path"));
	}

	@Test
	public void notExisting() {
		// uses TestPath to create Path instance that is abstract
		Path path = new TestPath(project, "res/compo/fake/fake.htm");
		assertFalse(path.exists());
	}

	@Test
	public void toFile() throws Throwable {
		assertPath("src/test/resources/project/res/compo/discography", "res/compo/discography", CompoPath.class);
		assertPath("src/test/resources/project/lib/js-lib/js-lib.js", "lib/js-lib/js-lib.js", TestPath.class);
		assertPath("src/test/resources/project/script/hc/view/DiscographyView.js", "script/hc/view/DiscographyView.js", TestPath.class);
		assertPath("src/test/resources/project/gen/js/controller/MainController.js", "gen/js/controller/MainController.js", TestPath.class);
	}

	private void assertPath(String expectedFile, String pathValue, Class<?> pathClass) throws Throwable {
		Path path = (Path) Classes.newInstance(pathClass, project, pathValue);
		assertThat(path.toFile(), equalTo(new File(expectedFile)));
	}

	@Test(expected = WoodException.class)
	public void toFile_Exception() {
		// uses TestPath to create Path instance that is abstract
		Path path = new TestPath(project, "res/compo/fake/fake.htm");
		path.toFile();
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

	private static class TestPath extends Path {
		public TestPath(Project project, String value) {
			super(project, value);
		}
	}
}
