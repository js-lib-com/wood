package js.wood.cli.core;

import java.nio.file.Path;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "deploy", description = "Project deploy on development runtime.")
public class ProjectDeploy extends Task {
	@Override
	protected ExitCode exec() {
		Path projectDir = files.getProjectDir();

		console.print("Deploy project %s...", projectDir);
		return ExitCode.SUCCESS;
	}
}
