package js.wood.cli.project;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;

import js.wood.cli.Task;
import js.wood.util.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "destroy", description = "Delete project and its runtime.")
public class ProjectDestroy extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = "--force-destroy", description = "Force destroy even if project description not found.")
	private boolean force;

	@Parameters(index = "0", description = "Project name, relative to current working directory.")
	private String name;

	@Override
	protected int exec() throws Exception {
		File workingDir = workingDir();
		File projectDir = new File(workingDir, name);
		if (!projectDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Project directory %s not found.", projectDir.getAbsolutePath()));
		}

		File descriptorFile = new File(projectDir, "project.xml");
		if (!force && !descriptorFile.exists()) {
			print("Project descriptor file not found. Is %s indeed a WOOD project?", projectDir);
			print("ALL DIRECTORY FILES WILL BE PERMANENTLY REMOVED!");
			print();
			print("If you are sure please use --force-destroy option.");
			print("Command aborted.");
			return 0;
		}

		print("You are about to destroy project '%s'.", name);
		print("Project location: %s", projectDir.getAbsolutePath());
		print();
		if (!confirm("Please confirm: yes | [no]", "yes")) {
			print("User abort.");
			return 0;
		}

		print("Destroying files for project %s...", projectDir);
		Files.removeFilesHierarchy(projectDir);
		if (!projectDir.delete()) {
			throw new IOException("Cannot remove project directory.");
		}

		return 0;
	}
}
