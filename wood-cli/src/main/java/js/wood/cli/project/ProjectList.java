package js.wood.cli.project;

import java.io.BufferedReader;
import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;
import java.nio.file.attribute.FileTime;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
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
	@Option(names = { "-a", "--attributes" }, description = "Add modification time and file size to generated listing.")
	private boolean attributes;
	@Option(names = { "-e", "--excludes" }, description = "Comma separated list of directories to exclude.", split = ",")
	private List<String> excludes = Collections.emptyList();

	@Parameters(index = "0", description = "Optional directory to list, default to project root.", arity = "0..1")
	private String path;

	private Utils utils = new Utils();
	private Path workingDir;
	private int found;

	@Override
	protected ExitCode exec() throws IOException {
		workingDir = workingPath();
		if (path != null) {
			workingDir = workingDir.resolve(path);
		}

		if (page) {
			pages();
		} else if (template) {
			templates();
		} else if (tree) {
			tree();
		} else {
			list();
		}

		console.crlf();
		console.info("Found %d objects.", found);
		return ExitCode.SUCCESS;
	}

	private void pages() throws IOException {
		Files.walkFileTree(workingDir, new PageFileVisitor(workingDir));
	}

	private void templates() throws IOException {
		Files.walkFileTree(workingDir, new TemplateFileVisitor(workingDir));
	}

	private void tree() throws IOException {
		Files.walkFileTree(workingDir, new TreeFileVisitor(workingDir));
	}

	private void list() throws IOException {
		Files.walkFileTree(workingDir, new ListFileVisitor(workingDir));
	}

	private void print(Path file, FileTime modifiedTime, long fileSize) {
		if (attributes) {
			LocalDateTime dt = modifiedTime.toInstant().atZone(ZoneId.systemDefault()).toLocalDateTime();
			DateTimeFormatter dtf = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
			console.print("%s\t%-8d %s", dt.format(dtf), fileSize, file);
		} else {
			console.print(file);
		}
	}

	class PageFileVisitor extends SimpleFileVisitor<Path> {
		private final Path workingDir;

		private FileTime modifiedTime;
		private long dirSize;

		public PageFileVisitor(Path workingDir) {
			this.workingDir = workingDir;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (utils.isExcluded(workingDir.relativize(dir))) {
				return FileVisitResult.SKIP_SUBTREE;
			}
			modifiedTime = attrs.lastModifiedTime();
			dirSize = 0;
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			dirSize += attrs.size();
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (utils.isXML(dir.resolve(dir.getFileName() + ".xml"), "page")) {
				print(workingDir.relativize(dir), modifiedTime, dirSize);
				++found;
			}
			return FileVisitResult.CONTINUE;
		}
	}

	class TemplateFileVisitor extends SimpleFileVisitor<Path> {
		private Path workingDir;

		private FileTime modifiedTime;
		private long dirSize;

		public TemplateFileVisitor(Path workingDir) {
			this.workingDir = workingDir;
		}

		@Override
		public FileVisitResult preVisitDirectory(Path dir, BasicFileAttributes attrs) throws IOException {
			if (utils.isExcluded(workingDir.relativize(dir))) {
				return FileVisitResult.SKIP_SUBTREE;
			}
			modifiedTime = attrs.lastModifiedTime();
			dirSize = 0;
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
			dirSize += attrs.size();
			return FileVisitResult.CONTINUE;
		}

		@Override
		public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
			if (utils.isXML(dir.resolve(dir.getFileName() + ".xml"), "template")) {
				print(workingDir.relativize(dir), modifiedTime, dirSize);
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
			print(workingDir.relativize(file), attrs.lastModifiedTime(), attrs.size());
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
