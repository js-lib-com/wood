package js.wood.cli.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.util.Collections;
import java.util.List;

import js.util.Strings;
import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "list", description = "Display project objects.")
public class ProjectList extends Task {
	@Option(names = { "-T", "--tree" }, description = "Display tree like structure.")
	private boolean tree;
	@Option(names = { "-t", "--template" }, description = "List only template components.")
	private boolean template;
	@Option(names = { "-p", "--page" }, description = "List only page components.")
	private boolean page;
	@Option(names = { "-e", "--excludes" }, description = "Comma separated list of directories to exclude.", split = ",")
	private List<String> excludes = Collections.emptyList();

	@Parameters(index = "0", description = "Optional directory to list, default to project root.", arity = "0..1")
	private String path;

	private Utils utils = new Utils();
	private int found;

	@Override
	protected ExitCode exec() throws IOException {
		Path dir = workingPath();
		if (path != null) {
			dir = dir.resolve(path);
		}

		if (page) {
			pages(dir);
		} else if (template) {
			templates(dir);
		} else if (tree) {
			tree(dir);
		} else {
			list(dir);
		}

		console.crlf();
		console.info("Found %d objects.", found);
		return ExitCode.SUCCESS;
	}

	private void pages(Path workingDir) throws IOException {
		Files.walkFileTree(workingDir, new PageFileVisitor(workingDir));
	}

	private void templates(Path workingDir) throws IOException {
		Files.walkFileTree(workingDir, new TemplateFileVisitor(workingDir));
	}

	private void tree(Path workingDir) throws IOException {
		Files.walkFileTree(workingDir, new TreeFileVisitor(workingDir));
	}

	private void list(Path workingDir) throws IOException {
		Files.walkFileTree(workingDir, new ListFileVisitor(workingDir));
	}

	class PageFileVisitor extends SimpleFileVisitor<Path> {
		private Path workingDir;

		public PageFileVisitor(Path workingDir) {
			this.workingDir = workingDir;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			Path relativeDir = workingDir.relativize(dir);
			if (utils.isExcluded(relativeDir)) {
				return FileVisitResult.SKIP_SUBTREE;
			}
			if (utils.isXML(dir.resolve(dir.getFileName() + ".xml"), "page")) {
				console.print(relativeDir);
				++found;
			}
			return FileVisitResult.CONTINUE;
		}
	}

	class TemplateFileVisitor extends SimpleFileVisitor<Path> {
		private Path workingDir;

		public TemplateFileVisitor(Path workingDir) {
			this.workingDir = workingDir;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			Path relativeDir = workingDir.relativize(dir);
			if (utils.isExcluded(relativeDir)) {
				return FileVisitResult.SKIP_SUBTREE;
			}
			if (utils.isXML(dir.resolve(dir.getFileName() + ".xml"), "template")) {
				console.print(relativeDir);
				++found;
			}
			return FileVisitResult.CONTINUE;
		}
	}

	class TreeFileVisitor extends SimpleFileVisitor<Path> {
		private Path workingDir;

		public TreeFileVisitor(Path workingDir) {
			this.workingDir = workingDir;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			Path relativeDir = workingDir.relativize(dir);
			if (utils.isExcluded(relativeDir)) {
				return FileVisitResult.SKIP_SUBTREE;
			}
			console.print("+ %s", relativeDir);
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			console.print("|\t- %s", file.getFileName());
			++found;
			return FileVisitResult.CONTINUE;
		}
	}

	class ListFileVisitor extends SimpleFileVisitor<Path> {
		private Path workingDir;

		public ListFileVisitor(Path workingDir) {
			this.workingDir = workingDir;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			return utils.isExcluded(workingDir.relativize(dir)) ? FileVisitResult.SKIP_SUBTREE : FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			console.print(workingDir.relativize(file));
			++found;
			return FileVisitResult.CONTINUE;
		}
	}

	class Utils {
		boolean isExcluded(Path dir) {
			for (String exclude : excludes) {
				if (dir.toString().startsWith(exclude)) {
					return true;
				}
			}
			return false;
		}

		boolean isXML(Path file, String root) throws IOException {
			if (!Files.exists(file)) {
				return false;
			}
			try (BufferedReader reader = Files.newBufferedReader(file)) {
				String line = reader.readLine();
				if (line.startsWith("<?")) {
					line = reader.readLine();
				}
				if (line == null) {
					return false;
				}
				if (line.startsWith(Strings.concat('<', root, '>'))) {
					return true;
				}
			}
			return false;
		}
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	void setUtils(Utils utils) {
		this.utils = utils;
	}

	void setTree(boolean tree) {
		this.tree = tree;
	}

	void setTemplate(boolean template) {
		this.template = template;
	}

	void setPage(boolean page) {
		this.page = page;
	}

	void setExcludes(List<String> excludes) {
		this.excludes = excludes;
	}

	void setPath(String path) {
		this.path = path;
	}

	int getFound() {
		return found;
	}
}
