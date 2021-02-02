package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
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

public class ProjectTest extends WoodTestCase implements IReferenceHandler {
	private Project project;

	@Before
	public void beforeTest() throws Exception {
		project = project("project");
	}

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}

	private DirPath dirPath(String path) {
		return new DirPath(project, path);
	}

	@Test
	public void initialization() throws FileNotFoundException {
		project = project("project");

		assertEquals(new File("src/test/resources/project"), project.getProjectDir());
		assertNotNull(field(project, "descriptor"));

		assertEquals(CT.ASSETS_DIR + "/", project.getAssetsDir().toString());
		assertEquals("res/asset/favicon.ico", project.getFavicon().toString());

		assertEquals("project", project.getName());
		assertEquals("Test Project", project.getDisplay());
		assertEquals("Project used as fixture for unit testing.", project.getDescription());

		assertTrue(project.getThemeStyles().isEmpty());
		assertEquals(4, project.getLocales().size());
	}

	@Test
	public void themeStylesScanner() throws Exception {
		project = project("project");
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
		assertEquals(4, styles.size());
		assertTrue(styles.contains(filePath("res/theme/reset.css")));
		assertTrue(styles.contains(filePath("res/theme/fx.css")));
		assertTrue(styles.contains(filePath("res/theme/form.css")));
		assertTrue(styles.contains(filePath("res/theme/style.css")));
	}

	@Override
	public String onResourceReference(IReference reference, FilePath sourcePath) throws IOException {
		Variables variables = new Variables(sourcePath.getDirPath());
		if (project.getAssetsDir().exists()) {
			invoke(variables, "load", project.getAssetsDir());
		}
		return variables.get(null, reference, sourcePath, this);
	}

	@Test
	public void getMediaFile() throws FileNotFoundException {
		project = project("project");

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
		project = project("project");
		FilePath source = filePath("res/template/page/page.htm");
		Reference reference = new Reference(source, ResourceType.IMAGE, "icon/logo");
		assertMedia("res/template/page/icon/logo.png", null, reference, source);
	}

	@Test
	public void getMediaFileWithCompoName() {
		project = project("project");
		FilePath source = filePath("res/template/page/page.htm");
		Reference reference = new Reference(source, ResourceType.IMAGE, "page");
		assertMedia("res/template/page/page.jpg", null, reference, source);
	}

	private void assertMedia(String expected, String language, Reference reference, FilePath source) {
		assertEquals(expected, project.getMediaFile(language != null ? new Locale(language) : null, reference, source).value());
	}

	@Test
	public void isExcluded() {
		project = project("project");
		assertTrue(project.isExcluded(dirPath("res/page/about")));
		assertFalse(project.isExcluded(dirPath("res/page/index")));
	}

	// ------------------------------------------------------
	// Exceptions

	@Test
	public void badConstructor() {
		try {
			project("fake-project");
			fail("Bad directory should rise illegal argument exception.");
		} catch (Exception e) {
			assertTrue(e instanceof IllegalArgumentException);
		}
	}
}
