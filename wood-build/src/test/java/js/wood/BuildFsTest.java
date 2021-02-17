package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.hamcrest.BaseMatcher;
import org.hamcrest.Description;
import org.hamcrest.Matcher;
import org.junit.Before;
import org.junit.Test;

import js.util.Files;

public class BuildFsTest {
	private BuilderProject project;

	@Before
	public void beforeTest() throws Exception {
		File projectRoot = new File("src/test/resources/project");
		File buildDir = new File(projectRoot, CT.DEF_BUILD_DIR);
		project = new BuilderProject(projectRoot, buildDir);
		if (buildDir.exists()) {
			Files.removeFilesHierarchy(buildDir);
		}
	}

	@Test
	public void writePage() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);

		Component compo = new Component(new CompoPath(project, "res/page/index"), (IReference reference, FilePath sourceFile) -> sourceFile.value());
		compo.scan();
		PageDocument page = new PageDocument(compo);

		buildFS.writePage(compo, page.getDocument());
		assertTrue(exists("htm/index.htm"));
	}

	@Test
	public void writePage_Twice() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);

		Component compo = new Component(new CompoPath(project, "res/page/index"), (IReference reference, FilePath sourceFile) -> sourceFile.value());
		compo.scan();
		PageDocument page = new PageDocument(compo);

		buildFS.writePage(compo, page.getDocument());
		assertTrue(exists("htm/index.htm"));
		File buildDir = new File(project.getProjectRoot(), CT.DEF_BUILD_DIR);
		new File(buildDir, "htm/index.htm").delete();

		// second attempt to write page in the same build FS instance is ignored
		buildFS.writePage(compo, page.getDocument());
		assertFalse(exists("htm/index.htm"));
	}

	@Test
	public void writePage_Locale() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);

		Component compo = new Component(new CompoPath(project, "res/page/index"), (IReference reference, FilePath sourceFile) -> sourceFile.value());
		compo.scan();
		PageDocument page = new PageDocument(compo);

		buildFS.setLocale(new Locale("ro"));
		buildFS.writePage(compo, page.getDocument());
		assertTrue(exists("ro/htm/index.htm"));
	}

	@Test
	public void writePage_BuildNumber() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 4);

		Component compo = new Component(new CompoPath(project, "res/page/index"), (IReference reference, FilePath sourceFile) -> sourceFile.value());
		compo.scan();
		PageDocument page = new PageDocument(compo);

		buildFS.writePage(compo, page.getDocument());
		assertTrue(exists("htm/index-004.htm"));
	}

	@Test
	public void writeFavicon() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);

		assertFalse(exists("favicon.ico"));
		assertThat(buildFS.writeFavicon(null, new FilePath(project, "res/asset/favicon.ico")), equalTo("../img/favicon.ico"));
		assertTrue(exists("img/favicon.ico"));
	}

	@Test
	public void writePageMedia() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writePageMedia(null, mediaFile), equalTo("../img/background.jpg"));
		assertTrue(exists("img/background.jpg"));
	}

	@Test
	public void writePageMedia_Locale() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		buildFS.setLocale(new Locale("ro"));
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writePageMedia(null, mediaFile), equalTo("../img/background.jpg"));
		assertTrue(exists("ro/img/background.jpg"));
	}

	@Test
	public void writePageMedia_BuildNumber() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 4);
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writePageMedia(null, mediaFile), equalTo("../img/background-004.jpg"));
		assertTrue(exists("img/background-004.jpg"));
	}

	@Test
	public void writeStyleMedia() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writeStyleMedia(mediaFile), equalTo("../img/background.jpg"));
		assertTrue(exists("img/background.jpg"));
	}

	@Test
	public void writeStyleMedia_Locale() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		buildFS.setLocale(new Locale("ro"));
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writeStyleMedia(mediaFile), equalTo("../img/background.jpg"));
		assertTrue(exists("ro/img/background.jpg"));
	}

	@Test
	public void writeStyleMedia_BuildNumber() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 4);
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writeStyleMedia(mediaFile), equalTo("../img/background-004.jpg"));
		assertTrue(exists("img/background-004.jpg"));
	}

	@Test
	public void writeScriptMedia() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writeScriptMedia(project.getName(), mediaFile), equalTo("/project/img/background.jpg"));
		assertTrue(exists("img/background.jpg"));
	}

	@Test
	public void writeScriptMedia_Locale() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		buildFS.setLocale(new Locale("ro"));
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writeScriptMedia(project.getName(), mediaFile), equalTo("/project/ro/img/background.jpg"));
		assertTrue(exists("ro/img/background.jpg"));
	}

	@Test
	public void writeScriptMedia_BuildNumber() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 4);
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writeScriptMedia(project.getName(), mediaFile), equalTo("/project/img/background-004.jpg"));
		assertTrue(exists("img/background-004.jpg"));
	}

	@Test
	public void writeStyle() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		FilePath styleFile = new FilePath(project, "res/theme/style.css");

		assertThat(buildFS.writeStyle(null, styleFile, nullReferenceHandler()), equalTo("../css/style.css"));
		assertTrue(exists("css/style.css"));
	}

	@Test
	public void writeStyle_Locale() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		buildFS.setLocale(new Locale("ro"));
		FilePath styleFile = new FilePath(project, "res/theme/style.css");

		assertThat(buildFS.writeStyle(null, styleFile, nullReferenceHandler()), equalTo("../css/style.css"));
		assertTrue(exists("ro/css/style.css"));
	}

	@Test
	public void writeStyle_BuildNumber() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 4);
		FilePath styleFile = new FilePath(project, "res/theme/style.css");

		assertThat(buildFS.writeStyle(null, styleFile, nullReferenceHandler()), equalTo("../css/style-004.css"));
		assertTrue(exists("css/style-004.css"));
	}

	@Test
	public void writeScript() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		FilePath scriptFile = project.getFile("script/hc/page/Index.js");

		assertThat(buildFS.writeScript(null, scriptFile, nullReferenceHandler()), equalTo("../js/Index.js"));
		assertTrue(exists("js/Index.js"));
	}

	@Test
	public void writeScript_Locale() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 0);
		buildFS.setLocale(new Locale("ro"));
		FilePath scriptFile = project.getFile("script/hc/page/Index.js");

		assertThat(buildFS.writeScript(null, scriptFile, nullReferenceHandler()), equalTo("../js/Index.js"));
		assertTrue(exists("ro/js/Index.js"));
	}

	@Test
	public void writeScript_BuildNumber() throws IOException {
		BuildFS buildFS = new TestBuildFS(project, 4);
		FilePath scriptFile = project.getFile("script/hc/page/Index.js");

		assertThat(buildFS.writeScript(null, scriptFile, nullReferenceHandler()), equalTo("../js/Index-004.js"));
		assertTrue(exists("js/Index-004.js"));
	}

	@Test
	public void dirFactory() throws Exception {
		BuildFS buildFS = new TestBuildFS(project, 0);
		assertThat(buildFS.createDirectory("images"), isDir("src/test/resources/project/build/site/images"));
		buildFS.setLocale(new Locale("ro"));
		assertThat(buildFS.createDirectory("images"), isDir("src/test/resources/project/build/site/ro/images"));
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

	@Test(expected = IllegalArgumentException.class)
	public void nullLocale() {
		BuildFS buildFS = new TestBuildFS(project, 0);
		buildFS.setLocale(null);
	}

	private boolean exists(String fileName) {
		File buildDir = new File(project.getProjectRoot(), CT.DEF_BUILD_DIR);
		return new File(buildDir, fileName).exists();
	}

	private static class TestBuildFS extends BuildFS {
		public TestBuildFS(BuilderProject project, int buildNumber) {
			super(new File(project.getProjectRoot(), CT.DEF_BUILD_DIR), buildNumber);
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

	private static IReferenceHandler nullReferenceHandler() {
		return new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourceFile) throws IOException {
				return "null";
			}

			@Override
			public String toString() {
				return "null reference handler";
			}
		};
	}
}
