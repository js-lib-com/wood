package js.wood.cli.project;

import java.io.BufferedOutputStream;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.Reader;
import java.io.Writer;
import java.util.HashMap;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import js.io.VariablesWriter;
import js.wood.cli.Task;
import js.wood.cli.TemplateType;
import js.wood.util.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create named project into current directory.")
public class ProjectCreate extends Task {
	@Option(names = { "-z", "--type" }, description = "Project template from ${WOOD_HOME}/template/project directory.")
	private String type;

	@Option(names = { "-a", "--author" }, description = "Developer name.")
	private String author;
	@Option(names = { "-t", "--title" }, description = "Site title.")
	private String title;
	@Option(names = { "-d", "--description" }, description = "Project short description.")
	private String description;
	@Option(names = { "-l", "--locale" }, description = "List of comma separated locale.")
	private String locale;

	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Project name.")
	private String name;

	@Override
	protected int exec() throws IOException {
		File projectDir = new File(name);
		if (projectDir.exists()) {
			throw new IOException("Existing directory.");
		}
		print("Creating project %s.", name);
		if (!projectDir.mkdir()) {
			throw new IOException("Fail to create project directory.");
		}

		if (author == null) {
			author = System.getProperty("user.name");
		}
		if (author == null) {
			author = input("developer name");
		}
		if (title == null) {
			title = input("site title");
		}
		if (description == null) {
			description = input("project short description");
		}
		if (locale == null) {
			locale = input("list of comma separated locale");
		}

		Map<String, String> variables = new HashMap<>();
		variables.put("author", author);
		variables.put("title", title);
		variables.put("description", description);
		variables.put("locale", locale);

		createProject(projectDir, Files.basename(type), variables);

		return 0;
	}

	private void createProject(File projectDir, String templateName, Map<String, String> variables) throws IOException {
		try (ZipInputStream zipInputStream = template(TemplateType.project, templateName)) {
			ZipEntry zipEntry;
			while ((zipEntry = zipInputStream.getNextEntry()) != null) {
				String zipEntryName = zipEntry.getName();

				if (zipEntryName.endsWith("/")) {
					mkdirs(projectDir, zipEntryName);
					continue;
				}

				if (zipEntryName.startsWith("$")) {
					copy(projectDir, zipInputStream, zipEntryName, variables);
				} else {
					copy(projectDir, zipInputStream, zipEntryName);
				}
			}
		}
	}

	private void mkdirs(File projectDir, String dirName) throws IOException {
		File dir = new File(projectDir, dirName);
		if (verbose) {
			print("Create directory '%s'.", dir);
		}
		if (!dir.mkdirs()) {
			throw new IOException("Cannot create directory " + dir);
		}
	}

	private void copy(File projectDir, ZipInputStream zipInputStream, String zipEntryName, Map<String, String> variables) throws IOException {
		char[] buffer = new char[2048];

		// by convention, for formatted files, ZIP entry name starts with dollar ($)
		File file = new File(projectDir, zipEntryName.substring(1));
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

	private void copy(File projectDir, ZipInputStream zipInputStream, String zipEntryName) throws IOException {
		byte[] buffer = new byte[2048];

		File file = new File(projectDir, zipEntryName);
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

	// --------------------------------------------------------------------------------------------
	// Test support

	void setName(String name) {
		this.name = name;
	}

	void setAuthor(String author) {
		this.author = author;
	}

	void setLocale(String locale) {
		this.locale = locale;
	}
}
