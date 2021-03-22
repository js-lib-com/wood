package js.wood.cli;

import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import js.io.VariablesWriter;
import js.lang.BugError;
import js.util.Strings;

public class TemplateProcessor {
	private final File targetDir;
	private final boolean verbose;

	public TemplateProcessor(File targetDir, boolean verbose) {
		this.targetDir = targetDir;
		this.verbose = verbose;
	}

	public void exec(TemplateType type, String templateName, Map<String, String> variables) throws IOException {
		try (ZipInputStream zipInputStream = template(type, templateName)) {
			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				String zipEntryName = Strings.injectVariables(zipEntry.getName(), variables);

				if (zipEntryName.endsWith("/")) {
					mkdirs(zipEntryName);
					continue;
				}

				String[] zipEntryNameSegments = zipEntryName.split("/");
				String fileName = zipEntryNameSegments[zipEntryNameSegments.length - 1];
				// by convention, for formatted files, file name starts with dollar ($)
				if (fileName.startsWith("!")) {
					zipEntryNameSegments[zipEntryNameSegments.length - 1] = fileName.substring(1);
					copy(zipInputStream, Strings.join(zipEntryNameSegments, '/'), variables);
				} else {
					copy(zipInputStream, zipEntryName);
				}
			}
		}
	}

	private void mkdirs(String path) throws IOException {
		File dir = new File(targetDir, path);
		if (verbose) {
			print("Create directory '%s'.", dir);
		}
		if (!dir.mkdirs()) {
			throw new IOException("Cannot create directory " + dir);
		}
	}

	private void copy(ZipInputStream zipInputStream, String zipEntryName, Map<String, String> variables) throws IOException {
		char[] buffer = new char[2048];

		File file = new File(targetDir, zipEntryName);
		if (verbose) {
			print("Create file '%s'.", file);
		}

		Reader reader = new InputStreamReader(zipInputStream);
		try (Writer writer = new VariablesWriter(new BufferedWriter(new FileWriter(file)), variables)) {
			int len;
			while ((len = reader.read(buffer)) > 0) {
				writer.write(buffer, 0, len);
			}
		}
	}

	private void copy(ZipInputStream zipInputStream, String zipEntryName) throws IOException {
		byte[] buffer = new byte[2048];

		File file = new File(targetDir, zipEntryName);
		if (verbose) {
			print("Create file '%s'.", file);
		}

		try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(new FileOutputStream(file), buffer.length)) {
			int len;
			while ((len = zipInputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, len);
			}
		}
	}

	private static ZipInputStream template(TemplateType type, String name) throws FileNotFoundException {
		String woodHome = System.getProperty("WOOD_HOME");
		if (woodHome == null) {
			throw new BugError("Missing property WOOD_HOME.");
		}

		File woodHomeDir = new File(woodHome);
		File templateFile = new File(woodHomeDir, Strings.concat("template", File.separatorChar, type, File.separatorChar, name, ".zip"));
		return new ZipInputStream(new BufferedInputStream(new FileInputStream(templateFile)));
	}

	protected static void print(String format, Object... args) {
		System.out.printf(format, args);
		System.out.println();
	}
}
