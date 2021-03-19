package js.wood.cli.compo;

import static java.lang.String.format;

import java.awt.Desktop;
import java.io.File;
import java.net.URI;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.util.Classes;
import js.wood.cli.Task;
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

	@Option(names = { "-p", "--preview" }, description = "Force preview mode.")
	private boolean preview;

	@Parameters(index = "0", description = "Component path relative to project root.")
	private String path;

	@Override
	protected int exec() throws Exception {
		print("Opening component %s...", path);

		File workingDir = workingDir();
		File compoDir = new File(workingDir, path);
		if (!compoDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Component %s not found.", path));
		}

		// WOOD project naming convention: descriptor file basename is the same as component directory
		File descriptorFile = new File(compoDir, compoDir.getName() + ".xml");
		if (!descriptorFile.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Invalid component %s. Missing descriptor.", path));
		}
		DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
		Document descriptorDoc = documentBuilder.loadXML(descriptorFile);

		int port = 80;
		String context;
		if (!preview && descriptorDoc.getRoot().getTag().equals("page")) {
			context = workingDir.getName();
			// WOOD project naming convention: layout file basename is the same as component directory
			path = compoDir.getName() + ".htm";
		} else {
			context = workingDir.getName() + "-preview";
		}

		Desktop.getDesktop().browse(new URI(format("http://localhost:%d/%s/%s", port, context, path)));

		return 0;
	}
}
