package com.jslib.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.jslib.wood.CT;
import com.jslib.wood.FilePath;
import com.jslib.wood.Project;

@RunWith(MockitoJUnitRunner.class)
public class ProjectDescriptorTest {
	@Mock
	private FilePath descriptorFile;

	private ProjectDescriptor descriptor;

	@Before
	public void beforeTest() throws FileNotFoundException {
		when(descriptorFile.exists()).thenReturn(true);
	}

	@Test
	public void GivenProperties_WhenCreateInstance_ThenRetrieve() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<operators>XMLNS</operators>" + //
				"	<build-dir>target/site</build-dir>" + //
				"	<asset-dir>asset</asset-dir>" + //
				"	<theme-dir>theme</theme-dir>" + //
				"	<exclude-dirs>page/about</exclude-dirs>" + //
				"	<authors>Iulian Rotaru</authors>" + //
				"	<title>Project Display</title>" + //
				"	<favicon>res/app-icon.png</favicon>" + //
				"	<pwa-manifest>res/app-manifest.json</pwa-manifest>" + //
				"	<pwa-worker>script/sw.js</pwa-worker>" + //
				"	<language>en</language>" + //
				"</project>";

		// when
		descriptor = descriptor(xml);

		// then
		assertThat(descriptor.getOperatorsNaming(), equalTo(OperatorsNaming.XMLNS));
		assertThat(descriptor.getBuildDir(), equalTo("target/site"));
		assertThat(descriptor.getAssetDir(), equalTo("asset"));
		assertThat(descriptor.getThemeDir(), equalTo("theme"));
		assertThat(descriptor.getExcludeDirs(), equalTo(Collections.singletonList("page/about")));
		assertThat(descriptor.getAuthors(), equalTo(Collections.singletonList("Iulian Rotaru")));
		assertThat(descriptor.getTitle(), equalTo("Project Display"));
		assertThat(descriptor.getFavicon(), equalTo("res/app-icon.png"));
		assertThat(descriptor.getPwaManifest(), equalTo("res/app-manifest.json"));
		assertThat(descriptor.getPwaWorker(), equalTo("script/sw.js"));
		assertThat(descriptor.getLanguage(), equalTo(Collections.singletonList("en")));
	}

	@Test
	public void GivenMultipleLanguage_WhenGetLanguage_ThenRetrieveAll() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<language>en, de, fr, ro</language>" + //
				"</project>";

		// when
		List<String> languages = descriptor(xml).getLanguage();

		// then
		assertThat(languages, notNullValue());
		assertThat(languages, hasSize(4));
		assertThat(languages, contains("en", "de", "fr", "ro"));
	}

	@Test
	public void GivenMultipleExcludeDirs_WheGetExcludeDirs_ThenRetrieveAll() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<exclude-dirs>page/about, page/contact</exclude-dirs>" + //
				"</project>";

		// when
		List<String> excludes = descriptor(xml).getExcludeDirs();

		// then
		assertThat(excludes, notNullValue());
		assertThat(excludes, hasSize(2));
		assertThat(excludes.get(0), equalTo("page/about"));
		assertThat(excludes.get(1), equalTo("page/contact"));
	}

	@Test
	public void GivenMissingProperties_WhenCreateInstance_ThenDefaults() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"</project>";

		// when
		descriptor = descriptor(xml);

		// then
		assertThat(descriptor.getOperatorsNaming(), equalTo(OperatorsNaming.DATA_ATTR));
		assertThat(descriptor.getBuildDir(), equalTo(CT.DEF_BUILD_DIR));
		assertThat(descriptor.getAssetDir(), equalTo(CT.DEF_ASSET_DIR));
		assertThat(descriptor.getThemeDir(), equalTo(CT.DEF_THEME_DIR));
		assertThat(descriptor.getExcludeDirs(), emptyIterable());
		assertThat(descriptor.getAuthors().size(), equalTo(0));
		assertThat(descriptor.getTitle(), nullValue());
		assertThat(descriptor.getFavicon(), equalTo("favicon.ico"));
		assertThat(descriptor.getPwaManifest(), equalTo("manifest.json"));
		assertThat(descriptor.getPwaWorker(), equalTo("worker.js"));
		assertThat(descriptor.getLanguage(), equalTo(Collections.singletonList("en")));
	}

	@Test
	public void GivenExistingProperty_WhenGetValue_ThenRetrieve() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<title>Test Project</title>" + //
				"</project>";
		descriptor = descriptor(xml);

		// when
		String value = descriptor.getValue("title");

		// then
		assertThat(value, equalTo("Test Project"));
	}

	@Test
	public void GivenNotExistingTitle_WhenGetValue_ThenRetrieveProjectDirName() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"</project>";
		descriptor = descriptor(xml);

		Project project = Mockito.mock(Project.class);
		when(project.getProjectRoot()).thenReturn(new File("test-project"));
		when(descriptorFile.getProject()).thenReturn(project);

		// when
		String value = descriptor.getValue("title");

		// then
		assertThat(value, equalTo("Test Project"));
	}

	// --------------------------------------------------------------------------------------------

	private ProjectDescriptor descriptor(String xml) {
		when(descriptorFile.getReader()).thenReturn(new StringReader(xml));
		return new ProjectDescriptor(descriptorFile);
	}
}
