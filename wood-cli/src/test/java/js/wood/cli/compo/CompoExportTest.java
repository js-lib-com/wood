package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.io.StringReader;
import java.nio.file.Path;
import java.util.Arrays;

import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpDelete;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import js.wood.cli.Config;
import js.wood.cli.Console;
import js.wood.cli.ExitCode;
import js.wood.cli.FilesUtil;
import picocli.CommandLine;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;

@RunWith(MockitoJUnitRunner.class)
public class CompoExportTest {
	@Mock
	private HttpClientBuilder httpClientBuilder;
	@Mock
	private CloseableHttpClient httpClient;
	@Mock
	private CloseableHttpResponse httpResponse;
	@Mock
	private StatusLine statusLine;

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
	private Path descriptorFile;
	@Mock
	private CompoName compoName;

	private CompoExport task;

	@Before
	public void beforeTest() throws IOException {
		when(httpClientBuilder.build()).thenReturn(httpClient);
		when(httpClient.execute(any())).thenReturn(httpResponse);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(200);

		when(commandSpec.commandLine()).thenReturn(mock(CommandLine.class));
		when(config.get("repository.url")).thenReturn("http://server.com/");
		when(compoName.isValid()).thenReturn(true);
		when(compoName.path()).thenReturn("res/compo/dialog");

		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve("res/compo/dialog")).thenReturn(compoDir);
		when(compoDir.resolve("dialog.xml")).thenReturn(descriptorFile);

		when(files.getFileName(compoDir)).thenReturn("dialog");
		when(files.exists(compoDir)).thenReturn(true);
		when(files.exists(descriptorFile)).thenReturn(true);

		String descriptorDoc = "" + //
				"<compo>" + //
				"	<groupId>com.js-lib.web</groupId>" + //
				"	<artifactId>dialog</artifactId>" + //
				"	<version>1.0</version>" + //
				"</compo>";
		when(files.getReader(descriptorFile)).thenReturn(new StringReader(descriptorDoc));

		task = new CompoExport();
		task.setConfig(config);
		task.setConsole(console);
		task.setFiles(files);
		task.setCommandSpec(commandSpec);
		task.setName(compoName);
		task.setVerbose(true);
		task.setHttpClientBuilder(httpClientBuilder);
	}

	@Test
	public void GivenDefaultOptions_ThenUploadComponentFiles() throws Exception {
		// given
		Path compoFile = compoDir.resolve("dialog.htm");
		when(files.listFiles(compoDir)).thenReturn(Arrays.asList(compoFile));

		String htm = "<html><head></head><body></body></html>";
		when(files.getInputStream(compoFile)).thenReturn(new ByteArrayInputStream(htm.getBytes()));

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(httpClient, times(1)).execute(any(HttpDelete.class));
		verify(httpClient, times(1)).execute(any(HttpPost.class));
	}

	@Test
	public void GivenMissingCompoDir_ThenAbort() throws IOException, SAXException {
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
	public void GivenMissingCompoDescriptor_ThenAbort() throws IOException, SAXException {
		// given
		when(files.exists(descriptorFile)).thenReturn(false);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(files, times(0)).cleanDirectory(compoDir, false);
		verify(console, times(1)).print("Command abort.");
	}

	@Test
	public void GivenMissingCompoCoordinates_ThenAbort() throws IOException, SAXException {
		// given
		when(files.getReader(descriptorFile)).thenReturn(new StringReader("<compo></compo>"));

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.ABORT));
		verify(files, times(0)).cleanDirectory(compoDir, false);
		verify(console, times(1)).print("Command abort.");
	}

	@Test(expected = ParameterException.class)
	public void GivenInvalidCompoName_ThenParameterException() throws IOException, SAXException {
		// given
		when(compoName.isValid()).thenReturn(false);

		// when
		task.exec();

		// then
	}

	@Test(expected = IOException.class)
	public void GivenCleanupRepositoryComponentFail_ThenIOException() throws Exception {
		// given
		when(statusLine.getStatusCode()).thenReturn(500);

		// when
		task.exec();

		// then
	}

	@Test(expected = IOException.class)
	public void GivenUploadComponentFileFail_ThenIOException() throws Exception {
		// given
		task.setVerbose(false);
		Path compoFile = compoDir.resolve("dialog.htm");
		when(files.listFiles(compoDir)).thenReturn(Arrays.asList(compoFile));

		String htm = "<html><head></head><body></body></html>";
		when(files.getInputStream(compoFile)).thenReturn(new ByteArrayInputStream(htm.getBytes()));

		when(statusLine.getStatusCode()).thenReturn(200).thenReturn(500);

		// when
		task.exec();

		// then
	}
}
