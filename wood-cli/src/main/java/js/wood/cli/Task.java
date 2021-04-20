package js.wood.cli;

import java.io.File;
import java.io.IOException;
import java.io.PrintWriter;
import java.io.StringWriter;
import java.nio.file.FileSystems;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.lang.BugError;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public abstract class Task implements Runnable {
	@Option(names = "--time", description = "Measure execution time. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean time;
	@Option(names = { "-x", "--exception" }, description = "Print stack trace on exception.")
	private boolean stacktrace;

	protected Console console;
	protected Config config;
	protected FilesUtil files;

	protected Task() {
		this.console = new Console();
		this.files = new FilesUtil(FileSystems.getDefault(), this.console);
	}

	protected Task(Task parent) {
		this.console = parent.console;
		this.config = parent.config;
		this.files = parent.files;
	}

	public void setConsole(Console console) {
		this.console = console;
	}

	public void setConfig(Config config) {
		this.config = config;
	}

	public void setFiles(FilesUtil files) {
		this.files = files;
	}

	@Override
	public void run() {
		long start = System.nanoTime();
		ExitCode exitCode = ExitCode.SUCCESS;
		try {
			exitCode = exec();
		} catch (IOException e) {
			handleException(e);
			exitCode = ExitCode.SYSTEM_FAIL;
		} catch (Throwable t) {
			handleException(t);
			exitCode = ExitCode.APPLICATION_FAIL;
		}
		if (time) {
			console.print("Processing time: %.04f msec.", (System.nanoTime() - start) / 1000000.0);
		}
		System.exit(exitCode.ordinal());
	}

	protected abstract ExitCode exec() throws Exception;

	private void handleException(Throwable t) {
		if (stacktrace) {
			StringWriter buffer = new StringWriter();
			t.printStackTrace(new PrintWriter(buffer));
			console.error(buffer);
		} else {
			StringBuilder message = new StringBuilder();
			message.append(t.getClass().getSimpleName());
			if (t.getMessage() != null) {
				message.append(": ");
				message.append(t.getMessage());
			}
			console.error(message.toString());
		}
	}

	// D:\java\wood-1.0\bin\wood-cli-1.0.4-SNAPSHOT.jar
	private static final Pattern JAR_PATH_PATTERN = Pattern.compile("^(.+)[\\\\/]bin[\\\\/].+\\.jar$");

	public static String getWoodHome() {
		File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		Matcher matcher = JAR_PATH_PATTERN.matcher(jarFile.getAbsolutePath());
		if (!matcher.find()) {
			String woodHome = System.getProperty("WOOD_HOME");
			if (woodHome != null) {
				return woodHome;
			}
			throw new BugError("Invalid jar file pattern.");
		}
		return matcher.group(1);
	}
}
