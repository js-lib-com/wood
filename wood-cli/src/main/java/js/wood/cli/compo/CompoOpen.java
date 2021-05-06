package js.wood.cli.compo;

import static java.lang.String.format;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;

import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "open", description = "Open component in system browser.")
public class CompoOpen extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-s", "--server" }, description = "Open on remote development server.")
	private boolean server;
	@Option(names = { "-p", "--preview" }, description = "Force preview mode.")
	private boolean preview;

	@Parameters(index = "0", description = "Component name, path relative to project root. Ex: res/page/home", converter = CompoNameConverter.class)
	private CompoName name;

	private Desktop desktop = Desktop.getDesktop();

	@Override
	protected ExitCode exec() throws IOException, URISyntaxException {
		if (!name.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name.value()));
		}

		Path projectDir = files.getProjectDir();
		String projectName = files.getFileName(projectDir);
		Path compoDir = projectDir.resolve(name.path());
		if (!files.exists(compoDir)) {
			console.print("Missing component directory %s.", compoDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}
		String compoName = files.getFileName(compoDir);

		// WOOD project naming convention: descriptor file basename is the same as component directory
		Path descriptorFile = compoDir.resolve(compoName + ".xml");
		boolean page = files.exists(descriptorFile) && files.isXML(descriptorFile, "page");
		if (preview || !page) {
			if (server) {
				console.print("Preview on remote development server not supported.");
				console.print("Command abort.");
				return ExitCode.ABORT;
			}
			CompoPreview preview = new CompoPreview(this, desktop, name);
			return preview.exec();
		}

		console.print("Opening component %s...", name);
		String pageURI = null;
		if (server) {
			String server = config.get("dev.server");
			if (server == null) {
				console.print("Missing dev.server property.");
				console.print("Command abort.");
				return ExitCode.ABORT;
			}
			pageURI = format("https://%s/apps/%s/%s.htm", server, projectName, compoName);
		} else {
			int port = config.get("runtime.port", int.class);
			String context = config.get("runtime.context", projectName);
			// WOOD project naming convention: layout file basename is the same as component directory
			pageURI = format("http://localhost:%d/%s/%s.htm", port, context, compoName);
		}
		desktop.browse(URI.create(pageURI));
		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Tests support

	void setCommandSpec(CommandSpec commandSpec) {
		this.commandSpec = commandSpec;
	}

	void setName(CompoName name) {
		this.name = name;
	}

	void setPreview(boolean preview) {
		this.preview = preview;
	}

	void setDesktop(Desktop desktop) {
		this.desktop = desktop;
	}
}
