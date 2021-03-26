package js.wood.cli.project;

import java.io.File;
import java.io.IOException;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import js.wood.util.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "clean", description = "Clean build files from current working directory.")
public class ProjectClean extends Task {
	@Option(names = { "-t", "--target" }, description = "Build directory relative to working directory. Default: ${DEFAULT-VALUE}.", defaultValue = "site", paramLabel = "target")
	private String targetDir;

	@Override
	protected ExitCode exec() throws IOException {
		File workingDir = workingDir();
		File buildDir = new File(workingDir, targetDir);

		print("Cleaning build files for project%s...", workingDir);
		Files.removeFilesHierarchy(buildDir);

		return ExitCode.SUCCESS;
	}
}
