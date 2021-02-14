package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
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
import js.util.Files;
import js.wood.BuilderProject;
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

public class BuilderProjectTest implements IReferenceHandler {
	private BuilderProject project;

	@Before
	public void beforeTest() throws IOException {
		project = new BuilderProject(new File("src/test/resources/project"));
		File buildDir = new File(project.getProjectDir(), CT.DEF_BUILD_DIR);
		if (buildDir.exists()) {
			Files.removeFilesHierarchy(buildDir);
		}
	}

	@Test
	public void initialization() throws FileNotFoundException {
		assertTrue(project.getLayoutFiles().isEmpty());
	}

	@Test
	public void layoutsScanner() throws Exception {
		FilesHandler handler = Classes.newInstance("js.wood.BuilderProject$LayoutsScanner", project);

		for (String file : new String[] { "res/page/index/strings.xml", //
				"res/page/index/index.htm", //
				"res/page/index/fake.hml", //
				"res/template/page/logo.xml", //
				"res/template/page/page.htm", //
				"res/template/page/page.css" }) {
			handler.onFile(filePath(file));
		}

		Set<LayoutFile> layouts = project.getLayoutFiles();
		assertThat(layouts, hasSize(2));
		assertTrue(layouts.contains(new LayoutFile(project, filePath("res/page/index/index.htm"))));
		assertTrue(layouts.contains(new LayoutFile(project, filePath("res/template/page/page.htm"))));
	}

	@Test
	public void variablesScanner() throws Exception {
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

		assertThat(projectVariables.get(dirPath("res/asset")), notNullValue());
		assertThat(projectVariables.get(dirPath("res/theme")), notNullValue());

		IVariables dirVariables = projectVariables.get(dirPath("res/page/index"));
		assertThat(dirVariables, notNullValue());

		FilePath source = filePath("res/page/index/index.htm");
		assertThat(variable(dirVariables, source, "en", ResourceType.STRING, "title"), equalTo("Index Page"));
		assertThat(variable(dirVariables, source, "de", ResourceType.STRING, "title"), equalTo("Indexseite"));
		// if a locale has not a variable uses default locale, in this case 'en'
		assertThat(variable(dirVariables, source, "jp", ResourceType.STRING, "title"), equalTo("Index Page"));
		assertThat(variable(dirVariables, source, "en", ResourceType.COLOR, "page-header-bg"), equalTo("#000000"));
	}

	private String variable(IVariables variables, FilePath source, String language, ResourceType type, String name) {
		return variables.get(new Locale(language), new Reference(source, type, name), source, this);
	}

	@Test
	public void emptyVariablesScanner() throws Exception {
		FilesHandler handler = Classes.newInstance("js.wood.BuilderProject$VariablesScanner", project);

		DirPath dir = dirPath("res/template/sidebar");
		handler.onDirectory(dir);

		Map<DirPath, IVariables> projectVariables = project.getVariables();
		assertFalse(projectVariables.isEmpty());

		IVariables dirVariables = projectVariables.get(dir);
		assertThat(dirVariables, notNullValue());
	}

	@Test
	public void pagesDiscovery() throws IOException {
		project.scan();

		final Set<LayoutFile> expected = new HashSet<LayoutFile>();
		expected.add(new LayoutFile(project, filePath("res/page/index/index.htm")));
		expected.add(new LayoutFile(project, filePath("res/page/video-player/video-player.htm")));
		expected.add(new LayoutFile(project, filePath("res/page/videos/videos.htm")));

		final Set<LayoutFile> found = new HashSet<>();
		for (LayoutFile layoutFile : project.getLayoutFiles()) {
			if (layoutFile.isPage()) {
				found.add(layoutFile);
			}
		}

		assertTrue(expected.equals(found));
	}

	@Test
	public void descriptorValues() {
		project = new BuilderProject(new File("src/test/resources/scripts"));
		project.scan();
		CompoPath compoPath = new CompoPath(project, "index");

		final IVariables variables = project.getVariables().get(compoPath);
		ComponentDescriptor descriptor = new ComponentDescriptor(project.getFile("res/index/index.xml"), new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourceFile) throws IOException {
				return variables.get(new Locale("en"), reference, sourceFile, this);
			}
		});

		assertThat(descriptor.getDisplay(null), equalTo("Index Page"));
		assertThat(descriptor.getDescription(null), equalTo("Index page description."));
	}

	@Test
	public void isExcluded() {
		project = new BuilderProject(new File("src/test/resources/project"));
		assertTrue(project.isExcluded(new CompoPath(project, "page/about")));
		assertTrue(project.isExcluded(new DirPath(project, "res/page/about")));
		assertTrue(project.isExcluded(new FilePath(project, "res/page/about/about.htm")));
		assertFalse(project.isExcluded(new CompoPath(project, "page/index")));
	}

	@Test
	public void isExcluded_RootContext() {
		project = new BuilderProject(new File("src/test/resources/root-project"));
		assertTrue(project.isExcluded(new CompoPath(project, "page/about")));
		assertTrue(project.isExcluded(new DirPath(project, "res/page/about")));
		assertTrue(project.isExcluded(new FilePath(project, "res/page/about/about.htm")));
		assertFalse(project.isExcluded(new CompoPath(project, "page/index")));
	}

	@Override
	public String onResourceReference(IReference reference, FilePath sourcePath) throws IOException {
		Variables variables = new Variables(sourcePath.getDirPath());
		if (project.getAssetsDir().exists()) {
			try {
				Classes.invoke(variables, "load", project.getAssetsDir());
			} catch (Exception e) {
				e.printStackTrace();
			}
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
