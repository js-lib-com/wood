package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doCallRealMethod;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Arrays;
import java.util.Locale;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.util.Classes;
import js.util.Files;

@RunWith(MockitoJUnitRunner.class)
public class BuildFsTest {
	private DocumentBuilder documentBuilder;

	@Mock
	private Component compo;
	@Mock
	private IReferenceHandler referenceHandler;

	private File buildDir;
	private BuildFS buildFS;

	@Before
	public void beforeTest() throws Exception {
		documentBuilder = Classes.loadService(DocumentBuilder.class);

		File projectRoot = new File("src/test/resources/build-fs");
		buildDir = new File(projectRoot, "site");
		if (buildDir.exists()) {
			Files.removeFilesHierarchy(buildDir);
		}
		buildFS = new TestBuildFS(buildDir, 0);
	}

	@Test
	public void writePage() throws IOException {
		Component compo = compo();
		PageDocument page = new PageDocument(compo);

		buildFS.writePage(compo, page.getDocument());
		assertTrue(buildFile("htm/index.htm").exists());
	}

	@Test
	public void writePage_Twice() throws IOException {
		Component compo = compo();
		PageDocument page = new PageDocument(compo);

		buildFS.writePage(compo, page.getDocument());
		assertTrue(buildFile("htm/index.htm").exists());
		buildFile("htm/index.htm").delete();

		// second attempt to write page in the same build FS instance is ignored
		buildFS.writePage(compo, page.getDocument());
		assertFalse(buildFile("htm/index.htm").exists());
	}

	@Test
	public void writePage_Locale() throws IOException {
		Component compo = compo();
		PageDocument page = new PageDocument(compo);

		buildFS.setLocale(new Locale("ro"));
		buildFS.writePage(compo, page.getDocument());
		assertTrue(buildFile("ro/htm/index.htm").exists());
	}

	@Test
	public void writePage_BuildNumber() throws IOException {
		Component compo = compo();
		PageDocument page = new PageDocument(compo);

		BuildFS buildFS = new TestBuildFS(buildDir, 4);
		buildFS.writePage(compo, page.getDocument());
		assertTrue(buildFile("htm/index-004.htm").exists());
	}

	@Test
	public void writeFavicon() throws IOException {
		FilePath favicon = file("favicon.ico");

		assertFalse(buildFile("favicon.ico").exists());
		assertThat(buildFS.writeFavicon(null, favicon), equalTo("../img/favicon.ico"));
		assertTrue(buildFile("img/favicon.ico").exists());
	}

	@Test
	public void writeFavicon_NotExisting() throws IOException {
		FilePath favicon = file("favicon.ico");
		when(favicon.exists()).thenReturn(false);

		assertFalse(buildFile("favicon.ico").exists());
		assertThat(buildFS.writeFavicon(null, favicon), equalTo("../img/favicon.ico"));
		assertFalse(buildFile("img/favicon.ico").exists());
	}

	@Test
	public void writePageMedia() throws IOException {
		FilePath mediaFile = file("background.jpg");
		assertThat(buildFS.writePageMedia(null, mediaFile), equalTo("../img/background.jpg"));
		assertTrue(buildFile("img/background.jpg").exists());
	}

	@Test
	public void writePageMedia_Locale() throws IOException {
		FilePath mediaFile = file("background.jpg");
		buildFS.setLocale(new Locale("ro"));

		assertThat(buildFS.writePageMedia(null, mediaFile), equalTo("../img/background.jpg"));
		assertTrue(buildFile("ro/img/background.jpg").exists());
	}

	@Test
	public void writePageMedia_BuildNumber() throws IOException {
		FilePath mediaFile = file("background.jpg");
		BuildFS buildFS = new TestBuildFS(buildDir, 4);

		assertThat(buildFS.writePageMedia(null, mediaFile), equalTo("../img/background-004.jpg"));
		assertTrue(buildFile("img/background-004.jpg").exists());
	}

