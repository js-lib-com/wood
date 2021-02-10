package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.util.Files;
import js.wood.BuilderProject;
import js.wood.CompoPath;
import js.wood.FilePath;
import js.wood.LayoutFile;
import js.wood.WoodException;

public class LayoutFileTest {
	private BuilderProject project;

	@Before
	public void beforeTest() throws Exception {
		project = new BuilderProject("src/test/resources/layout-file");
		if (project.getSiteDir().exists()) {
			Files.removeFilesHierarchy(project.getSiteDir());
		}
	}

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}

	@Test
	public void content() {
		LayoutFile layoutFile = new LayoutFile(project, filePath("res/page/index/index.htm"));

		assertThat(layoutFile.getCompoPath().value(), equalTo("res/page/index/"));
		assertFalse((boolean) Classes.getFieldValue(layoutFile, "hasBody"));

		Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
		assertTrue(editables.isEmpty());

		Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
		assertThat(templates, hasSize(1));
		assertThat(templates.iterator().next(), equalTo("page-body"));

		CompoPath templatePath = Classes.getFieldValue(layoutFile, "templatePath");
		assertThat(templatePath.value(), equalTo("res/template/sidebar-page/"));
	}

	@Test
	public void template() {
		LayoutFile layoutFile = new LayoutFile(project, filePath("res/template/sidebar-page/sidebar-page.htm"));

		assertThat(layoutFile.getCompoPath().value(), equalTo("res/template/sidebar-page/"));
		assertFalse((boolean) Classes.getFieldValue(layoutFile, "hasBody"));

		Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
		assertThat(editables, hasSize(1));
		assertThat(editables.iterator().next(), equalTo("sidebar"));

		Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
		assertThat(templates, hasSize(1));
		assertThat(templates.iterator().next(), equalTo("page-body"));

		CompoPath templatePath = Classes.getFieldValue(layoutFile, "templatePath");
		assertThat(templatePath.value(), equalTo("res/template/page/"));
	}

	@Test
	public void pageTemplate() {
		LayoutFile layoutFile = new LayoutFile(project, filePath("res/template/page/page.htm"));

		assertThat(layoutFile.getCompoPath().value(), equalTo("res/template/page/"));
		assertTrue((boolean) Classes.getFieldValue(layoutFile, "hasBody"));

		Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
		assertThat(editables, hasSize(1));
		assertThat(editables.iterator().next(), equalTo("page-body"));

		Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
		assertTrue(templates.isEmpty());

		CompoPath templatePath = Classes.getFieldValue(layoutFile, "templatePath");
		assertThat(templatePath, nullValue());
	}

	@Test
	public void widget() {
		LayoutFile layoutFile = new LayoutFile(project, filePath("res/compo/widget/widget.htm"));

		assertThat(layoutFile.getCompoPath().value(), equalTo("res/compo/widget/"));
		assertFalse((boolean) Classes.getFieldValue(layoutFile, "hasBody"));

		Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
		assertTrue(editables.isEmpty());

		Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
		assertTrue(templates.isEmpty());

		assertThat(Classes.getFieldValue(layoutFile, "templatePath"), nullValue());
	}

	@Test
	public void singleIndex() {
		LayoutFile layoutFile = new LayoutFile(project, filePath("res/page/single-index/single-index.htm"));

		assertThat(layoutFile.getCompoPath().value(), equalTo("res/page/single-index/"));
		assertFalse((boolean) Classes.getFieldValue(layoutFile, "hasBody"));

		Set<String> editables = Classes.getFieldValue(layoutFile, "editables");
		assertTrue(editables.isEmpty());

		Set<String> templates = Classes.getFieldValue(layoutFile, "templates");
		assertThat(templates, hasSize(1));
		assertThat(templates.iterator().next(), equalTo("page-body"));

		CompoPath templatePath = Classes.getFieldValue(layoutFile, "templatePath");
		assertThat(templatePath.value(), equalTo("res/template/page/"));
	}

	@Test
	public void contentIsPage() {
		layoutFiles(project, "res/template/sidebar-page/sidebar-page.htm", "res/template/page/page.htm");

		LayoutFile layoutFile = new LayoutFile(project, filePath("res/page/index/index.htm"));
		assertThat(Classes.getFieldValue(layoutFile, "isPage"), nullValue());
		assertTrue(layoutFile.isPage());
	}

	@Test
	public void templateIsPage() {
		layoutFiles(project, "res/template/page/page.htm");

		LayoutFile layoutFile = new LayoutFile(project, filePath("res/template/sidebar-page/sidebar-page.htm"));
		assertThat(Classes.getFieldValue(layoutFile, "isPage"), nullValue());
		assertFalse(layoutFile.isPage());
	}

	@Test
	public void pageTemplateIsPage() {
		layoutFiles(project);

		LayoutFile layoutFile = new LayoutFile(project, filePath("res/template/page/page.htm"));
		assertThat(Classes.getFieldValue(layoutFile, "isPage"), nullValue());
		assertFalse(layoutFile.isPage());
	}

	@Test
	public void widgetIsPage() {
		layoutFiles(project);

		LayoutFile layoutFile = new LayoutFile(project, filePath("res/compo/widget/widget.htm"));
		assertFalse(layoutFile.isPage());
	}

	@Test
	public void singleIndexIsPage() {
		layoutFiles(project, "res/template/page/page.htm");

		LayoutFile layoutFile = new LayoutFile(project, filePath("res/page/single-index/single-index.htm"));
		assertTrue(layoutFile.isPage());
	}

	@Test
	public void notResolvedTemplates() {
		layoutFiles(project, "res/template/sidebar-page/sidebar-page.htm", "res/template/page/page.htm");

		LayoutFile layoutFile = new LayoutFile(project, filePath("res/page/bad-index/bad-index.htm"));
		try {
			assertTrue(layoutFile.isPage());
			fail("Bad template reference should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("unresolved templates"));
		}
	}

	@Test
	public void editableOverwritten() {
		layoutFiles(project, "res/template/over-page/over-page.htm", "res/template/page/page.htm");

		LayoutFile layoutFile = new LayoutFile(project, filePath("res/page/over-index/over-index.htm"));
		try {
			assertTrue(layoutFile.isPage());
			fail("Overwritten editable should rise exception.");
		} catch (Exception e) {
			assertTrue(e instanceof WoodException);
			assertTrue(e.getMessage().contains("overwritten"));
		}
	}

	private void layoutFiles(BuilderProject project, String... paths) {
		Collection<LayoutFile> layouts = new HashSet<>();
		Classes.setFieldValue(project, "layouts", layouts);
		for (String path : paths) {
			layouts.add(new LayoutFile(project, filePath(path)));
		}
	}
}
