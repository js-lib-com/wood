package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.IOException;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.lang.BugError;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;
import js.wood.WOOD;
import js.wood.cli.ExitCode;
import js.wood.cli.Task;
import js.wood.cli.TemplateProcessor;
import js.wood.cli.TemplateType;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.Option;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "create", description = "Create component.")
public class CompoCreate extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Option(names = { "-t", "--template" }, description = "Template component name, path relative to project root. Ex: res/template/page", converter = CompoNameConverter.class, paramLabel = "template")
	private CompoName compoTemplate;
	@Option(names = "--no-script", description = "Do not create script file.")
	private boolean noScript;
	@Option(names = { "-p", "--page" }, description = "Component to create is a page.")
	private boolean page;

	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component name, path relative to project root. Ex: res/page/home")
	private String name;

	private TemplateProcessor templateProcessor = new TemplateProcessor();

	@Override
	protected ExitCode exec() throws IOException {
		Path projectDir = files.getProjectDir();
		Path compoDir = projectDir.resolve(name);
		if (files.exists(compoDir)) {
			console.print("Component %s already existing.", compoDir);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		console.print("Create component %s.", compoDir);
		files.createDirectory(compoDir);
		String compoName = files.getFileName(compoDir);

		if (compoTemplate != null) {
			Path compoTemplateDir = projectDir.resolve(compoTemplate.path());
			if (!files.exists(compoTemplateDir)) {
				console.print("Missing component template %s.", compoTemplateDir);
				console.print("Command abort.");
				return ExitCode.ABORT;
			}

			Path templateLayoutFile = files.findFile(compoTemplateDir, "htm");
			if (templateLayoutFile == null) {
				console.print("Missing layout file for component template %s.", compoTemplateDir);
				console.print("Command abort.");
				return ExitCode.ABORT;
			}

			DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
			Document templateDoc = documentBuilder.loadXML(files.getReader(templateLayoutFile));
			Element editableElement = templateDoc.getByXPathNS(WOOD.NS, "//*[@editable]");
			if (editableElement == null) {
				throw new BugError("Bad template component %s. Missing editable element.", compoTemplateDir);
			}

			List<String> params = new ArrayList<>();
			Pattern paramPattern = Pattern.compile("\\@param\\/([a-z]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
			Matcher paramMatcher = paramPattern.matcher(Strings.load(files.getReader(templateLayoutFile)));
			if (paramMatcher.find()) {
				console.crlf();
				console.print("Template parameters:");
				for (int i = 0; i < paramMatcher.groupCount(); ++i) {
					String param = paramMatcher.group(i + 1);
					params.add(Strings.concat(param, ":", console.input(format("template %s", param), param)));
				}
				console.crlf();
			}

			Map<String, String> variables = new HashMap<>();
			variables.put("page", compoName);

			variables.put("tag", editableElement.getTag());
			variables.put("class", compoName);
			variables.put("template", compoTemplate.path());
			variables.put("editable", editableElement.getAttrNS(WOOD.NS, "editable"));
			variables.put("editable", "section");
			variables.put("template-params", format("w:param=\"%s\"", Strings.join(params, ';')));
			variables.put("xmlns", WOOD.NS);

			variables.put("root", page ? "page" : "component");
			variables.put("groupId", "com.js-lib");
			variables.put("artifactId", Files.basename(compoTemplate.path()));
			variables.put("version", "1.0");
			variables.put("title", Strings.toTitleCase(Strings.concat(compoName, page ? " page" : " component")));
			variables.put("description", Strings.concat(Strings.toTitleCase(compoName), page ? " page" : " component", " description."));

			templateProcessor.setTargetDir(compoDir.toFile());
			templateProcessor.setVerbose(verbose);
			templateProcessor.exec(TemplateType.compo, "page", variables);

		}

		return ExitCode.SUCCESS;
	}

	// --------------------------------------------------------------------------------------------
	// Tests support

	void setCompoTemplate(CompoName compoTemplate) throws IOException {
		this.compoTemplate = compoTemplate;
	}

	void setTemplateProcessor(TemplateProcessor processor) {
		this.templateProcessor = processor;
	}
}
