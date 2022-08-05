package com.jslib.wood;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.File;
import java.util.Arrays;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.wood.impl.MediaQueryDefinition;
import com.jslib.wood.impl.ProjectDescriptor;

@RunWith(MockitoJUnitRunner.class)
public class ProjectFindMediaFileTest {
	@Mock
	private ProjectDescriptor descriptor;

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

		when(descriptor.getBuildDir()).thenReturn("build");
		when(descriptor.getAssetDir()).thenReturn("res/asset");
		when(descriptor.getThemeDir()).thenReturn("res/theme");

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
		project = new Project(projectRoot, descriptor);

		sourceDirPath = new FilePath(project, sourceDir);
	}

	@Test
	public void GivenExistingLocale_ThenFoundVariant() {
		// given
		Locale locale = Locale.JAPANESE;

		// when
		FilePath mediaFile = Project.findMediaFile(sourceDirPath, reference, locale);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo_ja.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLocale(), equalTo(Locale.JAPANESE));
	}

	@Test
	public void GivenNullLocale_ThenFoundDefault() {
		// given
		Locale locale = null;

		// when
		FilePath mediaFile = Project.findMediaFile(sourceDirPath, reference, locale);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLocale(), nullValue());
	}

	/**
	 * The same as {@link #GivenNullLocale_ThenFoundDefault()} but ensure that default locale media file is last in directory
	 * files list.
	 */
	@Test
	public void GivenNullLocaleAndDefaultLast_ThenFoundDefault() {
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
		assertThat(mediaFile.getVariants().getLocale(), nullValue());
	}

	@Test
	public void GivenMissingLocale_ThenFoundDefault() {
		// given
		Locale locale = Locale.GERMANY;

		// when
		FilePath mediaFile = Project.findMediaFile(sourceDirPath, reference, locale);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLocale(), nullValue());
	}

	@Test
	public void GivenMissingLocaleAndDefaultLast_ThenFoundDefault() {
		// given
		Locale locale = Locale.GERMANY;
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new XFile("page.htm"), //
				new XFile("icon.png"), //
				new XFile("logo_w800.png"), //
				new XFile("logo_ja.png"), //
				new XFile("logo.png") });
		sourceDirPath = new FilePath(project, sourceDir);

		// when
		FilePath mediaFile = Project.findMediaFile(sourceDirPath, reference, locale);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLocale(), nullValue());
	}

	@Test
	public void GivenReferenceWithSubdir_ThenFound() {
		// given
		Reference reference = mock(Reference.class);
		when(reference.hasPath()).thenReturn(true);
		when(reference.getPath()).thenReturn("icon");

		FilePath mediaFile = mock(FilePath.class);
		when(mediaFile.value()).thenReturn("logo.png");
		when(mediaFile.getBasename()).thenReturn("logo");

		FilePath mediaDir = mock(FilePath.class);
		when(mediaDir.getSubdirPath("icon")).thenReturn(mediaDir);
		when(mediaDir.findFirst(any())).thenReturn(mediaFile);

		// when
		FilePath foundMediaFile = Project.findMediaFile(mediaDir, reference, null);

		// then
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
