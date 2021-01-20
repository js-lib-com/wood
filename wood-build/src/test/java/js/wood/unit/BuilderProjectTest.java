package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.HashSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.wood.BuilderProject;
import js.wood.BuilderTestCase;
import js.wood.CT;
import js.wood.CompoPath;
import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.IReference;
import js.wood.IReferenceHandler;
import js.wood.IVariables;
import js.wood.LayoutFile;
import js.wood.impl.ComponentDescriptor;
import js.wood.impl.FilesHandler;
import js.wood.impl.Reference;
import js.wood.impl.ResourceType;
import js.wood.impl.Variables;

public class BuilderProjectTest extends BuilderTestCase implements IReferenceHandler {
	private BuilderProject project;

	@Before
	public void beforeTest() {
		project = project("scripts");
	}

	@Test
	public void initialization() throws FileNotFoundException {
		project = project("project");
		assertEquals(new File(project.getProjectDir(), CT.DEF_SITE_DIR), project.getSiteDir());
		assertEquals("build/site/", project.getSitePath());
		assertTrue(project.getLayouts().isEmpty());
	}

	@Test
	public void layoutsScanner() throws Exception {
		project = project("project");
		FilesHandler handler = Classes.newInstance("js.wood.BuilderProject$LayoutsScanner", project);

		for (String file : new String[] { "res/page/index/strings.xml", //
				"res/page/index/index.htm", //
				"res/page/index/fake.hml", //
				"res/template/page/logo.xml", //
				"res/template/page/page.htm", //
				"res/template/page/page.css" }) {
			handler.onFile(filePath(file));
		}

		Set<LayoutFile> layouts = project.getLayouts();
		assertEquals(2, layouts.size());
		assertTrue(layouts.contains(new LayoutFile(project, filePath("res/page/index/index.htm"))));
		assertTrue(layouts.contains(new LayoutFile(project, filePath("res/template/page/page.htm"))));
	}

	@Test
	public void variablesScanner() throws Exception {
		project = project("project");
		FilesHandler handler = Classes.newInstance("js.wood.BuilderProject$VariablesScanner", project);

		for (String file : new String[] { "res/page/index/strings.xml", //
				"res/page/index/strings_de.xml", //
				"res/page/index/index.css", //
				"res/asset/colors.xml", //
				"res/asset/favicon.ico" }) {
			FilePath filePath = filePath(file);
			handler.onDirectory(filePath.getDirPath());
			handler.onFile(filePath);
		}

		Map<DirPath, IVariables> projectVariables = project.getVariables();
		assertFalse(projectVariables.isEmpty());

		assertNotNull(projectVariables.get(dirPath("res/asset")));
		assertNotNull(projectVariables.get(dirPath("res/theme")));

		IVariables dirVariables = projectVariables.get(dirPath("res/page/index"));
		assertNotNull(dirVariables);
		assertNotNull(field(dirVariables, "assetVariables"));

		FilePath source = filePath("res/page/index/index.htm");
		assertEquals("Index Page", variable(dirVariables, source, "en", ResourceType.STRING, "title"));
		assertEquals("Indexseite", variable(dirVariables, source, "de", ResourceType.STRING, "title"));
		// if a locale has not a variable uses default locale, in this case 'en'
		assertEquals("Index Page", variable(dirVariables, source, "jp", ResourceType.STRING, "title"));
		assertEquals("#000000", variable(dirVariables, source, "en", ResourceType.COLOR, "page-header-bg"));
	}

	private String variable(IVariables variables, FilePath source, String language, ResourceType type, String name) {
		return variables.get(new Locale(language), new Reference(source, type, name), source, this);
	}

	@Test
	public void emptyVariablesScanner() throws Exception {
		project = project("project");
		FilesHandler handler = Classes.newInstance("js.wood.BuilderProject$VariablesScanner", project);

		DirPath dir = dirPath("res/template/sidebar");
		handler.onDirectory(dir);

		Map<DirPath, IVariables> projectVariables = project.getVariables();
		assertFalse(projectVariables.isEmpty());

		IVariables dirVariables = projectVariables.get(dir);
		assertNotNull(dirVariables);
		assertNotNull(field(dirVariables, "assetVariables"));

		Map<?, ?> languageValues = field(dirVariables, "localeValues");
		assertTrue(languageValues.isEmpty());
	}

	@Test
	public void pagesDiscovery() throws IOException {
		project = project("project");
		project.scanBuildFiles();

		final Set<LayoutFile> expected = new HashSet<LayoutFile>();
		expected.add(new LayoutFile(project, filePath("res/page/index/index.htm")));
		expected.add(new LayoutFile(project, filePath("res/page/video-player/video-player.htm")));
		expected.add(new LayoutFile(project, filePath("res/page/videos/videos.htm")));

		final Set<LayoutFile> found = new HashSet<>();
		for (LayoutFile layoutFile : project.getLayouts()) {
			if (layoutFile.isPage()) {
				found.add(layoutFile);
			}
		}

		assertTrue(expected.equals(found));
	}

	public void descriptorValues() {
		project.scanBuildFiles();
		CompoPath compoPath = new CompoPath(project, "page/index");

		final IVariables variables = project.getVariables().get(compoPath);
		ComponentDescriptor descriptor = new ComponentDescriptor(project.getFile("res/page/index/index.xml"), new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourceFile) throws IOException {
				return variables.get(new Locale("en"), reference, sourceFile, this);
			}
		});

		assertEquals("Index Page", descriptor.getTitle(null));
		assertEquals("Index page description.", descriptor.getDescription(null));
	}

	@Override
	public String onResourceReference(IReference reference, FilePath sourcePath) throws IOException {
		Variables variables = new Variables(project, sourcePath.getDirPath());
		if (project.getAssetsDir().exists()) {
			invoke(variables, "load", project.getAssetsDir());
		}
		return variables.get(null, reference, sourcePath, this);
	}

	private DirPath dirPath(String path) {
		return new DirPath(project, path);
	}

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}
}
