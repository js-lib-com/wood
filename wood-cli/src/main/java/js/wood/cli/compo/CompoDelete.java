package js.wood.cli.compo;

import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "delete", description = "Remove component.")
public class CompoDelete extends Task {
	@Override
	protected int exec() throws Exception {
		return 0;
	}
}
