package js.wood.cli.compo;

import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create component.")
public class CompoCreate extends Task {
	@Option(names = "--no-script", description = "Do not create script file.")
	private boolean noScript;

	@Parameters(index = "0", description = "Component qualified name.")
	private String name;

	@Override
	protected int exec() {
		print("Create component %s.", name);
		return 0;
	}
}
