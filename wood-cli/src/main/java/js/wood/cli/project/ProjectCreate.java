package js.wood.cli.project;

import java.io.File;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;

import js.wood.cli.Task;
import js.wood.cli.TemplateProcessor;
import js.wood.cli.TemplateType;
import js.wood.util.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create named project into current directory.")
public class ProjectCreate extends Task {
	@Option(names = { "-p", "--project-type" }, description = "Project template from ${WOOD_HOME}/template/project directory.", required = true)
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

		TemplateProcessor processor = new TemplateProcessor(projectDir, TemplateType.project, verbose);
		processor.exec(Files.basename(type), variables);

		return 0;
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
