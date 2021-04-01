package js.wood.cli.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.cli.project.ProjectList.Utils;

@RunWith(MockitoJUnitRunner.class)
public class ProjectListUtilsTest {
	@Mock
	private FileSystemProvider provider;
	@Mock
	private FileSystem fileSystem;

	@Mock
	private Path workingDir;

	private ProjectList task;

	@Before
	public void beforeTest() throws IOException {
		when(fileSystem.provider()).thenReturn(provider);
		task = new ProjectList();
	}

	@Test
	public void isExcluded_GivenEmptyExcludesOption_ThenReturnFalse() {
		// given
		Utils utils = task.new Utils();

		// when
		boolean excluded = utils.isExcluded(workingDir);

		// then
		assertThat(excluded, is(false));
	}

	@Test
	public void isExcluded_GivenDirectoryNotInExcludesOption_ThenReturnFalse() {
		// given
		when(workingDir.toString()).thenReturn("test");
		task.setExcludes(Arrays.asList("fake"));
		Utils utils = task.new Utils();

		// when
		boolean excluded = utils.isExcluded(workingDir);

		// then
		assertThat(excluded, is(false));
	}

	@Test
	public void isExcluded_GivenDirectoryIsInExcludesOption_ThenReturnTrue() {
		// given
		when(workingDir.toString()).thenReturn("test");
		task.setExcludes(Arrays.asList("test"));
		Utils utils = task.new Utils();

		// when
		boolean excluded = utils.isExcluded(workingDir);

		// then
		assertThat(excluded, is(true));
	}

	@Test
	public void isXML_GivenRootMatches_ThenReturnTrue() throws IOException {
		// given
		Path file = mock(Path.class);
		when(file.getFileSystem()).thenReturn(fileSystem);
		when(provider.newInputStream(file)).thenReturn(new ByteArrayInputStream("<?xml version='1.0' ?>\n<page>\n</page>\n".getBytes()));
		Utils utils = task.new Utils();

		// when
		boolean xml = utils.isXML(file, "page");

		// then
		assertThat(xml, is(true));
	}

	@Test
	public void isXML_GivenRootDoesNotMatches_ThenReturnTrue() throws IOException {
		// given
		Path file = mock(Path.class);
		when(file.getFileSystem()).thenReturn(fileSystem);
		when(provider.newInputStream(file)).thenReturn(new ByteArrayInputStream("<page>\n</page>\n".getBytes()));
		Utils utils = task.new Utils();

		// when
		boolean xml = utils.isXML(file, "template");

		// then
		assertThat(xml, is(false));
	}

	@Test
	public void isXML_GivenMissingContent_ThenReturnFalse() throws IOException {
		// given
		Path file = mock(Path.class);
		when(file.getFileSystem()).thenReturn(fileSystem);
		when(provider.newInputStream(file)).thenReturn(new ByteArrayInputStream("<?xml version='1.0' ?>".getBytes()));
		Utils utils = task.new Utils();

		// when
		boolean xml = utils.isXML(file, "page");

		// then
		assertThat(xml, is(false));
	}

	@Test
	public void isXML_GivenFileDoesNotExist_ThenReturnFalse() throws IOException {
		// given
		Path file = mock(Path.class);
		when(file.getFileSystem()).thenReturn(fileSystem);
		doThrow(IOException.class).when(provider).checkAccess(file);
		Utils utils = task.new Utils();

		// when
		boolean xml = utils.isXML(file, "page");

		// then
		assertThat(xml, is(false));
	}

}
