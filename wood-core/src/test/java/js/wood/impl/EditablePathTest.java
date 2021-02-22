package js.wood.impl;

import static org.mockito.Mockito.*;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.FilePath;
import js.wood.Project;
import js.wood.WoodException;

@RunWith(MockitoJUnitRunner.class)
public class EditablePathTest {
	@Mock
	private Project project;

	@Before
	public void beforeTest() {
		when(project.getProjectRoot()).thenReturn(new File("."));
	}

	@Test
	public void constructor() {
		FilePath layoutPath = new FilePath(project, "res/page/index/index.htm");
		EditablePath path = new EditablePath(layoutPath, "res/template/page#page-body");
		
		assertThat(path.value(), equalTo("res/template/page/"));
		assertThat(path.getName(), equalTo("page"));
		assertThat(path.getEditableName(), equalTo("page-body"));
		assertThat(path.toString(), equalTo("res/template/page#page-body"));
	}

	@Test
	public void constructor_BadPath() {
		FilePath layoutPath = new FilePath(project, "res/page/index/index.htm");

		for (String path : new String[] { "", "/template/page", "/dir/template/page#body", "template/page/page.htm#body" }) {
			try {
				new EditablePath(layoutPath, path);
				fail("Editable path constructor with bad path value should rise exception.");
			} catch (Exception e) {
				assertTrue(e instanceof WoodException);
			}
		}
	}
	
	@Test
	public void editablePathAccept() {
		assertTrue(EditablePath.accept("template/page#body"));
		assertTrue(EditablePath.accept("res/template/page#body"));

		assertFalse(EditablePath.accept(""));
		assertFalse(EditablePath.accept("template/page"));
		assertFalse(EditablePath.accept("template/page/page.htm#body"));
	}

}
