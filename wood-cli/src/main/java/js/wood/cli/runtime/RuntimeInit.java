package js.wood.cli.runtime;

import java.nio.file.Path;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import picocli.CommandLine.Command;
import picocli.CommandLine.Parameters;

@Command(name = "init", description = "Initialize project runtime.")
public class RuntimeInit extends Task {
	@Parameters(index = "0", description = "Runtime name.")
	private String name;

	@Override
	protected ExitCode exec() throws Exception {
		Path runtimesHomeDir = files.getPath(config.get("runtime.home"));
		Path projectRuntimeDir = runtimesHomeDir.resolve(name);
		if (!files.exists(projectRuntimeDir)) {
			console.print("Missing runtime directory %s", projectRuntimeDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		Path serverXmlFile = projectRuntimeDir.resolve("conf/server.xml");
		if (!files.exists(projectRuntimeDir)) {
			console.print("Invalid runtime %s. Missing server.xml file.", name);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		DocumentBuilder docBuilder = Classes.loadService(DocumentBuilder.class);
		Document serverXmlDoc = docBuilder.loadXML(files.getInputStream(serverXmlFile));
		Element connectorElement = serverXmlDoc.getByXPath("//Connector[contains(@protocol,'HTTP')]");
		if (connectorElement == null) {
			console.print("Invalid runtime %s. Missing connector from server.xml file.", name);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}
		
		String connectorPort = connectorElement.getAttr("port");
		if (connectorPort == null) {
			console.print("Invalid runtime %s. Missing port attribute from server.xml file.", name);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}
		
		try {
			int port = Integer.parseInt(connectorPort);
			config.set("runtime.port", port);
		} catch (NumberFormatException e) {
			console.print("Invalid runtime %s. Not numeric port attribute on server.xml file.", name);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		config.set("runtime.name", name);
		return ExitCode.SUCCESS;
	}
}
