package js.wood;

import static org.mockito.Mockito.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class CompoPathTest {
	@Mock
	private Project project;

	@Before
	public void beforeTest() {
		when(project.getProjectRoot()).thenReturn(new File("."));
	}

	@Test
	public void constructor() {
		File path = Mockito.mock(File.class);
		when(path.getPath()).thenReturn("res/page");

		CompoPath compoPath = new CompoPath(project, path);
		assertThat(compoPath.value(), equalTo("res/page"));
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
	public void getLayoutPath() {
		FilePath layoutPath = Mockito.mock(FilePath.class);
		when(layoutPath.value()).thenReturn("res/page/page.htm");
		when(project.createFilePath("res/page/page.htm")).thenReturn(layoutPath);

		File path = Mockito.mock(File.class);
		when(path.isDirectory()).thenReturn(true);
		when(path.getPath()).thenReturn("res/page");
		when(path.getName()).thenReturn("page");

		CompoPath compo = new CompoPath(project, path);
		assertThat(compo.getLayoutPath().value(), equalTo("res/page/page.htm"));
	}

	/**
	 * Create CompoPath from Java file that is not directory meaning that its layout file is inline. This inline layout path
	 * returns false on {@link FilePath#exists()} and we should have WoodException.
	 */
	@Test(expected = WoodException.class)
	public void getLayoutPath_MissingInlineComponent() {
		FilePath layoutPath = Mockito.mock(FilePath.class);
		when(project.createFilePath("res/page.htm")).thenReturn(layoutPath);

		File path = Mockito.mock(File.class);
		when(path.getPath()).thenReturn("res/page/");

		CompoPath compo = new CompoPath(project, path);
		compo.getLayoutPath();
	}

	@Test
	public void equals() {
		final String path = "res/compo/video-player/";
		Path dirPath = new DirPath(project, path);
		Path compoPath = new CompoPath(project, path);
		assertTrue(dirPath.equals(compoPath));
	}

	@Test
	public void accept() {
		assertTrue(CompoPath.accept("res/compo"));
		assertTrue(CompoPath.accept("path/compo"));
		assertTrue(CompoPath.accept("path/compo/"));
		assertTrue(CompoPath.accept("res/path/compo"));

		assertFalse(CompoPath.accept("compo"));
		assertFalse(CompoPath.accept("res/path/compo/compo.htm"));
		assertFalse(CompoPath.accept("res/path/compo/compo.htm#fragment-id"));
		assertFalse(CompoPath.accept("res/path/compo/compo.css"));
	}
}
