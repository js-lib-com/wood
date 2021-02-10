package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import js.wood.FilePath;
import js.wood.PreviewProject;

public class PreviewProjectTest {
	private PreviewProject project;

	@Before
	public void beforeTest() {
		project = new PreviewProject("src/test/resources/project");
	}

	@Test
	public void previewThemeStyles() {
		List<FilePath> styles = project.getThemeStyles();
		assertThat(styles, notNullValue());
		assertThat(styles, hasSize(4));

		List<String> fileNames = styles.stream().map(file -> file.value()).collect(Collectors.toList());
		assertThat(fileNames, contains("res/theme/form.css", "res/theme/fx.css", "res/theme/reset.css", "res/theme/style.css"));
	}
}
