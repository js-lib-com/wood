package js.wood.cli.project;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;

import js.wood.cli.Task;
import js.wood.util.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "destroy", description = "Delete project and its runtime.")
public class ProjectDestroy extends Task {
	@Option(names = "--force", description = "Do not ask for confirmation.")
	private boolean force;

	@Parameters(index = "0", description = "Project name.")
	private String name;

	@Override
	protected int exec() throws Exception {
		File workingDir = workingDir();
		File projectDir = new File(workingDir, name);
		if (!projectDir.exists()) {
			throw new FileNotFoundException(projectDir.getAbsolutePath());
		}

		if (!force) {
			print("You are about to destroy project '%s'.", name);
			print("Project location: %s", projectDir.getAbsolutePath());
			print();
			if (!confirm("Please confirm: yes | [no]", "yes")) {
				print("User abort.");
				return 0;
			}
		}

		print("Destroying files for project %s...", projectDir);
		Files.removeFilesHierarchy(projectDir);
		if(!projectDir.delete()) {
			throw new IOException("Cannot remove project directory.");
		}

		return 0;
	}
}
