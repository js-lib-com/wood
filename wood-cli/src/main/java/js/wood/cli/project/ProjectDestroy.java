package js.wood.cli.project;

import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "destroy", description = "Delete project and runtime.")
public class ProjectDestroy extends Task {
	@Override
	protected int exec() throws Exception {
		return 0;
	}
}
