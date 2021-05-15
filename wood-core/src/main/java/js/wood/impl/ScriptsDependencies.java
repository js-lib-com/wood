package js.wood.impl;

import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.xml.xpath.XPathExpressionException;

import org.xml.sax.SAXException;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;
import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.IScriptDescriptor;
import js.wood.Project;

public class ScriptsDependencies {
	private final Map<String, List<IScriptDescriptor>> dependencies = new HashMap<>();

	public ScriptsDependencies() {
	}

	public void scan(Project project) {
		scan(project.getProjectDir(), project.getExcludes());
	}

	private void scan(DirPath dir, List<DirPath> excludes) {
		dir.files(new FilesHandler() {
			@Override
			public void onDirectory(DirPath dir) {
				if (!excludes.contains(dir)) {
					scan(dir, excludes);
				}
			}

			@Override
			public boolean accept(FilePath file) {
				return file.isComponentDescriptor();
			}

			@Override
			public void onFile(FilePath file) {
				try {
					DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);
					Document document = documentBuilder.loadXML(file.getReader());

					for (Element scriptElement : document.findByXPath("//script")) {
						for (Element dependencyElement : scriptElement.getChildren()) {
							String scriptSource = scriptElement.getAttr("src");
							List<IScriptDescriptor> scriptDependencies = dependencies.get(scriptSource);
							if (scriptDependencies == null) {
								scriptDependencies = new ArrayList<>();
								dependencies.put(scriptSource, scriptDependencies);
							}
							scriptDependencies.add(ScriptDescriptor.create(dependencyElement));
						}
					}
				} catch (IOException | SAXException | XPathExpressionException e) {
				}
			}
		});
	}

	public List<IScriptDescriptor> getScriptDependencies(String source) {
		List<IScriptDescriptor> scriptDependencies = dependencies.get(source);
		return scriptDependencies != null ? scriptDependencies : Collections.emptyList();
	}
}
