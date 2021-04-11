package js.wood.cli;

import java.nio.file.Path;
import java.util.Properties;

import picocli.CommandLine.Command;

@Command(name = "setup", description = "Set up a new WOOD install.")
public class Setup extends Task {
	@Override
	protected ExitCode exec() throws Exception {
		console.print("WOOD setup.");

		Path woodHome = files.getPath(getWoodHome());
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
