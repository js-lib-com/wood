package js.wood.cli;

import js.wood.cli.compo.CompoCommands;
import js.wood.cli.compo.CompoCreate;
import js.wood.cli.compo.CompoDelete;
import js.wood.cli.compo.CompoExport;
import js.wood.cli.compo.CompoImport;
import js.wood.cli.compo.CompoMove;
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
		projectCommands.addSubcommand("create", ProjectCreate.class);
		projectCommands.addSubcommand("build", ProjectBuild.class);
		projectCommands.addSubcommand("clean", ProjectClean.class);
		projectCommands.addSubcommand("deploy", ProjectDeploy.class);
		projectCommands.addSubcommand("destroy", ProjectDestroy.class);

		CommandLine compoCommands = new CommandLine(new CompoCommands());
		compoCommands.addSubcommand("create", CompoCreate.class);
		compoCommands.addSubcommand("delete", CompoDelete.class);
		compoCommands.addSubcommand("rename", CompoRename.class);
		compoCommands.addSubcommand("move", CompoMove.class);
		compoCommands.addSubcommand("import", CompoImport.class);
		compoCommands.addSubcommand("export", CompoExport.class);

		CommandLine runtimeCommands = new CommandLine(new RuntimeCommands());
		runtimeCommands.addSubcommand("create", RuntimeCreate.class);
		runtimeCommands.addSubcommand("start", RuntimeStart.class);
		runtimeCommands.addSubcommand("stop", RuntimeStop.class);
		runtimeCommands.addSubcommand("destroy", RuntimeDestroy.class);

		CommandLine commandLine = new CommandLine(new Main());
		commandLine.addSubcommand("project", projectCommands);
		commandLine.addSubcommand("compo", compoCommands);
		commandLine.addSubcommand("runtime", runtimeCommands);
		commandLine.addSubcommand("build", ProjectBuild.class);

		System.exit(commandLine.execute(args));
	}
}
