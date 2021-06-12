package js.wood.cli.compo;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;
import java.io.StringWriter;
import java.nio.file.FileVisitor;
import java.nio.file.Path;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.commons.cli.Config;
import com.jslib.commons.cli.FilesUtil;

@RunWith(MockitoJUnitRunner.class)
public class CompoImportCopyTest {
	@Mock
	private Config config;
	@Mock
	private FilesUtil files;
	@Mock
	private CompoImport.CompoRepository repository;

	@Mock
	Path projectDir;
	/** Component directory path on local components repository. */
	@Mock
	private Path repositoryCompoDir;
	/** Component directory path on project. */
	@Mock
	private Path projectCompoDir;

	private CompoCoordinates compoCoordinates;

	private CompoImport compoImport;

	@Before
	public void beforeTest() {
		compoCoordinates = new CompoCoordinates("", "", "");

		when(files.getProjectDir()).thenReturn(projectDir);
		when(repository.getCompoDir(compoCoordinates)).thenReturn(repositoryCompoDir);

		compoImport = new CompoImport();
		compoImport.setConfig(config);
		compoImport.setFiles(files);
		compoImport.setRepository(repository);
	}

	@Test
	public void GivenTestSetup_WhenCompoCopy_ThenScanBothRepositoryAndProjectCompoDirs() throws IOException {
		// given

		// when
		compoImport.copyComponent(compoCoordinates, projectCompoDir);

		// then
		verify(files, times(1)).cleanDirectory(projectCompoDir, false);
		verify(files, times(1)).walkFileTree(eq(repositoryCompoDir), any());
		verify(files, times(1)).walkFileTree(eq(projectCompoDir), any());
	}

	@Test
	public void GivenRepositoryVisitorCapture_WhenVisitFile_ThenFileResolveAndCopy() throws IOException {
		// given
		@SuppressWarnings("unchecked")
		ArgumentCaptor<FileVisitor<Path>> visitor = ArgumentCaptor.forClass(FileVisitor.class);

		compoImport.copyComponent(compoCoordinates, projectCompoDir);
		verify(files, times(1)).walkFileTree(eq(repositoryCompoDir), visitor.capture());

		Path file = Mockito.mock(Path.class);
		when(files.getFileName(file)).thenReturn("dialog.htm");

		// when
		visitor.getValue().visitFile(file, null);

		// then
		verify(projectCompoDir, times(1)).resolve("dialog.htm");
		verify(files, times(1)).copy(file, null);
	}

	@Test
	public void GivenRepositoryVisitorCaptureAndCompoFile_WhenVisitFile_ThenRenameFile() throws IOException {
		// given
		@SuppressWarnings("unchecked")
		ArgumentCaptor<FileVisitor<Path>> visitor = ArgumentCaptor.forClass(FileVisitor.class);

		compoCoordinates = new CompoCoordinates("", "dialog", "");
		when(repository.getCompoDir(compoCoordinates)).thenReturn(repositoryCompoDir);
		when(files.getFileName(projectCompoDir)).thenReturn("geo");

		compoImport.copyComponent(compoCoordinates, projectCompoDir);
		verify(files, times(1)).walkFileTree(eq(repositoryCompoDir), visitor.capture());

		Path file = Mockito.mock(Path.class);
		when(files.getFileName(file)).thenReturn("dialog.htm");

		// when
		visitor.getValue().visitFile(file, null);

		// then
		verify(projectCompoDir, times(1)).resolve("geo.htm");
		verify(files, times(1)).copy(file, null);
	}

	@Test
	public void GivenProjectVisitorCapture_WhenVisitFile_ThenReadWriterOnUpdateLayoutOperators() throws IOException {
		// given
		@SuppressWarnings("unchecked")
		ArgumentCaptor<FileVisitor<Path>> visitor = ArgumentCaptor.forClass(FileVisitor.class);

		compoImport.copyComponent(compoCoordinates, projectCompoDir);
		verify(files, times(1)).walkFileTree(eq(projectCompoDir), visitor.capture());

		Path file = Mockito.mock(Path.class);
		when(files.getFileName(file)).thenReturn("dialog.htm");
		when(files.getReader(file)).thenReturn(new StringReader("<body></body>"));
		when(files.getWriter(file)).thenReturn(new StringWriter());

		// when
		visitor.getValue().visitFile(file, null);

		// then
		verify(files, times(1)).getReader(file);
		verify(files, times(1)).getWriter(file);
	}

