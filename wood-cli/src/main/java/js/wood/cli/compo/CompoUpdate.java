package js.wood.cli.compo;

import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "update", description = "Update component to latest compatible version from repository.")
public class CompoUpdate extends Task {
	@Option(names = { "-u", "--upgrade" }, description = "Update even if version major is different.")
	private boolean upgrade;

	@Parameters(index = "0", description = "Component name, path relative to project root.", converter = CompoNameConverter.class)
	private CompoName name;

	@Override
	protected int exec() throws Exception {
		print("Update component %s", name);
		return 0;
	}
}
