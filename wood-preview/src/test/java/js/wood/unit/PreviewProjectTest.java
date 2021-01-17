package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.List;

import org.junit.Before;
import org.junit.Test;

import js.wood.FilePath;
import js.wood.PreviewProject;

public class PreviewProjectTest extends PreviewTestCase {
	private PreviewProject project;

	@Before
	public void beforeTest() {
		project = project("project");
	}

	@Test
	public void previewThemeStyles() {
		List<FilePath> styles = project.getThemeStyles();
		assertEquals(4, styles.size());

		assertTrue(styles.contains(filePath("res/theme/reset.css")));
		assertTrue(styles.contains(filePath("res/theme/fx.css")));
		assertTrue(styles.contains(filePath("res/theme/form.css")));
		assertTrue(styles.contains(filePath("res/theme/style.css")));
	}

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}
}
