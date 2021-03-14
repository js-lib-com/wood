package js.wood.cli.runtime;

import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "create", description = "Create runtime.")
public class RuntimeCreate extends Task {
	@Override
	protected int exec() throws Exception {
		return 0;
	}
}
