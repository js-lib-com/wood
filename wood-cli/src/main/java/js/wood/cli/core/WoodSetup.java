package js.wood.cli.core;

import java.nio.file.Path;
import java.util.Properties;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Command;

@Command(name = "setup", description = "Set up a new WOOD install.")
public class WoodSetup extends Task {
	@Override
	protected ExitCode exec() throws Exception {
		console.print("WOOD setup.");

		Path woodHome = files.getPath(getHome());
		Properties properties = config.getGlobalProperties();
		properties.put("wood.home", woodHome.toString());
		properties.put("repository.dir", woodHome.resolve("repository").toString());

		properties.put("user.name", console.input("User name"));
		properties.put("user.email", console.input("User email"));
		properties.put("runtime.home", console.input("Runtime home"));

		Path propertiesFile = woodHome.resolve("bin/wood.properties");
		properties.store(files.getOutputStream(propertiesFile), null);

		return ExitCode.SUCCESS;
	}
}
