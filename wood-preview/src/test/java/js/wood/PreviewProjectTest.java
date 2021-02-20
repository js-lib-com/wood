package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.util.List;
import java.util.stream.Collectors;

import org.junit.Before;
import org.junit.Test;

import js.wood.PreviewProject;
import js.wood.ThemeStyles;

public class PreviewProjectTest {
	private PreviewProject project;

	@Before
	public void beforeTest() {
		project = new PreviewProject(new File("src/test/resources/project"));
	}

	@Test
	public void previewThemeStyles() {
		ThemeStyles styles = project.getThemeStyles();
		assertThat(styles, notNullValue());
		assertThat(styles.reset, notNullValue());
		assertThat(styles.fx, notNullValue());
		assertThat(styles.styles, hasSize(2));
		
		List<String> fileNames = styles.styles.stream().map(file -> file.value()).collect(Collectors.toList());
		assertThat(fileNames, contains("res/theme/form.css", "res/theme/style.css"));
	}
}
