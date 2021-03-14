package js.wood.cli.runtime;

import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "start", description = "Start runtime.")
public class RuntimeStart extends Task {
	@Override
	protected int exec() throws Exception {
		return 0;
	}
}
