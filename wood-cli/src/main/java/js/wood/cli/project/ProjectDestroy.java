package js.wood.cli.project;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
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
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about deleted files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Project name, relative to current working directory.")
	private String name;

	@Override
	protected ExitCode exec() throws IOException  {
		Path workingDir = files.getWorkingDir();
		Path projectDir = workingDir.resolve(name);
		if (!files.exists(projectDir)) {
			throw new ParameterException(commandSpec.commandLine(), format("Project directory %s not found.", projectDir));
		}

		Path descriptorFile = projectDir.resolve("project.xml");
		if (!force && !files.exists(descriptorFile)) {
			console.print("Project descriptor file not found. Is %s indeed a WOOD project?", projectDir);
			console.warning("All directory files will be permanently removed!");
			console.crlf();
			console.print("If you are sure please use --force-destroy option.");
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		console.warning("You are about to destroy project '%s'.", name);
		console.warning("Project location: %s", projectDir);
		console.crlf();
		if (!console.confirm("Please confirm: yes | [no]", "yes")) {
			console.print("User cancel.");
			return ExitCode.CANCEL;
		}

		console.print("Destroying files for project %s...", projectDir);
		files.cleanDirectory(projectDir, verbose);

		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Tests support

	void setCommandSpec(CommandSpec commandSpec) {
		this.commandSpec = commandSpec;
	}

	void setForce(boolean force) {
		this.force = force;
	}

	void setName(String name) {
		this.name = name;
	}
}
