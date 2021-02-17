package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;
import java.io.IOException;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import js.util.Classes;
import js.util.Files;
import js.wood.impl.ComponentDescriptor;
import js.wood.impl.Variables;

public class BuilderProjectTest implements IReferenceHandler {
	private BuilderProject project;

	@Before
	public void beforeTest() throws IOException {
		File projectRoot = new File("src/test/resources/project");
		File buildDir = new File(projectRoot, CT.DEF_BUILD_DIR);
		project = new BuilderProject(projectRoot, buildDir);
		if (buildDir.exists()) {
			Files.removeFilesHierarchy(buildDir);
		}
	}

	@Test
	public void descriptorValues() throws IOException {
		File projectRoot = new File("src/test/resources/scripts");
		File buildDir = new File(projectRoot, CT.DEF_BUILD_DIR);
		project = new BuilderProject(projectRoot, buildDir);
		CompoPath compoPath = new CompoPath(project, "res/index");

		final Variables variables = project.getVariables().get(compoPath);
		ComponentDescriptor descriptor = new ComponentDescriptor(new FilePath(project, "res/index/index.xml"), new IReferenceHandler() {
			@Override
			public String onResourceReference(IReference reference, FilePath sourceFile) throws IOException {
				return variables.get(new Locale("en"), reference, sourceFile, this);
			}
		});

		assertThat(descriptor.getDisplay(null), equalTo("Index Page"));
		assertThat(descriptor.getDescription(null), equalTo("Index page description."));
	}

	@Override
	public String onResourceReference(IReference reference, FilePath sourcePath) throws IOException {
		Variables variables = new Variables(sourcePath.getParentDirPath());
		if (project.getAssetsDir().exists()) {
			try {
				Classes.invoke(variables, "load", project.getAssetsDir());
			} catch (Exception e) {
				e.printStackTrace();
			}
		}
		return variables.get(null, reference, sourcePath, this);
	}
}
