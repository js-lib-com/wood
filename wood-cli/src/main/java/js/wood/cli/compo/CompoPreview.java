package js.wood.cli.compo;

import static java.lang.String.format;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

import js.wood.cli.ExitCode;
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

	@Parameters(index = "0", description = "Component name, path relative to project root. Ex: res/page/home", converter = CompoNameConverter.class)
	private CompoName name;

	@Override
	protected ExitCode exec() throws Exception {
		if (!name.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name.value()));
		}

		File workingDir = workingDir();
		File compoDir = new File(workingDir, name.toString());
		if (!compoDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name));
		}

		print("Opening component preview %s...", name);
		int port = 80;
		String context = workingDir.getName() + "-preview";
		Desktop.getDesktop().browse(new URI(format("http://localhost:%d/%s/%s", port, context, name)));

		return ExitCode.SUCCESS;
	}
}
