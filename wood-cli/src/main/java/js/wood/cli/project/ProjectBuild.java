package js.wood.cli.project;

import java.io.IOException;
import java.nio.file.Path;

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
		Path workingDir = files.getWorkingDir();
		Path buildDir = workingDir.resolve(targetDir);

		if (clean) {
			console.print("Cleaning build files %s...", buildDir);
			files.cleanDirectory(buildDir, verbose);
		}

		builderConfig.setProjectDir(workingDir.toFile());
		builderConfig.setBuildDir(buildDir.toFile());
		builderConfig.setBuildNumber(buildNumber);

		console.print("Building project %s...", workingDir);
		Builder builder = builderConfig.createBuilder();
		builder.build();

		String runtimeName = config.get("runtime.name", runtime != null ? runtime : files.getFileName(workingDir));
		String contextName = config.get("runtime.context", files.getFileName(workingDir));
		Path deployDir = files.createDirectories(config.get("runtime.home"), runtimeName, "webapps", contextName);

		console.print("Deploying project %s...", workingDir);
		files.copyFiles(buildDir, deployDir, verbose);
		return ExitCode.SUCCESS;
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

	void setRuntime(String runtime) {
		this.runtime = runtime;
	}

	void setTargetDir(String targetDir) {
		this.targetDir = targetDir;
	}
}
