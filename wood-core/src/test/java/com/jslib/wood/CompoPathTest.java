package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.*;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

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
	public void GivenValidPath_WhenConstructor_ThenInitInternalState() {
		// GIVEN
		File path = Mockito.mock(File.class);
		when(path.getPath()).thenReturn("res/page");

		// WHEN
		CompoPath compoPath = new CompoPath(project, path);

		// THEN
		assertThat(compoPath.value(), equalTo("res/page/"));
	}

	@Test(expected = WoodException.class)
	public void GivenAbsolutePath_WhenConstructor_ThenException() {
		new CompoPath(project, "/res/compo");
	}

	@Test(expected = WoodException.class)
	public void GivenPathWithSpace_WhenConstructor_ThenException() {
		new CompoPath(project, "/res/pa th/compo");
	}

	@Test
	public void GivenCompoDirWithExistingLayout_WhenGetLayoutPath_ThenHtmlFile() {
		// GIVEN
		FilePath layoutPath = mock(FilePath.class);
		when(layoutPath.value()).thenReturn("res/page/page.htm");
		when(project.createFilePath("res/page/page.htm")).thenReturn(layoutPath);

		File path = mock(File.class);
		when(path.isDirectory()).thenReturn(true);
		when(path.getPath()).thenReturn("res/page");

		CompoPath compo = new CompoPath(project, path);

		// WHEN
		FilePath compoLayoutPath = compo.getLayoutPath();

		// THEN
		assertThat(compoLayoutPath.value(), equalTo("res/page/page.htm"));
	}

	/**
	 * Create CompoPath from Java file that is not directory meaning that its layout file is inline. This inline layout path
	 * returns false on {@link FilePath#exists()} and we should have WoodException.
	 */
	@Test
	public void GivenInlineCompoWithMissingLayout_WhenGetLayoutPath_ThenReturnLayout() {
		// GIVEN
		FilePath layoutPath = mock(FilePath.class);
		when(project.createFilePath("res/page.htm")).thenReturn(layoutPath);

		File path = mock(File.class);
		when(path.getPath()).thenReturn("res/page/");

		CompoPath compo = new CompoPath(project, path);
		
		// WHEN
		FilePath compoLayoutPath = compo.getLayoutPath();
		
		// THEN
		assertThat(compoLayoutPath, notNullValue());
		assertFalse(compoLayoutPath.exists());
	}

	@Test
	public void GivenSamePath_WhenCreateDirAndCompoPaths_ThenEquals() {
		// GIVEN
		final String path = "res/compo/video-player/";

		// WHEN
		FilePath dirPath = new FilePath(project, path);
		FilePath compoPath = new CompoPath(project, path);

		// THEN
        assertEquals(dirPath, compoPath);
	}

	@Test
	public void GivenValidPath_WhenAccept_ThenTrue() {
		assertTrue(CompoPath.accept("res/compo"));
		assertTrue(CompoPath.accept("path/compo"));
		assertTrue(CompoPath.accept("path/compo/"));
		assertTrue(CompoPath.accept("res/path/compo"));
		assertTrue(CompoPath.accept("compo"));
	}

	@Test
	public void GivenInvalidPath_WhenAccept_ThenFalse() {
		assertFalse(CompoPath.accept("res/path/compo/compo.htm"));
		assertFalse(CompoPath.accept("res/path/compo/compo.htm#fragment-id"));
		assertFalse(CompoPath.accept("res/path/compo/compo.css"));
	}
}
