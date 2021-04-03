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
public class CompoUpdateTest {
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

	private CompoUpdate task;

	@Before
	public void beforeTest() {
		when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));

		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve(anyString())).thenReturn(compoDir);
		when(files.exists(compoDir)).thenReturn(true);

		when(compoName.isValid()).thenReturn(true);
		when(compoName.path()).thenReturn("lib/captcha");

		task = new CompoUpdate();
		task.setCommandSpec(commandSpec);
		task.setConsole(console);
		task.setFiles(files);
		task.setName(compoName);
	}

	@Test
	public void GivenDefaultOptions_ThenUpdate() throws Exception {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
	}

	@Test(expected = ParameterException.class)
	public void GivenInvalidCompoName_ThenParameterException() throws IOException {
		// given
		when(compoName.isValid()).thenReturn(false);

		// when
		task.exec();

		// then
	}

	@Test
	public void GivenMissingCompoDir_ThenAbort() throws IOException {
		// given
		when(files.exists(compoDir)).thenReturn(false);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(files, times(0)).cleanDirectory(compoDir, false);
		verify(console, times(1)).print("Command abort.");
	}
}
