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
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import com.jslib.commons.cli.Config;
import com.jslib.commons.cli.Console;
import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.FilesUtil;
import com.jslib.commons.cli.TemplateProcessor;

import js.lang.BugError;

@RunWith(MockitoJUnitRunner.class)
public class CompoCreateTest {
	@Mock
	private Console console;
	@Mock
	private Config config;
	@Mock
	private FilesUtil files;
	@Mock
	private TemplateProcessor templateProcessor;

	@Mock
	private Path projectDir;
	@Mock
	private Path projectDescriptorFile;
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
	public void beforeTest() throws IOException {
		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve((String) null)).thenReturn(compoDir);

		when(projectDir.resolve("project.xml")).thenReturn(projectDescriptorFile);
		String projectDescriptorDoc = "<project></project>";
		when(files.getReader(projectDescriptorFile)).thenReturn(new StringReader(projectDescriptorDoc));
		
		when(compoTemplate.path()).thenReturn("template/page");
		when(projectDir.resolve("template/page")).thenReturn(compoTemplateDir);

		when(config.getex("project.package")).thenReturn("app");
		
		task = new CompoCreate();
		task.setConsole(console);
		task.setConfig(config);
		task.setFiles(files);
		task.setTemplateProcessor(templateProcessor);
	}

	@Test
	public void GivenDefaultOptions_ThenJustCreateCompoDir() throws IOException, XPathExpressionException, SAXException {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(files, times(1)).createDirectory(compoDir);
		verify(templateProcessor, times(0)).exec(eq("compo"), eq(null), any());
	}

	@Test
	public void GivenCompoDirExists_ThenAbort() throws IOException, XPathExpressionException, SAXException {
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
	public void GivenCompoDirCreationFail_ThenIOException() throws IOException, XPathExpressionException, SAXException {
		// given
		doThrow(IOException.class).when(files).createDirectory(compoDir);

		// when
		task.exec();

		// then
	}

	@Test
	public void GivenTemplateOption_ThenCreateCompo() throws IOException, XPathExpressionException, SAXException {
		// given
		when(files.getFileName(compoDir)).thenReturn("about");
		when(files.exists(compoTemplateDir)).thenReturn(true);
		when(files.getFileByExtension(compoTemplateDir, ".htm")).thenReturn(templateLayoutFile);

		String templateLayoutDoc = "<body xmlns:w='js-lib.com/wood'><section w:editable='section'></section></body>";
		when(files.getReader(templateLayoutFile)).thenReturn(new StringReader(templateLayoutDoc)).thenReturn(new StringReader(templateLayoutDoc));

		task.setCompoTemplate(compoTemplate);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(templateProcessor, times(1)).setTargetDir(null);
		verify(templateProcessor, times(1)).setVerbose(false);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> variablesArgument = ArgumentCaptor.forClass(Map.class);
		verify(templateProcessor, times(1)).exec(eq("compo"), eq("page"), variablesArgument.capture());

		Map<String, String> variables = variablesArgument.getValue();
		assertThat(variables.get("page"), equalTo("about"));
		assertThat(variables.get("tag"), equalTo("section"));
		assertThat(variables.get("class"), equalTo("about"));
		assertThat(variables.get("templateAttr"), equalTo("w:template"));
		assertThat(variables.get("templatePath"), equalTo("template/page"));
		assertThat(variables.get("templateParams"), equalTo("w:param=\"\""));
		assertThat(variables.get("editable"), equalTo("section"));
		assertThat(variables.get("xmlns"), equalTo("xmlns:w=\"js-lib.com/wood\""));
		assertThat(variables.get("root"), equalTo("component"));
		assertThat(variables.get("groupId"), equalTo("com.js-lib"));
		assertThat(variables.get("artifactId"), equalTo("page"));
		assertThat(variables.get("version"), equalTo("1.0"));
		assertThat(variables.get("title"), equalTo("About Component"));
		assertThat(variables.get("description"), equalTo("About component description."));
	}

	@Test
	public void GivenTemplateOptionAndDataAttrNaming_ThenCreateCompo() throws IOException, XPathExpressionException, SAXException {
		// given
		when(files.getFileName(compoDir)).thenReturn("about");
		when(files.exists(compoTemplateDir)).thenReturn(true);
		when(files.getFileByExtension(compoTemplateDir, ".htm")).thenReturn(templateLayoutFile);

		String projectDescriptorDoc = "<project><naming>DATA_ATTR</naming></project>";
		when(files.getReader(projectDescriptorFile)).thenReturn(new StringReader(projectDescriptorDoc));

		String templateLayoutDoc = "<body><section data-editable='section'></section></body>";
		when(files.getReader(templateLayoutFile)).thenReturn(new StringReader(templateLayoutDoc)).thenReturn(new StringReader(templateLayoutDoc));

		task.setCompoTemplate(compoTemplate);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(templateProcessor, times(1)).setTargetDir(null);
		verify(templateProcessor, times(1)).setVerbose(false);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<Map<String, String>> variablesArgument = ArgumentCaptor.forClass(Map.class);
		verify(templateProcessor, times(1)).exec(eq("compo"), eq("page"), variablesArgument.capture());

		Map<String, String> variables = variablesArgument.getValue();
		assertThat(variables.get("page"), equalTo("about"));
		assertThat(variables.get("tag"), equalTo("section"));
		assertThat(variables.get("class"), equalTo("about"));
		assertThat(variables.get("templateAttr"), equalTo("data-template"));
		assertThat(variables.get("templatePath"), equalTo("template/page"));
		assertThat(variables.get("templateParams"), equalTo("data-param=\"\""));
		assertThat(variables.get("editable"), equalTo("section"));
		assertThat(variables.get("xmlns"), equalTo(""));
		assertThat(variables.get("root"), equalTo("component"));
		assertThat(variables.get("groupId"), equalTo("com.js-lib"));
		assertThat(variables.get("artifactId"), equalTo("page"));
		assertThat(variables.get("version"), equalTo("1.0"));
		assertThat(variables.get("title"), equalTo("About Component"));
		assertThat(variables.get("description"), equalTo("About component description."));
	}

	@Test
	public void GivenTemplateOptionAndLayoutParameters_ThenConsoleInput() throws IOException, XPathExpressionException, SAXException {
		// given
		when(files.exists(compoTemplateDir)).thenReturn(true);
		when(files.getFileByExtension(compoTemplateDir, ".htm")).thenReturn(templateLayoutFile);

		String layoutDocument = "<body xmlns:w='js-lib.com/wood'><section w:editable='section'><h1>@param/title</h1></section></body>";
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
	public void GivenTemplateOptionAndMissingTemplateDir_ThenAbort() throws IOException, XPathExpressionException, SAXException {
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
	public void GivenTemplateOptionAndMissingTemplateLayout_ThenAbort() throws IOException, XPathExpressionException, SAXException {
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

	@Ignore
	@Test(expected = BugError.class)
	public void GivenTemplateOptionAndMissingEditable_ThenBugError() throws IOException, XPathExpressionException, SAXException {
		// given
		when(files.exists(compoTemplateDir)).thenReturn(true);
		when(files.getFileByExtension(compoTemplateDir, ".htm")).thenReturn(templateLayoutFile);

		// w:editable is misspelled; it has 's' at the end
		String layoutDocument = "<body xmlns:w='com.js-lib/wood'><section w:editables='section'></section></body>";
		when(files.getReader(templateLayoutFile)).thenReturn(new StringReader(layoutDocument)).thenReturn(new StringReader(layoutDocument));

		task.setCompoTemplate(compoTemplate);

		// when
		task.exec();

		// then
	}
}
