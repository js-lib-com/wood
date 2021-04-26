package js.wood.cli.core;

import java.io.IOException;
import java.nio.file.Path;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clean", description = "Clean build files from current working directory.")
public class ProjectClean extends Task {
	@Option(names = { "-t", "--target" }, description = "Build target directory, relative to project root.")
	private String target;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deleted files.")
	private boolean verbose;

	public ProjectClean() {
		super();
	}

	public ProjectClean(Task parent, String target, boolean verbose) {
		super(parent);
		this.target = target;
		this.verbose = verbose;
	}

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

		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	void setTarget(String target) {
		this.target = target;
	}
}
