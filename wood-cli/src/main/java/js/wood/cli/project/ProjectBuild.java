package js.wood.cli.project;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;

import js.lang.BugError;
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
	@Option(names = { "-r", "--runtime" }, description = "Runtime server name. Default: project name.")
	private String runtime;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deployed files.")
	private boolean verbose;

	@Override
	protected int exec() throws IOException {
		File workingDir = workingDir();
		File buildDir = new File(workingDir, targetDir);

		if (clean) {
			print("Cleaning build files %s...", buildDir);
			Files.removeFilesHierarchy(buildDir);
		}

		print("Building project %s...", workingDir);

		BuilderConfig builderConfig = new BuilderConfig();
		builderConfig.setProjectDir(workingDir);
		builderConfig.setBuildDir(buildDir);
		builderConfig.setBuildNumber(buildNumber);

		Builder builder = new Builder(builderConfig);
		builder.build();

		File runtimeDir = new File(config.get("runtime.home", File.class), runtime != null ? runtime : workingDir.getName());
		if (!runtimeDir.exists()) {
			// it is legal to not have runtime in which case deploy is not performed
			return 0;
		}

		File webappsDir = new File(runtimeDir, "webapps");
		if (!webappsDir.exists()) {
			throw new BugError("Invalid runtime. Web apps directory not found.");
		}
		File deployDir = new File(webappsDir, workingDir.getName());
		// ensure deploy directory is created
		deployDir.mkdir();

		print("Deploying project %s...", workingDir);
		deploy(buildDir.getPath().length(), deployDir, buildDir);
		return 0;
	}

	private void deploy(int buildDirPathLength, File deployDir, File currentDir) throws IOException {
		File[] files = currentDir.listFiles();
		if (files == null) {
			throw new IOException(format("Fail to list files on directory %s.", currentDir));
		}
		for (File file : files) {
			if (file.isDirectory()) {
				deploy(buildDirPathLength, deployDir, file);
				continue;
			}

			String path = file.getPath().substring(buildDirPathLength + 1);
			File deployFile = new File(deployDir, path);
			deployFile.getParentFile().mkdirs();
			if (verbose) {
				print("Deploy file %s.", file);
			}
			Files.copy(file, deployFile);
		}
	}
}
