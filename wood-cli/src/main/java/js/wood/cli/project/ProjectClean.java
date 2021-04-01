package js.wood.cli.project;

import java.io.IOException;
import java.nio.file.Path;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clean", description = "Clean build files from current working directory.")
public class ProjectClean extends Task {
	@Option(names = { "-t", "--target" }, description = "Build directory relative to working directory. Default: ${DEFAULT-VALUE}.", defaultValue = "site")
	private String target;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deleted files.")
	private boolean verbose;

	@Override
	protected ExitCode exec() throws IOException {
		Path workingDir = files.getWorkingDir();
		Path buildDir = workingDir.resolve(target);

		console.print("Cleaning build files for project %s...", workingDir);
		files.cleanDirectory(buildDir, verbose);

		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	void setTarget(String target) {
		this.target = target;
	}
}
