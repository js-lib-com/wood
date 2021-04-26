package js.wood.cli.compo;

import static java.lang.String.format;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;

import js.io.VariablesWriter;
import js.util.Classes;
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

	private Desktop desktop = Desktop.getDesktop();

	public CompoPreview() {
		super();
	}

	public CompoPreview(Task parent, Desktop desktop, CompoName name) {
		super(parent);
		this.desktop = desktop;
		this.name = name;
	}

	@Override
	protected ExitCode exec() throws IOException, URISyntaxException {
		if (!name.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", name.value()));
		}

		Path projectDir = files.getProjectDir();
		Path compoDir = projectDir.resolve(name.path());
		if (!files.exists(compoDir)) {
			console.print("Missing component directory %s.", compoDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		String projectName = files.getFileName(projectDir);
		String runtimeName = config.get("runtime.name", projectName);
		String contextName = config.get("runtime.context", projectName) + "-preview";
		Path deployDir = files.createDirectories(config.get("runtime.home"), runtimeName, "webapps", contextName);

		Path webxmlFile = deployDir.resolve("WEB-INF/web.xml");
		if (!files.exists(webxmlFile)) {
			files.createDirectories(webxmlFile.getParent());

			Map<String, String> variables = new HashMap<>();
			variables.put("display-name", config.get("project.display", projectName));
			variables.put("description", config.get("project.description", projectName));
			variables.put("project-dir", projectDir.toAbsolutePath().toString());
			files.copy(Classes.getResourceAsReader("WEB-INF/preview-web.xml"), new VariablesWriter(files.getWriter(webxmlFile), variables));

			console.print("Created missing preview configuration.");
			console.print("Please allow a moment for runtime to updates.");
			return ExitCode.ABORT;
		}

		console.print("Opening component preview %s...", name);
		int port = config.get("runtime.port", int.class);
		desktop.browse(new URI(format("http://localhost:%d/%s/%s", port, contextName, name)));

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

	void setDesktop(Desktop desktop) {
		this.desktop = desktop;
	}
}
