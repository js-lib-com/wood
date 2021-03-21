package js.wood.cli.runtime;

import picocli.CommandLine.Command;

@Command(name = "stop", description = "Stop project runtime.")
public class RuntimeStop extends RuntimeScriptTask {
	@Override
	protected String getScriptName() {
		return "shutdown";
	}
}
