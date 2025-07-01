package com.jslib.wood;

import com.jslib.wood.impl.IOperatorsHandler;
import com.jslib.wood.impl.ProjectDescriptor;
import com.jslib.wood.util.FilesUtil;
import org.junit.Test;

import java.io.*;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertFalse;

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
        assertThat(project.getTitle(), equalTo("Project"));
        assertThat(project.getPwaManifest().value(), equalTo("manifest.json"));
        assertThat(project.getPwaWorker().value(), equalTo("worker.js"));

        List<String> languages = project.getLanguages();
        assertThat(languages, hasSize(2));
        assertThat(languages.get(0), equalTo("en"));
        assertThat(languages.get(1), equalTo("ro"));

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

        IOperatorsHandler operatorsHandler = project.getOperatorsHandler();
        assertThat(operatorsHandler, notNullValue());

        assertFalse(project.hasNamespace());
    }

    private void assertDescriptor() {
        ProjectDescriptor descriptor = project.getDescriptor();
        assertThat(descriptor, notNullValue());
    }

    private void assertManifest() throws IOException {
        FilePath manifestFile = project.getPwaManifest();
        Reader reader = new SourceReader(manifestFile, this);
        Writer writer = new StringWriter();
        FilesUtil.copy(reader, writer);
    }

    // --------------------------------------------------------------------------------------------

    private static final Map<Reference, String> VARIABLES = new HashMap<>();

    static {
        VARIABLES.put(new Reference(Reference.Type.STRING, "app-name"), "app");
        VARIABLES.put(new Reference(Reference.Type.STRING, "app-short-name"), "app");
        VARIABLES.put(new Reference(Reference.Type.STRING, "app-description"), "App description.");
    }

    @Override
    public String onResourceReference(Reference reference, FilePath sourceFile) throws WoodException {
        String language = "en";
        if (reference.isVariable()) {
            String value = VARIABLES.get(reference);
            if (value == null) {
                throw new WoodException("Missing variable value for reference |%s| from source |%s|.", reference, sourceFile);
            }
            return value;
        }

        // discover media file and returns its absolute URL path
        FilePath mediaFile = sourceFile.getProject().getResourceFile(language, reference, sourceFile);
        if (mediaFile == null) {
            throw new WoodException("Missing media file for reference |%s| from source |%s|.", reference, sourceFile);
        }

        return mediaFile.getParentDir().value() + mediaFile.getName();
    }
}
