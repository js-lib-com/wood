package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.lang.BugError;
import js.wood.cli.Console;
import js.wood.cli.ExitCode;
import js.wood.cli.FilesUtil;
import js.wood.cli.TemplateProcessor;
import js.wood.cli.TemplateType;

@RunWith(MockitoJUnitRunner.class)
public class CompoCreateTest {
	@Mock
	private Console console;
	@Mock
	private FilesUtil files;
	@Mock
	private TemplateProcessor templateProcessor;

	@Mock
	private Path projectDir;
	@Mock
	private Path compoDir;
	@Mock
	private CompoName compoTemplate;
	@Mock
	private Path compoTemplateDir;
	@Mock
	private Path templateLayoutFile;

	private CompoCreate task;

	@Before
	public void beforeTest() {
		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve((String) null)).thenReturn(compoDir);

		when(compoTemplate.path()).thenReturn("template/page");
		when(projectDir.resolve("template/page")).thenReturn(compoTemplateDir);

		task = new CompoCreate();
		task.setConsole(console);
		task.setFiles(files);
		task.setTemplateProcessor(templateProcessor);
	}

	@Test
	public void GivenDefaultOptions_ThenJustCreateCompoDir() throws IOException {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(files, times(1)).createDirectory(compoDir);
		verify(templateProcessor, times(0)).exec(eq(TemplateType.compo), eq(null), any());
	}

	@Test
	public void GivenCompoDirExists_ThenAbort() throws IOException {
		// given
		when(files.exists(compoDir)).thenReturn(true);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(console, times(1)).print(anyString(), eq(compoDir));
		verify(console, times(1)).print(anyString());
	}

	@Test(expected = IOException.class)
	public void GivenCompoDirCreationFail_ThenIOException() throws IOException {
		// given
		doThrow(IOException.class).when(files).createDirectory(compoDir);

		// when
		task.exec();

		// then
	}

	@Test
	public void GivenTemplateOption_ThenCreateCompo() throws IOException {
		// given
		when(files.exists(compoTemplateDir)).thenReturn(true);
		when(files.getFileByExtension(compoTemplateDir, ".htm")).thenReturn(templateLayoutFile);

		String layoutDocument = "<body><section w:editable='section'></section></body>";
		when(files.getReader(templateLayoutFile)).thenReturn(new StringReader(layoutDocument)).thenReturn(new StringReader(layoutDocument));

		task.setCompoTemplate(compoTemplate);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(templateProcessor, times(1)).setTargetDir(null);
		verify(templateProcessor, times(1)).setVerbose(false);
		verify(templateProcessor, times(1)).exec(eq(TemplateType.compo), eq("page"), any());
	}

	@Test
	public void GivenTemplateOptionAndLayoutParameters_ThenConsoleInput() throws IOException {
		// given
		when(files.exists(compoTemplateDir)).thenReturn(true);
		when(files.getFileByExtension(compoTemplateDir, ".htm")).thenReturn(templateLayoutFile);

		String layoutDocument = "<body><section w:editable='section'><h1>@param/title</h1></section></body>";
		when(files.getReader(templateLayoutFile)).thenReturn(new StringReader(layoutDocument)).thenReturn(new StringReader(layoutDocument));

		task.setCompoTemplate(compoTemplate);

		// when
		task.exec();

		// then
		verify(console, times(2)).crlf();
		verify(console, times(1)).print("Template parameters:");
		verify(console, times(1)).input("template title", "title");
	}

	@Test
	public void GivenTemplateOptionAndMissingTemplateDir_ThenAbort() throws IOException {
		// given
		when(files.exists(compoTemplateDir)).thenReturn(false);
		task.setCompoTemplate(compoTemplate);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(console, times(1)).print(anyString(), eq(compoTemplateDir));
		verify(console, times(1)).print(anyString());
	}

	@Test
	public void GivenTemplateOptionAndMissingTemplateLayout_ThenAbort() throws IOException {
		// given
		when(files.exists(compoTemplateDir)).thenReturn(true);
		when(files.getFileByExtension(compoTemplateDir, ".htm")).thenReturn(null);
		task.setCompoTemplate(compoTemplate);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(console, times(1)).print(anyString(), eq(compoTemplateDir));
		verify(console, times(1)).print(anyString());
	}

	@Test(expected = BugError.class)
	public void GivenTemplateOptionAndMissingEditable_ThenBugError() throws IOException {
		// given
		when(files.exists(compoTemplateDir)).thenReturn(true);
		when(files.getFileByExtension(compoTemplateDir, ".htm")).thenReturn(templateLayoutFile);

		// w:editable is misspelled; it has 's' at the end
		String layoutDocument = "<body><section w:editables='section'></section></body>";
		when(files.getReader(templateLayoutFile)).thenReturn(new StringReader(layoutDocument)).thenReturn(new StringReader(layoutDocument));

		task.setCompoTemplate(compoTemplate);

		// when
		task.exec();

		// then
	}
}
