package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertTrue;
import static org.junit.Assert.fail;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.wood.CT;
import js.wood.FilePath;
import js.wood.IReferenceHandler;
import js.wood.Project;
import js.wood.Reference;
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

	@Test
	public void initialization() throws FileNotFoundException {
		project = new Project(new File("src/test/resources/project"));

		assertThat(project.getProjectRoot(), equalTo(new File("src/test/resources/project")));
		assertThat(Classes.getFieldValue(project, "descriptor"), notNullValue());

		assertThat(project.getAssetsDir().toString(), equalTo(CT.ASSETS_DIR + "/"));
		assertThat(project.getFavicon().toString(), equalTo("res/asset/favicon.ico"));

		assertThat(project.getName(), equalTo("project"));
		assertThat(project.getDisplay(), equalTo("Test Project"));
		assertThat(project.getDescription(), equalTo("Project used as fixture for unit testing."));

		assertThat(project.getLocales(), hasSize(4));
	}

	@Override
	public String onResourceReference(Reference reference, FilePath sourcePath) throws IOException {
		Variables variables = new Variables(sourcePath.getParentDirPath());
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
