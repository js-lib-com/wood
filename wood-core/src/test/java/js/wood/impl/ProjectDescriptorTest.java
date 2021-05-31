package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.Arrays;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.FilePath;

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
	public void GivenPropertiesThenRetrieve() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<locale>en</locale>" + //
				"	<authors>j(s)-lib</authors>" + //
				"	<naming-strategy>XMLNS</naming-strategy>" + //
				"	<manifest>res/app-manifest.json</manifest>" + //
				"	<favicon>res/app-icon.png</favicon>" + //
				"	<excludes>page/about</excludes>" + //
				"</project>";

		// when
		descriptor = descriptor(xml);

		// then
		assertThat(descriptor.getLocales(), equalTo(Arrays.asList(Locale.ENGLISH)));
		assertThat(descriptor.getAuthors(), contains("j(s)-lib"));
		assertThat(descriptor.getNamingStrategy(), equalTo(NamingStrategy.XMLNS));
		assertThat(descriptor.getManifest(), equalTo("res/app-manifest.json"));
		assertThat(descriptor.getFavicon(), equalTo("res/app-icon.png"));
		assertThat(descriptor.getExcludes(), equalTo(Arrays.asList("page/about")));
	}

	@Test
	public void GivenMultipleLocale_ThenRetrieveAll() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<locale>en, de, fr, ro</locale>" + //
				"</project>";

		// when
		descriptor = descriptor(xml);

		// then
		List<Locale> locales = descriptor.getLocales();
		assertThat(locales, notNullValue());
		assertThat(locales, hasSize(4));
		assertThat(locales, contains(new Locale("en"), new Locale("de"), new Locale("fr"), new Locale("ro")));
	}

	@Test
	public void GivenMultipleExcludes_ThenRetrieveAll() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<excludes>page/about, page/contact</excludes>" + //
				"</project>";

		// when
		descriptor = descriptor(xml);

		// then
		List<String> excludes = descriptor.getExcludes();
		assertThat(excludes, notNullValue());
		assertThat(excludes, hasSize(2));
		assertThat(excludes.get(0), equalTo("page/about"));
		assertThat(excludes.get(1), equalTo("page/contact"));
	}

	@Test
	public void GivenMissingProperties_ThenDefaults() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"</project>";

		// when
		descriptor = descriptor(xml);

		// then
		assertThat(descriptor.getLocales(), equalTo(Arrays.asList(Locale.ENGLISH)));
		assertThat(descriptor.getAuthors().size(), equalTo(0));
		assertThat(descriptor.getNamingStrategy(), equalTo(NamingStrategy.XMLNS));
		assertThat(descriptor.getExcludes(), emptyIterable());
		assertThat(descriptor.getManifest(), equalTo("manifest.json"));
		assertThat(descriptor.getFavicon(), equalTo("favicon.ico"));
	}

	// --------------------------------------------------------------------------------------------

	private ProjectDescriptor descriptor(String xml) {
		when(descriptorFile.getReader()).thenReturn(new StringReader(xml));
		return new ProjectDescriptor(descriptorFile);
	}
}
