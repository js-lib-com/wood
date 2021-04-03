package js.wood.cli.project;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.cli.Console;
import js.wood.cli.FilesUtil;
import js.wood.cli.project.ProjectList.ListFileVisitor;
import js.wood.cli.project.ProjectList.PageFileVisitor;
import js.wood.cli.project.ProjectList.TemplateFileVisitor;
import js.wood.cli.project.ProjectList.TreeFileVisitor;
import js.wood.cli.project.ProjectList.Utils;

@RunWith(MockitoJUnitRunner.class)
public class ProjectListTest {
	@Mock
	private Console console;
	@Mock
	private BasicFileAttributes attributes;

	@Mock
	private Utils utils;
	@Mock
	private FilesUtil files;

	@Mock
	private Path projectDir;

	private ProjectList task;

	@Before
	public void beforeTest() throws IOException {
		when(files.getProjectDir()).thenReturn(projectDir);

		task = new ProjectList();
		task.setConsole(console);
		task.setFiles(files);
	}

	@Test
	public void exec_GivenPageOption_ThenUsePageFileVisitor() throws IOException {
		// given
		task.setPage(true);

		// when
		task.exec();

		// then
		verify(files, times(1)).walkFileTree(eq(projectDir), any(PageFileVisitor.class));
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 0);
	}

	@Test
	public void exec_GivenTemplateOption_ThenListNoTemplates() throws IOException {
		// given
		task.setTemplate(true);

		// when
		task.exec();

		// then
		verify(files, times(1)).walkFileTree(eq(projectDir), any(TemplateFileVisitor.class));
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 0);
	}

	@Test
	public void exec_GivenTreeOption_ThenUseTreeFileVisitor() throws IOException {
		// given
		task.setTree(true);

		// when
		task.exec();

		// then
		verify(files, times(1)).walkFileTree(eq(projectDir), any(TreeFileVisitor.class));
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 0);
	}

	@Test
	public void exec_GivenNoOptions_ThenUseListFileVisitor() throws IOException {
		// given

		// when
		task.exec();

		// then
		verify(files, times(1)).walkFileTree(eq(projectDir), any(ListFileVisitor.class));
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 0);
	}

	@Test
	public void exec_GivenNoOptionsAndPathParameter_ThenUseListFileVisitor() throws IOException {
		// given
		task.setPath("res");
		when(projectDir.resolve("res")).thenReturn(projectDir);

		// when
		task.exec();

		// then
		verify(files, times(1)).walkFileTree(eq(projectDir), any(ListFileVisitor.class));
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 0);
	}

	@Test
	public void page_GivenDirectoryIsExcluded_ThenSkipSubtree() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(true);
		PageFileVisitor visitor = task.new PageFileVisitor(projectDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(projectDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.SKIP_SUBTREE));
	}

	@Test
	public void page_GivenDirectoryIsNotExcluded_ThenContinue() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(false);
		PageFileVisitor visitor = task.new PageFileVisitor(projectDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(projectDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}

	@Test
	public void page_GivenIsPage_ThenFound() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isXML(null, "page")).thenReturn(true);
		PageFileVisitor visitor = task.new PageFileVisitor(projectDir);

		// when
		visitor.preVisitDirectory(projectDir, attributes);
		visitor.visitFile(null, attributes);
		visitor.postVisitDirectory(projectDir, null);

		// then
		verify(console, times(1)).print(null);
		assertThat(task.getFound(), equalTo(1));
	}

	@Test
	public void page_GivenIsNotPage_ThenNotFound() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isXML(null, "page")).thenReturn(false);
		PageFileVisitor visitor = task.new PageFileVisitor(projectDir);

		// when
		visitor.preVisitDirectory(projectDir, attributes);
		visitor.visitFile(null, attributes);
		visitor.postVisitDirectory(projectDir, null);

		// then
		verify(console, times(0)).print(null);
		assertThat(task.getFound(), equalTo(0));
	}

	@Test
	public void template_GivenDirectoryIsExcluded_ThenSkipSubtree() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(true);
		TemplateFileVisitor visitor = task.new TemplateFileVisitor(projectDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(projectDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.SKIP_SUBTREE));
	}

	@Test
	public void template_GivenDirectoryIsNotExcluded_ThenContinue() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(false);
		TemplateFileVisitor visitor = task.new TemplateFileVisitor(projectDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(projectDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}

	@Test
	public void template_GivenIsPage_ThenFound() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isXML(null, "template")).thenReturn(true);
		TemplateFileVisitor visitor = task.new TemplateFileVisitor(projectDir);

		// when
		visitor.preVisitDirectory(projectDir, attributes);
		visitor.visitFile(null, attributes);
		visitor.postVisitDirectory(projectDir, null);

		// then
		verify(console, times(1)).print(null);
		assertThat(task.getFound(), equalTo(1));
	}

	@Test
	public void template_GivenIsNotPage_ThenNotFound() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isXML(null, "template")).thenReturn(false);
		TemplateFileVisitor visitor = task.new TemplateFileVisitor(projectDir);

		// when
		visitor.preVisitDirectory(projectDir, attributes);
		visitor.visitFile(null, attributes);
		visitor.postVisitDirectory(projectDir, null);

		// then
		verify(console, times(0)).print(null);
		assertThat(task.getFound(), equalTo(0));
	}

	@Test
	public void tree_GivenDirectoryIsExcluded_ThenSkipSubtree() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(true);
		TreeFileVisitor visitor = task.new TreeFileVisitor(projectDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(projectDir, attributes);

		// then
		verify(console, times(0)).print("+ %s", (Object) null);
		assertThat(result, equalTo(FileVisitResult.SKIP_SUBTREE));
	}

	@Test
	public void tree_GivenDirectoryIsNotExcluded_ThenPrintAndContinue() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(false);
		TreeFileVisitor visitor = task.new TreeFileVisitor(projectDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(projectDir, attributes);

		// then
		verify(console, times(1)).print("+ %s", (Object) null);
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}

	@Test
	public void list_GivenDirectoryIsExcluded_ThenSkipSubtree() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(true);
		ListFileVisitor visitor = task.new ListFileVisitor(projectDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(projectDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.SKIP_SUBTREE));
	}

	@Test
	public void list_GivenDirectoryIsNotExcluded_ThenContinue() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(false);
		ListFileVisitor visitor = task.new ListFileVisitor(projectDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(projectDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}

	@Test
	public void list_GivenVisitFile_ThenPrintAndContinue() throws IOException {
		// given
		task.setUtils(utils);
		ListFileVisitor visitor = task.new ListFileVisitor(projectDir);

		// when
		FileVisitResult result = visitor.visitFile(projectDir, attributes);

		// then
		verify(console, times(1)).print(null);
		assertThat(task.getFound(), equalTo(1));
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}
}
