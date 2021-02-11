package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.wood.CT;
import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.IReference;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.impl.FilesHandler;
import js.wood.impl.Reference;
import js.wood.impl.ResourceType;
import js.wood.impl.Variables;

public class ProjectTest implements IReferenceHandler {
	private Project project;

	@Before
	public void beforeTest() throws Exception {
		project = new Project(new File("src/test/resources/project"));
	}

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}

	private DirPath dirPath(String path) {
		return new DirPath(project, path);
	}

	@Test
	public void initialization() throws FileNotFoundException {
		project = new Project(new File("src/test/resources/project"));

		assertThat(project.getProjectDir(), equalTo(new File("src/test/resources/project")));
		assertThat(Classes.getFieldValue(project, "descriptor"), notNullValue());

		assertThat(project.getAssetsDir().toString(), equalTo(CT.ASSETS_DIR + "/"));
		assertThat(project.getFavicon().toString(), equalTo("res/asset/favicon.ico"));

		assertThat(project.getName(), equalTo("project"));
		assertThat(project.getDisplay(), equalTo("Test Project"));
		assertThat(project.getDescription(), equalTo("Project used as fixture for unit testing."));

		assertTrue(project.getThemeStyles().isEmpty());
		assertThat(project.getLocales(), hasSize(4));
	}

	@Test
	public void themeStylesScanner() throws Exception {
		project = new Project(new File("src/test/resources/project"));
		FilesHandler handler = Classes.newInstance("js.wood.Project$ThemeStylesScanner", project);

		for (String file : new String[] { "res/page/index/index.css", //
				"res/theme/form.css", //
				"res/theme/style.css", //
				"res/theme/reset.css", //
				"res/theme/fx.css", //
				"res/theme/background.jpg" }) {
			handler.onFile(filePath(file));
		}

		List<FilePath> styles = project.getThemeStyles();
		assertThat(styles, hasSize(4));
		assertTrue(styles.contains(filePath("res/theme/reset.css")));
		assertTrue(styles.contains(filePath("res/theme/fx.css")));
		assertTrue(styles.contains(filePath("res/theme/form.css")));
		assertTrue(styles.contains(filePath("res/theme/style.css")));
	}

	@Override
	public String onResourceReference(IReference reference, FilePath sourcePath) throws IOException {
		Variables variables = new Variables(sourcePath.getDirPath());
		if (project.getAssetsDir().exists()) {
			try {
				Classes.invoke(variables, "load", sourcePath.getProject().getAssetsDir());
			} catch (Exception e) {
				throw new IllegalStateException(e);
			}
		}
		return variables.get(null, reference, sourcePath, this);
	}

	@Test
	public void getMediaFile() throws FileNotFoundException {
		project = new Project(new File("src/test/resources/project"));

		FilePath source = filePath("res/template/page/page.htm");
		Reference reference = new Reference(source, ResourceType.IMAGE, "logo");

		assertMedia("res/template/page/logo.jpg", null, reference, source);
		assertMedia("res/template/page/logo.jpg", "en", reference, source);
		assertMedia("res/template/page/logo.jpg", "jp", reference, source);
		assertMedia("res/template/page/logo_de.jpg", "de", reference, source);
		assertMedia("res/template/page/logo_fr.jpg", "fr", reference, source);
		assertMedia("res/template/page/logo_ro.jpg", "ro", reference, source);
	}

	@Test
	public void getMediaFileFromSubdirectory() throws FileNotFoundException {
		project = new Project(new File("src/test/resources/project"));
		FilePath source = filePath("res/template/page/page.htm");
		Reference reference = new Reference(source, ResourceType.IMAGE, "icon/logo");
		assertMedia("res/template/page/icon/logo.png", null, reference, source);
	}

	@Test
	public void getMediaFileWithCompoName() {
		project = new Project(new File("src/test/resources/project"));
		FilePath source = filePath("res/template/page/page.htm");
		Reference reference = new Reference(source, ResourceType.IMAGE, "page");
		assertMedia("res/template/page/page.jpg", null, reference, source);
	}

	private void assertMedia(String expected, String language, Reference reference, FilePath source) {
		assertThat(project.getMediaFile(language != null ? new Locale(language) : null, reference, source).value(), equalTo(expected));
	}

	@Test
	public void isExcluded() {
		project = new Project(new File("src/test/resources/project"));
		assertTrue(project.isExcluded(dirPath("res/page/about")));
		assertFalse(project.isExcluded(dirPath("res/page/index")));
	}

	// ------------------------------------------------------
	// Exceptions

	@Test
	public void badConstructor() {
		try {
			new Project(new File("src/test/resources/fake-project"));
			fail("Bad directory should rise illegal argument exception.");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}
}
