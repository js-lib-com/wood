package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.TreeSet;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.wood.CT;
import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.IReference;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.WoodException;
import js.wood.impl.FilesHandler;
import js.wood.impl.Reference;
import js.wood.impl.ResourceType;
import js.wood.impl.ScriptFile;
import js.wood.impl.Variables;

@SuppressWarnings({ "rawtypes", "unchecked" })
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
		assertNotNull(project.getConfig());

		assertEquals(CT.ASSETS_DIR + "/", project.getAssetsDir().toString());
		assertEquals(new File(project.getProjectDir(), CT.DEF_SITE_DIR), project.getSiteDir());
		assertEquals("build/site/", project.getSitePath());
		assertEquals("res/asset/favicon.ico", project.getFavicon().toString());

		assertEquals("project", project.getName());
		assertEquals("Test Project", project.getDisplay());
		assertEquals("Project used as fixture for unit testing.", project.getDescription());

		assertTrue(project.getThemeStyles().isEmpty());
		assertEquals(4, project.getLocales().size());
	}

	@Test
	public void scriptsScanner() throws Exception {
		project = project("project");
		FilesHandler handler = Classes.newInstance("js.wood.Project$ScriptsScanner", project);

		for (String file : new String[] { "res/page/index/strings.xml", //
				"script/hc/format/ReleasedDate.js", //
				"script/js/compo/Dialog.js", //
				"res/template/page/page.css" }) {
			handler.onFile(filePath(file));
		}

		Map<FilePath, ScriptFile> scripts = field(project, "scripts");
		assertEquals(2, scripts.size());
		assertTrue(scripts.containsValue(new ScriptFile(project, filePath("script/hc/format/ReleasedDate.js"))));
		assertTrue(scripts.containsValue(new ScriptFile(project, filePath("script/js/compo/Dialog.js"))));
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
		assertEquals(filePath("res/theme/reset.css"), styles.get(0));
		assertEquals(filePath("res/theme/fx.css"), styles.get(1));
		assertTrue(styles.contains(filePath("res/theme/form.css")));
		assertTrue(styles.contains(filePath("res/theme/style.css")));
	}

	@Override
	public String onResourceReference(IReference reference, FilePath sourcePath) throws IOException {
		Variables variables = new Variables(project, sourcePath.getDirPath());
		if (project.getAssetsDir().exists()) {
			invoke(variables, "load", project.getAssetsDir());
		}
		return variables.get(null, reference, sourcePath, this);
	}

	public void getScriptFiles() throws Throwable {
		project = project("scripts");

		Set scriptClasses = new TreeSet();
		scriptClasses.add("js.widget.Box");
		scriptClasses.add("js.ua.System");
		scriptClasses.add("js.format.DateFormat");
		scriptClasses.add("js.widget.Paging");

		project.previewScriptFiles();
		Collection<ScriptFile> scriptFiles = project.getScriptFiles(scriptClasses);

		ScriptFile[] expected = new ScriptFile[] { new ScriptFile(project, filePath("script/js/widget/Description.js")), //
				new ScriptFile(project, filePath("gen/js/controller/MainController.js")), //
				new ScriptFile(project, filePath("lib/js-lib/js-lib.js")) };

		assertEquals(expected.length, scriptFiles.size());
		for (int i = 0; i < expected.length; ++i) {
			assertTrue(scriptFiles.contains(expected[i]));
		}
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
	public void previewScriptFiles() {
		project = project("scripts");

		Collection<ScriptFile> scripts = project.previewScriptFiles();
		assertEquals(8, scripts.size());

		assertScripts(scripts, "script/js/format/RichText.js");
		assertScripts(scripts, "script/js/widget/Description.js");
		assertScripts(scripts, "lib/js-lib/js-lib.js");
		assertScripts(scripts, "lib/sdk/analytics.js");
		assertScripts(scripts, "lib/google-maps-api.js");
		assertScripts(scripts, "script/js/wood/IndexPage.js");
		assertScripts(scripts, "script/js/wood/GeoMap.js");
		assertScripts(scripts, "gen/js/controller/MainController.js");
	}

	private void assertScripts(Collection<ScriptFile> scripts, String expected) {
		assertTrue(scripts.contains(new ScriptFile(project, filePath(expected))));
	}

	@Test
	public void scanScriptFiles() throws Throwable {
		project = project("scripts");

		Classes.invoke(project, "scanScriptFiles", dirPath("script"));
		Map<FilePath, ScriptFile> scriptFiles = Classes.getFieldValue(project, "scripts");

		List<ScriptFile> scripts = new ArrayList<ScriptFile>(scriptFiles.values());
		Collections.sort(scripts, new Comparator<ScriptFile>() {
			@Override
			public int compare(ScriptFile o1, ScriptFile o2) {
				return o1.getSourceFile().value().compareTo(o2.getSourceFile().value());
			}
		});

		assertEquals(4, scripts.size());

		int index = 0;
		assertEquals("script/js/format/RichText.js", scripts.get(index++).toString());
		assertEquals("script/js/widget/Description.js", scripts.get(index++).toString());
		assertEquals("script/js/wood/GeoMap.js", scripts.get(index++).toString());
		assertEquals("script/js/wood/IndexPage.js", scripts.get(index++).toString());
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

	@Test
	public void missingScriptFiles() throws Throwable {
		project = project("scripts");
		Set scriptClasses = new HashSet();
		scriptClasses.add("comp.prj.FakeClass");

		try {
			project.getScriptFiles(scriptClasses);
			fail("Missing script file for class should rise exception");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
		}
	}
}
