package js.wood.cli.core;

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
		if(target == null) {
			target = config.get("build.target");
		}
		if(target == null) {
			console.print("Missing build directory parameter.");
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		Path projectDir = files.getProjectDir();
		Path buildDir = projectDir.resolve(target);

		console.print("Cleaning build files for project %s...", projectDir);
		files.cleanDirectory(buildDir, verbose);

		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	void setTarget(String target) {
		this.target = target;
	}
}
