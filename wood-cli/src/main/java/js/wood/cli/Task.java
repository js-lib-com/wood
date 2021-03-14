package js.wood.cli;

import java.io.File;
import java.nio.file.Paths;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public abstract class Task implements Runnable {
	@Option(names = "--time", description = "Measure execution time. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean time;

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

	protected static File workingDir() {
		return Paths.get("").toAbsolutePath().toFile();
	}
}
