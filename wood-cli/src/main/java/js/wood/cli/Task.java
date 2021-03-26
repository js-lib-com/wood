package js.wood.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystem;
import java.nio.file.FileSystems;
import java.nio.file.Path;
import java.nio.file.Paths;

import js.lang.BugError;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public abstract class Task implements Runnable {
	@Option(names = "--time", description = "Measure execution time. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean time;

	protected FileSystem fileSystem;
	protected Console console;
	protected Config config;

	protected Task() {
		this.fileSystem = FileSystems.getDefault();
		this.console = new Console();
	}

	public void setFileSystem(FileSystem fileSystem) {
		this.fileSystem = fileSystem;
	}

	public void setConsole(Console console) {
		this.console = console;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	@Override
	public void run() {
		long start = System.nanoTime();
		ExitCode exitCode = ExitCode.SUCCESS;
		try {
			exitCode = exec();
		} catch (IOException e) {
			e.printStackTrace();
			exitCode = ExitCode.SYSTEM_FAIL;
		} catch (Exception e) {
			e.printStackTrace();
			exitCode = ExitCode.APPLICATION_FAIL;
		}
		if (time) {
			print("Processing time: %.04f msec.", (System.nanoTime() - start) / 1000000.0);
		}
		System.exit(exitCode.ordinal());
	}

	protected abstract ExitCode exec() throws Exception;

	protected static void print(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
	}

	protected static void print() {
		System.out.println();
	}

	private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	protected static String input(String message, String... defaultValue) throws IOException {
		System.out.print("- ");
		System.out.print(message);
		System.out.print(": ");
		if (defaultValue.length == 1) {
			System.out.printf("[%s]: ", defaultValue[0]);
		}
		String value = reader.readLine();
		return value.isEmpty() ? defaultValue[0] : value;
	}

	protected static boolean confirm(String message, String positiveAnswer) throws IOException {
		System.out.print(message);
		System.out.print(": ");
		String answer = reader.readLine();
		return answer.equalsIgnoreCase(positiveAnswer);
	}

	protected Path workingPath() {
		return fileSystem.getPath("").toAbsolutePath();
	}

	protected File workingDir() {
		return fileSystem.getPath("").toAbsolutePath().toFile();
	}

	protected static File projectDir() {
		File projectDir = Paths.get("").toAbsolutePath().toFile();
		File propertiesFile = new File(projectDir, ".project.properties");
		if (!propertiesFile.exists()) {
			throw new BugError("Invalid project. Missing project properties file %s.", propertiesFile);
		}
		return projectDir;
	}

	protected String propertyEOL(String key) {
		String property = System.getProperty(key);
		if (property == null) {
			throw new BugError("Missing property %s.", key);
		}
		return property.endsWith("/") ? property.substring(0, property.length() - 1) : property;
	}
}
