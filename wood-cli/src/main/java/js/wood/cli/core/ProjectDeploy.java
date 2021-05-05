package js.wood.cli.core;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;

import js.io.FilesIterator;
import js.io.FilesOutputStream;
import js.io.StreamHandler;
import js.lang.GType;
import js.net.client.HttpRmiClient;
import js.util.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "deploy", description = "Project deploy on development runtime.")
public class ProjectDeploy extends Task {
	private static final String APPS_MANAGER_CLASS = "com.jslib.wood.apps.AppsManager";

	@Option(names = { "-t", "--target" }, description = "Build directory relative to project root.")
	private String target;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deployed files.")
	private boolean verbose;

	/**
	 * Optional, potential harmful flag, to force removing of all stale files from target directory. If
	 * <code>removeStaleFiles</code> flag is true, all descendant files from target directory that are not present into source
	 * directory are permanently removed. Depending on usage pattern, this may be potentially harmful for which reason removing
	 * stale files is optional and default to false.
	 */
	@Option(names = "--remove-stale-files", description = "Remove of all stale files from target directory.")
	private boolean removeStaleFiles;

	@Override
	protected ExitCode exec() throws Exception {
		if (target == null) {
			target = config.get("build.target");
		}
		if (target == null) {
			console.print("Missing build target parameter or build.target property.");
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		String devServer = config.get("dev.server");
		if (devServer == null) {
			console.print("Missing dev.server property.");
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		Path projectDir = files.getProjectDir();
		Path buildPath = projectDir.resolve(target);
		if (!files.exists(buildPath)) {
			console.print("Missing build directory %s.", buildPath);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}
		if (files.isEmpty(buildPath)) {
			console.print("Empty build directory %s.", buildPath);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		File buildDir = buildPath.toFile();
		String projectName = files.getFileName(projectDir);

		console.print("Compute build files digest.");
		SortedMap<String, byte[]> sourceFiles = new TreeMap<String, byte[]>();
		for (String file : FilesIterator.getRelativeNamesIterator(buildDir)) {
			sourceFiles.put(Files.path2unix(file), Files.getFileDigest(new File(buildDir, file)));
		}

		console.print("Get dirty files list from server %s", devServer);
		HttpRmiClient rmi = new HttpRmiClient(devServer, APPS_MANAGER_CLASS);
		rmi.setReturnType(new GType(List.class, String.class));
		rmi.setExceptions(IOException.class);

		final List<String> dirtyFiles = rmi.invoke("getDirtyFiles", projectName, sourceFiles, removeStaleFiles);
		if (dirtyFiles.isEmpty()) {
			console.print("No files to deploy. Exit command.");
			return ExitCode.SUCCESS;
		}
		if (verbose) {
			dirtyFiles.forEach(dirtyFile -> console.print(dirtyFile));
		}

		console.print("Synchronize dirty files on server %s", devServer);
		rmi = new HttpRmiClient(devServer, APPS_MANAGER_CLASS);
		rmi.setExceptions(IOException.class);

		rmi.invoke("synchronize", projectName, new StreamHandler<FilesOutputStream>(FilesOutputStream.class) {
			@Override
			protected void handle(FilesOutputStream files) throws IOException {
				files.addFiles(buildDir, dirtyFiles);
			}
		});
		return ExitCode.SUCCESS;
	}
}
