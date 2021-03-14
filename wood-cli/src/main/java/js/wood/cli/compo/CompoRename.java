package js.wood.cli.compo;

import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "rename", description = "Rename component.")
public class CompoRename extends Task {
	@Override
	protected int exec() throws Exception {
		return 0;
	}
}
