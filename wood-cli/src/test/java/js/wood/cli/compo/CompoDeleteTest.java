package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;

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
public class CompoDeleteTest {
	@Mock
	private CommandSpec commandSpec;

	@Mock
	private Console console;
	@Mock
	private FilesUtil files;

	@Mock
	private Path projectDir;
	@Mock
	private Path compoDir;
	@Mock
	private CompoName compoName;

	private CompoDelete task;

	@Before
	public void beforeTest() {
		when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));
		when(compoName.isValid()).thenReturn(true);
		when(compoName.path()).thenReturn("res/compo/dialog");

		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve("res/compo/dialog")).thenReturn(compoDir);
		when(files.exists(compoDir)).thenReturn(true);

		task = new CompoDelete();
		task.setCommandSpec(commandSpec);
		task.setConsole(console);
		task.setFiles(files);
		task.setName(compoName);
	}

	@Test
	public void GivenUserConfirm_ThenCleanDirectory() throws IOException {
		// given
		when(console.confirm(anyString(), anyString())).thenReturn(true);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(files, times(1)).cleanDirectory(compoDir, false);
	}

	@Test
	public void GivenCompoIsUsed_ThenAbort() throws IOException {
		// given
		when(files.findFilesByContentPattern(projectDir, ".htm", "res/compo/dialog")).thenReturn(Arrays.asList(mock(Path.class)));

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(files, times(0)).cleanDirectory(compoDir, false);
		verify(console, times(1)).print("Command abort.");
	}

	@Test
	public void GivenUserNotConfirm_ThenCancel() throws Exception {
		// given
		when(console.confirm(anyString(), anyString())).thenReturn(false);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.CANCEL));
		verify(files, times(0)).cleanDirectory(compoDir, false);
		verify(console, times(1)).print("User cancel.");
	}

	@Test(expected = ParameterException.class)
	public void GivenInvalidCompoName_ThenParameterException() throws IOException {
		// given
		when(compoName.isValid()).thenReturn(false);

		// when
		task.exec();

		// then
	}

	@Test(expected = ParameterException.class)
	public void GivenMissingCompoDir_ThenParameterException() throws IOException {
		// given
		when(files.exists(compoDir)).thenReturn(false);

		// when
		task.exec();

		// then
	}
}
