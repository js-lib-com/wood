package js.wood.cli.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.Path;
import java.nio.file.spi.FileSystemProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.cli.Console;
import js.wood.cli.ExitCode;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDestroyTest {
	@Mock
	private FileSystemProvider provider;
	@Mock
	private FileSystem fileSystem;
	@Mock
	private Console console;

	@Mock
	private Path rootDir;
	@Mock
	private Path projectDir;
	@Mock
	private Path descriptorFile;

	private ProjectDestroy task;

	@Before
	public void beforeTest() {
		when(fileSystem.provider()).thenReturn(provider);
		when(fileSystem.getPath("")).thenReturn(rootDir);
		when(rootDir.toAbsolutePath()).thenReturn(rootDir);
		when(rootDir.resolve("test")).thenReturn(projectDir);
		when(projectDir.getFileSystem()).thenReturn(fileSystem);
		when(projectDir.resolve("project.xml")).thenReturn(descriptorFile);
		when(descriptorFile.getFileSystem()).thenReturn(fileSystem);

		task = new ProjectDestroy();
		task.setFileSystem(fileSystem);
		task.setConsole(console);
		task.setName("test");
	}

	@Test
	public void exec_GivenUserConfirm_ThenProjectDestroy() throws IOException {
		// given
		when(console.confirm(anyString(), anyString())).thenReturn(true);
		File projectDirFile = mock(File.class);
		when(projectDir.toFile()).thenReturn(projectDirFile);
		when(projectDirFile.isDirectory()).thenReturn(true);
		when(projectDirFile.listFiles()).thenReturn(new File[0]);
		when(projectDirFile.getAbsolutePath()).thenReturn("test");

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(console, times(1)).confirm(anyString(), anyString());
		verify(console, times(1)).crlf();
		verify(console, times(0)).print(anyString());
	}

	@Test
	public void exec_GivenUserNotConfirm_ThenCancel() throws IOException {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.CANCEL));
		verify(console, times(1)).crlf();
		verify(console, times(1)).print(anyString());
	}

	@Test(expected = ParameterException.class)
	public void exec_GivenMissingProjectDir_ThenThrowParameterException() throws IOException {
		// given
		doThrow(IOException.class).when(provider).checkAccess(projectDir);
		CommandSpec commandSpec = mock(CommandSpec.class);
		task.setCommandSpec(commandSpec);
		when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.BAD_PARAMETER));
		verify(console, times(0)).crlf();
		verify(console, times(0)).print(anyString());
	}

	@Test
	public void exec_GivenMissingDescriptorFile_ThenAbort() throws IOException {
		// given
		doThrow(IOException.class).when(provider).checkAccess(descriptorFile);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(console, times(0)).confirm(anyString(), anyString());
		verify(console, times(1)).print(anyString(), eq(projectDir));
		verify(console, times(2)).print(anyString());
		verify(console, times(1)).crlf();
		verify(console, times(1)).warning(anyString());
	}

	@Test
	public void exec_GivenMissingDescriptorFileAndUserForceAndConfirm_ThenProjectDestroy() throws IOException {
		// given
		task.setForce(true);
		when(console.confirm(anyString(), anyString())).thenReturn(true);
		File projectDirFile = mock(File.class);
		when(projectDir.toFile()).thenReturn(projectDirFile);
		when(projectDirFile.isDirectory()).thenReturn(true);
		when(projectDirFile.listFiles()).thenReturn(new File[0]);
		when(projectDirFile.getAbsolutePath()).thenReturn("test");

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(console, times(1)).confirm(anyString(), anyString());
		verify(console, times(1)).crlf();
		verify(console, times(0)).print(anyString());
	}
}
