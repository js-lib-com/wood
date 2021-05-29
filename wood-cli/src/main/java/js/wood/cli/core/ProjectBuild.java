package js.wood.cli.core;

import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;

import js.wood.build.Builder;
import js.wood.build.BuilderConfig;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "build", description = "Build project from current working directory.")
public class ProjectBuild extends Task {
	@Option(names = { "-c", "--clean" }, description = "Clean build. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean clean;
	@Option(names = { "-n", "--number" }, description = "Build number. Default: ${DEFAULT-VALUE}", defaultValue = "0", paramLabel = "number")
	private int buildNumber;
	@Option(names = { "-e", "--excludes" }, description = "Comma separated list of directories to exclude.", split = ",")
	private List<String> excludes = Collections.emptyList();
	@Option(names = { "-s", "--server" }, description = "Deploy on remote development server.")
	private boolean server;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deployed files.")
	private boolean verbose;

	private BuilderConfig builderConfig = new BuilderConfig();

	@Override
	protected ExitCode exec() throws Exception {
		String target = config.get("build.dir");
		if (target == null) {
			console.print("Missing build.dir property.");
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
		builderConfig.setBuildNumber(buildNumber);

		String projectName = files.getFileName(projectDir);
		console.print("Building project %s...", projectName);
		Builder builder = builderConfig.createBuilder();
		builder.build();

		String runtimeName = config.getex("runtime.name", projectName);
		String contextName = config.getex("runtime.context", projectName);
		Path deployDir = files.createDirectories(config.getex("runtime.home"), runtimeName, "webapps", contextName);

		console.print("Deploying project %s...", projectName);
		if (server) {
			ProjectDeploy deploy = new ProjectDeploy(this, verbose);
			return deploy.exec();
		}

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
}
