package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "update", description = "Update component to latest compatible version from repository.")
public class CompoUpdate extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-u", "--upgrade" }, description = "Update even if version major is different.")
	private boolean upgrade;

	@Parameters(index = "0", description = "Component name, path relative to project root.", converter = CompoNameConverter.class)
	private CompoName name;

	@Override
	protected ExitCode exec() throws IOException {
		if (!name.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name.value()));
		}

		Path projectDir = files.getProjectDir();
		Path compoDir = projectDir.resolve(name.path());
		if (!files.exists(compoDir)) {
			console.print("Missing component directory %s.", compoDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		console.print("Update component %s", name);
		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Tests support

	void setCommandSpec(CommandSpec commandSpec) {
		this.commandSpec = commandSpec;
	}

	void setName(CompoName name) {
		this.name = name;
	}
}
