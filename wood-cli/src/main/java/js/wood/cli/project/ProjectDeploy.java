package js.wood.cli.project;

import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "deploy", description = "Project deploy on development runtime.")
public class ProjectDeploy extends Task {
	@Override
	protected int exec() {
		print("Project deploy.");
		return 0;
	}
}
