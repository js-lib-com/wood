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
		List<FilePath> styles = project.previewThemeStyles();
		assertEquals(4, styles.size());

		assertEquals(filePath("res/theme/reset.css"), styles.get(0));
		assertEquals(filePath("res/theme/fx.css"), styles.get(1));
		assertTrue(styles.contains(filePath("res/theme/form.css")));
		assertTrue(styles.contains(filePath("res/theme/style.css")));
	}

	private FilePath filePath(String path) {
		return new FilePath(project, path);
	}
}
