package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
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
import js.wood.impl.NamingStrategy;
import js.wood.impl.ProjectDescriptor;
import js.wood.impl.ResourceType;
import js.wood.impl.XmlnsOperatorsHandler;

@RunWith(MockitoJUnitRunner.class)
public class ProjectTest {
	@Mock
	private ProjectDescriptor descriptor;

	@Mock
	private IReferenceHandler referenceHandler;

	private Directory projectRoot;

	private Project project;

	@Before
	public void beforeTest() throws Exception {
		when(descriptor.getNamingStrategy()).thenReturn(NamingStrategy.XMLNS);
		projectRoot = new Directory(".");
		project = new Project(projectRoot, descriptor);
	}

	@Test
	public void construtor_NoDescriptor() throws FileNotFoundException {
		projectRoot = new Directory("root/path/project");
		project = new Project(projectRoot);

		assertThat(project.getProjectRoot(), equalTo(new File("root/path/project")));
		assertThat(project.getProjectDir().value(), equalTo("."));
		assertThat(project.getDescriptor(), notNullValue());

		assertThat(project.getAssetDir().toString(), equalTo(CT.DEF_ASSET_DIR));
		assertThat(project.getThemeDir().toString(), equalTo(CT.DEF_THEME_DIR));
		assertThat(project.getManifest().toString(), equalTo("manifest.json"));
		assertThat(project.getFavicon().toString(), equalTo("favicon.ico"));

		assertThat(project.getAuthor(), nullValue());
		assertThat(project.getDisplay(), equalTo("Project"));
		assertThat(project.getDescription(), equalTo("Project"));

		assertThat(project.getLocales(), hasSize(1));
		assertThat(project.getLocales(), contains(Locale.ENGLISH));
		assertThat(project.getDefaultLocale(), equalTo(Locale.ENGLISH));

		assertThat(project.getExcludes(), empty());
		assertThat(project.getOperatorsHandler(), instanceOf(XmlnsOperatorsHandler.class));
	}

	@Test
	public void construtor_WithDescriptor() throws FileNotFoundException {
		when(descriptor.getAuthor()).thenReturn("Iulian Rotaru");
		when(descriptor.getDisplay(anyString())).thenReturn("Project Display");
		when(descriptor.getDescription(anyString())).thenReturn("Project description.");
		when(descriptor.getLocales()).thenReturn(Arrays.asList(Locale.FRANCE, Locale.GERMAN));
		when(descriptor.getExcludes()).thenReturn(Arrays.asList("res/page/trivia/", "res/page/experiment/"));
		when(descriptor.getManifest()).thenReturn("manifest.json");
		when(descriptor.getFavicon()).thenReturn("favicon.ico");

		projectRoot = new Directory("root/path/project");
		project = new Project(projectRoot, descriptor);

		assertThat(project.getProjectRoot(), equalTo(new File("root/path/project")));
		assertThat(project.getProjectDir().value(), equalTo("."));
		assertThat(project.getDescriptor(), equalTo(descriptor));

		assertThat(project.getAssetDir().toString(), equalTo(CT.DEF_ASSET_DIR));
		assertThat(project.getThemeDir().toString(), equalTo(CT.DEF_THEME_DIR));
		assertThat(project.getManifest().toString(), equalTo("manifest.json"));
		assertThat(project.getFavicon().toString(), equalTo("favicon.ico"));

		assertThat(project.getAuthor(), equalTo("Iulian Rotaru"));
		assertThat(project.getDisplay(), equalTo("Project Display"));
		assertThat(project.getDescription(), equalTo("Project description."));

		assertThat(project.getLocales(), hasSize(2));
		assertThat(project.getLocales(), contains(Locale.FRANCE, Locale.GERMAN));
		assertThat(project.getDefaultLocale(), equalTo(Locale.FRANCE));

		assertThat(project.getExcludes(), hasSize(2));
		assertThat(project.getExcludes(), contains(new FilePath(project, "res/page/trivia/"), new FilePath(project, "res/page/experiment/")));
		assertThat(project.getOperatorsHandler(), instanceOf(XmlnsOperatorsHandler.class));
	}

	@Test
	public void getMediaQueryDefinition() {
		when(descriptor.getMediaQueryDefinitions()).thenReturn(Arrays.asList(new MediaQueryDefinition("w800", "min-width: 800px", 0)));
		project = new Project(projectRoot, descriptor);

		MediaQueryDefinition query = project.getMediaQueryDefinition("w800");
		assertThat(query, notNullValue());
		assertThat(query.getAlias(), equalTo("w800"));
		assertThat(query.getExpression(), equalTo("min-width: 800px"));
		assertThat(query.getWeight(), equalTo(1));
	}

	@Test
	public void getMediaQueryDefinition_NotFound() {
		assertThat(project.getMediaQueryDefinition("w800"), nullValue());
	}

