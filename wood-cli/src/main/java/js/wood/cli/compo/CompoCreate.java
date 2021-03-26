package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Arrays;
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

	@Option(names = { "-t", "--template" }, description = "Template component name, path relative to project root. Ex: res/template/page", converter = CompoNameConverter.class)
	private CompoName template;
	@Option(names = "--no-script", description = "Do not create script file.")
	private boolean noScript;
	@Option(names = { "-p", "--page" }, description = "Component to create is a page.")
	private boolean page;

	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about created files.")
	private boolean verbose;

	@Parameters(index = "0", description = "Component name, path relative to project root. Ex: res/page/home")
	private String name;

	@Override
	protected ExitCode exec() throws Exception {
		File projectDir = projectDir();

		File compoDir = new File(projectDir, name);
		if (compoDir.exists()) {
			throw new BugError("Component already exist %s.", compoDir);
		}
		print("Create component %s.", compoDir);
		if (!compoDir.mkdirs()) {
			throw new IOException(format("Cannot create component path %s.", compoDir));
		}

		if (template != null) {
			File templateDir = new File(projectDir, template.path());
			if (!templateDir.exists()) {
				throw new BugError("Missing template component %s.", templateDir);
			}
			File templateFile = Arrays.stream(templateDir.listFiles()).filter(file -> file.getPath().endsWith(".htm")).findFirst().get();

			DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
			Document templateDoc = documentBuilder.loadXML(templateFile);
			Element editableElement = templateDoc.getByXPathNS(WOOD.NS, "//*[@editable]");
			if (editableElement == null) {
				throw new BugError("Bad template component %s. Missing editable element.", templateDir);
			}

			List<String> params = new ArrayList<>();
			Pattern paramPattern = Pattern.compile("\\@param\\/([a-z]+)", Pattern.CASE_INSENSITIVE | Pattern.MULTILINE);
			Matcher paramMatcher = paramPattern.matcher(Strings.load(templateFile));
			if (paramMatcher.find()) {
				print();
				print("Template parameters:");
				for (int i = 0; i < paramMatcher.groupCount(); ++i) {
					String param = paramMatcher.group(i + 1);
					params.add(Strings.concat(param, ":", input(format("template %s", param), param)));
				}
				print();
			}

			Map<String, String> variables = new HashMap<>();
			variables.put("page", compoDir.getName());

			variables.put("tag", editableElement.getTag());
			variables.put("class", compoDir.getName());
			variables.put("template", template.path());
			variables.put("editable", editableElement.getAttrNS(WOOD.NS, "editable"));
			variables.put("editable", "section");
			variables.put("template-params", format("w:param=\"%s\"", Strings.join(params, ';')));
			variables.put("xmlns", WOOD.NS);

			variables.put("root", page ? "page" : "component");
			variables.put("groupId", "com.js-lib");
			variables.put("artifactId", Files.basename(template.path()));
			variables.put("version", "1.0");
			variables.put("title", Strings.toTitleCase(Strings.concat(compoDir.getName(), page ? " page" : " component")));
			variables.put("description", Strings.concat(Strings.toTitleCase(compoDir.getName()), page ? " page" : " component", " description."));

			TemplateProcessor processor = new TemplateProcessor(compoDir, verbose);
			processor.exec(TemplateType.compo, "page", variables);

		}

		return ExitCode.SUCCESS;
	}
}
