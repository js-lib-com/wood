package js.wood.cli.project;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.cli.Console;
import js.wood.cli.ExitCode;
import js.wood.cli.FilesUtil;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDestroyTest {
	@Mock
	private CommandSpec commandSpec;
	@Mock
	private Console console;
	@Mock
	private FilesUtil files;

	@Mock
	private Path workingDir;
	@Mock
	private Path projectDir;
	@Mock
	private Path descriptorFile;

	private ProjectDestroy task;

	@Before
	public void beforeTest() {
		when(files.getWorkingDir()).thenReturn(workingDir);
		when(files.exists(projectDir)).thenReturn(true);
		when(files.exists(descriptorFile)).thenReturn(true);

		when(workingDir.resolve("test")).thenReturn(projectDir);
		when(projectDir.resolve("project.xml")).thenReturn(descriptorFile);

		task = new ProjectDestroy();
		task.setCommandSpec(commandSpec);
		task.setConsole(console);
		task.setFiles(files);
		task.setName("test");
	}

	@Test
	public void GivenUserConfirm_ThenProjectDestroy() throws IOException {
		// given
		when(console.confirm(anyString(), anyString())).thenReturn(true);

		// when
		ExitCode exitCode = task.exec();

		// then
		verify(files, times(1)).cleanDirectory(eq(projectDir), anyBoolean());
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(console, times(1)).confirm(anyString(), anyString());
		verify(console, times(1)).crlf();
		verify(console, times(0)).print(anyString());
	}

	@Test
	public void GivenUserNotConfirm_ThenCancel() throws IOException {
		// given
		when(console.confirm(anyString(), anyString())).thenReturn(false);

		// when
		ExitCode exitCode = task.exec();

		// then
		verify(files, times(0)).cleanDirectory(eq(projectDir), anyBoolean());
		assertThat(exitCode, equalTo(ExitCode.CANCEL));
		verify(console, times(1)).crlf();
		verify(console, times(1)).print(anyString());
	}

	@Test(expected = ParameterException.class)
	public void GivenMissingProjectDir_ThenThrowParameterException() throws IOException {
		// given
		when(files.exists(projectDir)).thenReturn(false);
		CommandSpec commandSpec = mock(CommandSpec.class);
		task.setCommandSpec(commandSpec);
		when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));

		// when
		ExitCode exitCode = task.exec();

		// then
		verify(files, times(0)).cleanDirectory(eq(projectDir), anyBoolean());
		assertThat(exitCode, equalTo(ExitCode.BAD_PARAMETER));
		verify(console, times(0)).crlf();
		verify(console, times(0)).print(anyString());
	}

	@Test
	public void GivenMissingDescriptorFile_ThenAbort() throws IOException {
		// given
		when(files.exists(descriptorFile)).thenReturn(false);

		// when
		ExitCode exitCode = task.exec();

		// then
		verify(files, times(0)).cleanDirectory(eq(projectDir), anyBoolean());
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(console, times(0)).confirm(anyString(), anyString());
		verify(console, times(1)).print(anyString(), eq(projectDir));
		verify(console, times(2)).print(anyString());
		verify(console, times(1)).crlf();
		verify(console, times(1)).warning(anyString());
	}

	@Test
	public void GivenMissingDescriptorFileAndUserForceAndConfirm_ThenProjectDestroy() throws IOException {
		// given
		task.setForce(true);
		when(console.confirm(anyString(), anyString())).thenReturn(true);

		// when
		ExitCode exitCode = task.exec();

		// then
		verify(files, times(1)).cleanDirectory(eq(projectDir), anyBoolean());
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(console, times(1)).confirm(anyString(), anyString());
		verify(console, times(1)).crlf();
		verify(console, times(0)).print(anyString());
	}
}
