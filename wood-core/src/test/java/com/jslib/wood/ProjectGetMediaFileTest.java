package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.wood.impl.ProjectDescriptor;

@RunWith(MockitoJUnitRunner.class)
public class ProjectGetMediaFileTest {
	@Mock
	private ProjectDescriptor descriptor;

	@Mock
	private FilePath mediaFile;
	@Mock
	private FilePath sourceDir;
	@Mock
	private FilePath sourceFile;
	@Mock
	private FilePath assetDir;

	@Mock
	private Reference reference;

	private Project project;

	@Before
	public void beforeTest() throws Exception {
		when(descriptor.getBuildDir()).thenReturn("build");
		when(descriptor.getAssetDir()).thenReturn("res/asset");
		when(descriptor.getThemeDir()).thenReturn("res/theme");

		when(sourceDir.findFirst(any())).thenReturn(mediaFile);
		when(sourceFile.getParentDir()).thenReturn(sourceDir);

		project = new Project(new File("."), descriptor);
		project.setAssetDir(assetDir);
	}

	@Test
	public void GivenMediaFileInSourceDir_ThenFoundAndDoNotSearchAsset() {
		// given

		// when
		FilePath foundFile = project.getResourceFile("de", reference, sourceFile);

		// then
		assertThat(foundFile, equalTo(mediaFile));
		verify(sourceDir, times(1)).findFirst(any());
		verify(assetDir, times(0)).findFirst(any());
	}

	@Test
	public void GivenMediaFileInSourceDirAndNullLanguage_ThenFoundAndDoNotSearchAsset() {
		// given

		// when
		FilePath foundFile = project.getResourceFile(null, reference, sourceFile);

		// then
		assertThat(foundFile, equalTo(mediaFile));
		verify(sourceDir, times(1)).findFirst(any());
		verify(assetDir, times(0)).findFirst(any());
	}

	@SuppressWarnings("all")
	@Test
	public void GivenMediaFileInSourceDirAndReferenceWithPath_ThenFoundAndDoNotSearchAsset() {
		// given
		when(reference.hasPath()).thenReturn(true);
		// return the same source directory since we actually need a not null path
		when(sourceDir.getSubDirectoryPath(null)).thenReturn(sourceDir);

		// when
		FilePath foundFile = project.getResourceFile("de", reference, sourceFile);

		// then
		assertThat(foundFile, equalTo(mediaFile));
		verify(sourceDir, times(1)).findFirst(any());
		verify(assetDir, times(0)).findFirst(any());
	}

	/**
	 * Media file does not exist into source directory but do exist into assets. Searches twice on source directory - with given
	 * language and without language, and once on asset where it is found.
	 */
	@Test
	public void GivenMediaFileNotInSourceDir_ThenDoSearchSourceTwiceAndAssetOnce() {
		// given
		when(sourceDir.findFirst(any())).thenReturn(null);
		when(assetDir.findFirst(any())).thenReturn(mediaFile);

		// when
		FilePath foundFile = project.getResourceFile("de", reference, sourceFile);

		// then
		assertThat(foundFile, equalTo(mediaFile));
		verify(sourceDir, times(2)).findFirst(any());
		verify(assetDir, times(1)).findFirst(any());
	}

	/**
	 * Same as {@link #GivenMediaFileNotInSourceDir_ThenDoSearchSourceTwiceAndAssetOnce()} but searches source directory only
	 * once because language are not provided - language parameter is null.
	 */
	@Test
	public void GivenMediaFileNotInSourceDirAndNullLanguage_ThenDoSearchSourceOnceAndAssetOnce() {
		// given
		when(sourceDir.findFirst(any())).thenReturn(null);
		when(assetDir.findFirst(any())).thenReturn(mediaFile);

		// when
		FilePath foundFile = project.getResourceFile(null, reference, sourceFile);

		// then
		assertThat(foundFile, equalTo(mediaFile));
		verify(sourceDir, times(1)).findFirst(any());
		verify(assetDir, times(1)).findFirst(any());
	}

	/**
	 * If media files does not exist either in source directory nor in assets returns null. Searches twice both source and asset
	 * directories - with given language and without language.
	 */
	@Test
	public void GivenMissingMediaFile_ThenNull() {
		// given
		when(sourceDir.findFirst(any())).thenReturn(null);
		when(assetDir.findFirst(any())).thenReturn(null);

		// when
		FilePath foundFile = project.getResourceFile("de", reference, sourceFile);

		// then
		assertThat(foundFile, nullValue());
		verify(sourceDir, times(2)).findFirst(any());
		verify(assetDir, times(2)).findFirst(any());
	}

	/**
	 * If media files does not exist either in source directory nor in assets returns null. Searches only once both source and
	 * asset directories because there are no language provided - null language parameter.
	 */
	@Test
	public void GivenMissingMediaFileAndNullLanguage_ThenNull() {
		// given
		when(sourceDir.findFirst(any())).thenReturn(null);
		when(assetDir.findFirst(any())).thenReturn(null);

		// when
		FilePath foundFile = project.getResourceFile(null, reference, sourceFile);

		// then
		assertThat(foundFile, nullValue());
		verify(sourceDir, times(1)).findFirst(any());
		verify(assetDir, times(1)).findFirst(any());
	}

	@Test
	public void GivenExistingFileWithoutParent_ThenSearchOnlyOnAssetDir() {
		// given
		when(sourceFile.getParentDir()).thenReturn(null);
		when(assetDir.findFirst(any())).thenReturn(mediaFile);

		// when
		FilePath foundFile = project.getResourceFile("de", reference, sourceFile);

		// then
		assertThat(foundFile, equalTo(mediaFile));
		verify(sourceDir, times(0)).findFirst(any());
		verify(assetDir, times(1)).findFirst(any());
	}

	@Test
	public void GivenExistingFileWithoutParentAndNullLanguage_ThenSearchOnlyOnAssetDir() {
		// given
		when(sourceFile.getParentDir()).thenReturn(null);
		when(assetDir.findFirst(any())).thenReturn(mediaFile);

		// when
		FilePath foundFile = project.getResourceFile(null, reference, sourceFile);

		// then
		assertThat(foundFile, equalTo(mediaFile));
		verify(sourceDir, times(0)).findFirst(any());
		verify(assetDir, times(1)).findFirst(any());
	}
}
