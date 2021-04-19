package js.wood.cli.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
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
import js.wood.cli.core.ProjectClean;

@RunWith(MockitoJUnitRunner.class)
public class ProjectCleanTest {
	@Mock
	private Console console;
	@Mock
	private FilesUtil files;
	@Mock
	private Path projectDir;
	@Mock
	private Path buildDir;

	private ProjectClean task;

	@Before
	public void beforeTest() {
		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve((String) null)).thenReturn(buildDir);

		task = new ProjectClean();
		task.setConsole(console);
		task.setFiles(files);
	}

	@Test
	public void GivenDefaultOptions_ThenCleanBuildDirectory() throws IOException {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		verify(files, times(1)).cleanDirectory(eq(buildDir), anyBoolean());
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(console, times(1)).print(anyString(), eq(projectDir));
	}

	@Test
	public void GivenTargetOption_ThenSetBuildDir() throws IOException {
		// given
		task.setTarget("build/site");

		// when
		task.exec();

		// then
		verify(projectDir, times(1)).resolve("build/site");
	}
}
