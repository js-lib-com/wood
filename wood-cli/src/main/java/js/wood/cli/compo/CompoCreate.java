package js.wood.cli.compo;

import static java.lang.String.format;

import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = "create", description = "Create component.")
public class CompoCreate extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = "--no-script", description = "Do not create script file.")
	private boolean noScript;

	@Parameters(index = "0", description = "Component name, path relative to project root. Ex: res/page/home", converter = CompoNameConverter.class)
	private CompoName name;

	@Override
	protected int exec() throws Exception {
		if (!name.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name.value()));
		}
		print("Create component %s.", name);
		return 0;
	}
}
