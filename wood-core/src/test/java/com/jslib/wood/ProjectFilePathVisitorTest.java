package com.jslib.wood;

import com.jslib.wood.dom.Element;
import com.jslib.wood.impl.ScriptDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.StringReader;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for visitor on project files scanning. Note that current version of the file path visitor just scans component
 * descriptors and collects scripts' dependencies. As a consequence these tests are only about scripts' dependencies.
 */
@RunWith(MockitoJUnitRunner.class)
public class ProjectFilePathVisitorTest {
    @Mock
    private Project project;
    @Mock
    private FilePath file;

    // map key is the script source that is guaranteed to be unique
    private Map<String, List<IScriptDescriptor>> scriptDependencies;
    private Project.IFilePathVisitor visitor;

    @Before
    public void beforeTest() {
        when(file.isComponentDescriptor()).thenReturn(true);
        scriptDependencies = new HashMap<>();
        visitor = new Project.FilePathVisitor(scriptDependencies);
    }

    @Test
    public void GivenCompoDescriptorWithScriptLocalDependency_ThenCollectScriptDependency() throws Exception {
        // GIVEN
        String document = "<compo>" + //
                "	<script src='lib/geo-map'>" + //
                "		<dependency src='lib/js-lib/js-lib.js'></dependency>" + //
                "	</script>" + //
                "</compo>";
        when(file.getReader()).thenReturn(new StringReader(document));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(1));
        assertTrue(scriptDependencies.containsKey("lib/geo-map"));

        List<IScriptDescriptor> dependencies = scriptDependencies.get("lib/geo-map");
        assertThat(dependencies, notNullValue());
        assertThat(dependencies, hasSize(1));
        assertThat(dependencies.get(0), notNullValue());
        assertThat(dependencies.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
    }

    @Test
    public void GivenDescriptorIsTemplateDescriptor_ThenStillCollectScriptDependency() throws Exception {
        // GIVEN
        String document = "<template>" + //
                "	<script src='lib/geo-map'>" + //
                "		<dependency src='lib/js-lib/js-lib.js'></dependency>" + //
                "	</script>" + //
                "</template>";
        when(file.getReader()).thenReturn(new StringReader(document));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(1));
        assertTrue(scriptDependencies.containsKey("lib/geo-map"));

        List<IScriptDescriptor> dependencies = scriptDependencies.get("lib/geo-map");
        assertThat(dependencies, notNullValue());
        assertThat(dependencies, hasSize(1));
        assertThat(dependencies.get(0), notNullValue());
        assertThat(dependencies.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
    }

    @Test
    public void GivenDescriptorIsCustomDescriptor_ThenStillCollectScriptDependency() throws Exception {
        // GIVEN
        String document = "<custom-descriptor>" + //
                "	<script src='lib/geo-map'>" + //
                "		<dependency src='lib/js-lib/js-lib.js'></dependency>" + //
                "	</script>" + //
                "</custom-descriptor>";
        when(file.getReader()).thenReturn(new StringReader(document));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(1));
        assertTrue(scriptDependencies.containsKey("lib/geo-map"));

        List<IScriptDescriptor> dependencies = scriptDependencies.get("lib/geo-map");
        assertThat(dependencies, notNullValue());
        assertThat(dependencies, hasSize(1));
        assertThat(dependencies.get(0), notNullValue());
        assertThat(dependencies.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
    }

    @Test
    public void GivenCompoDescriptorWithScriptThirdPartyDependency_ThenCollectScriptDependency() throws Exception {
        // GIVEN
        String document = "<compo>" + //
                "	<script src='lib/geo-map'>" + //
                "		<dependency src='http://js-lib.com/library.js'></dependency>" + //
                "	</script>" + //
                "</compo>";
        when(file.getReader()).thenReturn(new StringReader(document));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(1));
        assertTrue(scriptDependencies.containsKey("lib/geo-map"));

