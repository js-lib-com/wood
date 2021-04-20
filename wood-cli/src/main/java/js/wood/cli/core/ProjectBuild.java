package js.wood.cli.core;

import java.io.IOException;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

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
	@Option(names = { "-t", "--target" }, description = "Build directory relative to project root.")
	private String target;
	@Option(names = { "-n", "--number" }, description = "Build number. Default: ${DEFAULT-VALUE}", defaultValue = "0", paramLabel = "number")
	private int buildNumber;
	@Option(names = { "-r", "--runtime" }, description = "Runtime server name. Default: project name.")
	private String runtime;
	@Option(names = { "-e", "--excludes" }, description = "Comma separated list of directories to exclude.", split = ",")
	private List<String> excludes = Collections.emptyList();
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deployed files.")
	private boolean verbose;

	private BuilderConfig builderConfig = new BuilderConfig();

	@Override
	protected ExitCode exec() throws IOException {
		if (target == null) {
			target = config.get("build.target");
		}
		if (target == null) {
			console.print("Missing build target parameter or build.target property.");
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		Path projectDir = files.getProjectDir();
		Path buildDir = projectDir.resolve(target);
		if (!files.exists(buildDir)) {
			console.print("Missing build directory %s.", buildDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		if (clean) {
			ProjectClean clean = new ProjectClean(this, target, verbose);
			clean.exec();
		}

		builderConfig.setProjectDir(projectDir.toFile());
		builderConfig.setBuildDir(buildDir.toFile());
		builderConfig.setBuildNumber(buildNumber);

		console.print("Building project %s...", projectDir);
		Builder builder = builderConfig.createBuilder();
		builder.build();

		ProjectDeploy deploy = new ProjectDeploy(this, target, runtime, verbose);
		return deploy.exec();
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

	void setTarget(String target) {
		this.target = target;
	}
}