	@Test(expected = IOException.class)
	public void writePageMedia_IOException() throws IOException {
		FilePath mediaFile = file("background.jpg");
		when(mediaFile.getReader()).thenReturn(new ExceptionalReader());
		buildFS.writePageMedia(null, mediaFile);
	}

	@Test
	public void writeStyleMedia() throws IOException {
		FilePath mediaFile = file("background.jpg");
		assertThat(buildFS.writeStyleMedia(mediaFile), equalTo("../img/background.jpg"));
		assertTrue(buildFile("img/background.jpg").exists());
	}

	@Test
	public void writeStyleMedia_Locale() throws IOException {
		FilePath mediaFile = file("background.jpg");
		buildFS.setLocale(new Locale("ro"));

		assertThat(buildFS.writeStyleMedia(mediaFile), equalTo("../img/background.jpg"));
		assertTrue(buildFile("ro/img/background.jpg").exists());
	}

	@Test
	public void writeStyleMedia_BuildNumber() throws IOException {
		FilePath mediaFile = file("background.jpg");
		BuildFS buildFS = new TestBuildFS(buildDir, 4);

		assertThat(buildFS.writeStyleMedia(mediaFile), equalTo("../img/background-004.jpg"));
		assertTrue(buildFile("img/background-004.jpg").exists());
	}

	@Test(expected = IOException.class)
	public void writeStyleMedia_IOException() throws IOException {
		FilePath mediaFile = file("background.jpg");
		when(mediaFile.getReader()).thenReturn(new ExceptionalReader());
		buildFS.writeStyleMedia(mediaFile);
	}

	@Test
	public void writeStyle() throws IOException {
		DirPath sourceDir = Mockito.mock(DirPath.class);
		when(sourceDir.filter(any())).thenReturn(Arrays.asList());

		FilePath styleFile = file("style.css");
		when(styleFile.getParentDirPath()).thenReturn(sourceDir);

		assertThat(buildFS.writeStyle(null, styleFile, referenceHandler), equalTo("../css/style.css"));
		assertTrue(buildFile("css/style.css").exists());
	}

	@Test
	public void writeStyle_Locale() throws IOException {
		DirPath sourceDir = Mockito.mock(DirPath.class);
		when(sourceDir.filter(any())).thenReturn(Arrays.asList());

		FilePath styleFile = file("style.css");
		when(styleFile.getParentDirPath()).thenReturn(sourceDir);

		buildFS.setLocale(new Locale("ro"));

		assertThat(buildFS.writeStyle(null, styleFile, referenceHandler), equalTo("../css/style.css"));
		assertTrue(buildFile("ro/css/style.css").exists());
	}

	@Test
	public void writeStyle_BuildNumber() throws IOException {
		DirPath sourceDir = Mockito.mock(DirPath.class);
		when(sourceDir.filter(any())).thenReturn(Arrays.asList());

		FilePath styleFile = file("style.css");
		when(styleFile.getParentDirPath()).thenReturn(sourceDir);

		BuildFS buildFS = new TestBuildFS(buildDir, 4);
		assertThat(buildFS.writeStyle(null, styleFile, referenceHandler), equalTo("../css/style-004.css"));
		assertTrue(buildFile("css/style-004.css").exists());
	}

	@Test(expected = IOException.class)
	public void writeStyle_IOException() throws IOException {
		DirPath sourceDir = Mockito.mock(DirPath.class);
		when(sourceDir.filter(any())).thenReturn(Arrays.asList());

		FilePath styleFile = file("style.css");
		when(styleFile.getParentDirPath()).thenReturn(sourceDir);
		when(styleFile.getReader()).thenReturn(new ExceptionalReader());

		buildFS.writeStyle(null, styleFile, referenceHandler);
	}

	@Test
	public void writeScript() throws IOException {
		FilePath scriptFile = file("Index.js");

		assertThat(buildFS.writeScript(null, scriptFile, referenceHandler), equalTo("../js/Index.js"));
		assertTrue(buildFile("js/Index.js").exists());
	}

