package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.IOException;
import java.io.Reader;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;
import com.jslib.commons.cli.TemplateProcessor;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.lang.BugError;
import js.util.Classes;
import js.util.Files;
import js.util.Strings;
import js.wood.WOOD;
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
	protected ExitCode exec() throws IOException, SAXException, XPathExpressionException {
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
		String className = Strings.toTitleCase(compoName);

		if (compoTemplate != null) {
			Path compoTemplateDir = projectDir.resolve(compoTemplate.path());
			if (!files.exists(compoTemplateDir)) {
				console.print("Missing component template %s.", compoTemplateDir);
				console.print("Command abort.");
				return ExitCode.ABORT;
			}

			Path templateLayoutFile = files.getFileByExtension(compoTemplateDir, ".htm");
			if (templateLayoutFile == null) {
				console.print("Missing layout file for component template %s.", compoTemplateDir);
				console.print("Command abort.");
				return ExitCode.ABORT;
			}

			TemplateDocument templateDoc = createTemplateDocument(getNamingStrategy(projectDir.resolve("project.xml")), files.getReader(templateLayoutFile));
			if (!templateDoc.hasEditable()) {
				// throw new BugError("Bad template component %s. Missing editable element.", compoTemplateDir);
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

			variables.put("author", config.get("user.name"));
			variables.put("package", config.get("project.package"));
			variables.put("class", compoName);
			variables.put("className", className);
			variables.put("templateAttr", templateDoc.getTemplateAttrName());
			variables.put("templatePath", compoTemplate.path());
			variables.put("templateParams", templateDoc.getParamAttr(Strings.join(params, ';')));
			variables.put("xmlns", templateDoc.getXmlnsAttr());

			String scriptPath = Strings.concat(config.getex("script.dir"), '/', config.getex("project.package").replace('.', '/'), '/', className, ".js");
			if ("true".equalsIgnoreCase(config.get("compo.script"))) {
				variables.put("compo-script", "true");
			}
			else {
				variables.put("scriptPath", scriptPath);
			}

			if (templateDoc.hasEditable()) {
				variables.put("tag", templateDoc.getEditableTag());
				variables.put("editable", templateDoc.getEditableOperand());
			} else {
				variables.put("tag", templateDoc.getRootTag());
			}

			variables.put("root", page ? "page" : "component");
			variables.put("groupId", "com.js-lib");
			variables.put("artifactId", Files.basename(compoTemplate.path()));
			variables.put("version", "1.0");
			variables.put("title", Strings.toTitleCase(Strings.concat(compoName, page ? " page" : " component")));
			variables.put("description", Strings.concat(Strings.toTitleCase(compoName), page ? " page" : " component", " description."));

			templateProcessor.setTargetDir(compoDir.toFile());
			templateProcessor.setVerbose(verbose);
			templateProcessor.exec("compo", "page", variables);

			Reader reader = templateProcessor.getExcludedFileReader(compoName + ".js");
			if (reader != null) {
				Path scriptFile = projectDir.resolve(scriptPath);
				files.copy(reader, files.getWriter(scriptFile));
			}
		}

		return ExitCode.SUCCESS;
	}

	private String getNamingStrategy(Path projectDescriptorFile) throws IOException, SAXException {
		DocumentBuilder docBuilder = Classes.loadService(DocumentBuilder.class);
		Document doc = docBuilder.loadXMLNS(files.getReader(projectDescriptorFile));
		Element namingElement = doc.getByTag("naming");
		return namingElement != null ? namingElement.getText() : "XMLNS";
	}

	private static TemplateDocument createTemplateDocument(String namingStrategy, Reader reader) throws IOException, SAXException {
		switch (namingStrategy) {
		case "XMLNS":
			return new XmlnsTemplateDoc(reader);
		case "DATA_ATTR":
			return new DataAttrTemplateDoc(reader);
		case "ATTR":
			return new AttrTemplateDoc(reader);
		default:
			throw new BugError("Invalid naming strategy %s.", namingStrategy);
		}
	}

	private static abstract class TemplateDocument {
		private final DocumentBuilder docBuilder = Classes.loadService(DocumentBuilder.class);
		protected final Document doc;
		protected final Element editable;

		protected TemplateDocument(Reader reader) throws IOException, SAXException {
			this.doc = docBuilder.loadXMLNS(reader);
			this.editable = getEditable();
		}

		public String getRootTag() {
			return doc.getRoot().getTag();
		}

		public boolean hasEditable() {
			return editable != null;
		}

		public String getEditableTag() {
			return editable.getTag();
		}

		protected abstract Element getEditable();

		public abstract String getEditableOperand();

		public abstract String getTemplateAttrName();

		public abstract String getParamAttr(String value);

		public abstract String getXmlnsAttr();
	}

	private static class XmlnsTemplateDoc extends TemplateDocument {
		protected XmlnsTemplateDoc(Reader reader) throws IOException, SAXException {
			super(reader);
		}

		@Override
		protected Element getEditable() {
			try {
				return doc.getByXPathNS(WOOD.NS, "//*[@wood:editable]");
			} catch (XPathExpressionException e) {
				// hard coded XPath expression is invalid
				throw new BugError(e);
			}
		}

		@Override
		public String getEditableOperand() {
			return editable.getAttrNS(WOOD.NS, "editable");
		}

		@Override
		public String getTemplateAttrName() {
			return "w:template";
		}

		@Override
		public String getParamAttr(String value) {
			return format("w:param=\"%s\"", value);
		}

		@Override
		public String getXmlnsAttr() {
			return format("xmlns:w=\"%s\"", WOOD.NS);
		}
	}

	private static class DataAttrTemplateDoc extends TemplateDocument {
		protected DataAttrTemplateDoc(Reader reader) throws IOException, SAXException {
			super(reader);
		}

		@Override
		protected Element getEditable() {
			return doc.getByAttr("data-editable");
		}

		@Override
		public String getEditableOperand() {
			return editable.getAttr("data-editable");
		}

		@Override
		public String getTemplateAttrName() {
			return "data-template";
		}

		@Override
		public String getParamAttr(String value) {
			return format("data-param=\"%s\"", value);
		}

		@Override
		public String getXmlnsAttr() {
			return "";
		}
	}

	private static class AttrTemplateDoc extends TemplateDocument {
		protected AttrTemplateDoc(Reader reader) throws IOException, SAXException {
			super(reader);
		}

		@Override
		protected Element getEditable() {
			return doc.getByAttr("editable");
		}

		@Override
		public String getEditableOperand() {
			return editable.getAttr("editable");
		}

		@Override
		public String getTemplateAttrName() {
			return "template";
		}

		@Override
		public String getParamAttr(String value) {
			return format("param=\"%s\"", value);
		}

		@Override
		public String getXmlnsAttr() {
			return "";
		}
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
