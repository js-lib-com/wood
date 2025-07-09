package com.jslib.wood;

import com.jslib.wood.impl.MediaQueryDefinition;
import com.jslib.wood.impl.ProjectDescriptor;
import com.jslib.wood.test.TestDirectory;
import com.jslib.wood.test.TestFile;
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
		when(descriptor.getMediaQueryDefinitions()).thenReturn(Collections.singletonList(new MediaQueryDefinition("w800","screen", "min-width: 800px")));

		when(descriptor.getBuildDir()).thenReturn("build");
		when(descriptor.getAssetDir()).thenReturn("res/asset");
		when(descriptor.getThemeDir()).thenReturn("res/theme");

		when(sourceDir.getPath()).thenReturn("res/page/");
		when(sourceDir.exists()).thenReturn(true);
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new TestFile("page.htm"), //
				new TestFile("logo.css"), //
				new TestFile("logo_jp.css"), //
				new TestFile("icon.png"), //
				new TestFile("logo.png"), //
				new TestFile("logo_w800.png"), //
				new TestFile("logo_jp.png") });

		when(reference.getName()).thenReturn("logo");
		// test cases focus only on media files
		when(reference.isMediaFile()).thenReturn(true);

        TestDirectory projectRoot = new TestDirectory(".");
		project = new Project(projectRoot, descriptor);

		sourceDirPath = new FilePath(project, sourceDir);
	}

	@Test
	public void GivenExistingLanguage_ThenFoundMediaFileWithVariant() {
		// GIVEN
		String language = "jp";

		// WHEN
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, language);

		// THEN
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo_jp.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), equalTo("jp"));
	}

	@Test
	public void GivenNullLanguage_ThenFoundDefaultMediaFile() {
		// GIVEN
		String language = null;

		// WHEN
		@SuppressWarnings("ConstantConditions")
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, language);

		// THEN
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), nullValue());
	}

	/**
	 * The same as {@link #GivenNullLanguage_ThenFoundDefaultMediaFile()} but ensure that default language media file is last in directory
	 * files list.
	 */
	@Test
	public void GivenNullLanguageAndDefaultLast_ThenFoundDefaultMediaFile() {
		// GIVEN
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new TestFile("page.htm"), //
				new TestFile("logo.css"), //
				new TestFile("logo_jp.css"), //
				new TestFile("icon.png"), //
				new TestFile("logo_w800.png"), //
				new TestFile("logo_ja.png"), //
				new TestFile("logo.png") });
		sourceDirPath = new FilePath(project, sourceDir);

		// WHEN
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, null);

		// THEN
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), nullValue());
	}

	@Test
	public void GivenMissingLanguage_ThenFoundDefaultMediaFile() {
		// GIVEN
		String language = "de";

		// WHEN
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, language);

		// THEN
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), nullValue());
	}

	@Test
	public void GivenMissingLanguageAndDefaultLast_ThenFoundDefaultMediaFile() {
		// GIVEN
		String language = "de";
		when(sourceDir.listFiles()).thenReturn(new File[] { //
				new TestFile("page.htm"), //
				new TestFile("logo.css"), //
				new TestFile("logo_jp.css"), //
				new TestFile("icon.png"), //
				new TestFile("logo_w800.png"), //
				new TestFile("logo_ja.png"), //
				new TestFile("logo.png") });
		sourceDirPath = new FilePath(project, sourceDir);

		// WHEN
		FilePath mediaFile = Project.findResourceFile(sourceDirPath, reference, language);

		// THEN
		assertThat(mediaFile, notNullValue());
		assertThat(mediaFile.value(), equalTo("logo.png"));
		assertThat(mediaFile.getBasename(), equalTo("logo"));
		assertThat(mediaFile.getVariants().getLanguage(), nullValue());
	}

	@Test
	public void GivenReferenceWithSubdir_ThenMediaFileFound() {
		// GIVEN
		Reference reference = mock(Reference.class);
		when(reference.hasPath()).thenReturn(true);
		when(reference.getPath()).thenReturn("icon");

		FilePath mediaFile = mock(FilePath.class);
		when(mediaFile.value()).thenReturn("logo.png");
		when(mediaFile.getBasename()).thenReturn("logo");

		FilePath mediaDir = mock(FilePath.class);
		when(mediaDir.getSubDirectoryPath("icon")).thenReturn(mediaDir);
		when(mediaDir.findFirst(any())).thenReturn(mediaFile);

		// WHEN
		FilePath foundMediaFile = Project.findResourceFile(mediaDir, reference, null);

		// THEN
		assertThat(foundMediaFile, notNullValue());
		assertThat(foundMediaFile.value(), equalTo("logo.png"));
		assertThat(foundMediaFile.getBasename(), equalTo("logo"));
	}
}
