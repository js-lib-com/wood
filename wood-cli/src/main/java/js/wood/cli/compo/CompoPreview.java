package js.wood.cli.compo;

import static java.lang.String.format;

import java.awt.Desktop;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;
import com.jslib.commons.cli.Velocity;

import js.wood.preview.EventsServlet;
import js.wood.preview.FileSystemWatcher;
import js.wood.preview.ForwardFilter;
import js.wood.preview.PreviewServlet;
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
		String runtimeName = config.getex("runtime.name", projectName);
		String contextName = config.getex("runtime.context", projectName) + "-preview";
		Path deployDir = files.createDirectories(config.get("runtime.home"), runtimeName, "webapps", contextName);

		Path webxmlFile = deployDir.resolve("WEB-INF/web.xml");
		if (!files.exists(webxmlFile)) {
			files.createDirectories(webxmlFile.getParent());

			Velocity template = new Velocity("WEB-INF/preview-web.vtl");
			template.put("FileSystemWatcher", FileSystemWatcher.class.getCanonicalName());
			template.put("ForwardFilter", ForwardFilter.class.getCanonicalName());
			template.put("PreviewServlet", PreviewServlet.class.getCanonicalName());
			template.put("EventsServlet", EventsServlet.class.getCanonicalName());
			template.put("display", config.get("project.display", projectName));
			template.put("description", config.get("project.description", projectName));
			template.put("projectDir", projectDir.toAbsolutePath().toString());
			template.put("buildDir", config.get("build.target"));
			template.writeTo(files.getWriter(webxmlFile));

			console.print("Created missing preview configuration.");
			console.print("Please allow a moment for runtime to updates.");
			return ExitCode.ABORT;
		}

		console.print("Opening component preview %s...", name);
		int port = config.getex("runtime.port", int.class);
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
