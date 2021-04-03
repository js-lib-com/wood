package js.wood.cli.compo;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.DirectoryStream;
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
public class CompoRenameTest {
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
	private Path compoDirParent;
	@Mock
	private Path compoFile;
	@Mock
	private Path newCompoDir;
	@Mock
	private Path newCompoFile;

	@Mock
	private CompoName compoName;
	@Mock
	private TextReplace textReplace;

	private CompoRename task;

	@SuppressWarnings("unchecked")
	@Before
	public void beforeTest() throws IOException {
		when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));

		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve(anyString())).thenReturn(compoDir);
		when(compoDir.getParent()).thenReturn(compoDirParent);
		when(compoDirParent.resolve("captcha-ex")).thenReturn(newCompoDir);
		when(compoDir.resolve("captcha-ex.htm")).thenReturn(newCompoFile);

		when(files.exists(compoDir)).thenReturn(true);
		when(files.getFileName(compoDir)).thenReturn("captcha");
		when(files.getFileName(newCompoDir)).thenReturn("captcha-ex");
		when(files.listFiles(eq(compoDir), any(DirectoryStream.Filter.class))).thenReturn(Arrays.asList(compoFile));
		when(files.getExtension(compoFile)).thenReturn("htm");

		when(compoName.isValid()).thenReturn(true);
		when(compoName.path()).thenReturn("lib/captcha");

		task = new CompoRename();
		task.setCommandSpec(commandSpec);
		task.setConsole(console);
		task.setFiles(files);
		task.setName(compoName);
		task.setNewname("captcha-ex");
		task.setTextReplace(textReplace);
	}

	@Test
	public void GivenDefaultOptions_ThenRename() throws IOException {
		// given
		task.setVerbose(true);
		
		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(files, times(1)).move(compoFile, newCompoFile);
		verify(files, times(1)).move(compoDir, newCompoDir);
		verify(console, times(1)).print(anyString(), eq(compoFile), eq(newCompoFile));
		verify(textReplace, times(1)).replaceAll(eq(null), eq("lib/captcha"), eq("lib/captcha-ex"));
		verify(textReplace, times(1)).replaceAll(eq(null), eq("lib/captcha/captcha.js"), eq("lib/captcha-ex/captcha-ex.js"));
	}

	@Test
	public void GivenRootCompo_ThenRename() throws IOException {
		// given
		when(compoName.path()).thenReturn("captcha");
		
		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(files, times(1)).move(compoFile, newCompoFile);
		verify(files, times(1)).move(compoDir, newCompoDir);
		verify(console, times(0)).print(anyString(), eq(compoFile), eq(newCompoFile)); // no verbose
		verify(textReplace, times(1)).replaceAll(eq(null), eq("captcha"), eq("captcha-ex"));
		verify(textReplace, times(1)).replaceAll(eq(null), eq("captcha/captcha.js"), eq("captcha-ex/captcha-ex.js"));
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
	public void GivenNewCompoDirExists_ThenAbort() throws IOException {
		// given
		when(files.exists(newCompoDir)).thenReturn(true);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(files, times(0)).cleanDirectory(compoDir, false);
		verify(console, times(1)).print("Command abort.");
	}
}
