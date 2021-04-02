package js.wood.cli;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Iterator;

import js.lang.BugError;
import js.util.Params;

public class FilesUtil {
	private final FileSystem fileSystem;
	private final Console console;

	public FilesUtil() {
		this.fileSystem = null;
		this.console = null;
	}

	public FilesUtil(FileSystem fileSystem, Console console) {
		this.fileSystem = fileSystem;
		this.console = console;
	}

	public Path getWorkingDir() {
		return fileSystem.getPath("").toAbsolutePath();
	}

	public String getFileName(Path file) {
		return file.getFileName().toString();
	}

	public Path getProjectDir() {
		Path projectDir = fileSystem.getPath("").toAbsolutePath();
		Path propertiesFile = projectDir.resolve(".project.properties");
		if (!Files.exists(propertiesFile)) {
			throw new BugError("Invalid project. Missing project properties file %s.", propertiesFile);
		}
		return projectDir;
	}

	public void createDirectory(Path dir) throws IOException {
		Params.notNull(dir, "Directory");
		Files.createDirectory(dir);
	}

	public Path createDirectories(String first, String... more) throws IOException {
		Params.notNullOrEmpty(first, "First path component");
		Params.notNullOrEmpty(more, "More path components");
		Path dir = fileSystem.getPath(first, more);
		// there is no exception if deploy directory already exist
		Files.createDirectories(dir);
		return dir;
	}

	public void cleanDirectory(Path dir, boolean verbose) throws IOException {
		// walk file tree is depth-first so that the most inner files and directories are removed first
		Files.walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (verbose) {
					console.print("Delete file %s.", file);
				}
				Files.delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (verbose) {
					console.print("Delete directory %s.", dir);
				}
				Files.delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public void copyFiles(Path sourceDir, Path targetDir, boolean verbose) throws IOException {
		Files.walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (verbose) {
					console.print("Deploy file %s.", file);
				}
				Files.copy(file, targetDir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public boolean exists(Path file) {
		return Files.exists(file);
	}

	public void walkFileTree(Path start, FileVisitor<Path> visitor) throws IOException {
		Files.walkFileTree(start, visitor);
	}

	public Path findFile(Path dir, String extension) throws IOException {
		Iterator<Path> it = Files.walk(dir, 1).iterator();
		while (it.hasNext()) {
			Path path = it.next();
			if (Files.isDirectory(path)) {
				continue;
			}
			if (path.endsWith(extension)) {
				return path;
			}
		}
		return null;
	}

	public Reader getReader(Path file) throws IOException {
		return Files.newBufferedReader(file);
	}
}
