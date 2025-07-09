package com.jslib.wood;

import com.jslib.wood.impl.MediaQueryDefinition;
import com.jslib.wood.impl.ProjectDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.junit.Assert.assertTrue;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectInitTest {
	@Mock
	private ProjectDescriptor descriptor;

	@Before
	public void beforeTest() throws Exception {
		when(descriptor.getBuildDir()).thenReturn("build");
		when(descriptor.getAssetDir()).thenReturn("res/asset");
		when(descriptor.getThemeDir()).thenReturn("res/theme");
	}

	private Project project() {
		return new Project(new File("."), descriptor);
	}

	@Test
	public void GivenNonEmptyDescriptor_WhenConstructor_ThenStateInitialized() {
		// GIVEN
		when(descriptor.getAuthors()).thenReturn(Collections.singletonList("Iulian Rotaru"));
		when(descriptor.getFavicon()).thenReturn("favicon.ico");
		when(descriptor.getPwaWorker()).thenReturn("sw.js");
		when(descriptor.getLanguage()).thenReturn(Arrays.asList("fr","de"));
		when(descriptor.getExcludeDirs()).thenReturn(Arrays.asList("res/page/trivia/", "res/page/experiment/"));

		// WHEN
		File projectRoot = new File("root/path/project");
		Project project = new Project(projectRoot, descriptor);

		// THEN
		assertThat(project.getProjectRoot(), equalTo(new File("root/path/project")));
		assertThat(project.getDescriptor(), equalTo(descriptor));

		assertThat(project.getBuildDir().value(), equalTo("build/"));
		assertThat(project.getAssetDir().value(), equalTo("res/asset/"));
		assertThat(project.getThemeDir().value(), equalTo("res/theme/"));
		assertThat(project.getPwaManifest().value(), equalTo("manifest.json"));
		assertThat(project.getFavicon().value(), equalTo("favicon.ico"));
		assertThat(project.getPwaWorker().value(), equalTo("sw.js"));

		assertThat(project.getAuthors(), contains("Iulian Rotaru"));
		assertThat(project.getTitle(), nullValue());

		assertThat(project.getLanguages(), hasSize(2));
		assertThat(project.getLanguages(), contains("fr", "de"));
		assertThat(project.getDefaultLanguage(), equalTo("fr"));

		assertThat(project.getExcludes(), hasSize(3));
		assertTrue(project.getExcludes().contains(new File(projectRoot, "res/page/trivia/")));
		assertTrue(project.getExcludes().contains(new File(projectRoot, "res/page/experiment/")));
		assertTrue(project.getExcludes().contains(new File(projectRoot, "build")));
	}

	@Test
	public void GivenMediaQueryOnDescriptor_WhenGetMediaQueryDefinition_ThenNotNull() {
		// GIVEN
		when(descriptor.getMediaQueryDefinitions()).thenReturn(Collections.singletonList(new MediaQueryDefinition("w800","screen", "min-width: 800px")));

		// WHEN
		MediaQueryDefinition query = project().getMediaQueryDefinition("w800");

		// THEN
		assertThat(query, notNullValue());
		assertThat(query.getAlias(), equalTo("w800"));
		assertThat(query.getExpression(), equalTo("min-width: 800px"));
	}

	@Test
	public void GivenNoMediaQueryOnDescriptor_WhenGetMediaQueryDefinition_ThenNull() {
		// GIVEN

		// WHEN
		MediaQueryDefinition query = project().getMediaQueryDefinition("w800");

		// THEN
		assertThat(query, nullValue());
	}

	@Test
	public void GivenMetasOnDescriptor_WhenGetMetaDescriptors_ThenNotEmpty() {
		// GIVEN
		when(descriptor.getMetaDescriptors()).thenReturn(Collections.singletonList(Mockito.mock(IMetaDescriptor.class)));

		// WHEN
		List<IMetaDescriptor> metas = project().getMetaDescriptors();

		// THEN
		assertThat(metas, hasSize(1));
	}

	@Test
	public void GivenNoMetasOnDescriptor_WhenGetMetaDescriptors_ThenEmpty() {
		// GIVEN

		// WHEN
		List<IMetaDescriptor> metas = project().getMetaDescriptors();

		// THEN
		assertThat(metas, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void GivenChangeAttempt_WhenGetMetaDescriptors_ThenException() {
		project().getMetaDescriptors().add(Mockito.mock(IMetaDescriptor.class));
	}

	@Test
	public void GivenLinksOnDescriptor_WhenGetLinkDescriptors_ThenNotEmpty() {
		// GIVEN
		when(descriptor.getLinkDescriptors()).thenReturn(Collections.singletonList(Mockito.mock(ILinkDescriptor.class)));

		// WHEN
		List<ILinkDescriptor> links = project().getLinkDescriptors();

		// THEN
		assertThat(links, hasSize(1));
	}

	@Test
	public void GivenNoLinksOnDescriptor_WhenGetLinkDescriptors_ThenEmpty() {
		// GIVEN

		// WHEN
		List<ILinkDescriptor> links = project().getLinkDescriptors();

		// THEN
		assertThat(links, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void GivenChangeAttempt_WhenGetLinkDescriptors_ThenException() {
		project().getLinkDescriptors().add(Mockito.mock(ILinkDescriptor.class));
	}

	@Test
	public void GivenScriptsOnDescriptor_WhenGetScriptDescriptors_ThenNotEmpty() {
		// GIVEN
		when(descriptor.getScriptDescriptors()).thenReturn(Collections.singletonList(Mockito.mock(IScriptDescriptor.class)));

		// WHEN
		List<IScriptDescriptor> scripts = project().getScriptDescriptors();

		// THEN
		assertThat(scripts, hasSize(1));
	}

	@Test
	public void GivenNoScriptsOnDescriptor_WhenGetScriptDescriptors_ThenEmpty() {
		// GIVEN

		// WHEN
		List<IScriptDescriptor> scripts = project().getScriptDescriptors();

		// THEN
		assertThat(scripts, empty());
	}

	@Test
	public void GivenScriptWithDependencies_WhenGetScriptDependencies_ThenRetrieve() {
		// GIVEN
		Project project = project();
		List<IScriptDescriptor> dependencies = Collections.singletonList(null);
		project.getScriptDependencies().put("sw.js", dependencies);

		// WHEN
		List<IScriptDescriptor> scripts = project.getScriptDependencies("sw.js");

		// THEN
		assertThat(scripts, not(empty()));
	}

	@Test(expected = UnsupportedOperationException.class)
	public void GivenChangeAttempt_WhenGetScriptDescriptors_ThenException() {
		project().getScriptDescriptors().add(Mockito.mock(IScriptDescriptor.class));
	}

	@Test
	public void GivenEmptyThemeDir_WhenGetThemeStyles_ThenNotNull() {
		// GIVEN

		// WHEN
		ThemeStyles styles = project().getThemeStyles();

		// THEN
		assertThat(styles, notNullValue());
	}
}
