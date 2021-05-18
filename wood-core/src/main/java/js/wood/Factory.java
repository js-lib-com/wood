package js.wood;

import java.io.File;

import js.util.Files;

public class Factory {
	private final Project project;

	public Factory(Project project) {
		this.project = project;
	}

	public FilePath createFilePath(File file) {
		return createFilePath(Files.getRelativePath(project.getProjectRoot(), file, true));
	}

	public FilePath createFilePath(String path) {
		return new FilePath(project, path);
	}

	public CompoPath createCompoPath(String path) {
		return new CompoPath(project, path);
	}

	public Component createComponent(FilePath layoutPath, IReferenceHandler referenceHandler) {
		return new Component(layoutPath, referenceHandler);
	}
}
