package js.wood.cli.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.cli.Config;
import js.wood.cli.Console;
import js.wood.cli.ExitCode;
import js.wood.cli.FilesUtil;
import js.wood.cli.TemplateProcessor;
import js.wood.cli.TemplateType;

@RunWith(MockitoJUnitRunner.class)
public class ProjectCreateTest {
	@Mock
	private Config config;
	@Mock
	private Console console;
	@Mock
	private FilesUtil files;
	@Mock
	private TemplateProcessor template;
	@Mock
	private Path workingDir;
	@Mock
	private Path projectDir;

	private ProjectCreate task;

	@Before
	public void beforeTest() throws IOException {
		when(files.getWorkingDir()).thenReturn(workingDir);
		when(workingDir.resolve("test")).thenReturn(projectDir);
		when(projectDir.toFile()).thenReturn(mock(File.class));

		task = new ProjectCreate();
		task.setConfig(config);
		task.setConsole(console);
		task.setFiles(files);
		task.setTemplate(template);
		task.setName("test");
	}

	@Test
	public void GivenDefaultOptions_ThenExecProjectTemplate() throws IOException {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(files, times(1)).createDirectory(projectDir);
		verify(template, times(1)).setTargetDir(any(File.class));
		verify(template, times(1)).setVerbose(false);
		verify(template, times(1)).exec(eq(TemplateType.project), eq(null), any());
	}

	@Test
	public void GivenDefaultOptions_ThenConsoleInput() throws IOException {
		// given

		// when
		task.exec();

		// then
		verify(config, times(1)).get("user.name");
		verify(console, times(1)).input("developer name");
		verify(console, times(1)).input("site title");
		verify(console, times(1)).input("project short description");
		verify(console, times(1)).input("list of comma separated locale");
	}

	@Test
	public void GivenProjectDirExists_ThenAbort() throws IOException {
		// given
		when(files.exists(projectDir)).thenReturn(true);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(console, times(1)).print(anyString(), eq(projectDir));
		verify(console, times(1)).print(anyString());
	}

	@Test(expected = IOException.class)
	public void GivenProjectDirCreationFail_ThenIOException() throws IOException {
		// given
		doThrow(IOException.class).when(files).createDirectory(projectDir);

		// when
		task.exec();

		// then
	}
}
