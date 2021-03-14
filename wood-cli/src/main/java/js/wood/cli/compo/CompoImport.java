package js.wood.cli.compo;

import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "import", description = "Import component.")
public class CompoImport extends Task {
	@Parameters(index = "0", description = "Component versioned name.")
	private String name;
	@Parameters(index = "1", description = "Project library path.")
	private String path;

	@Override
	protected int exec() {
		print("Import component %s into %s", name, path);
		return 0;
	}
}
