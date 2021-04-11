package js.wood.cli;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Properties;

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
import js.wood.cli.compo.CompoUpdate;
import js.wood.cli.config.ConfigCommands;
import js.wood.cli.config.ConfigList;
import js.wood.cli.project.ProjectBuild;
import js.wood.cli.project.ProjectClean;
import js.wood.cli.project.ProjectCommands;
import js.wood.cli.project.ProjectCreate;
import js.wood.cli.project.ProjectDeploy;
import js.wood.cli.project.ProjectDestroy;
import js.wood.cli.project.ProjectList;
import js.wood.cli.runtime.RuntimeCommands;
import js.wood.cli.runtime.RuntimeCreate;
import js.wood.cli.runtime.RuntimeDestroy;
import js.wood.cli.runtime.RuntimeStart;
import js.wood.cli.runtime.RuntimeStop;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "wood", description = "Command line interface for WOOD tools.", mixinStandardHelpOptions = true, version = "wood, version 1.0.4-SNAPSHOT")
public class Main {
	public static void main(String... args) throws IOException {
		Properties globalProperties = new Properties();
		Properties projectProperties = new Properties();
		Config config = new Config(globalProperties, projectProperties);

		Main main = new Main(config);
		// use wood.home property to detect if WOOD install is properly initialized; force 'setup' if not
		if (!config.has("wood.home")) {
			args = new String[] { "setup" };
		}
		main.run(args);
	}

	private final Config config;

	public Main(Config config) {
		this.config = config;
	}

	private void run(String... args) {
		CommandLine projectCommands = new CommandLine(ProjectCommands.class);
		projectCommands.addSubcommand(task(ProjectCreate.class));
		projectCommands.addSubcommand(task(ProjectBuild.class));
		projectCommands.addSubcommand(task(ProjectClean.class));
		projectCommands.addSubcommand(task(ProjectDeploy.class));
		projectCommands.addSubcommand(task(ProjectDestroy.class));
		projectCommands.addSubcommand(task(ProjectList.class));

		CommandLine compoCommands = new CommandLine(new CompoCommands());
		compoCommands.addSubcommand(task(CompoCreate.class));
		compoCommands.addSubcommand(task(CompoDelete.class));
		compoCommands.addSubcommand(task(CompoRename.class));
		compoCommands.addSubcommand(task(CompoMove.class));
		compoCommands.addSubcommand(task(CompoOpen.class));
		compoCommands.addSubcommand(task(CompoPreview.class));
		compoCommands.addSubcommand(task(CompoImport.class));
		compoCommands.addSubcommand(task(CompoExport.class));
		compoCommands.addSubcommand(task(CompoUpdate.class));

		CommandLine runtimeCommands = new CommandLine(new RuntimeCommands());
		runtimeCommands.addSubcommand(task(RuntimeCreate.class));
		runtimeCommands.addSubcommand(task(RuntimeStart.class));
		runtimeCommands.addSubcommand(task(RuntimeStop.class));
		runtimeCommands.addSubcommand(task(RuntimeDestroy.class));

		CommandLine configCommands = new CommandLine(new ConfigCommands());
		configCommands.addSubcommand(task(ConfigList.class));

		CommandLine commandLine = new CommandLine(this);
		commandLine.addSubcommand(projectCommands);
		commandLine.addSubcommand(compoCommands);
		commandLine.addSubcommand(runtimeCommands);
		commandLine.addSubcommand(configCommands);
		commandLine.addSubcommand(task(ProjectBuild.class));
		commandLine.addSubcommand(task(Update.class));
		commandLine.addSubcommand(task(Setup.class));

		System.exit(commandLine.execute(args));
	}

	private Object task(Class<? extends Task> taskClass) {
		Annotation commandAnnotation = taskClass.getAnnotation(Command.class);
		if (commandAnnotation == null) {
			throw new BugError("Not annotated task class |%s|.", taskClass);
		}
		try {
			Task task = taskClass.newInstance();
			task.setConfig(config);
			return task;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new BugError("Not instantiable task class |%s|.", taskClass);
		}
	}
}
