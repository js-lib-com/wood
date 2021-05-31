package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
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

@RunWith(MockitoJUnitRunner.class)
public class ProjectMediaFileTest {
	@Mock
	private ProjectDescriptor descriptor;
	@Mock
	private ProjectProperties properties;
	
	@Mock
	private IReferenceHandler referenceHandler;
	@Mock
	private File sourceDir;
	@Mock
	private FilePath sourceFile;
	@Mock
	private Reference reference;

	private Directory projectRoot;

	private Project project;
	private FilePath sourceDirPath;

	@Before
	public void beforeTest() throws Exception {
		when(descriptor.getMediaQueryDefinitions()).thenReturn(Arrays.asList(new MediaQueryDefinition("w800", "min-width: 800px", 0)));

		when(properties.getBuildDir()).thenReturn("build");
		when(properties.getAssetDir(anyString())).thenReturn("res/asset");
		when(properties.getThemeDir(anyString())).thenReturn("res/theme");

		when(sourceDir.getPath()).thenReturn("res/page/");
		when(sourceDir.exists()).thenReturn(true);
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new XFile("page.htm"), //
				new XFile("icon.png"), //
				new XFile("logo.png"), //
				new XFile("logo_w800.png"), //
				new XFile("logo_ja.png") });

		// findMediaFile is always used with verified reference and does not perform its own check
		// therefore resource type can be anything, including null
		// as a consequence next Mockito line is not necessary and is commented out
		// when(reference.getResourceType()).thenReturn(ResourceType.IMAGE);

		when(reference.getName()).thenReturn("logo");

		projectRoot = new Directory(".");
		project = new Project(projectRoot, descriptor, properties);
		sourceDirPath = new FilePath(project, sourceDir);
	}

	@Test
	public void Given_WhenFindMediaFile_ThenFound() {
		// given

		// when
		FilePath mediaFile = Project.findMediaFile(sourceDirPath, reference, Locale.JAPANESE);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo_ja.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLocale(), equalTo(Locale.JAPANESE));
	}

	@Test
	public void GivenNullLocale_WhenFindMediaFile_ThenFoundDefault() {
		// given
		Locale locale = null;

		// when
		FilePath mediaFile = Project.findMediaFile(sourceDirPath, reference, locale);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
	}

	/** The same as {@link #nullLocale()} but ensure that default locale media file is last in directory files list. */
	@Test
	public void GivenNullLocaleAndDefaultLast_WhenFindMediaFile_ThenFoundDefault() {
		// given
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new XFile("page.htm"), //
				new XFile("icon.png"), //
				new XFile("logo_w800.png"), //
				new XFile("logo_ja.png"), //
				new XFile("logo.png") });
		sourceDirPath = new FilePath(project, sourceDir);

		// when
		FilePath mediaFile = Project.findMediaFile(sourceDirPath, reference, null);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
	}

	@Test
	public void missingLocale() {
		FilePath mediaFile = Project.findMediaFile(sourceDirPath, reference, Locale.GERMANY);
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLocale(), nullValue());
	}

	@Test
	public void missingLocale_FilesOrder() {
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new XFile("page.htm"), //
				new XFile("icon.png"), //
				new XFile("logo_w800.png"), //
				new XFile("logo_ja.png"), //
				new XFile("logo.png") });
		sourceDirPath = new FilePath(project, sourceDir);

		FilePath mediaFile = Project.findMediaFile(sourceDirPath, reference, Locale.GERMANY);
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLocale(), nullValue());
	}

	@Test
	public void referenceSubdir() {
		Reference reference = Mockito.mock(Reference.class);
		when(reference.hasPath()).thenReturn(true);
		when(reference.getPath()).thenReturn("icon");

		FilePath mediaFile = Mockito.mock(FilePath.class);
		when(mediaFile.value()).thenReturn("logo.png");
		when(mediaFile.getBasename()).thenReturn("logo");

		FilePath mediaDir = Mockito.mock(FilePath.class);
		when(mediaDir.getSubdirPath("icon")).thenReturn(mediaDir);
		when(mediaDir.findFirst(any())).thenReturn(mediaFile);

		FilePath foundMediaFile = Project.findMediaFile(mediaDir, reference, null);
		assertThat(foundMediaFile, notNullValue());
		assertThat(foundMediaFile.value(), equalTo("logo.png"));
		assertThat(foundMediaFile.getBasename(), equalTo("logo"));
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

	private static class XFile extends File {
		private static final long serialVersionUID = -5975578621510948684L;

		public XFile(String pathname) {
			super(pathname);
		}

		@Override
		public boolean isFile() {
			return true;
		}
	}
}
