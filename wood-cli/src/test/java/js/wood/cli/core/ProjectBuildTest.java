package js.wood.cli.core;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
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

import com.jslib.commons.cli.Config;
import com.jslib.commons.cli.Console;
import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.FilesUtil;

import js.wood.build.Builder;
import js.wood.build.BuilderConfig;

@RunWith(MockitoJUnitRunner.class)
public class ProjectBuildTest {
	@Mock
	private Console console;
	@Mock
	private Config config;
	@Mock
	private FilesUtil files;

	@Mock
	private BuilderConfig builderConfig;
	@Mock
	private Builder builder;

	@Mock
	private Path projectDir;
	@Mock
	private Path buildDir;
	@Mock
	private Path deployDir;
	@Mock
	private Path webxmlFile;

	private ProjectBuild task;

	@Before
	public void beforeTest() throws IOException {
		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve("build")).thenReturn(buildDir);
		when(builderConfig.createBuilder()).thenReturn(builder);

		when(config.get("build.dir")).thenReturn("build");
		when(config.getex("runtime.home")).thenReturn("runtimes");
		when(config.getex(eq("runtime.name"), anyString())).thenReturn("test");
		when(config.getex(eq("runtime.context"), anyString())).thenReturn("context");

		when(files.getFileName(projectDir)).thenReturn("test");
		when(files.exists(buildDir)).thenReturn(true);
		when(files.createDirectories("runtimes", "test", "webapps", "context")).thenReturn(deployDir);

		task = new ProjectBuild();
		task.setConsole(console);
		task.setConfig(config);
		task.setFiles(files);
		task.setBuilderConfig(builderConfig);
	}

	@Test
	public void GivenDefaultOptions_ThenBuildAndDeploy() throws Exception {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));

		verify(builderConfig, times(1)).setProjectDir(any());
		verify(builderConfig, times(1)).setBuildNumber(0);
		verify(builder, times(1)).build();

		verify(files, times(0)).cleanDirectory(any(), eq(false));
		verify(files, times(1)).createDirectories("runtimes", "test", "webapps", "context");
		verify(files, times(1)).copyFiles(buildDir, deployDir, false);
	}

	@Test
	public void GivenCleanOptionSet_ThenBuildClean() throws Exception {
		// given
		task.setClean(true);

		// when
		task.exec();

		// then
		verify(files, times(1)).cleanDirectory(any(), eq(false));
	}

	@Test
	public void GivenBuildNumberOptionSet_ThenSetBuilderConfig() throws Exception {
		// given
		task.setBuildNumber(1964);

		// when
		task.exec();

		// then
		verify(builderConfig, times(1)).setBuildNumber(1964);
	}

	@Test
	public void GivenMissingBuildDir_ThenAbort() throws Exception {
		// given
		when(files.exists(buildDir)).thenReturn(false);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
	}
}
