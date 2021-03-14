package js.wood.cli.runtime;

import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "stop", description = "Stop runtime.")
public class RuntimeStop extends Task {
	@Override
	protected int exec() throws Exception {
		return 0;
	}
}
