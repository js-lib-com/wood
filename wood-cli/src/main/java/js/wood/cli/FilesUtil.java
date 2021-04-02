package js.wood.cli;

import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystemException;
import java.nio.file.FileVisitResult;
import java.nio.file.FileVisitor;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.ArrayList;
import java.util.List;

import js.lang.BugError;
import js.util.Params;
import js.util.Strings;

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
		if (!exists(propertiesFile)) {
			throw new BugError("Invalid project. Missing project properties file %s.", propertiesFile);
		}
		return projectDir;
	}

	public void createDirectory(Path dir) throws IOException {
		Params.notNull(dir, "Directory");
		fileSystem.provider().createDirectory(dir);
	}

	public Path createDirectories(String first, String... more) throws IOException {
		Params.notNullOrEmpty(first, "First path component");
		Params.notNullOrEmpty(more, "More path components");

		Path dir = fileSystem.getPath(first, more);
		Path parent = dir.getParent();
		while (parent != null) {
			if (exists(parent)) {
				break;
			}
			parent = parent.getParent();
		}
		if (parent == null) {
			throw new FileSystemException(dir.toString(), null, "Unable to determine if root directory exists");
		}

		Path child = parent;
		for (Path name : parent.relativize(dir)) {
			child = child.resolve(name);
			createDirectory(child);
		}

		return dir;
	}

	public void cleanDirectory(Path dir, boolean verbose) throws IOException {
		// walk file tree is depth-first so that the most inner files and directories are removed first
		walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (verbose) {
					console.print("Delete file %s.", file);
				}
				fileSystem.provider().delete(file);
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				if (verbose) {
					console.print("Delete directory %s.", dir);
				}
				fileSystem.provider().delete(dir);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public void copyFiles(Path sourceDir, Path targetDir, boolean verbose) throws IOException {
		walkFileTree(sourceDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (verbose) {
					console.print("Deploy file %s.", file);
				}
				fileSystem.provider().copy(file, targetDir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	public boolean isDirectory(Path path) {
		try {
			return fileSystem.provider().readAttributes(path, BasicFileAttributes.class).isDirectory();
		} catch (IOException unused) {
			return false;
		}
	}

	public boolean exists(Path file) {
		try {
			fileSystem.provider().checkAccess(file);
			return true;
		} catch (IOException unused) {
			return false;
		}
	}

	public void walkFileTree(Path start, FileVisitor<Path> visitor) throws IOException {
		Files.walkFileTree(start, visitor);
	}

	public Path getFileByExtension(Path dir, String extension) throws IOException {
		class FoundFile {
			Path path = null;
		}
		final FoundFile foundFile = new FoundFile();

		walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.endsWith(extension)) {
					foundFile.path = file;
					return FileVisitResult.TERMINATE;
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return foundFile.path;
	}

	public List<Path> findFilesByExtension(Path dir, String extension) throws IOException {
		List<Path> files = new ArrayList<>();
		walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.endsWith(extension)) {
					files.add(file);
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return files;
	}

	public List<Path> findFilesByContentPattern(Path dir, String extension, String pattern) throws IOException {
		List<Path> files = new ArrayList<>();
		walkFileTree(dir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (file.endsWith(extension)) {
					if (Strings.load(getReader(file)).contains(pattern)) {
						files.add(file);
					}
				}
				return FileVisitResult.CONTINUE;
			}
		});
		return files;
	}

	public Reader getReader(Path file) throws IOException {
		return new InputStreamReader(fileSystem.provider().newInputStream(file));
	}
}