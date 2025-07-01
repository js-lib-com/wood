package com.jslib.wood;


import com.jslib.wood.impl.MediaQueryDefinition;
import com.jslib.wood.impl.ProjectDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.io.File;
import java.util.Collections;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class ProjectFindMediaFileTest {
	@Mock
	private ProjectDescriptor descriptor;

	@Mock
	private File sourceDir;
	@Mock
	private Reference reference;

    private Project project;
	private FilePath sourceDirPath;

	@Before
	public void beforeTest() throws Exception {
		when(descriptor.getMediaQueryDefinitions()).thenReturn(Collections.singletonList(new MediaQueryDefinition("w800", "min-width: 800px", 0)));

		when(descriptor.getBuildDir()).thenReturn("build");
		when(descriptor.getAssetDir()).thenReturn("res/asset");
		when(descriptor.getThemeDir()).thenReturn("res/theme");

		when(sourceDir.getPath()).thenReturn("res/page/");
		when(sourceDir.exists()).thenReturn(true);
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new XFileTest("page.htm"), //
				new XFileTest("icon.png"), //
				new XFileTest("logo.png"), //
				new XFileTest("logo_w800.png"), //
				new XFileTest("logo_jp.png") });

		// findMediaFile is always used with verified reference and does not perform its own check
		// therefore resource type can be anything, including null
		// as a consequence next Mockito line is not necessary and is commented out
		// when(reference.getResourceType()).thenReturn(ResourceType.IMAGE);

		when(reference.getName()).thenReturn("logo");

        DirectoryTest projectRoot = new DirectoryTest(".");
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

	@SuppressWarnings("all")
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
				new XFileTest("page.htm"), //
				new XFileTest("icon.png"), //
				new XFileTest("logo_w800.png"), //
				new XFileTest("logo_ja.png"), //
				new XFileTest("logo.png") });
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
				new XFileTest("page.htm"), //
				new XFileTest("icon.png"), //
				new XFileTest("logo_w800.png"), //
				new XFileTest("logo_ja.png"), //
				new XFileTest("logo.png") });
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
		when(mediaDir.getSubDirectoryPath("icon")).thenReturn(mediaDir);
		when(mediaDir.findFirst(any())).thenReturn(mediaFile);

		// when
		FilePath foundMediaFile = Project.findResourceFile(mediaDir, reference, null);

		// then
		assertThat(foundMediaFile, notNullValue());
		assertThat(foundMediaFile.value(), equalTo("logo.png"));
		assertThat(foundMediaFile.getBasename(), equalTo("logo"));
	}

	// --------------------------------------------------------------------------------------------

	private static class DirectoryTest extends File {
		private static final long serialVersionUID = -4499496665524589579L;

		public DirectoryTest(String path) {
			super(path);
		}

		@Override
		public boolean isDirectory() {
			return true;
		}
	}

	private static class XFileTest extends File {
		private static final long serialVersionUID = -5975578621510948684L;

		public XFileTest(String pathname) {
			super(pathname);
		}

		@Override
		public boolean isFile() {
			return true;
		}
	}
}
