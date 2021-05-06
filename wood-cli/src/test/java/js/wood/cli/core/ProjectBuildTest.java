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

import js.wood.Builder;
import js.wood.BuilderConfig;

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

		when(config.get(eq("runtime.home"))).thenReturn("runtimes");
		when(config.get(eq("runtime.name"), anyString())).thenReturn("test");
		when(config.get(eq("runtime.context"), anyString())).thenReturn("context");

		when(files.getFileName(projectDir)).thenReturn("test");
		when(files.exists(buildDir)).thenReturn(true);
		when(files.createDirectories("runtimes", "test", "webapps", "context")).thenReturn(deployDir);

		task = new ProjectBuild();
		task.setConsole(console);
		task.setConfig(config);
		task.setFiles(files);
		task.setBuilderConfig(builderConfig);
		task.setTarget("build");
	}

	@Test
	public void GivenDefaultOptions_ThenBuildAndDeploy() throws IOException {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		
		verify(builderConfig, times(1)).setProjectDir(any());
		verify(builderConfig, times(1)).setBuildDir(any());
		verify(builderConfig, times(1)).setBuildNumber(0);
		verify(builder, times(1)).build();

		verify(files, times(0)).cleanDirectory(any(), eq(false));
		verify(files, times(1)).createDirectories("runtimes", "test", "webapps", "context");
		verify(files, times(1)).copyFiles(buildDir, deployDir, false);
	}

	@Test
	public void GivenCleanOptionSet_ThenBuildClean() throws IOException {
		// given
		task.setClean(true);

		// when
		task.exec();

		// then
		verify(files, times(1)).cleanDirectory(any(), eq(false));
	}

	@Test
	public void GivenBuildNumberOptionSet_ThenSetBuilderConfig() throws IOException {
		// given
		task.setBuildNumber(1964);

		// when
		task.exec();

		// then
		verify(builderConfig, times(1)).setBuildNumber(1964);
	}
	
	@Test
	public void GivenRuntimeOptionSet_ThenSetDeployDirPath() throws IOException {
		// given
		when(config.get("runtime.name", "kids-cademy")).thenReturn("kids-cademy");
		task.setRuntime("kids-cademy");
		when(files.createDirectories("runtimes", "kids-cademy", "webapps", "context")).thenReturn(deployDir);
		
		// when
		task.exec();
		
		// then
		verify(files, times(1)).createDirectories("runtimes", "kids-cademy", "webapps", "context");
	}
	
	@Test
	public void GivenMissingBuildDir_ThenAbort() throws IOException {
		// given
		when(files.exists(buildDir)).thenReturn(false);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
	}
}