	@Test
	public void writeScript_Locale() throws IOException {
		FilePath scriptFile = file("Index.js");
		buildFS.setLocale(new Locale("ro"));

		assertThat(buildFS.writeScript(null, scriptFile, referenceHandler), equalTo("../js/Index.js"));
		assertTrue(buildFile("ro/js/Index.js").exists());
	}

	@Test
	public void writeScript_BuildNumber() throws IOException {
		FilePath scriptFile = file("Index.js");
		BuildFS buildFS = new TestBuildFS(buildDir, 4);

		assertThat(buildFS.writeScript(null, scriptFile, referenceHandler), equalTo("../js/Index-004.js"));
		assertTrue(buildFile("js/Index-004.js").exists());
	}

	@Test(expected = IOException.class)
	public void writeScript_IOException() throws IOException {
		FilePath scriptFile = file("Index.js");
		when(scriptFile.getReader()).thenReturn(new ExceptionalReader());
		buildFS.writeScript(null, scriptFile, referenceHandler);
	}

	@Test
	public void dirFactory() throws Exception {
		assertThat(buildFS.createDirectory("images"), isDir("src/test/resources/build-fs/site/images"));
		buildFS.setLocale(new Locale("ro"));
		assertThat(buildFS.createDirectory("images"), isDir("src/test/resources/build-fs/site/ro/images"));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullLocale() {
		buildFS.setLocale(null);
	}

	@Test(expected = WoodException.class)
	public void writeFile_MissingExtension() throws IOException {
		FilePath mediaFile = file("style");
		BuildFS buildFS = new TestBuildFS(buildDir, 4);
		buildFS.writeStyleMedia(mediaFile);
	}

	// --------------------------------------------------------------------------------------------

	private File buildFile(String path) {
		return new File(buildDir, path);
	}

	private Component compo() {
		Document doc = documentBuilder.parseXML("<body></body>");
		when(compo.getLayout()).thenReturn(doc.getRoot());
		when(compo.getLayoutFileName()).thenReturn("index.htm");
		return compo;
	}

	private static FilePath file(String fileName) throws IOException {
		FilePath file = Mockito.mock(FilePath.class);
		when(file.exists()).thenReturn(true);
		when(file.getName()).thenReturn(fileName);
		when(file.getReader()).thenReturn(new StringReader("CONTENT"));
		doCallRealMethod().when(file).copyTo((Writer) any());
		return file;
	}

	private static Matcher<File> isDir(String path) {
		return new BaseMatcher<File>() {
			@Override
			public boolean matches(Object item) {
				return item.equals(new File(path.replace('/', File.separatorChar))) && ((File) item).isDirectory();
			}

			@Override
			public void describeTo(Description description) {
				description.appendText(path);
			}
		};
	}

	private static class TestBuildFS extends BuildFS {
		public TestBuildFS(File buildDir, int buildNumber) {
			super(buildDir, buildNumber);
		}

		@Override
		protected File getPageDir(Component compo) {
			return createDirectory("htm");
		}

		@Override
		protected File getStyleDir() {
			return createDirectory("css");
		}

		@Override
		protected File getScriptDir() {
			return createDirectory("js");
		}

		@Override
		protected File getMediaDir() {
			return createDirectory("img");
		}

		@Override
		protected String formatPageName(String pageName) {
			return pageName;
		}

		@Override
		protected String formatStyleName(FilePath styleFile) {
			return styleFile.getName();
		}

		@Override
		protected String formatScriptName(FilePath scriptFile) {
			return scriptFile.getName();
		}

		@Override
		protected String formatMediaName(FilePath mediaFile) {
			return mediaFile.getName();
		}
	}

	private static class ExceptionalReader extends Reader {
		@Override
		public int read(char[] cbuf, int off, int len) throws IOException {
			throw new IOException();
		}

		@Override
		public void close() throws IOException {
			throw new IOException();
		}
	}
}
