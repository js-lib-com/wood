package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;

import js.wood.CompoPath;
import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.Path;
import js.wood.Project;
import js.wood.WoodException;

public class CompoPathTest {
	private Project project;

	@Before
	public void beforeTest() {
		project = new Project(new File("src/test/resources/project"));
	}

	@Test
	public void constructor() {
		assertCompoPath("res/compo/discography", "res/compo/discography/discography.htm", "res/compo/discography/", "discography");
		assertCompoPath("res/compo/discography", "res/compo/discography/discography.htm", "res/compo/discography/", "discography");
	}

	private void assertCompoPath(String pathValue, String layout, String value, String name) {
		CompoPath p = new CompoPath(project, pathValue);
		assertThat(p.getLayoutPath().value(), equalTo(layout));
		assertThat(p.value(), equalTo(value));
		assertThat(p.getName(), equalTo(name));
	}

	@Test(expected = WoodException.class)
	public void constructor_MissingInlineComponent() {
		new CompoPath(project, "res/compo/missing-component");
	}

	@Test(expected = WoodException.class)
	public void constructor_BadSourceDirectory() {
		new CompoPath(project, "/res/compo");
	}

	@Test(expected = WoodException.class)
	public void constructor_BadPath() {
		new CompoPath(project, "/res/pa th/compo");
	}

	@Test
	public void getFilePath() {
		CompoPath compo = new CompoPath(project, "res/compo/discography");
		FilePath file = compo.getFilePath("picture.png");
		assertThat(file.value(), equalTo("res/compo/discography/picture.png"));
		assertThat(file.getParentDirPath().value(), equalTo("res/compo/discography/"));
		assertThat(file.getName(), equalTo("picture.png"));
	}

	@Test
	public void getLayoutPath() {
		CompoPath compo = new CompoPath(project, "res/compo/discography");
		assertThat(compo.getLayoutPath().value(), equalTo("res/compo/discography/discography.htm"));
	}

	@Test
	public void getLayoutPath_InlineComponent() {
		CompoPath compo = new CompoPath(project, "res/compo/select");
		assertThat(compo.getLayoutPath().value(), equalTo("res/compo/select.htm"));
	}

	@Test
	public void equals() {
		final String path = "res/compo/video-player";
		Path dirPath = new DirPath(project, path);
		Path compoPath = new CompoPath(project, path);
		assertTrue(dirPath.equals(compoPath));
	}

	@Test
	public void accept() {
		assertFalse(CompoPath.accept("compo"));
		assertTrue(CompoPath.accept("res/compo"));
		assertTrue(CompoPath.accept("path/compo"));
		assertTrue(CompoPath.accept("res/path/compo"));
		assertFalse(CompoPath.accept("res/path/compo/compo.htm"));
		assertFalse(CompoPath.accept("res/path/compo/compo.htm#fragment-id"));
		assertFalse(CompoPath.accept("res/path/compo/compo.css"));
	}

	@Test
	public void accept_LogicFlaw() {
		// here is a component path logic flaw
		// because res directory is optional source directory cannot reliable be detected
		// accordingly syntax description next path value should be rejected but is accepted
		assertTrue(CompoPath.accept("dir/template/page"));
	}
}
