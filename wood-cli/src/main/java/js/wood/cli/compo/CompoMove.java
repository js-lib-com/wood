package js.wood.cli.compo;

import static java.lang.String.format;
import static js.util.Strings.concat;

import java.io.File;
import java.io.IOException;

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

	@Override
	protected ExitCode exec() throws Exception {
		if (!name.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name.value()));
		}

		File workingDir = workingDir();
		File compoDir = new File(workingDir, name.path());
		if (!compoDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name));
		}

		if (target.endsWith("/")) {
			target = target.substring(0, target.length() - 1);
		}
		File targetDir = new File(workingDir, target);
		if (!targetDir.exists()) {
			print("Directory %s is missing. To create it?", targetDir);
			if (confirm("Please confirm: yes | [no]", "yes")) {
				if (!targetDir.mkdirs()) {
					throw new IOException(format("Cannot create directory %s.", targetDir));
				}
			} else {
				print("User abort.");
				return ExitCode.ABORT;
			}
		}

		File targetCompoDir = new File(targetDir, compoDir.getName());
		if (targetCompoDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Target component directory %s already existing.", targetCompoDir));
		}
		if (!targetCompoDir.mkdir()) {
			throw new IOException(format("Fail to create directory %s", targetCompoDir));
		}

		print("Moving %s component to %s...", compoDir, targetDir);

		File[] compoFiles = compoDir.listFiles();
		if (compoFiles == null) {
			throw new IOException(format("Fail to list files for directory %s.", compoDir));
		}

		for (File compoFile : compoFiles) {
			File targetCompoFile = new File(targetCompoDir, compoFile.getName());
			if (verbose) {
				print("Move %s file to %s.", compoFile, targetCompoFile);
			}
			if (!compoFile.renameTo(targetCompoFile)) {
				throw new IOException(format("Fail to move %s.", compoFile));
			}
		}
		if (!compoDir.delete()) {
			throw new IOException(format("Fail to delete directory %s.", compoDir));
		}

		TextReplace textReplace = new TextReplace();
		textReplace.addExclude("");
		textReplace.setFileExtension("htm");
		textReplace.replaceAll(workingDir, name.path(), format("%s/%s", target, compoDir.getName()));

		String compoScript = concat(name.path(), '/', compoDir.getName(), ".js");
		String newCompoScript = concat(target, '/', compoDir.getName(), '/', compoDir.getName(), ".js");
		File newCompoScriptFile = new File(workingDir, newCompoScript);
		if (newCompoScriptFile.exists()) {
			textReplace.setFileExtension("xml");
			textReplace.replaceAll(workingDir, compoScript, newCompoScript);
		}
		return ExitCode.SUCCESS;
	}
}
