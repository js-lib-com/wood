package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.awt.Desktop;
import java.io.IOException;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import javax.xml.xpath.XPathExpressionException;

import org.junit.Before;
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

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
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
	private Path deployDir;
	@Mock
	private Path webxmlFile;

	@Mock
	private CompoName compoName;
	@Mock
	private Desktop desktop;

	private CompoPreview task;

	@Before
	public void beforeTest() throws IOException {
		when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));
		when(config.get("runtime.home")).thenReturn("runtimes");
		when(config.getex("runtime.port", int.class)).thenReturn(8080);
		when(config.getex("runtime.name", "test")).thenReturn("test");
		when(config.getex("runtime.context", "test")).thenReturn("app");

		when(files.getProjectDir()).thenReturn(projectDir);
		when(files.getFileName(projectDir)).thenReturn("test");
		when(projectDir.toAbsolutePath()).thenReturn(projectDir);
		when(projectDir.toString()).thenReturn("project/dir");
		when(projectDir.resolve(anyString())).thenReturn(compoDir);
		when(files.exists(compoDir)).thenReturn(true);

		when(files.createDirectories("runtimes", "test", "webapps", "app-preview")).thenReturn(deployDir);
		when(deployDir.resolve("WEB-INF/web.xml")).thenReturn(webxmlFile);
		when(files.exists(webxmlFile)).thenReturn(true);

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

	@Test
	public void GivenMissingWebXml_ThenCreateItAndAbort() throws IOException, URISyntaxException, SAXException, XPathExpressionException {
		// given
		when(config.get("project.display", "test")).thenReturn("display");
		when(config.get("project.description", "test")).thenReturn("description");
		when(config.get("build.dir")).thenReturn("build");

		StringWriter webxmlWriter = new StringWriter();
		when(files.exists(webxmlFile)).thenReturn(false);
		when(files.getWriter(webxmlFile)).thenReturn(webxmlWriter);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));

		DocumentBuilder docBuilder = Classes.loadService(DocumentBuilder.class);
		Document doc = docBuilder.parseXML(webxmlWriter.toString());
		assertThat(doc.getByTag("display-name").getText(), equalTo("display"));
		assertThat(doc.getByTag("description").getText(), equalTo("description"));

		Element projectDirElement = doc.getByXPath("//param-name[text()='PROJECT_DIR']");
		assertThat(projectDirElement, notNullValue());
		assertThat(projectDirElement.getNextSibling().getText(), equalTo("project/dir"));

		Element excludeDirsElement = doc.getByXPath("//param-name[text()='EXCLUDE_DIRS']");
		assertThat(excludeDirsElement, notNullValue());
		assertThat(excludeDirsElement.getNextSibling().getText(), equalTo("build"));
	}
}
