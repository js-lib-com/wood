package js.wood.cli.compo;

import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "move", description = "Move component.")
public class CompoMove extends Task {
	@Override
	protected int exec() throws Exception {
		return 0;
	}
}
