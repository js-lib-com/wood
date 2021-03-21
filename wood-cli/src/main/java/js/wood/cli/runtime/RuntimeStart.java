package js.wood.cli.runtime;

import picocli.CommandLine.Command;

@Command(name = "start", description = "Start project runtime.")
public class RuntimeStart extends RuntimeScriptTask {
	@Override
	protected String getScriptName() {
		return "startup";
	}
}
