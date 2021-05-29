package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.Assert.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.impl.MediaQueryDefinition;
import js.wood.impl.ProjectDescriptor;
import js.wood.impl.ProjectProperties;
import js.wood.impl.ResourceType;

@RunWith(MockitoJUnitRunner.class)
public class ProjectInitTest {
	@Mock
	private ProjectDescriptor descriptor;
	@Mock
	private ProjectProperties properties;
	@Mock
	private Project.IFilePathVisitor scanHandler;

	@Mock
	private FilePath assetDir;
	@Mock
	private FilePath themeDir;
	@Mock
	private IReferenceHandler referenceHandler;

	@Before
	public void beforeTest() throws Exception {
		when(properties.getBuildDir()).thenReturn("build");
		when(properties.getAssetDir(anyString())).thenReturn("res/asset");
		when(properties.getThemeDir(anyString())).thenReturn("res/theme");
	}

	private Project project() {
		return new Project(new File("."), descriptor, properties, scanHandler);
	}

	@Test
	public void GivenNonEmptyDescriptor_WhenConstrutor_ThenStateInitialized() throws FileNotFoundException {
		// given
		when(descriptor.getAuthors()).thenReturn(Arrays.asList("Iulian Rotaru"));
		when(descriptor.getDisplay(anyString())).thenReturn("Project Display");
		when(descriptor.getDescription(anyString())).thenReturn("Project description.");
		when(descriptor.getManifest()).thenReturn("manifest.json");
		when(descriptor.getFavicon()).thenReturn("favicon.ico");
		when(descriptor.getLocales()).thenReturn(Arrays.asList(Locale.FRANCE, Locale.GERMAN));
		when(descriptor.getExcludes()).thenReturn(Arrays.asList("res/page/trivia/", "res/page/experiment/"));

		// when
		File projectRoot = new File("root/path/project");
		Project project = new Project(projectRoot, descriptor, properties, scanHandler);

		// then
		assertThat(project.getProjectRoot(), equalTo(new File("root/path/project")));
		assertThat(project.getDescriptor(), equalTo(descriptor));

		assertThat(project.getAssetDir().value(), equalTo("res/asset/"));
		assertThat(project.getThemeDir().value(), equalTo("res/theme/"));
		assertThat(project.getManifest().value(), equalTo("manifest.json"));
		assertThat(project.getFavicon().value(), equalTo("favicon.ico"));

		assertThat(project.getAuthors(), contains("Iulian Rotaru"));
		assertThat(project.getDisplay(), equalTo("Project Display"));
		assertThat(project.getDescription(), equalTo("Project description."));

		assertThat(project.getLocales(), hasSize(2));
		assertThat(project.getLocales(), contains(Locale.FRANCE, Locale.GERMAN));
		assertThat(project.getDefaultLocale(), equalTo(Locale.FRANCE));

		assertThat(project.getExcludes(), hasSize(3));
		assertTrue(project.getExcludes().contains(new File(projectRoot, "res/page/trivia/")));
		assertTrue(project.getExcludes().contains(new File(projectRoot, "res/page/experiment/")));
		assertTrue(project.getExcludes().contains(new File(projectRoot, "build")));
	}

	@Test
	public void GivenMediaQueryOnDescriptor_WhenGetMediaQueryDefinition_ThenNotNull() {
		// given
		when(descriptor.getMediaQueryDefinitions()).thenReturn(Arrays.asList(new MediaQueryDefinition("w800", "min-width: 800px", 0)));

		// when
		MediaQueryDefinition query = project().getMediaQueryDefinition("w800");

		// then
		assertThat(query, notNullValue());
		assertThat(query.getAlias(), equalTo("w800"));
		assertThat(query.getExpression(), equalTo("min-width: 800px"));
		assertThat(query.getWeight(), equalTo(1));
	}

	@Test
	public void GivenNoMediaQueryOnDescriptor_WhenGetMediaQueryDefinition_ThenNull() {
		// given

		// when
		MediaQueryDefinition query = project().getMediaQueryDefinition("w800");

		// then
		assertThat(query, nullValue());
	}

