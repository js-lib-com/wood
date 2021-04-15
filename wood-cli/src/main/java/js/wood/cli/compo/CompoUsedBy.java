package js.wood.cli.compo;

import static java.lang.String.format;

import java.nio.file.Path;
import java.util.List;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;
import picocli.CommandLine.Model.CommandSpec;

@Command(name = "used", description = "List components using a given component.")
public class CompoUsedBy extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Parameters(index = "0", description = "Component name, path relative to project root. Ex: res/page/home", converter = CompoNameConverter.class)
	private CompoName name;

	@Override
	protected ExitCode exec() throws Exception {
		if (!name.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name.value()));
		}
		Path projectDir = files.getProjectDir();

		List<Path> usedByFiles = files.findFilesByContentPattern(projectDir, ".htm", name.path());
		if (usedByFiles.isEmpty()) {
			console.print("Component %s is not used.", name);
			return ExitCode.SUCCESS;
		}

		console.print("Component %s is used by:", name);
		for (Path usedByFile : usedByFiles) {
			console.print("- %s", projectDir.relativize(usedByFile));
		}

		return ExitCode.SUCCESS;
	}
}
