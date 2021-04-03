package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import js.wood.cli.TextReplace;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@RunWith(MockitoJUnitRunner.class)
public class CompoMoveTest {
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
	private Path compoFile;
	@Mock
	private Path targetDir;
	@Mock
	private Path targetCompoDir;
	@Mock
	private Path targetCompoFile;
	@Mock
	private Path newCompoScriptFile;

	@Mock
	private CompoName compoName;
	@Mock
	private TextReplace textReplace;

	private CompoMove task;

	@Before
	public void beforeTest() throws IOException {
		when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));

		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve("lib/captcha")).thenReturn(compoDir);
		when(projectDir.resolve("res/compo")).thenReturn(targetDir);
		when(targetDir.resolve((Path) null)).thenReturn(targetCompoDir);
		when(targetCompoDir.resolve(compoFile)).thenReturn(targetCompoFile);
		when(projectDir.resolve("res/compo/captcha/captcha.js")).thenReturn(newCompoScriptFile);

		when(files.exists(compoDir)).thenReturn(true);
		when(files.getFileName(compoDir)).thenReturn("captcha");
		when(files.exists(targetDir)).thenReturn(true);
		when(files.exists(newCompoScriptFile)).thenReturn(true);
		when(files.listFiles(compoDir)).thenReturn(Arrays.asList(compoFile));

		when(compoName.isValid()).thenReturn(true);
		when(compoName.path()).thenReturn("lib/captcha");

		task = new CompoMove();
		task.setCommandSpec(commandSpec);
		task.setConsole(console);
		task.setFiles(files);
		task.setName(compoName);
		task.setTarget("res/compo");
		task.setTextReplace(textReplace);
	}

	@Test
	public void GivenDefaultOptions_ThenMove() throws IOException {
		// given
		task.setVerbose(true);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(files, times(1)).createDirectoryIfNotExist(targetCompoDir);
		verify(files, times(1)).move(compoFile, targetCompoFile);
		verify(files, times(1)).delete(compoDir);
		verify(console, times(1)).print(anyString(), eq(compoFile), eq(targetCompoFile));
		verify(textReplace, times(1)).replaceAll(eq(null), eq("lib/captcha"), eq("res/compo/captcha"));
		verify(textReplace, times(1)).replaceAll(eq(null), eq("lib/captcha/captcha.js"), eq("res/compo/captcha/captcha.js"));
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

	@Test
	public void GivenMissingTargetDir_ThenCreateIt() throws IOException {
		// given
		when(files.exists(targetDir)).thenReturn(false);
		when(console.confirm(anyString(), anyString())).thenReturn(true);

		// when
		task.exec();

		// then
		verify(files, times(1)).createDirectory(targetDir);
	}

	@Test
	public void GivenMissingTargetDirAndNoUserConfirm_ThenCancel() throws IOException {
		// given
		when(files.exists(targetDir)).thenReturn(false);
		when(console.confirm(anyString(), anyString())).thenReturn(false);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.CANCEL));
		verify(console, times(1)).print("User cancel.");
		verify(files, times(0)).createDirectory(targetDir);
	}
}
