package js.wood.cli.project;

import java.io.File;
import java.io.IOException;
import java.util.List;
import java.util.Properties;

import js.util.Strings;
import js.wood.cli.Task;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;

@Command(name = "create", description = "Create WOOD project.")
public class ProjectCreate extends Task {
	@Option(names = "--type", description = "Project type. Valid values: ${COMPLETION-CANDIDATES}. Default: ${DEFAULT-VALUE}.", defaultValue = "standard")
	private Type type;
	@Option(names = { "-a", "--author" }, description = "Developer name included in generated pages.")
	private String author;
	@Option(names = { "-t", "--title" }, description = "Project title included in generated pages.")
	private String title;
	@Option(names = { "-d", "--description" }, description = "Project description included in generated pages.")
	private String description;
	@Option(names = { "-l", "--locale" }, description = "List of comma separated locale.", split = ",", required = true)
	private List<String> locale;

	@Parameters(index = "0", description = "Project name.")
	private String name;

	@Override
	protected int exec() throws IOException {
		print("Create project %s.", name);
		print(System.getProperty("user.name"));
		print(Strings.join(locale, ':'));

		File projectDir = new File(name);
		if (projectDir.exists()) {
			throw new IOException("Existing directory.");
		}
		projectDir.mkdir();

		Properties properties = new Properties();
		properties.put("author", author);

		createProject("standard", properties);

		return 0;
	}

	private static void createProject(String template, Properties properties) {

	}

	public static enum Type {
		simple, standard, full
	}
}
