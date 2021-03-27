package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.PathMatcher;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.attribute.BasicFileAttributes;

import js.util.Strings;
import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "delete", description = "Remove component.")
public class CompoDelete extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about processed files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component name, path relative to project root. Ex: res/page/home", converter = CompoNameConverter.class)
	private CompoName name;

	private boolean found;

	@Override
	protected ExitCode exec() throws Exception {
		if (!name.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name.value()));
		}

		Path workingDir = workingPath();
		Path compoDir = workingDir.resolve(name.path());
		if (!Files.exists(compoDir)) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name));
		}

		PathMatcher matcher = workingDir.getFileSystem().getPathMatcher("glob:**.htm");
		Files.walkFileTree(workingDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (!matcher.matches(file)) {
					return FileVisitResult.CONTINUE;
				}
				if (!Strings.load(Files.newBufferedReader(file)).contains(name.path())) {
					return FileVisitResult.CONTINUE;
				}

				if (!found) {
					console.warning("Component %s is used by:", name);
				}
				found = true;
				console.warning("- %s", workingDir.relativize(file));
				return FileVisitResult.CONTINUE;
			}
		});

		if (found) {
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		console.warning("All component '%s' files will be permanently deleted.", name);
		if (!console.confirm("Please confirm: yes | [no]", "yes")) {
			console.print("User cancel.");
			return ExitCode.CANCEL;
		}

		Files.walkFileTree(compoDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult postVisitDirectory(Path dir, IOException exc) throws IOException {
				Files.delete(dir);
				if (verbose) {
					console.print("Remove component directory %s.", workingDir.relativize(dir));
				}
				return FileVisitResult.CONTINUE;
			}

			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				Files.delete(file);
				if (verbose) {
					console.print("Remove component file %s.", workingDir.relativize(file));
				}
				return FileVisitResult.CONTINUE;
			}
		});

		return ExitCode.SUCCESS;
	}
}
