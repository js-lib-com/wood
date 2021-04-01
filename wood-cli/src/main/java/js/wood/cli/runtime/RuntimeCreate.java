package js.wood.cli.runtime;

import static java.lang.String.format;

import java.io.File;
import java.util.HashMap;
import java.util.Map;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import js.wood.cli.TemplateProcessor;
import js.wood.cli.TemplateType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "create", description = "Create runtime.")
public class RuntimeCreate extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-r", "--runtime-type" }, description = "Runtime template from ${WOOD_HOME}/template/runtime directory.", required = true)
	private String type;
	@Option(names = { "-p", "--port" }, description = "Listening port. Default: ${DEFAULT-VALUE}", defaultValue = "8080")
	private int port;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Unique runtime name. Default: project name.", arity = "0..1")
	private String name;

	private TemplateProcessor template = new TemplateProcessor();

	@Override
	protected ExitCode exec() throws Exception {
		File projectDir = workingDir();
		if (name == null) {
			name = projectDir.getName();
		}
		print("Creating runtime %s...", name);

		File runtimeDir = new File(config.get("runtime.home", File.class), name);
		if (runtimeDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Runtime %s already existing.", runtimeDir));
		}

		Map<String, String> variables = new HashMap<>();
		variables.put("port", Integer.toString(port));

		template.setTargetDir(runtimeDir);
		template.setVerbose(verbose);
		template.exec(TemplateType.runtime, type, variables);

		config.set("runtime.name", name);
		config.set("runtime.port", port);
		return ExitCode.SUCCESS;
	}
}
