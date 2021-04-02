package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "delete", description = "Remove component.")
public class CompoDelete extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about processed files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component name, path relative to project root. Ex: res/page/home", converter = CompoNameConverter.class)
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

		List<Path> usedByFiles = files.findFilesByContentPattern(projectDir, ".htm", name.path());
		if (!usedByFiles.isEmpty()) {
			console.warning("Component %s is used by:", name);
			for (Path usedByFile : usedByFiles) {
				console.warning("- %s", projectDir.relativize(usedByFile));
			}
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		console.warning("All component '%s' files will be permanently deleted.", name);
		if (!console.confirm("Please confirm: yes | [no]", "yes")) {
			console.print("User cancel.");
			return ExitCode.CANCEL;
		}

		files.cleanDirectory(compoDir, verbose);

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
