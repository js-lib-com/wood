package js.wood.cli.project;

import java.io.IOException;
import java.nio.file.FileVisitResult;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.SimpleFileVisitor;
import java.nio.file.StandardCopyOption;
import java.nio.file.attribute.BasicFileAttributes;

import js.wood.Builder;
import js.wood.BuilderConfig;
import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "build", description = "Build project from current working directory.")
public class ProjectBuild extends Task {
	@Option(names = { "-c", "--clean" }, description = "Clean build. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean clean;
	@Option(names = { "-t", "--target" }, description = "Build directory relative to working directory. Default: ${DEFAULT-VALUE}.", defaultValue = "site", paramLabel = "target")
	private String targetDir;
	@Option(names = { "-n", "--number" }, description = "Build number. Default: ${DEFAULT-VALUE}", defaultValue = "0", paramLabel = "number")
	private int buildNumber;
	@Option(names = { "-r", "--runtime" }, description = "Runtime server name. Default: project name.")
	private String runtime;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deployed files.")
	private boolean verbose;

	private BuilderConfig builderConfig = new BuilderConfig();

	@Override
	protected ExitCode exec() throws IOException {
		Path workingDir = workingPath();
		Path buildDir = workingDir.resolve(targetDir);

		if (clean) {
			console.print("Cleaning build files %s...", buildDir);
			cleanBuildFiles(buildDir);
		}

		console.print("Building project %s...", workingDir);

		builderConfig.setProjectDir(workingDir.toFile());
		builderConfig.setBuildDir(buildDir.toFile());
		builderConfig.setBuildNumber(buildNumber);

		Builder builder = builderConfig.createBuilder();
		builder.build();

		String workingDirName = workingDir.getFileName().toString();
		String contextName = config.get("runtime.context", workingDirName);
		String runtimeName = config.get("runtime.name", runtime != null ? runtime : workingDirName);
		Path deployDir = fileSystem.getPath(config.get("runtime.home"), runtimeName, "webapps", contextName);
		// ensure deploy directory is created; there is no exception if deploy directory already exist
		Files.createDirectories(deployDir);

		console.print("Deploying project %s...", workingDir);
		deployBuildFiles(buildDir, deployDir);
		return ExitCode.SUCCESS;
	}

	private void cleanBuildFiles(Path buildDir) throws IOException {
		// walk file tree is depth-first so that the most inner files and directories are removed first
		Files.walkFileTree(buildDir, new SimpleFileVisitor<Path>() {
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

	private void deployBuildFiles(Path buildDir, Path deployDir) throws IOException {
		Files.walkFileTree(buildDir, new SimpleFileVisitor<Path>() {
			@Override
			public FileVisitResult visitFile(Path file, BasicFileAttributes attrs) throws IOException {
				if (verbose) {
					print("Deploy file %s.", file);
				}
				Files.copy(file, deployDir.resolve(file), StandardCopyOption.REPLACE_EXISTING);
				return FileVisitResult.CONTINUE;
			}
		});
	}

	// --------------------------------------------------------------------------------------------
	// Tests support

	void setBuilderConfig(BuilderConfig builderConfig) {
		this.builderConfig = builderConfig;
	}

	void setClean(boolean clean) {
		this.clean = clean;
	}

	void setBuildNumber(int buildNumber) {
		this.buildNumber = buildNumber;
	}

	void setTargetDir(String targetDir) {
		this.targetDir = targetDir;
	}
}
