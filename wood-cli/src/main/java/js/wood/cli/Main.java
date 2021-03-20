package js.wood.cli;

import java.lang.annotation.Annotation;

import js.lang.BugError;
import js.wood.cli.compo.CompoCommands;
import js.wood.cli.compo.CompoCreate;
import js.wood.cli.compo.CompoDelete;
import js.wood.cli.compo.CompoExport;
import js.wood.cli.compo.CompoImport;
import js.wood.cli.compo.CompoMove;
import js.wood.cli.compo.CompoOpen;
import js.wood.cli.compo.CompoPreview;
import js.wood.cli.compo.CompoRename;
import js.wood.cli.project.ProjectBuild;
import js.wood.cli.project.ProjectClean;
import js.wood.cli.project.ProjectCommands;
import js.wood.cli.project.ProjectCreate;
import js.wood.cli.project.ProjectDeploy;
import js.wood.cli.project.ProjectDestroy;
import js.wood.cli.runtime.RuntimeCommands;
import js.wood.cli.runtime.RuntimeCreate;
import js.wood.cli.runtime.RuntimeDestroy;
import js.wood.cli.runtime.RuntimeStart;
import js.wood.cli.runtime.RuntimeStop;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "wood", description = "Command line interface for WOOD tools.", mixinStandardHelpOptions = true, version = "wood-cli, version 1.0.0")
public class Main {
	public static void main(String... args) {
		CommandLine projectCommands = new CommandLine(ProjectCommands.class);
		projectCommands.addSubcommand(task(ProjectCreate.class));
		projectCommands.addSubcommand(task(ProjectBuild.class));
		projectCommands.addSubcommand(task(ProjectClean.class));
		projectCommands.addSubcommand(task(ProjectDeploy.class));
		projectCommands.addSubcommand(task(ProjectDestroy.class));

		CommandLine compoCommands = new CommandLine(new CompoCommands());
		compoCommands.addSubcommand(task(CompoCreate.class));
		compoCommands.addSubcommand(task(CompoDelete.class));
		compoCommands.addSubcommand(task(CompoRename.class));
		compoCommands.addSubcommand(task(CompoMove.class));
		compoCommands.addSubcommand(task(CompoOpen.class));
		compoCommands.addSubcommand(task(CompoPreview.class));
		compoCommands.addSubcommand(task(CompoImport.class));
		compoCommands.addSubcommand(task(CompoExport.class));

		CommandLine runtimeCommands = new CommandLine(new RuntimeCommands());
		runtimeCommands.addSubcommand(task(RuntimeCreate.class));
		runtimeCommands.addSubcommand(task(RuntimeStart.class));
		runtimeCommands.addSubcommand(task(RuntimeStop.class));
		runtimeCommands.addSubcommand(task(RuntimeDestroy.class));

		CommandLine commandLine = new CommandLine(new Main());
		commandLine.addSubcommand(projectCommands);
		commandLine.addSubcommand(compoCommands);
		commandLine.addSubcommand(runtimeCommands);
		commandLine.addSubcommand(projectCommands.getSubcommands().get("build"));

		System.exit(commandLine.execute(args));
	}

	private static Object task(Class<? extends Task> taskClass) {
		Annotation commandAnnotation = taskClass.getAnnotation(Command.class);
		if (commandAnnotation == null) {
			throw new BugError("Not annotated task class |%s|.", taskClass);
		}
		try {
			Task task = taskClass.newInstance();
			return task;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new BugError("Not instantiable task class |%s|.", taskClass);
		}
	}
}
