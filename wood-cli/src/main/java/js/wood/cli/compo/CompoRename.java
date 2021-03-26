package js.wood.cli.compo;

import static java.lang.String.format;
import static js.util.Strings.concat;

import java.io.File;
import java.io.FileFilter;
import java.io.IOException;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import js.wood.cli.TextReplace;
import js.wood.util.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "rename", description = "Rename component but keep its current location.")
public class CompoRename extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about processed files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component path, relative to project root.", converter = CompoNameConverter.class)
	private CompoName name;
	@Parameters(index = "1", description = "Component new name.")
	private String newname;

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

		File newCompoDir = new File(compoDir.getParentFile(), newname);
		if (newCompoDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("New component %s already exists.", newCompoDir));
		}

		File[] files = compoDir.listFiles(new FileFilter() {
			@Override
			public boolean accept(File file) {
				return Files.basename(file).equals(compoDir.getName());
			}
		});
		if (files == null) {
			throw new IOException(format("Fail to list files for directory %s.", compoDir));
		}
		for (File file : files) {
			if (!file.renameTo(new File(compoDir, concat(newname, '.', Files.getExtension(file))))) {
				throw new IOException(format("Fail to rename file %s.", file));
			}
		}

		if (!compoDir.renameTo(newCompoDir)) {
			throw new IOException(format("Fail to rename component directory %s.", compoDir));
		}

		String compoPath = name.path();
		int pathSeparator = compoPath.lastIndexOf('/') + 1;
		String newCompoPath;
		if (pathSeparator > 0) {
			newCompoPath = compoPath.substring(0, pathSeparator) + newname;
		} else {
			newCompoPath = newname;
		}

		TextReplace textReplace = new TextReplace();
		textReplace.addExclude("");

		textReplace.setFileExtension("htm");
		textReplace.replaceAll(workingDir, compoPath, newCompoPath);

		String compoScript = concat(compoPath, '/', compoDir.getName(), ".js");
		String newCompoScript = concat(newCompoPath, '/', newCompoDir.getName(), ".js");
		File newCompoScriptFile = new File(workingDir, newCompoScript);
		if (newCompoScriptFile.exists()) {
			textReplace.setFileExtension("xml");
			textReplace.replaceAll(workingDir, compoScript, newCompoScript);
		}
		return ExitCode.SUCCESS;
	}
}
