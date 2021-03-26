package js.wood.cli.runtime;

import static java.lang.String.format;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.Arrays;
import java.util.List;

import js.lang.BugError;
import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Spec;

public abstract class RuntimeScriptTask extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about runtime script process.")
	private boolean verbose;

	private final ProcessBuilder builder;

	public RuntimeScriptTask() {
		builder = new ProcessBuilder();
	}

	protected abstract String getScriptName();

	@Override
	protected ExitCode exec() throws Exception {
		String name = config.get("runtime.name");
		File runtimeDir = new File(config.get("runtime.home", File.class), name);
		if (!runtimeDir.exists()) {
			throw new ParameterException(commandSpec.commandLine(), format("Runtime %s does not exist.", runtimeDir));
		}

		File binDir = new File(runtimeDir, "bin");
		if (!binDir.exists()) {
			throw new BugError("Invalid runtime %s. Missing binaries director.", runtimeDir);
		}
		File startupScript = new File(binDir, getScriptName() + getExtension());
		if (!startupScript.exists()) {
			throw new BugError("Invalid runtime. Missing startup script %s.", startupScript);
		}

		List<String> command = Arrays.asList(startupScript.getAbsolutePath());
		builder.command(command);
		builder.environment().put("CATALINA_HOME", runtimeDir.getAbsolutePath());
		builder.redirectErrorStream(true);

		print("Starting runtime %s...", runtimeDir);
		Process process = builder.start();

		Thread stdinReader = new Thread(() -> {
			try (BufferedReader in = new BufferedReader(new InputStreamReader(process.getInputStream()))) {
				String line;
				// when process exits its standard output is closed and input reader returns null
				while ((line = in.readLine()) != null) {
					if (verbose) {
						print(line);
					}
				}
			} catch (IOException e) {
				e.printStackTrace();
			}
		});
		stdinReader.start();

		int exitCode = process.waitFor();
		if (exitCode != 0) {
			print("Error on runtime startup. Exit code: %d.", exitCode);
		}
		stdinReader.join(4000);
		if (verbose) {
			print("Standard input reader thread closed.");
		}

		return ExitCode.SUCCESS;
	}

	private static String getExtension() {
		String osName = System.getProperty("os.name");
		if (osName == null) {
			osName = "Windows";
		}
		return osName.startsWith("Windows") ? ".bat" : ".sh";
	}
}
