package js.wood.cli.project;

import java.io.File;
import java.io.IOException;

import js.wood.Builder;
import js.wood.BuilderConfig;
import js.wood.cli.Task;
import js.wood.util.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "build", description = "Build project from current working directory.")
public class ProjectBuild extends Task {
	@Option(names = { "-c", "--clean" }, description = "Clean build. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean clean;
	@Option(names = { "-t", "--target" }, description = "Build directory relative to working directory. Default: ${DEFAULT-VALUE}.", defaultValue = "site", paramLabel = "target")
	private String targetDir;
	@Option(names = { "-n", "--number" }, description = "Build number. Default: ${DEFAULT-VALUE}", defaultValue = "0", paramLabel = "number")
	private int buildNumber;

	@Override
	protected int exec() throws IOException {
		File workingDir = workingDir();
		File buildDir = new File(workingDir, targetDir);

		if (clean) {
			print("Cleaning build files for project%s...", workingDir);
			Files.removeFilesHierarchy(buildDir);
		}

		print("Building project %s...", workingDir);

		BuilderConfig config = new BuilderConfig();
		config.setProjectDir(workingDir);
		config.setBuildDir(buildDir);
		config.setBuildNumber(buildNumber);

		Builder builder = new Builder(config);
		builder.build();

		return 0;
	}
}
