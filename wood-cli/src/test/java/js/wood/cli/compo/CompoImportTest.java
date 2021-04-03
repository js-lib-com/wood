package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.net.URL;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.util.Classes;
import js.wood.cli.Config;
import js.wood.cli.Console;
import js.wood.cli.ExitCode;
import js.wood.cli.FilesUtil;

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

	private CompoImport task;

	@Before
	public void beforeTest() throws IOException {
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

		task = new CompoImport();
		task.setConfig(config);
		task.setConsole(console);
		task.setFiles(files);
		task.setCoordinates(compoCoordinates);
		task.setPath("res/compo/dialog");
		task.setDocumentBuilder(documentBuilder);
	}

	private static Document parseHTML(String document) {
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
	public void GivenProjectCompoDirExist_ThenDoNotCreateIt() throws IOException {
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
		when(files.exists(repositoryCompoDir)).thenReturn(true);
		task.setReload(true);

		// when
		task.exec();

		// then
		verify(files, times(0)).createDirectory(repositoryCompoDir);
	}

	@Test
	public void GivenNotReloadAndRepositoryCompoNotExist_ThenCreateDirectoryAndDownload() throws IOException {
		// given
		when(files.exists(repositoryCompoDir)).thenReturn(false);
		task.setReload(false);

		// when
		task.exec();

		// then
		verify(files, times(1)).createDirectory(repositoryCompoDir);
		verify(documentBuilder, times(1)).loadHTML(new URL("http://server.com/com/js-lib/web/dialog/1.0/"));
	}

	@Test
	public void GivenNotReloadAndRepositoryCompoExist_ThenDoNotDownload() throws IOException {
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
