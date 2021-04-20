package js.wood.cli.core;

import java.io.IOException;
import java.nio.file.Path;
import java.nio.file.attribute.FileTime;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "deploy", description = "Project deploy on development runtime.")
public class ProjectDeploy extends Task {
	@Option(names = { "-t", "--target" }, description = "Build directory relative to project root.")
	private String target;
	@Option(names = { "-r", "--runtime" }, description = "Runtime server name. Default: project name.")
	private String runtime;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deployed files.")
	private boolean verbose;

	public ProjectDeploy() {
		super();
	}

	public ProjectDeploy(Task parent, String target, String runtime, boolean verbose) {
		super(parent);
		this.target = target;
		this.runtime = runtime;
		this.verbose = verbose;
	}

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

		String projectName = files.getFileName(projectDir);
		String runtimeName = config.get("runtime.name", runtime != null ? runtime : projectName);
		String contextName = config.get("runtime.context", projectName);
		Path deployDir = files.createDirectories(config.get("runtime.home"), runtimeName, "webapps", contextName);

		console.print("Deploying project %s...", projectDir);
		files.copyFiles(buildDir, deployDir, verbose);

		files.setLastModifiedTime(deployDir.resolve("WEB-INF/web.xml"), FileTime.fromMillis(System.currentTimeMillis()));
		return ExitCode.SUCCESS;
	}
}
