package js.wood;

import java.io.File;
import java.io.IOException;

import org.junit.Before;

import js.util.Classes;
import js.util.Files;

public class BuilderProjectTest implements IReferenceHandler {
	private BuilderProject project;

	@Before
	public void beforeTest() throws IOException {
		File projectRoot = new File("src/test/resources/project");
		File buildDir = new File(projectRoot, BuildFS.DEF_BUILD_DIR);
		project = new BuilderProject(projectRoot, buildDir);
		if (buildDir.exists()) {
			Files.removeFilesHierarchy(buildDir);
		}
	}

	@Override
	public String onResourceReference(Reference reference, FilePath sourcePath) throws IOException {
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