        List<IScriptDescriptor> dependencies = scriptDependencies.get("lib/geo-map");
        assertThat(dependencies, notNullValue());
        assertThat(dependencies, hasSize(1));
        assertThat(dependencies.get(0), notNullValue());
        assertThat(dependencies.get(0).getSource(), equalTo("http://js-lib.com/library.js"));
    }

    @Test
    public void GivenCompoDescriptorWithScriptMultipleDependencies_ThenCollectAllScriptDependencies() throws Exception {
        // GIVEN
        String document = "<compo>" + //
                "	<script src='lib/geo-map'>" + //
                "		<dependency src='http://google.com/lib.js'></dependency>" + //
                "		<dependency src='lib/js-lib/js-lib.js'></dependency>" + //
                "	</script>" + //
                "</compo>";
        when(file.getReader()).thenReturn(new StringReader(document));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(1));
        assertTrue(scriptDependencies.containsKey("lib/geo-map"));

        List<IScriptDescriptor> dependencies = scriptDependencies.get("lib/geo-map");
        assertThat(dependencies, notNullValue());
        assertThat(dependencies, hasSize(2));
        assertThat(dependencies.get(0), notNullValue());
        assertThat(dependencies.get(0).getSource(), equalTo("http://google.com/lib.js"));
        assertThat(dependencies.get(1), notNullValue());
        assertThat(dependencies.get(1).getSource(), equalTo("lib/js-lib/js-lib.js"));
    }

    @Test
    public void GivenAlreadyRegisteredScriptWithDependency_ThenAddDependencyFromCompoDescriptor() throws Exception {
        // GIVEN
        IScriptDescriptor dependency = mock(IScriptDescriptor.class);
        when(dependency.getSource()).thenReturn("http://google.com/lib.js");
        List<IScriptDescriptor> dependencies = new ArrayList<>();
        dependencies.add(dependency);
        scriptDependencies.put("lib/geo-map", dependencies);

        String document = "<compo>" + //
                "	<script src='lib/geo-map'>" + //
                "		<dependency src='lib/js-lib/js-lib.js'></dependency>" + //
                "	</script>" + //
                "</compo>";
        when(file.getReader()).thenReturn(new StringReader(document));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(1));
        assertTrue(scriptDependencies.containsKey("lib/geo-map"));

        dependencies = scriptDependencies.get("lib/geo-map");
        assertThat(dependencies, notNullValue());
        assertThat(dependencies, hasSize(2));
        assertThat(dependencies.get(0), notNullValue());
        assertThat(dependencies.get(0).getSource(), equalTo("http://google.com/lib.js"));
        assertThat(dependencies.get(1), notNullValue());
        assertThat(dependencies.get(1).getSource(), equalTo("lib/js-lib/js-lib.js"));
    }

    @Test
    public void GivenAlreadyRegisteredScriptWithSameDependency_ThenDoNotAddDependencyFromCompoDescriptor() throws Exception {
        // GIVEN
        Element scriptElement = mock(Element.class);
        when(scriptElement.getAttr("src")).thenReturn("lib/js-lib/js-lib.js");
        IScriptDescriptor dependency = ScriptDescriptor.create(scriptElement);
        List<IScriptDescriptor> dependencies = new ArrayList<>();
        dependencies.add(dependency);
        scriptDependencies.put("lib/geo-map", dependencies);
        assertThat(dependencies, hasSize(1));

        String document = "<compo>" + //
                "	<script src='lib/geo-map'>" + //
                "		<dependency src='lib/js-lib/js-lib.js'></dependency>" + //
                "	</script>" + //
                "</compo>";
        when(file.getReader()).thenReturn(new StringReader(document));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(1));
        assertTrue(scriptDependencies.containsKey("lib/geo-map"));

        dependencies = scriptDependencies.get("lib/geo-map");
        assertThat(dependencies, notNullValue());
        assertThat(dependencies, hasSize(1));
        assertThat(dependencies.get(0), notNullValue());
        assertThat(dependencies.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
    }

    @Test
    public void GivenCompoDescriptorWithScriptWithoutDependency_ThenNoDependenciesCollected() throws Exception {
        // GIVEN
        String document = "<compo>" + //
                "	<script src='lib/geo-map'></script>" + //
                "</compo>";
        when(file.getReader()).thenReturn(new StringReader(document));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(0));
    }

    @Test
    public void GivenCompoDescriptorWithoutScriptElement_ThenNoDependenciesCollected() throws Exception {
        // GIVEN
        when(file.getReader()).thenReturn(new StringReader("<compo></compo>"));

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(0));
    }

    @Test
    public void GivenFileNotCompoDescriptor_ThenNoDependenciesCollected() throws Exception {
        // GIVEN
        when(file.isComponentDescriptor()).thenReturn(false);

        // WHEN
        visitor.visitFile(project, file);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(0));
    }

    @Test
    public void GivenFileIsADirectory_ThenNoDependenciesCollected() throws Exception {
        // GIVEN
        FilePath dir = mock(FilePath.class);

        // WHEN
        visitor.visitFile(project, dir);

        // THEN
        assertThat(scriptDependencies.keySet(), hasSize(0));
    }
}
