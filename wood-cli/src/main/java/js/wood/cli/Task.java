package js.wood.cli;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.nio.file.FileSystems;
import java.nio.file.Paths;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.lang.BugError;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(mixinStandardHelpOptions = true)
public abstract class Task implements Runnable {
	@Option(names = "--time", description = "Measure execution time. Default: ${DEFAULT-VALUE}.", defaultValue = "false")
	private boolean time;

	protected Console console;
	protected Config config;
	protected FilesUtil files;

	protected Task() {
		this.console = new Console();
		this.files = new FilesUtil(FileSystems.getDefault(), this.console);
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

	protected File workingDir() {
		return Paths.get("").toAbsolutePath().toFile();
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