	@Test
	public void getMetaDescriptors() {
		when(descriptor.getMetaDescriptors()).thenReturn(Arrays.asList(Mockito.mock(IMetaDescriptor.class)));
		project = new Project(projectRoot, descriptor);
		List<IMetaDescriptor> metas = project.getMetaDescriptors();
		assertThat(metas, hasSize(1));
	}

	@Test
	public void getMetaDescriptors_Empty() {
		List<IMetaDescriptor> metas = project.getMetaDescriptors();
		assertThat(metas, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getMetaDescriptors_AttemptToChange() {
		project.getMetaDescriptors().add(Mockito.mock(IMetaDescriptor.class));
	}

	@Test
	public void getLinkDescriptors() {
		when(descriptor.getLinkDescriptors()).thenReturn(Arrays.asList(Mockito.mock(ILinkDescriptor.class)));
		project = new Project(projectRoot, descriptor);
		List<ILinkDescriptor> links = project.getLinkDescriptors();
		assertThat(links, hasSize(1));
	}

	@Test
	public void getLinkDescriptors_Empty() {
		List<ILinkDescriptor> links = project.getLinkDescriptors();
		assertThat(links, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getLinkDescriptors_AttemptToChange() {
		project.getLinkDescriptors().add(Mockito.mock(ILinkDescriptor.class));
	}

	@Test
	public void getScriptDescriptors() {
		when(descriptor.getScriptDescriptors()).thenReturn(Arrays.asList(Mockito.mock(IScriptDescriptor.class)));
		project = new Project(projectRoot, descriptor);
		List<IScriptDescriptor> scripts = project.getScriptDescriptors();
		assertThat(scripts, hasSize(1));
	}

	@Test
	public void getScriptDescriptors_Empty() {
		List<IScriptDescriptor> scripts = project.getScriptDescriptors();
		assertThat(scripts, empty());
	}

	@Test(expected = UnsupportedOperationException.class)
	public void getScriptDescriptors_AttemptToChange() {
		project.getScriptDescriptors().add(Mockito.mock(IScriptDescriptor.class));
	}

	@Test
	public void getMediaFile() throws FileNotFoundException {
		when(descriptor.getLocales()).thenReturn(Arrays.asList(Locale.FRANCE, Locale.GERMAN));
		project = new Project(projectRoot, descriptor);

		FilePath mediaFile = Mockito.mock(FilePath.class);

		FilePath sourceDir = Mockito.mock(FilePath.class);
		when(sourceDir.findFirst(any())).thenReturn(mediaFile);

		FilePath sourceFile = Mockito.mock(FilePath.class);
		when(sourceFile.getParentDir()).thenReturn(sourceDir);

		Reference reference = new Reference(sourceFile, ResourceType.IMAGE, "logo");
		FilePath foundFile = project.getMediaFile(Locale.GERMAN, reference, sourceFile);
		assertThat(foundFile, equalTo(mediaFile));
	}

	@Test
	public void getMediaFile_NoComponent() throws FileNotFoundException {
		when(descriptor.getLocales()).thenReturn(Arrays.asList(Locale.FRANCE, Locale.GERMAN));
		project = new Project(projectRoot, descriptor);

		FilePath sourceDir = Mockito.mock(FilePath.class);
		FilePath sourceFile = Mockito.mock(FilePath.class);
		when(sourceFile.getParentDir()).thenReturn(sourceDir);

		Reference reference = new Reference(sourceFile, ResourceType.IMAGE, "logo");
		FilePath foundFile = project.getMediaFile(Locale.GERMAN, reference, sourceFile);
		assertThat(foundFile, nullValue());
	}

	@Test
	public void getMediaFile_DefaultLocale() throws FileNotFoundException {
		when(descriptor.getLocales()).thenReturn(Arrays.asList(Locale.GERMAN, Locale.FRANCE));
		project = new Project(projectRoot, descriptor);

		FilePath mediaFile = Mockito.mock(FilePath.class);

		FilePath sourceDir = Mockito.mock(FilePath.class);
		when(sourceDir.findFirst(any())).thenReturn(mediaFile);

		FilePath sourceFile = Mockito.mock(FilePath.class);
		when(sourceFile.getParentDir()).thenReturn(sourceDir);

		Reference reference = new Reference(sourceFile, ResourceType.IMAGE, "logo");
		FilePath foundFile = project.getMediaFile(Locale.GERMAN, reference, sourceFile);
		assertThat(foundFile, equalTo(mediaFile));
	}

	@Test
	public void getThemeStyles() {
		project = new Project(projectRoot);
		assertThat(project.getThemeStyles(), notNullValue());
	}

	// --------------------------------------------------------------------------------------------

	private static class Directory extends File {
		private static final long serialVersionUID = -4499496665524589579L;

		public Directory(String path) {
			super(path);
		}

		@Override
		public boolean isDirectory() {
			return true;
		}
	}
}
