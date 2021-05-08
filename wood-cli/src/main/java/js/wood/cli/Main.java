package js.wood.cli;

import java.io.IOException;
import java.lang.annotation.Annotation;
import java.util.Properties;

import com.jslib.commons.cli.Config;
import com.jslib.commons.cli.Console;
import com.jslib.commons.cli.Home;
import com.jslib.commons.cli.Task;

import js.io.IConsole;
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
import js.wood.cli.compo.CompoUsedBy;
import js.wood.cli.config.ConfigCommands;
import js.wood.cli.config.ConfigList;
import js.wood.cli.core.ProjectBuild;
import js.wood.cli.core.ProjectClean;
import js.wood.cli.core.ProjectDeploy;
import js.wood.cli.core.ProjectList;
import js.wood.cli.core.WoodSetup;
import js.wood.cli.core.WoodUpdate;
import js.wood.cli.icons.CreateIcons;
import js.wood.cli.runtime.RuntimeCommands;
import js.wood.cli.runtime.RuntimeInit;
import js.wood.cli.runtime.RuntimeStart;
import js.wood.cli.runtime.RuntimeStop;
import js.wood.cli.runtime.RuntimeUpdate;
import picocli.CommandLine;
import picocli.CommandLine.Command;

@Command(name = "wood", description = "Command line interface for WOOD tools.", mixinStandardHelpOptions = true, version = "wood, version 1.0.5-SNAPSHOT")
public class Main {
	public static void main(String... args) throws IOException {
		Home.setMainClass(Main.class);
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
	private final IConsole console;

	public Main(Config config) {
		this.config = config;
		this.console = new Console();
	}

	private void run(String... args) {
		CommandLine createCommands = new CommandLine(new CreateCommands());
		createCommands.addSubcommand(task(CreateIcons.class));

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
		compoCommands.addSubcommand(task(CompoUsedBy.class));

		CommandLine runtimeCommands = new CommandLine(new RuntimeCommands());
		runtimeCommands.addSubcommand(task(RuntimeInit.class));
		runtimeCommands.addSubcommand(task(RuntimeStart.class));
		runtimeCommands.addSubcommand(task(RuntimeStop.class));
		runtimeCommands.addSubcommand(task(RuntimeUpdate.class));

		CommandLine configCommands = new CommandLine(new ConfigCommands());
		configCommands.addSubcommand(task(ConfigList.class));

		CommandLine commandLine = new CommandLine(this);
		commandLine.addSubcommand(createCommands);
		commandLine.addSubcommand(compoCommands);
		commandLine.addSubcommand(runtimeCommands);
		commandLine.addSubcommand(configCommands);
		commandLine.addSubcommand(task(ProjectBuild.class));
		commandLine.addSubcommand(task(ProjectClean.class));
		commandLine.addSubcommand(task(ProjectDeploy.class));
		commandLine.addSubcommand(task(ProjectList.class));
		commandLine.addSubcommand(task(WoodSetup.class));
		commandLine.addSubcommand(task(WoodUpdate.class));

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
			task.setConsole(console);
			return task;
		} catch (InstantiationException | IllegalAccessException e) {
			throw new BugError("Not instantiable task class |%s|.", taskClass);
		}
	}
}
