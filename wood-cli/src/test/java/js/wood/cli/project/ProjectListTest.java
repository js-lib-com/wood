package js.wood.cli.project;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.LinkOption;
import java.nio.file.Path;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.spi.FileSystemProvider;
import java.util.Arrays;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.cli.Console;
import js.wood.cli.project.ProjectList.ListFileVisitor;
import js.wood.cli.project.ProjectList.PageFileVisitor;
import js.wood.cli.project.ProjectList.TemplateFileVisitor;
import js.wood.cli.project.ProjectList.TreeFileVisitor;
import js.wood.cli.project.ProjectList.Utils;

@RunWith(MockitoJUnitRunner.class)
public class ProjectListTest {
	@Mock
	private FileSystemProvider provider;
	@Mock
	private FileSystem fileSystem;
	@Mock
	private Console console;
	@Mock
	private BasicFileAttributes attributes;

	@Mock
	private Utils utils;

	@Mock
	private Path workingDir;

	private ProjectList task;

	@Before
	public void beforeTest() throws IOException {
		when(fileSystem.provider()).thenReturn(provider);
		when(fileSystem.getPath("")).thenReturn(workingDir);
		when(workingDir.toAbsolutePath()).thenReturn(workingDir);
		when(workingDir.getFileSystem()).thenReturn(fileSystem);

		when(provider.readAttributes(workingDir, BasicFileAttributes.class, LinkOption.NOFOLLOW_LINKS)).thenReturn(attributes);

		task = new ProjectList();
		task.setFileSystem(fileSystem);
		task.setConsole(console);
	}

