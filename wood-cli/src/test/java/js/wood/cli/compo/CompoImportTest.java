package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
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
import com.jslib.commons.cli.HttpRequest;

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
	private HttpRequest httpRequest;

	@Mock
	private Path repositoryDir;
	@Mock
	private Path repositoryCompoDir;
	@Mock
	private Path projectDir;
	@Mock
	private Path projectCompoDir;
	@Mock
	private Path descriptorFile;
	@Mock
	private Path scriptFile;

	private CompoImport task;

	@Before
	public void beforeTest() throws IOException, SAXException, XPathExpressionException, URISyntaxException {
		when(config.getex("repository.dir")).thenReturn("/home/user/repository");
		when(config.getex("repository.url")).thenReturn("http://server.com");

		when(console.input("local path")).thenReturn("res/compo/dialog");
		when(console.confirm(anyString(), anyString())).thenReturn(true);

		String descriptor = "" + //
				"<compo>" + //
				"</compo>";
		when(files.getReader(descriptorFile)).thenReturn(new StringReader(descriptor)).thenReturn(new StringReader(descriptor));
		when(files.getWriter(descriptorFile)).thenReturn(new StringWriter());

		when(files.getPath("/home/user/repository")).thenReturn(repositoryDir);
		when(repositoryDir.resolve("com/js-lib/web/dialog/1.0")).thenReturn(repositoryCompoDir);
		when(repositoryCompoDir.resolve("dialog.xml")).thenReturn(descriptorFile);
		when(files.exists(descriptorFile)).thenReturn(true);

		when(files.getProjectDir()).thenReturn(projectDir);
		when(projectDir.resolve("res/compo/dialog")).thenReturn(projectCompoDir);
		when(files.getFileName(projectCompoDir)).thenReturn("dialog");
		when(projectCompoDir.resolve("dialog.xml")).thenReturn(descriptorFile);

		String indexPage = "" + //
				"<home>" + //
				"	<h1>Index Page</h1>" + //
				"	<a href='dialog.htm'>file</a>" + //
				"	<footer><a href='http://server.com/home.htm'>home.htm</a></footer>" + //
				"</home>";
		DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
		when(httpRequest.loadHTML(any())).thenReturn(builder.parseHTML(indexPage));

		task = new CompoImport();
		task.setConfig(config);
		task.setConsole(console);
		task.setFiles(files);
		task.setHttpRequest(httpRequest);

		task.setCoordinates(new CompoCoordinates("com.js-lib.web", "dialog", "1.0"));
		task.setPath("res/compo/dialog");
	}

	@Test
	public void GivenTestSetup_ThenDownloadComponentFiles() throws Exception {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));
		verify(httpRequest, times(1)).loadHTML(any());
		verify(httpRequest, times(1)).download(any(), any());
	}

	@Test
	public void GivenTestSetup_ThenCopyComponentFiles() throws Exception {
		// given

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));

		verify(files, times(1)).createDirectories(repositoryCompoDir);
		verify(files, times(1)).cleanDirectory(repositoryCompoDir, false);

		verify(files, times(1)).createDirectory(projectCompoDir);
		verify(files, times(1)).cleanDirectory(projectCompoDir, false);

		verify(files, times(1)).walkFileTree(eq(repositoryCompoDir), any());
		verify(files, times(1)).walkFileTree(eq(projectCompoDir), any());
	}

	@Test
	public void GivenNullCoordinates_ThenLoadThemFromIndexPage() throws Exception {
		// given
		task.setCoordinates(null);

		when(console.input("component group")).thenReturn("com.js-lib.web");
		when(console.input("component artifact")).thenReturn("dialog");
		when(console.input("component version")).thenReturn("1.0");

		// when
		ExitCode exitCode = task.exec();

		// then
		assertThat(exitCode, equalTo(ExitCode.SUCCESS));

		verify(httpRequest, times(1)).getApacheDirectoryIndex(eq(URI.create("http://server.com/com/js-lib/web/")), any());
		verify(httpRequest, times(1)).getApacheDirectoryIndex(eq(URI.create("http://server.com/com/js-lib/web/dialog/")), any());

		assertThat(task.getCoordinates(), notNullValue());
		assertThat(task.getCoordinates().getGroupId(), equalTo("com.js-lib.web"));
		assertThat(task.getCoordinates().getArtifactId(), equalTo("dialog"));
		assertThat(task.getCoordinates().getVersion(), equalTo("1.0"));
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

		verify(httpRequest, times(0)).loadHTML(any());
		verify(httpRequest, times(0)).download(any(), any());
	}

	@Test
	public void GivenProjectCompoDirExist_ThenDoNotCreateIt() throws Exception {
		// given
		when(files.exists(projectCompoDir)).thenReturn(true);

		// when
		task.exec();

		// then
		verify(files, times(0)).createDirectory(projectCompoDir);
	}

	@Test
	public void GivenRepositoryCompoDirExist_ThenDoNotCreateIt() throws Exception {
		// given
		when(files.exists(repositoryCompoDir)).thenReturn(true);

		// when
		task.exec();

		// then
		verify(files, times(0)).createDirectory(repositoryCompoDir);
	}

	@Test
	public void GivenNotReloadAndRepositoryCompoDirExist_ThenDoNotDownload() throws Exception {
		// given
		when(files.exists(repositoryCompoDir)).thenReturn(true);
		task.setReload(false);

		// when
		task.exec();

		// then
		verify(httpRequest, times(0)).loadHTML(any());
		verify(httpRequest, times(0)).download(any(), any());
	}

	@Test
	public void GivenValidParameters_WhenCreateURI_ThenValidURI() {
		// given
		String server = "http://maven.js-lib.com/";
		String[] paths = new String[] { "com.js-lib.web" };

		// when
		URI uri = CompoImport.URI(server, paths);

		// then
		assertThat(uri.toString(), equalTo("http://maven.js-lib.com/com/js-lib/web/"));
	}

	@Test
	public void GivenComponentDirectory_WhenCompoFile_ThenResolveFileName() {
		// given
		Path compoDir = mock(Path.class);
		when(files.getFileName(compoDir)).thenReturn("geo");

		// when
		task.compoFile(compoDir, "js");

		// then
		ArgumentCaptor<String> fileName = ArgumentCaptor.forClass(String.class);
		verify(compoDir, times(1)).resolve(fileName.capture());
		assertThat(fileName.getValue(), equalTo("geo.js"));
	}

	@Test
	public void GivenCompoCoordinates_WhenProjectCompoDir_ThenWalkProjectFiles() throws IOException {
		// given
		CompoCoordinates coordinates = new CompoCoordinates("", "", "");

		// when
		task.projectCompoDir(coordinates);

		// then
		verify(files, times(1)).walkFileTree(eq(projectDir), any());
	}

	@Test
	public void GivenProjectCompoDirVisitorCapture_WhenPreVisitDirectory_ThenTerminateWalking() throws Exception {
		// given
		CompoCoordinates coordinates = new CompoCoordinates();
		task.projectCompoDir(coordinates);

		@SuppressWarnings("unchecked")
		ArgumentCaptor<FileVisitor<Path>> visitorArgument = ArgumentCaptor.forClass(FileVisitor.class);
		verify(files, times(1)).walkFileTree(eq(projectDir), visitorArgument.capture());

		Path dir = mock(Path.class);
		when(files.getFileName(dir)).thenReturn("geo");
		when(dir.resolve("geo.xml")).thenReturn(descriptorFile);

		// when
		FileVisitor<Path> visitor = visitorArgument.getValue();
		FileVisitResult result = visitor.preVisitDirectory(dir, null);

		// then
		assertThat(result, equalTo(FileVisitResult.TERMINATE));
	}
}
