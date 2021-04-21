package js.wood.update;

import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.Arrays;
import java.util.List;
import java.util.Properties;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

public class Main {
	public static void main(String... args) throws InterruptedException, IOException {
		int exitCode = 0;
		try {
			boolean verbose = args.length == 1 && args[0].equals("--verbose");
			Main main = new Main(verbose);
			exitCode = main.exec(args);
		} catch (Throwable t) {
			t.printStackTrace();
			exitCode = -1;
		}
		System.exit(exitCode);
	}

	// D:\java\wood-1.0\bin\wood-update-1.0.4-SNAPSHOT.jar
	private static final Pattern JAR_PATH_PATTERN = Pattern.compile("^(.+)[\\\\/]bin[\\\\/](.+\\.jar)$");

	// D:\java\wood-1.0\bin\wood-assembly-1.0.5-SNAPSHOT.zip
	private static final Pattern ASSEMBLY_FILE_PATTERN = Pattern.compile("^wood-assembly.+\\.zip$");

	private final boolean verbose;

	protected Main(boolean verbose) {
		this.verbose = verbose;
	}

	public int exec(String... args) throws IOException {
		File jarFile = new File(Main.class.getProtectionDomain().getCodeSource().getLocation().getPath());
		Matcher matcher = JAR_PATH_PATTERN.matcher(jarFile.getAbsolutePath());
		if (!matcher.find()) {
			throw new RuntimeException("Invalid updater jar file pattern.");
		}
		File homeDir = new File(matcher.group(1));
		String updaterJar = matcher.group(2);

		File lockFile = new File(homeDir, ".lock");
		while (lockFile.exists()) {
			try {
				Thread.sleep(250);
			} catch (InterruptedException ignore) {
			}
		}
		

		print("");
		print("WOOD Updater: %s", updaterJar);

		File assemblyFile = assemblyFile(homeDir);
		if (assemblyFile == null) {
			throw new RuntimeException("Invalid WOOD updater invocation. Missing downloaded assembly file.");
		}
		File propertiesFile = new File(homeDir, "bin/wood.properties");

		print("WOOD Assembly: %s...", assemblyFile.getName());

		if (verbose) {
			print("");
		}
		print("Removing installed files...");
		cleanDirectory(new File(homeDir, "bin"), propertiesFile);
		cleanDirectory(new File(homeDir, "lib"));
		cleanDirectory(new File(homeDir, "manual"));

		if (verbose) {
			print("");
		}
		print("Copying new files...");
		try (ZipInputStream zipInputStream = new ZipInputStream(new FileInputStream(assemblyFile))) {
			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				String fileName = zipFileName(zipEntry.getName());
				if (fileName.isEmpty() || fileName.endsWith("/")) {
					continue;
				}

				if (fileName.equals("bin/wood.properties")) {
					mergeProperties(zipInputStream, propertiesFile);
					continue;
				}

				copy(zipInputStream, new File(homeDir, fileName));
			}
		}

		assemblyFile.delete();
		if (verbose) {
			print("");
		}
		print("Update complete. Press Enter.");
		return 0;
	}

	private void cleanDirectory(File dir, File... excludes) throws IOException {
		cleanDirectory(dir, Arrays.asList(excludes));
	}

	private void cleanDirectory(File dir, List<File> excludes) throws IOException {
		File[] files = dir.listFiles();
		if (files == null) {
			throw new IOException(format("Fail to list files from %s", dir));
		}

		for (File file : files) {
			if (file.isDirectory()) {
				cleanDirectory(file, excludes);
			}
			if (!excludes.contains(file)) {
				if (verbose) {
					print("Delete %s", file);
				}
				file.delete();
			}
		}
	}

	private void mergeProperties(ZipInputStream zipInputStream, File propertiesFile) throws IOException {
		if (verbose) {
			print("Copy file %s", propertiesFile);
			print("Merge global properties %s", propertiesFile);
		}

		Properties assemblyProperties = new Properties();
		assemblyProperties.load(zipInputStream);

		Properties properties = new Properties();
		try (FileInputStream inputStream = new FileInputStream(propertiesFile)) {
			properties.load(inputStream);
		}

		assemblyProperties.forEach((key, value) -> properties.merge(key, value, (oldValue, newValue) -> newValue));

		try (FileOutputStream outputStream = new FileOutputStream(propertiesFile)) {
			properties.store(outputStream, null);
		}
	}

	private void copy(ZipInputStream zipInputStream, File outputFile) throws IOException {
		if (verbose) {
			print("Copy file %s", outputFile);
		}
		outputFile.getParentFile().mkdirs();

		byte[] buffer = new byte[4096];
		try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(outputFile), buffer.length)) {
			int len;
			while ((len = zipInputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, len);
			}
		}
	}

	private static File assemblyFile(File homeDir) throws IOException {
		File[] files = homeDir.listFiles();
		if (files == null) {
			throw new IOException(format("Fail to list files from %s", homeDir));
		}

		for (File file : files) {
			Matcher matcher = ASSEMBLY_FILE_PATTERN.matcher(file.getName());
			if (matcher.find()) {
				return file;
			}
		}
		return null;
	}

	private static String zipFileName(String zipEntityName) {
		int index = zipEntityName.indexOf('/') + 1;
		return zipEntityName.substring(index);
	}

	private static void print(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
	}
}
