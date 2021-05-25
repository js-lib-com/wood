package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import javax.xml.xpath.XPathExpressionException;

import org.apache.http.HttpEntity;
import org.apache.http.StatusLine;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;
import org.xml.sax.SAXException;

import com.jslib.commons.cli.Config;
import com.jslib.commons.cli.Console;
import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.FilesUtil;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.util.Classes;

@RunWith(MockitoJUnitRunner.class)
public class CompoImportTest {
	@Mock
	private Config config;
	@Mock
	private Console console;
	@Mock
	private FilesUtil files;

	@Mock
	private Path repositoryDir;
	@Mock
	private Path repositoryCompoDir;
	@Mock
	private Path projectDir;
	@Mock
	private Path projectCompoDir;
	@Mock
	private CompoCoordinates compoCoordinates;
	@Mock
	private DocumentBuilder documentBuilder;

	@Mock
	private HttpClientBuilder httpClientBuilder;
	@Mock
	private CloseableHttpClient httpClient;
	@Mock
	private CloseableHttpResponse httpResponse;
	@Mock
	private HttpEntity httpEntity;
	@Mock
	private StatusLine statusLine;

	private CompoImport task;

	@Before
	public void beforeTest() throws IOException, SAXException {
		when(config.get("repository.dir")).thenReturn("/home/user/repository");
		when(config.get("repository.url")).thenReturn("http://server.com");

		when(compoCoordinates.getArtifactId()).thenReturn("dialog");
		when(compoCoordinates.toFile()).thenReturn("com/js-lib/web/dialog/1.0");
		when(compoCoordinates.toPath()).thenReturn("com/js-lib/web/dialog/1.0");

		when(files.getPath("/home/user/repository")).thenReturn(repositoryDir);
		when(repositoryDir.resolve("com/js-lib/web/dialog/1.0")).thenReturn(repositoryCompoDir);

		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve("res/compo/dialog")).thenReturn(projectCompoDir);
		when(projectCompoDir.resolve("dialog")).thenReturn(projectCompoDir);

		String indexPage = "" + //
				"<home>" + //
				"	<h1>Index Page</h1>" + //
				"	<a href='dialog.htm'>file</a>" + //
				"	<footer><a href='http://server.com/home.htm'>home.htm</a></footer>" + //
				"</home>";
		when(documentBuilder.loadHTML(new URL("http://server.com/com/js-lib/web/dialog/1.0/"))).thenReturn(parseHTML(indexPage));

		when(httpClientBuilder.build()).thenReturn(httpClient);
		when(httpClient.execute(any())).thenReturn(httpResponse);
		when(httpResponse.getStatusLine()).thenReturn(statusLine);
		when(statusLine.getStatusCode()).thenReturn(200);
		when(httpResponse.getEntity()).thenReturn(httpEntity);

		task = new CompoImport();
		task.setConfig(config);
		task.setConsole(console);
		task.setFiles(files);
		task.setCoordinates(compoCoordinates);
		task.setPath("res/compo/dialog");
		task.setDocumentBuilder(documentBuilder);
		task.setHttpClientBuilder(httpClientBuilder);
	}

	private static Document parseHTML(String document) throws SAXException {
		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		return builder.parseHTML(document);
	}

	@Test
	public void GivenDefaultOptionsAndUserConfirm_ThenCopyCompoFiles() throws Exception {
		// given
		when(console.confirm(anyString(), anyString())).thenReturn(true);
		task.setVerbose(true);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(files, times(1)).cleanDirectory(projectCompoDir, true);
		verify(files, times(1)).copyFiles(repositoryCompoDir, projectCompoDir, true);
	}

	@Test
	public void GivenProjectCompoDirExist_ThenDoNotCreateIt() throws IOException, XPathExpressionException, SAXException {
		// given
		when(files.exists(projectCompoDir)).thenReturn(true);

		// when
		task.exec();

		// then
		verify(files, times(0)).createDirectory(projectCompoDir);
	}

	@Test
	public void GivenReloadOption_ThenDownloadComponent() throws Exception {
		// given
		task.setReload(true);

		// when
		task.exec();

		// then
		verify(files, times(1)).cleanDirectory(repositoryCompoDir, false);
		verify(documentBuilder, times(1)).loadHTML(new URL("http://server.com/com/js-lib/web/dialog/1.0/"));
	}

	@Test
	public void GivenReloadOptionAndRepositoryCompoDirExist_ThenDoNotCreateDirectory() throws Exception {
		// given
		task.setReload(true);

		// when
		task.exec();

		// then
		verify(files, times(0)).createDirectory(repositoryCompoDir);
	}

	@Test
	public void GivenNotReloadAndRepositoryCompoNotExist_ThenCreateDirectoryAndDownload() throws IOException, XPathExpressionException, SAXException {
		// given
		when(files.exists(repositoryCompoDir)).thenReturn(false);
		task.setReload(false);

		// when
		task.exec();

		// then
		verify(files, times(1)).createDirectories(repositoryCompoDir);
		verify(documentBuilder, times(1)).loadHTML(new URL("http://server.com/com/js-lib/web/dialog/1.0/"));
	}

	@Test
	public void GivenNotReloadAndRepositoryCompoExist_ThenDoNotDownload() throws IOException, XPathExpressionException, SAXException {
		// given
		when(files.exists(repositoryCompoDir)).thenReturn(true);
		task.setReload(false);

		// when
		task.exec();

		// then
		verify(documentBuilder, times(0)).loadHTML(new URL("http://server.com/com/js-lib/web/dialog/1.0/"));
	}

	@Test
	public void GivenUserNotConfirm_ThenCancel() throws Exception {
		// given
		when(console.confirm(anyString(), anyString())).thenReturn(false);

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.CANCEL));
		verify(console, times(1)).print("User cancel.");
		verify(files, times(0)).cleanDirectory(projectCompoDir, false);
		verify(files, times(0)).copyFiles(repositoryCompoDir, projectCompoDir, false);
	}
}
