package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.cli.Config;
import js.wood.cli.Console;
import js.wood.cli.ExitCode;
import js.wood.cli.FilesUtil;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@RunWith(MockitoJUnitRunner.class)
public class CompoPreviewTest {
	@Mock
	private CommandSpec commandSpec;
	@Mock
	private Config config;

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
	@Mock
	private Desktop desktop;

	private CompoPreview task;

	@Before
	public void beforeTest() throws IOException {
		when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));
		when(config.get("runtime.port", int.class)).thenReturn(8080);
		when(config.get("runtime.context", (String) null)).thenReturn("app");

		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve(anyString())).thenReturn(compoDir);
		when(files.exists(compoDir)).thenReturn(true);

		when(compoName.isValid()).thenReturn(true);
		when(compoName.path()).thenReturn("res/page/about");

		task = new CompoPreview();
		task.setCommandSpec(commandSpec);
		task.setConfig(config);
		task.setConsole(console);
		task.setFiles(files);
		task.setName(compoName);
		task.setDesktop(desktop);
	}

	@Test
	public void GivenDefaultOptions_ThenOpenPreview() throws IOException, URISyntaxException {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));

		ArgumentCaptor<URI> uriArgument = ArgumentCaptor.forClass(URI.class);
		verify(desktop, times(1)).browse(uriArgument.capture());
		assertThat(uriArgument.getValue().toString(), containsString("app-preview"));
	}

	@Test(expected = ParameterException.class)
	public void GivenInvalidCompoName_ThenParameterException() throws IOException, URISyntaxException {
		// given
		when(compoName.isValid()).thenReturn(false);

		// when
		task.exec();

		// then
	}

	@Test
	public void GivenMissingCompoDir_ThenAbort() throws IOException, URISyntaxException {
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
