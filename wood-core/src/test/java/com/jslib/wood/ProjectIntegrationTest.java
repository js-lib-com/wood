package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.junit.Assert.assertFalse;

import java.io.File;
import java.io.IOException;
import java.io.Reader;
import java.io.StringWriter;
import java.io.Writer;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import org.junit.Test;

import com.jslib.util.Files;
import com.jslib.wood.impl.IOperatorsHandler;
import com.jslib.wood.impl.ProjectDescriptor;

public class ProjectIntegrationTest implements IReferenceHandler {
	private Project project;

	@Test
	public void createProject() throws IOException {
		File projectRoot = new File("src/test/resources/project");
		project = Project.create(projectRoot);

		assertThat(project, notNullValue());
		assertThat(project.getProjectRoot(), equalTo(projectRoot));
		assertThat(project.getBuildDir().value(), equalTo("target/site/"));
		assertThat(project.getAssetDir().value(), equalTo("res/asset/"));
		assertThat(project.getThemeDir().value(), equalTo("res/theme/"));

		assertDescriptor();
		assertManifest();

		assertThat(project.getAuthors(), equalTo(Arrays.asList("Iulian Rotaru", "Lucian Rotaru")));
		//assertThat(project.getTitle(), equalTo("Project"));
		assertThat(project.getManifest().value(), equalTo("manifest.json"));
		assertThat(project.getServiceWorker().value(), equalTo("worker.js"));

		List<Locale> locales = project.getLocales();
		assertThat(locales, hasSize(2));
		assertThat(locales.get(0), equalTo(Locale.forLanguageTag("en")));
		assertThat(locales.get(1), equalTo(Locale.forLanguageTag("ro")));

		assertThat(project.getMediaQueryDefinition("portrait"), notNullValue());
		assertThat(project.getMediaQueryDefinition("portrait").getExpression(), equalTo("orientation: portrait"));
		assertThat(project.getMediaQueryDefinition("xsd"), notNullValue());
		assertThat(project.getMediaQueryDefinition("xsd").getExpression(), equalTo("max-width: 560px"));
		assertThat(project.getMediaQueryDefinition("h700"), notNullValue());
		assertThat(project.getMediaQueryDefinition("h700").getExpression(), equalTo("max-height: 700px"));

		List<IMetaDescriptor> metas = project.getMetaDescriptors();
		assertThat(metas, notNullValue());

		List<ILinkDescriptor> links = project.getLinkDescriptors();
		assertThat(links, notNullValue());

		List<IScriptDescriptor> scripts = project.getScriptDescriptors();
		assertThat(scripts, notNullValue());

		List<IScriptDescriptor> dependencies = project.getScriptDependencies("");
		assertThat(dependencies, notNullValue());
		
		ThemeStyles themStyles = project.getThemeStyles();
		assertThat(themStyles, notNullValue());
		
		IOperatorsHandler operatorsHandler=project.getOperatorsHandler();
		assertThat(operatorsHandler, notNullValue());
		
		assertFalse(project.hasNamespace());
	}

	private void assertDescriptor() {
		ProjectDescriptor descriptor = project.getDescriptor();
		assertThat(descriptor, notNullValue());
	}

	private void assertManifest() throws IOException {
		FilePath manifestFile = project.getManifest();
		Reader reader = new SourceReader(manifestFile, this);
		Writer writer = new StringWriter();
		Files.copy(reader, writer);
	}

	// --------------------------------------------------------------------------------------------

	private static final Map<Reference, String> VARIABLES = new HashMap<>();
	static {
		VARIABLES.put(new Reference(Reference.Type.STRING, "app-name"), "app");
		VARIABLES.put(new Reference(Reference.Type.STRING, "app-short-name"), "app");
		VARIABLES.put(new Reference(Reference.Type.STRING, "app-description"), "App description.");
	}

	@Override
	public String onResourceReference(Reference reference, FilePath sourceFile) throws IOException, WoodException {
		Locale locale = new Locale("en");
		if (reference.isVariable()) {
			String value = VARIABLES.get(reference);
			if (value == null) {
				throw new WoodException("Missing variable value for reference |%s| from source |%s|.", reference, sourceFile);
			}
			return value;
		}

		// discover media file and returns its absolute URL path
		FilePath mediaFile = sourceFile.getProject().getResourceFile(locale, reference, sourceFile);
		if (mediaFile == null) {
			throw new WoodException("Missing media file for reference |%s| from source |%s|.", reference, sourceFile);
		}

		StringBuilder builder = new StringBuilder();
		builder.append(mediaFile.getParentDir().value());
		builder.append(mediaFile.getName());
		return builder.toString();
	}
}
