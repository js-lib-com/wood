package js.wood.cli.project;

import java.io.IOException;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;

import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import js.wood.cli.TemplateProcessor;
import js.wood.cli.TemplateType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create named project into current directory.")
public class ProjectCreate extends Task {
	@Option(names = { "-p", "--project-type" }, description = "Project template from ${wood.home}/template/project directory.", required = true)
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

	private TemplateProcessor template = new TemplateProcessor();

	@Override
	protected ExitCode exec() throws IOException {
		Path workingDir = files.getWorkingDir();
		Path projectDir = workingDir.resolve(name);
		if (files.exists(projectDir)) {
			console.print("Project directory %s already existing.", projectDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		console.print("Creating project %s.", name);
		files.createDirectory(projectDir);

		if (author == null) {
			author = config.get("user.name");
		}
		if (author == null) {
			author = console.input("developer name");
		}
		if (title == null) {
			title = console.input("site title");
		}
		if (description == null) {
			description = console.input("project short description");
		}
		if (locale == null) {
			locale = console.input("list of comma separated locale");
		}

		Map<String, String> variables = new HashMap<>();
		variables.put("package", "com.jslib.app");
		variables.put("package-path", variables.get("package").replace('.', '/'));
		variables.put("author", author);
		variables.put("title", title);
		variables.put("description", description);
		variables.put("locale", locale);

		template.setTargetDir(projectDir.toFile());
		template.setVerbose(verbose);
		template.exec(TemplateType.project, type, variables);

		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	void setTemplate(TemplateProcessor template) {
		this.template = template;
	}

	void setName(String name) {
		this.name = name;
	}
}
