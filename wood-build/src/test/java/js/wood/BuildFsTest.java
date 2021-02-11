package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.wood.BuildFS;
import js.wood.BuilderProject;
import js.wood.Component;
import js.wood.FilePath;
import js.wood.IReference;
import js.wood.IReferenceHandler;
import js.wood.Project;

public class BuildFsTest {
	private BuilderProject project;

	@Before
	public void beforeTest() throws Exception {
		project = new BuilderProject(new File("src/test/resources/project"));
	}

	@Test
	public void writePage() throws IOException {
		// resetProjectLocales(project);
		// BuildFS buildFS = new TestBuildFS(project);
		// PageDocument page = newInstance(PageDocument.class);
		//
		// buildFS.writePage(null, page, "index.htm");
		// assertTrue(exists("htm/index.htm"));
		// assertFalse(exists("en/htm/index.htm"));
		//
		// buildFS.setLanguage("ro");
		// buildFS.writePage(page, "index.htm");
		// assertTrue(exists("ro/htm/index.htm"));
		//
		// buildFS.setBuildNumber(4);
		// buildFS.setLanguage(null);
		//
		// buildFS.writePage(page, "index.htm");
		// assertTrue(exists("htm/index-004.htm"));
		// assertFalse(exists("en/htm/index-004.htm"));
		//
		// buildFS.setLanguage("ro");
		// buildFS.writePage(page, "index.htm");
		// assertTrue(exists("ro/htm/index-004.htm"));
	}

	@Test
	public void writeFavicon() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project, 0);

		assertFalse(exists("favicon.ico"));
		assertThat(buildFS.writeFavicon(null, new FilePath(project, "res/asset/favicon.ico")), equalTo("../img/favicon.ico"));
		assertTrue(exists("img/favicon.ico"));
	}

	@Test
	public void writeMedia() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project, 0);
		buildFS.setLocale(new Locale("en"));
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writePageMedia(null, mediaFile), equalTo("../img/background.jpg"));
		assertThat(buildFS.writeStyleMedia(mediaFile), equalTo("../img/background.jpg"));
		assertThat(buildFS.writeScriptMedia(mediaFile), equalTo("/project/img/background.jpg"));
		assertTrue(exists("img/background.jpg"));
	}


	@Test
	public void writeMedia_BuildNumber() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project, 4);
		FilePath mediaFile = new FilePath(project, "res/asset/background.jpg");

		assertThat(buildFS.writePageMedia(null, mediaFile), equalTo("../img/background-004.jpg"));
		assertThat(buildFS.writeStyleMedia(mediaFile), equalTo("../img/background-004.jpg"));
		assertThat(buildFS.writeScriptMedia(mediaFile), equalTo("/project/img/background-004.jpg"));
		assertTrue(exists("img/background-004.jpg"));
	}
	
	@Test
	public void writeStyle() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project, 0);
		FilePath styleFile = new FilePath(project, "res/theme/style.css");

		assertThat(buildFS.writeStyle(null, styleFile, nullReferenceHandler()), equalTo("../css/style.css"));
		assertTrue(exists("css/style.css"));
	}
	
	@Test
	public void writeStyle_BuildNumber() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project, 4);
		FilePath styleFile = new FilePath(project, "res/theme/style.css");

		assertThat(buildFS.writeStyle(null, styleFile, nullReferenceHandler()), equalTo("../css/style-004.css"));
		assertTrue(exists("css/style-004.css"));
	}

	@Test
	public void writeScript() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project, 0);
		FilePath scriptFile = project.getFile("script/hc/page/Index.js");

		assertThat(buildFS.writeScript(null, scriptFile, nullReferenceHandler()), equalTo("../js/Index.js"));
		assertTrue(exists("js/Index.js"));
	}

	@Test
	public void writeScript_BuildNumber() throws IOException {
		resetProjectLocales(project);
		BuildFS buildFS = new TestBuildFS(project, 4);
		FilePath scriptFile = project.getFile("script/hc/page/Index.js");

		assertThat(buildFS.writeScript(null, scriptFile, nullReferenceHandler()), equalTo("../js/Index-004.js"));
		assertTrue(exists("js/Index-004.js"));
	}

	@Test
	public void dirFactory() throws Exception {
		BuildFS buildFS = new TestBuildFS(project, 0);

		buildFS.setLocale(new Locale("en"));
		assertDir("src/test/resources/project/build/site/en/images", buildFS, "images");
		buildFS.setLocale(new Locale("ro"));
		assertDir("src/test/resources/project/build/site/ro/images", buildFS, "images");
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullLocale() {
		BuildFS buildFS = new TestBuildFS(project, 0);
		resetProjectLocales(project);
		buildFS.setLocale(null);
	}

	private boolean exists(String fileName) {
		return new File(project.getSiteDir(), fileName).exists();
	}

	private static class TestBuildFS extends BuildFS {
		public TestBuildFS(BuilderProject project, int buildNumber) {
			super(project, buildNumber);
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

	private static void assertDir(String expected, BuildFS buildFS, String dirName) throws Exception {
		assertThat(Classes.invoke(buildFS, BuildFS.class, "createDirectory", dirName).toString().replace('\\', '/'), equalTo(expected));
	}

	private static void resetProjectLocales(Project project) {
		List<Locale> locales = new ArrayList<Locale>();
		locales.add(new Locale("en"));
		Classes.setFieldValue(Classes.getFieldValue(project, Project.class, "descriptor"), "locales", locales);
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
