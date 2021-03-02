package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.IOException;
import java.io.StringReader;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.impl.FilesHandler;
import js.wood.impl.ResourceType;
import js.wood.impl.Variants;

@RunWith(MockitoJUnitRunner.class)
public class BuilderProjectTest {
	private Directory projectDir;
	private Directory buildDir;

	private BuilderProject project;

	@Before
	public void beforeTest() throws IOException {
		projectDir = new Directory("project");
		buildDir = new Directory("project/builder");
		project = new BuilderProject(projectDir, buildDir);
	}

	@Test
	public void constructor() {
		assertThat(project.getThemeStyles(), notNullValue());
		assertThat(project.getAssetVariables(), notNullValue());
		assertThat(project.getVariables(), notNullValue());
		assertThat(project.getPages(), notNullValue());
		assertFalse(project.isMultiLocale());
	}

	@Test(expected = WoodException.class)
	public void loadFile() throws IOException {
		project.loadFile("scripts/sdk.js");
	}

	@Test
	public void scan() {
		Path[] paths = new Path[3];

		DirPath dir = Mockito.mock(DirPath.class);
		when(dir.value()).thenReturn("res/page");
		when(dir.getName()).thenReturn("page");
		paths[0] = dir;

		FilePath file = Mockito.mock(FilePath.class);
		when(file.getParentDirPath()).thenReturn((DirPath) paths[0]);
		when(file.hasBaseName("page")).thenReturn(true);
		when(file.isXML("page")).thenReturn(true);
		paths[1] = file;

		file = Mockito.mock(FilePath.class);
		when(file.getParentDirPath()).thenReturn((DirPath) paths[0]);
		when(file.isXML(ResourceType.variables())).thenReturn(true);
		when(file.getVariants()).thenReturn(Mockito.mock(Variants.class));
		when(file.getReader()).thenReturn(new StringReader("<string></string>"));
		paths[2] = file;

		project.scan(new TestDirPath(project, paths));

		List<CompoPath> pages = project.getPages();
		assertThat(pages, hasSize(1));
		assertThat(pages.get(0).value(), equalTo("res/page/"));
	}

	private static class TestDirPath extends DirPath {
		private final Path[] paths;

		public TestDirPath(Project project, Path[] paths) {
			super(project);
			this.paths = paths;
		}

		@Override
		public void files(FilesHandler handler) {
			for (Path path : paths) {
				if (path instanceof DirPath) {
					handler.onDirectory((DirPath) path);
				} else {
					handler.onFile((FilePath) path);
				}
			}
		}
	}

	private static class Directory extends File {
		private static final long serialVersionUID = -5519843268072578663L;

		public Directory(String pathname) {
			super(pathname);
		}

		@Override
		public boolean isDirectory() {
			return true;
		}
	}
}
