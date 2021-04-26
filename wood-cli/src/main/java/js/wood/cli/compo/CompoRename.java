package js.wood.cli.compo;

import static java.lang.String.format;
import static js.util.Strings.concat;

import java.io.IOException;
import java.nio.file.Path;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;
import com.jslib.commons.cli.TextReplace;

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

		Path newCompoDir = compoDir.getParent().resolve(newname);
		if (files.exists(newCompoDir)) {
			console.print("Target component directory %s already exist.", newCompoDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		// rename component files into current component directory then rename directory too
		for (Path compoFile : files.listFiles(compoDir, path -> files.getFileBasename(path).equals(files.getFileName(compoDir)))) {
			Path newCompoFile = compoDir.resolve(concat(newname, '.', files.getExtension(compoFile)));
			if (verbose) {
				console.print("Rename %s file to %s.", compoFile, newCompoFile);
			}
			files.move(compoFile, newCompoFile);
		}
		files.move(compoDir, newCompoDir);

		String compoPath = name.path();
		int pathSeparator = compoPath.lastIndexOf('/') + 1;
		String newCompoPath;
		if (pathSeparator > 0) {
			newCompoPath = compoPath.substring(0, pathSeparator) + newname;
		} else {
			newCompoPath = newname;
		}

		textReplace.addExclude("");
		textReplace.setFileExtension("htm");
		textReplace.replaceAll(projectDir.toFile(), compoPath, newCompoPath);

		String compoScript = concat(compoPath, '/', files.getFileName(compoDir), ".js");
		String newCompoScript = concat(newCompoPath, '/', files.getFileName(newCompoDir), ".js");
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

	void setNewname(String newname) {
		this.newname = newname;
	}

	void setVerbose(boolean verbose) {
		this.verbose = verbose;
	}

	void setTextReplace(TextReplace textReplace) {
		this.textReplace = textReplace;
	}
}
