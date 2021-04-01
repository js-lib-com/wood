package js.wood.cli.project;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.FileSystem;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Collections;
import java.util.Iterator;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.Builder;
import js.wood.BuilderConfig;
import js.wood.cli.Config;
import js.wood.cli.Console;

@RunWith(MockitoJUnitRunner.class)
public class ProjectBuildTest {
	@Mock
	private FileSystemProvider provider;
	@Mock
	private FileSystem fileSystem;
	@Mock
	private BasicFileAttributes attributes;
	@Mock
	private Console console;
	@Mock
	private Config config;

	@Mock
	private BuilderConfig builderConfig;
	@Mock
	private Builder builder;

	@Mock
	private Path workingDir;
	@Mock
	private Path buildDir;
	@Mock
	private Path deployDir;

	private ProjectBuild task;

	@Before
	public void beforeTest() throws IOException {
		when(fileSystem.provider()).thenReturn(provider);
		when(fileSystem.getPath("")).thenReturn(workingDir);
		when(workingDir.getFileName()).thenReturn(mock(Path.class));
		when(workingDir.toAbsolutePath()).thenReturn(workingDir);
		when(workingDir.resolve("build")).thenReturn(buildDir);

		when(builderConfig.createBuilder()).thenReturn(builder);

		when(config.get(eq("runtime.name"), anyString())).thenReturn("test");
		when(config.get(eq("runtime.home"))).thenReturn("runtimes");
		when(fileSystem.getPath("runtimes", "test", "webapps", null)).thenReturn(deployDir);

		when(attributes.isDirectory()).thenReturn(true);
		when(provider.readAttributes(any(Path.class), eq(BasicFileAttributes.class), eq(LinkOption.NOFOLLOW_LINKS))).thenReturn(attributes);
		when(provider.newDirectoryStream(eq(buildDir), any())).thenReturn(new DirectoryStream<Path>() {
			@Override
			public void close() throws IOException {
			}

			@Override
			public Iterator<Path> iterator() {
				return Collections.emptyIterator();
			}
		});

		when(buildDir.getFileSystem()).thenReturn(fileSystem);
		when(deployDir.getFileSystem()).thenReturn(fileSystem);

		task = new ProjectBuild();
		task.setFileSystem(fileSystem);
		task.setConsole(console);
		task.setConfig(config);
		task.setBuilderConfig(builderConfig);
		task.setTargetDir("build");
	}

	@Test
	public void exec_GivenDefaultOptions_ThenBuildAndDeploy() throws IOException {
		// given

		// when
		task.exec();

		// then
		verify(builderConfig, times(1)).setProjectDir(any());
		verify(builderConfig, times(1)).setBuildDir(any());
		verify(builderConfig, times(1)).setBuildNumber(0);
		verify(builder, times(1)).build();
		
		verify(provider, times(0)).delete(any());
		verify(provider, times(1)).createDirectory(eq(deployDir), any());
	}

	@Test
	public void exec_GivenCleanOptionSet_ThenBuildClean() throws IOException {
		// given
		task.setClean(true);

		// when
		task.exec();

		// then
		verify(provider, times(1)).delete(eq(buildDir));
	}

	@Test
	public void exec_GivenBuildNumberOptionSet_ThenSetBuilderConfig() throws IOException {
		// given
		task.setBuildNumber(1964);

		// when
		task.exec();

		// then
		verify(builderConfig, times(1)).setBuildNumber(1964);
	}
}
