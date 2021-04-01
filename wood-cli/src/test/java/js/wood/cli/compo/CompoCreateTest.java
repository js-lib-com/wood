package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.cli.Console;
import js.wood.cli.ExitCode;
import js.wood.cli.FilesUtil;

@RunWith(MockitoJUnitRunner.class)
public class CompoCreateTest {
	@Mock
	private Console console;
	@Mock
	private FilesUtil files;
	@Mock
	private Path projectDir;
	@Mock
	private Path compoDir;

	private CompoCreate task;

	@Before
	public void beforeTest() {
		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve(anyString())).thenReturn(compoDir);

		task = new CompoCreate();
		task.setConsole(console);
		task.setFiles(files);
	}

	@Test
	public void Given_Then() throws Exception {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
	}
}