	@Test
	public void GivenMetasOnDescriptor_WhenGetMetaDescriptors_ThenNotEmpty() {
		// given
		when(descriptor.getMetaDescriptors()).thenReturn(Arrays.asList(Mockito.mock(IMetaDescriptor.class)));

		// when
		List<IMetaDescriptor> metas = project().getMetaDescriptors();

		// then
		assertThat(metas, hasSize(1));
	}

	@Test
	public void GivenNoMetasOnDescriptor_WhenGetMetaDescriptors_ThenEmpty() {
		// given

		// when
		List<IMetaDescriptor> metas = project().getMetaDescriptors();

		// then
		assertThat(metas, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void GivenChangeAttempt_WhenGetMetaDescriptors_ThenException() {
		project().getMetaDescriptors().add(Mockito.mock(IMetaDescriptor.class));
	}

	@Test
	public void GivenLinksOnDescriptor_WhenGetLinkDescriptors_ThenNotEmpty() {
		// given
		when(descriptor.getLinkDescriptors()).thenReturn(Arrays.asList(Mockito.mock(ILinkDescriptor.class)));

		// when
		List<ILinkDescriptor> links = project().getLinkDescriptors();

		// then
		assertThat(links, hasSize(1));
	}

	@Test
	public void GivenNoLinksOnDescriptor_WhenGetLinkDescriptors_ThenEmpty() {
		// given

		// when
		List<ILinkDescriptor> links = project().getLinkDescriptors();

		// then
		assertThat(links, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void GivenChangeAttempt_WhenGetLinkDescriptors_ThenException() {
		project().getLinkDescriptors().add(Mockito.mock(ILinkDescriptor.class));
	}

	@Test
	public void GivenScriptsOnDescriptor_WhenGetScriptDescriptors_ThenNotEmpty() {
		// given
		when(descriptor.getScriptDescriptors()).thenReturn(Arrays.asList(Mockito.mock(IScriptDescriptor.class)));

		// when
		List<IScriptDescriptor> scripts = project().getScriptDescriptors();

		// then
		assertThat(scripts, hasSize(1));
	}

	@Test
	public void GivenNoScriptsOnDescriptor_WhenGetScriptDescriptors_ThenEmpty() {
		// given

		// when
		List<IScriptDescriptor> scripts = project().getScriptDescriptors();

		// then
		assertThat(scripts, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void GivenChangeAttempt_WhenGetScriptDescriptors_ThenException() {
		project().getScriptDescriptors().add(Mockito.mock(IScriptDescriptor.class));
	}

	@Test
	public void GivenExistingFile_WhenGetMediaFile_ThenFound() throws FileNotFoundException {
		// given
		when(descriptor.getLocales()).thenReturn(Arrays.asList(Locale.FRANCE, Locale.GERMAN));

		FilePath mediaFile = mock(FilePath.class);

		FilePath sourceDir = mock(FilePath.class);
		when(sourceDir.findFirst(any())).thenReturn(mediaFile);

		FilePath sourceFile = mock(FilePath.class);
		when(sourceFile.getParentDir()).thenReturn(sourceDir);

		// when
		Reference reference = new Reference(sourceFile, ResourceType.IMAGE, "logo");
		FilePath foundFile = project().getMediaFile(Locale.GERMAN, reference, sourceFile);

		// then
		assertThat(foundFile, equalTo(mediaFile));
	}

	@Test
	public void GivenExistingFileAndDefaultLocale_WhenGetMediaFile_TheFound() throws FileNotFoundException {
		// given
		when(descriptor.getLocales()).thenReturn(Arrays.asList(Locale.GERMAN, Locale.FRANCE));

		FilePath mediaFile = mock(FilePath.class);

		FilePath sourceDir = mock(FilePath.class);
		when(sourceDir.findFirst(any())).thenReturn(mediaFile);

		FilePath sourceFile = mock(FilePath.class);
		when(sourceFile.getParentDir()).thenReturn(sourceDir);

		// when
		Reference reference = new Reference(sourceFile, ResourceType.IMAGE, "logo");
		FilePath foundFile = project().getMediaFile(Locale.GERMAN, reference, sourceFile);

		// then
		assertThat(foundFile, equalTo(mediaFile));
	}

	@Test
	public void GivenEmptyThemeDir_WhenGetThemeStyles_ThenNotNull() {
		// given

		// when
		ThemeStyles styles = project().getThemeStyles();

		// then
		assertThat(styles, notNullValue());
	}
}
