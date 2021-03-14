package js.wood.cli.runtime;

import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "destroy", description = "Destroy runtime.")
public class RuntimeDestroy extends Task {
	@Override
	protected int exec() throws Exception {
		return 0;
	}
}
