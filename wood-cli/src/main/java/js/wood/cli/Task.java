package js.wood.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.Paths;

import js.lang.BugError;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public abstract class Task implements Runnable {
	@Option(names = "--time", description = "Measure execution time. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean time;

	protected Config config;

	void setConfig(Config config) {
		this.config = config;
	}

	@Override
	public void run() {
		long start = System.nanoTime();
		int exitCode = 0;
		try {
			exitCode = exec();
		} catch (Exception e) {
			e.printStackTrace();
			exitCode = -1;
		}
		if (time) {
			print("Processing time: %.04f msec.", (System.nanoTime() - start) / 1000000.0);
		}
		System.exit(exitCode);
	}

	protected abstract int exec() throws Exception;

	protected static void print(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
	}

	protected static void print() {
		System.out.println();
	}

	private static final BufferedReader reader = new BufferedReader(new InputStreamReader(System.in));

	protected static String input(String message) throws IOException {
		System.out.print("- ");
		System.out.print(message);
		System.out.print(": ");
		return reader.readLine();
	}

	protected static boolean confirm(String message, String positiveAnswer) throws IOException {
		System.out.println(message);
		String answer = reader.readLine();
		return answer.equalsIgnoreCase(positiveAnswer);
	}

	protected static File workingDir() {
		return Paths.get("").toAbsolutePath().toFile();
	}

	protected String propertyEOL(String key) {
		String property = System.getProperty(key);
		if (property == null) {
			throw new BugError("Missing property %s.", key);
		}
		return property.endsWith("/") ? property.substring(0, property.length() - 1) : property;
	}
}
