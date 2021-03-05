package js.wood;

import java.io.File;

import js.util.Files;
import js.wood.impl.EditablePath;

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

	public DirPath createDirPath(File file) {
		return createDirPath(Files.getRelativePath(project.getProjectRoot(), file, true));
	}

	public DirPath createDirPath(String path) {
		if (!path.endsWith(Path.SEPARATOR)) {
			path += Path.SEPARATOR_CHAR;
		}
		return new DirPath(project, path);
	}

	public CompoPath createCompoPath(String path) {
		return new CompoPath(project, path);
	}

	public EditablePath createEditablePath(String path) {
		return new EditablePath(project, path);
	}
	
	public Component createComponent(FilePath layoutPath, IReferenceHandler referenceHandler) {
		return new Component(layoutPath, referenceHandler);
	}

	public Variables createVariables(DirPath dir) {
		return new Variables(dir);
	}
}
