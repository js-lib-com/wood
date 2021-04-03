package js.wood.cli.project;

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

import js.wood.Builder;
import js.wood.BuilderConfig;
import js.wood.cli.Config;
import js.wood.cli.Console;
import js.wood.cli.FilesUtil;

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

	private ProjectBuild task;

	@Before
	public void beforeTest() throws IOException {
		when(projectDir.resolve("build")).thenReturn(buildDir);
		when(builderConfig.createBuilder()).thenReturn(builder);

		when(config.get(eq("runtime.home"))).thenReturn("runtimes");
		when(config.get(eq("runtime.name"), anyString())).thenReturn("test");
		when(config.get(eq("runtime.context"), anyString())).thenReturn("context");

		when(files.getProjectDir()).thenReturn(projectDir);
		when(files.getFileName(projectDir)).thenReturn("test");
		when(files.createDirectories("runtimes", "test", "webapps", "context")).thenReturn(deployDir);

		task = new ProjectBuild();
		task.setConsole(console);
		task.setConfig(config);
		task.setFiles(files);
		task.setBuilderConfig(builderConfig);
		task.setTargetDir("build");
	}

	@Test
	public void GivenDefaultOptions_ThenBuildAndDeploy() throws IOException {
		// given

		// when
		task.exec();

		// then
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
		
		// when
		task.exec();
		
		// then
		verify(files, times(1)).createDirectories("runtimes", "kids-cademy", "webapps", "context");
	}
}
