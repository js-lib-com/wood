package js.wood.cli.compo;

import static java.lang.String.format;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "preview", description = "Preview component in system browser.")
public class CompoPreview extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Parameters(index = "0", description = "Component path relative to project root.")
	private String path;

	@Override
	protected int exec() throws Exception {
		print("Opening component preview %s...", path);

		File workingDir = workingDir();
		File compoDir = new File(workingDir, path);
		if (!compoDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", path));
		}

		int port = 80;
		String context = workingDir.getName() + "-preview";
		Desktop.getDesktop().browse(new URI(format("http://localhost:%d/%s/%s", port, context, path)));

		return 0;
	}
}
