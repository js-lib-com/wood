package js.wood.cli.core;

import java.io.IOException;
import java.nio.file.Path;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clean", description = "Clean build files from current working directory.")
public class ProjectClean extends Task {
	@Option(names = { "-t", "--target" }, description = "Build target directory, relative to project root.")
	private String target;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deleted files.")
	private boolean verbose;

	@Override
	protected ExitCode exec() throws IOException {
		if (target == null) {
			target = config.get("build.target");
		}
		if (target == null) {
			console.print("Missing build target parameter or build.target property.");
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		Path projectDir = files.getProjectDir();
		Path buildDir = projectDir.resolve(target);

		console.print("Cleaning build directory %s...", buildDir);
		files.cleanDirectory(buildDir, verbose);
		// files.cleanDirectory removes build directory; takes care to recreate it
		files.createDirectory(buildDir);

		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	void setTarget(String target) {
		this.target = target;
	}
}
