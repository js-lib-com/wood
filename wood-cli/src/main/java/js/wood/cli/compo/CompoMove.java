package js.wood.cli.compo;

import static java.lang.String.format;
import static js.util.Strings.concat;

import java.io.IOException;
import java.nio.file.Path;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import js.wood.cli.TextReplace;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "move", description = "Move component to another source directory.")
public class CompoMove extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about processed files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component path, relative to project root.", converter = CompoNameConverter.class)
	private CompoName name;
	@Parameters(index = "1", description = "Target directory, relative to project root.")
	private String target;

	private TextReplace textReplace = new TextReplace();

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
		String compoName = files.getFileName(compoDir);

		Path targetDir = projectDir.resolve(target);
		if (!files.exists(targetDir)) {
			console.print("Directory %s is missing. To create it?", targetDir);
			if (console.confirm("Please confirm: yes | [no]", "yes")) {
				files.createDirectory(targetDir);
			} else {
				console.print("User cancel.");
				return ExitCode.CANCEL;
			}
		}

		Path targetCompoDir = targetDir.resolve(compoDir.getFileName());
		files.createDirectoryIfNotExists(targetCompoDir);

		console.print("Moving %s component to %s...", compoDir, targetDir);
		for (Path compoFile : files.listFiles(compoDir)) {
			Path targetCompoFile = targetCompoDir.resolve(compoFile);
			if (verbose) {
				console.print("Move %s file to %s.", compoFile, targetCompoFile);
			}
			files.move(compoFile, targetCompoFile);
		}
		files.delete(compoDir);

		textReplace.addExclude("");
		textReplace.setFileExtension("htm");
		textReplace.replaceAll(projectDir.toFile(), name.path(), format("%s/%s", target, compoName));

		String compoScript = concat(name.path(), '/', compoName, ".js");
		String newCompoScript = concat(target, '/', compoName, '/', compoName, ".js");
		Path newCompoScriptFile = projectDir.resolve(newCompoScript);
		if (files.exists(newCompoScriptFile)) {
			textReplace.setFileExtension("xml");
			textReplace.replaceAll(projectDir.toFile(), compoScript, newCompoScript);
		}
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

	void setTarget(String target) {
		this.target = target;
	}

	void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	void setTextReplace(TextReplace textReplace) {
		this.textReplace = textReplace;
	}
}
