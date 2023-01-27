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
				new XFile("logo_jp.png") });

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
	public void GivenExistingLanguage_ThenFoundVariant() {
		// given
		String language = "jp";

		// when
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, language);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo_jp.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), equalTo("jp"));
	}

	@Test
	public void GivenNullLanguage_ThenFoundDefault() {
		// given
		String language = null;

		// when
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, language);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), nullValue());
	}

	/**
	 * The same as {@link #GivenNullLanguage_ThenFoundDefault()} but ensure that default language media file is last in directory
	 * files list.
	 */
	@Test
	public void GivenNullLanguageAndDefaultLast_ThenFoundDefault() {
		// given
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new XFile("page.htm"), //
				new XFile("icon.png"), //
				new XFile("logo_w800.png"), //
				new XFile("logo_ja.png"), //
				new XFile("logo.png") });
		sourceDirPath = new FilePath(project, sourceDir);

		// when
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, null);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), nullValue());
	}

	@Test
	public void GivenMissingLanguage_ThenFoundDefault() {
		// given
		String language = "de";

		// when
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, language);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), nullValue());
	}

	@Test
	public void GivenMissingLanguageAndDefaultLast_ThenFoundDefault() {
		// given
		String language = "de";
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new XFile("page.htm"), //
				new XFile("icon.png"), //
				new XFile("logo_w800.png"), //
				new XFile("logo_ja.png"), //
				new XFile("logo.png") });
		sourceDirPath = new FilePath(project, sourceDir);

		// when
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, language);

		// then
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), nullValue());
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
		FilePath foundMediaFile = Project.findResourceFile(mediaDir, reference, null);

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