	@Test
	public void exec_GivenPageOption_ThenListNoPages() throws IOException {
		// given
		task.setPage(true);

		// when
		task.exec();

		// then
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 0);
		assertThat(task.getFound(), equalTo(0));
	}

	@Test
	public void exec_GivenTemplateOption_ThenListNoTemplates() throws IOException {
		// given
		task.setTemplate(true);

		// when
		task.exec();

		// then
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 0);
		assertThat(task.getFound(), equalTo(0));
	}

	@Test
	public void exec_GivenTreeOption_ThenTreeListOnlyRootDirectory() throws IOException {
		// given
		task.setTree(true);

		// when
		task.exec();

		// then
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 1);
		assertThat(task.getFound(), equalTo(1));
	}

	@Test
	public void exec_GivenNoOptions_ThenListOnlyRootDirectory() throws IOException {
		// given

		// when
		task.exec();

		// then
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 1);
		assertThat(task.getFound(), equalTo(1));
	}

	@Test
	public void exec_GivenNoOptionsAndPathParameter_ThenListOnlyRootDirectory() throws IOException {
		// given
		task.setPath("res");
		when(workingDir.resolve("res")).thenReturn(workingDir);

		// when
		task.exec();

		// then
		verify(console, times(1)).crlf();
		verify(console, times(1)).info("Found %d objects.", 1);
		assertThat(task.getFound(), equalTo(1));
	}

	@Test
	public void page_GivenDirectoryIsExcluded_ThenSkipSubtree() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(true);
		PageFileVisitor visitor = task.new PageFileVisitor(workingDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(workingDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.SKIP_SUBTREE));
	}

	@Test
	public void page_GivenDirectoryIsNotExcluded_ThenContinue() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(false);
		PageFileVisitor visitor = task.new PageFileVisitor(workingDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(workingDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}

	@Test
	public void page_GivenIsPage_ThenFound() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isXML(null, "page")).thenReturn(true);
		PageFileVisitor visitor = task.new PageFileVisitor(workingDir);

		// when
		visitor.preVisitDirectory(workingDir, attributes);

		// then
		verify(console, times(1)).print(null);
		assertThat(task.getFound(), equalTo(1));
	}

	@Test
	public void page_GivenIsNotPage_ThenNotFound() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isXML(null, "page")).thenReturn(false);
		PageFileVisitor visitor = task.new PageFileVisitor(workingDir);

		// when
		visitor.preVisitDirectory(workingDir, attributes);

		// then
		verify(console, times(0)).print(null);
		assertThat(task.getFound(), equalTo(0));
	}

	@Test
	public void template_GivenDirectoryIsExcluded_ThenSkipSubtree() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(true);
		TemplateFileVisitor visitor = task.new TemplateFileVisitor(workingDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(workingDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.SKIP_SUBTREE));
	}

	@Test
	public void template_GivenDirectoryIsNotExcluded_ThenContinue() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(false);
		TemplateFileVisitor visitor = task.new TemplateFileVisitor(workingDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(workingDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}

	@Test
	public void template_GivenIsPage_ThenFound() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isXML(null, "template")).thenReturn(true);
		TemplateFileVisitor visitor = task.new TemplateFileVisitor(workingDir);

		// when
		visitor.preVisitDirectory(workingDir, attributes);

		// then
		verify(console, times(1)).print(null);
		assertThat(task.getFound(), equalTo(1));
	}

	@Test
	public void template_GivenIsNotPage_ThenNotFound() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isXML(null, "template")).thenReturn(false);
		TemplateFileVisitor visitor = task.new TemplateFileVisitor(workingDir);

		// when
		visitor.preVisitDirectory(workingDir, attributes);

		// then
		verify(console, times(0)).print(null);
		assertThat(task.getFound(), equalTo(0));
	}

	@Test
	public void tree_GivenDirectoryIsExcluded_ThenSkipSubtree() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(true);
		TreeFileVisitor visitor = task.new TreeFileVisitor(workingDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(workingDir, attributes);

		// then
		verify(console, times(0)).print("+ %s", (Object) null);
		assertThat(result, equalTo(FileVisitResult.SKIP_SUBTREE));
	}

	@Test
	public void tree_GivenDirectoryIsNotExcluded_ThenPrintAndContinue() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(false);
		TreeFileVisitor visitor = task.new TreeFileVisitor(workingDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(workingDir, attributes);

		// then
		verify(console, times(1)).print("+ %s", (Object) null);
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}

	@Test
	public void list_GivenDirectoryIsExcluded_ThenSkipSubtree() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(true);
		ListFileVisitor visitor = task.new ListFileVisitor(workingDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(workingDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.SKIP_SUBTREE));
	}

	@Test
	public void list_GivenDirectoryIsNotExcluded_ThenContinue() throws IOException {
		// given
		task.setUtils(utils);
		when(utils.isExcluded(null)).thenReturn(false);
		ListFileVisitor visitor = task.new ListFileVisitor(workingDir);

		// when
		FileVisitResult result = visitor.preVisitDirectory(workingDir, attributes);

		// then
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}

	@Test
	public void list_GivenVisitFile_ThenPrintAndContinue() throws IOException {
		// given
		task.setUtils(utils);
		ListFileVisitor visitor = task.new ListFileVisitor(workingDir);

		// when
		FileVisitResult result = visitor.visitFile(workingDir, attributes);

		// then
		verify(console, times(1)).print(null);
		assertThat(task.getFound(), equalTo(1));
		assertThat(result, equalTo(FileVisitResult.CONTINUE));
	}

	@Test
	public void utils_isExcluded_GivenEmptyExcludesOption_ThenReturnFalse() {
		// given
		Utils utils = task.new Utils();

		// when
		boolean excluded = utils.isExcluded(workingDir);

		// then
		assertThat(excluded, is(false));
	}

	@Test
	public void utils_isExcluded_GivenDirectoryNotInExcludesOption_ThenReturnFalse() {
		// given
		when(workingDir.toString()).thenReturn("test");
		task.setExcludes(Arrays.asList("fake"));
		Utils utils = task.new Utils();

		// when
		boolean excluded = utils.isExcluded(workingDir);

		// then
		assertThat(excluded, is(false));
	}

	@Test
	public void utils_isExcluded_GivenDirectoryIsInExcludesOption_ThenReturnTrue() {
		// given
		when(workingDir.toString()).thenReturn("test");
		task.setExcludes(Arrays.asList("test"));
		Utils utils = task.new Utils();

		// when
		boolean excluded = utils.isExcluded(workingDir);

		// then
		assertThat(excluded, is(true));
	}

	@Test
	public void utils_isXML_GivenRootMatches_ThenReturnTrue() throws IOException {
		// given
		Path file = mock(Path.class);
		when(file.getFileSystem()).thenReturn(fileSystem);
		when(provider.newInputStream(file)).thenReturn(new ByteArrayInputStream("<?xml version='1.0' ?>\n<page>\n</page>\n".getBytes()));
		Utils utils = task.new Utils();

		// when
		boolean xml = utils.isXML(file, "page");

		// then
		assertThat(xml, is(true));
	}

	@Test
	public void utils_isXML_GivenRootDoesNotMatches_ThenReturnTrue() throws IOException {
		// given
		Path file = mock(Path.class);
		when(file.getFileSystem()).thenReturn(fileSystem);
		when(provider.newInputStream(file)).thenReturn(new ByteArrayInputStream("<page>\n</page>\n".getBytes()));
		Utils utils = task.new Utils();

		// when
		boolean xml = utils.isXML(file, "template");

		// then
		assertThat(xml, is(false));
	}

	@Test
	public void utils_isXML_GivenMissingContent_ThenReturnFalse() throws IOException {
		// given
		Path file = mock(Path.class);
		when(file.getFileSystem()).thenReturn(fileSystem);
		when(provider.newInputStream(file)).thenReturn(new ByteArrayInputStream("<?xml version='1.0' ?>".getBytes()));
		Utils utils = task.new Utils();

		// when
		boolean xml = utils.isXML(file, "page");

		// then
		assertThat(xml, is(false));
	}

	@Test
	public void utils_isXML_GivenFileDoesNotExist_ThenReturnFalse() throws IOException {
		// given
		Path file = mock(Path.class);
		when(file.getFileSystem()).thenReturn(fileSystem);
		doThrow(IOException.class).when(provider).checkAccess(file);
		Utils utils = task.new Utils();

		// when
		boolean xml = utils.isXML(file, "page");

		// then
		assertThat(xml, is(false));
	}
}