	@Test
	public void GivenCompoCoordinates_WhenUpdateLayoutOperators_ThenCompoPath() throws IOException {
		// given
		String layout = "" + //
				"<body w:template='com.js-lib.web:geo-map:1.0.0' xmlns:w='js-lib.com/wood'>" + //
				"	<section w:content='section' w:compo='com.js-lib.web:geo-map:1.0.0' w:param='title:title;label:label'></section>" + //
				"</body>";
		
		Path layoutFile = Mockito.mock(Path.class);
		when(files.getReader(layoutFile)).thenReturn(new StringReader(layout));
		
		StringWriter updateLayout = new StringWriter();
		when(files.getWriter(layoutFile)).thenReturn(updateLayout);

		when(projectDir.relativize(any())).thenReturn(projectCompoDir);
		when(projectCompoDir.toString()).thenReturn("lib\\geo");
		
		// when
		compoImport.updateLayoutOperators(layoutFile, "XMLNS");

		// then
		assertThat(updateLayout.toString(), not(containsString("<?xml")));
		assertThat(updateLayout.toString(), containsString("w:template=\"lib/geo\""));
		assertThat(updateLayout.toString(), containsString("w:content=\"section\""));
		assertThat(updateLayout.toString(), containsString("w:compo=\"lib/geo\""));
		assertThat(updateLayout.toString(), containsString("w:param=\"title:title;label:label\""));
		assertThat(updateLayout.toString(), containsString("xmlns:w"));
	}

	@Test
	public void GivenDataAttrOperators_WhenUpdateLayoutOperators_ThenRenameOperator() throws IOException {
		// given
		String layout = "" + //
				"<body w:template='com.js-lib.web:geo-map:1.0.0' xmlns:w='js-lib.com/wood'>" + //
				"	<section w:content='section' w:compo='com.js-lib.web:geo-map:1.0.0' w:param='title:title;label:label'></section>" + //
				"</body>";
		
		Path layoutFile = Mockito.mock(Path.class);
		when(files.getReader(layoutFile)).thenReturn(new StringReader(layout));
		
		StringWriter updateLayout = new StringWriter();
		when(files.getWriter(layoutFile)).thenReturn(updateLayout);

		when(projectDir.relativize(any())).thenReturn(projectCompoDir);
		when(projectCompoDir.toString()).thenReturn("lib\\geo");
		
		// when
		compoImport.updateLayoutOperators(layoutFile, "DATA_ATTR");

		// then
		assertThat(updateLayout.toString(), not(containsString("<?xml")));
		assertThat(updateLayout.toString(), containsString("data-template=\"lib/geo\""));
		assertThat(updateLayout.toString(), containsString("data-content=\"section\""));
		assertThat(updateLayout.toString(), containsString("data-compo=\"lib/geo\""));
		assertThat(updateLayout.toString(), containsString("data-param=\"title:title;label:label\""));
		assertThat(updateLayout.toString(), not(containsString("xmlns:w")));
	}

	@Test
	public void GivenAttrOperators_WhenUpdateLayoutOperators_ThenRenameOperator() throws IOException {
		// given
		String layout = "" + //
				"<body w:template='com.js-lib.web:geo-map:1.0.0' xmlns:w='js-lib.com/wood'>" + //
				"	<section w:content='section' w:compo='com.js-lib.web:geo-map:1.0.0' w:param='title:title;label:label'></section>" + //
				"</body>";
		
		Path layoutFile = Mockito.mock(Path.class);
		when(files.getReader(layoutFile)).thenReturn(new StringReader(layout));
		
		StringWriter updateLayout = new StringWriter();
		when(files.getWriter(layoutFile)).thenReturn(updateLayout);

		when(projectDir.relativize(any())).thenReturn(projectCompoDir);
		when(projectCompoDir.toString()).thenReturn("lib\\geo");
		
		// when
		compoImport.updateLayoutOperators(layoutFile, "ATTR");

		// then
		assertThat(updateLayout.toString(), not(containsString("<?xml")));
		assertThat(updateLayout.toString(), containsString("template=\"lib/geo\""));
		assertThat(updateLayout.toString(), containsString("content=\"section\""));
		assertThat(updateLayout.toString(), containsString("compo=\"lib/geo\""));
		assertThat(updateLayout.toString(), containsString("param=\"title:title;label:label\""));
		assertThat(updateLayout.toString(), not(containsString("xmlns:w")));
	}
}
