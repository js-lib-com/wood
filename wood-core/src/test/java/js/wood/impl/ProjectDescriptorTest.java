package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.StringReader;
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
	private FilePath filePath;

	private ProjectDescriptor descriptor;

	@Before
	public void beforeTest() throws FileNotFoundException {
		when(filePath.exists()).thenReturn(true);
	}

	@Test
	public void properties() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<locale>en</locale>" + //
				"	<author>j(s)-lib</author>" + //
				"	<naming-strategy>XMLNS</naming-strategy>" + //
				"	<manifest>res/app-manifest.json</manifest>" + //
				"	<favicon>res/app-icon.png</favicon>" + //
				"</project>";
		descriptor = descriptor(xml);

		assertThat(descriptor.getAuthor(), equalTo("j(s)-lib"));
		assertThat(descriptor.getNamingStrategy(), equalTo(NamingStrategy.XMLNS));
		assertThat(descriptor.getManifest(), equalTo("res/app-manifest.json"));
		assertThat(descriptor.getFavicon(), equalTo("res/app-icon.png"));
	}

	@Test
	public void locales() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<locale>en, de, fr, ro</locale>" + //
				"</project>";
		descriptor = descriptor(xml);

		List<Locale> locales = descriptor.getLocales();
		assertThat(locales, notNullValue());
		assertThat(locales, hasSize(4));
		assertThat(locales, contains(new Locale("en"), new Locale("de"), new Locale("fr"), new Locale("ro")));
	}

	@Test
	public void excludes() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<locale>en</locale>" + //
				"	<excludes>page/about</excludes>" + //
				"</project>";
		descriptor = descriptor(xml);

		List<String> excludes = descriptor.getExcludes();
		assertThat(excludes, notNullValue());
		assertThat(excludes, hasSize(1));
		assertThat(excludes.get(0), equalTo("page/about"));
	}

	@Test
	public void defaults() throws FileNotFoundException {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<project>" + //
				"	<locale>en</locale>" + //
				"</project>";
		descriptor = descriptor(xml);

		assertThat(descriptor.getAuthor(), nullValue());
		assertThat(descriptor.getNamingStrategy(), equalTo(NamingStrategy.XMLNS));
		assertThat(descriptor.getExcludes(), emptyIterable());
		assertThat(descriptor.getManifest(), equalTo("manifest.json"));
		assertThat(descriptor.getFavicon(), equalTo("favicon.ico"));
	}

	private ProjectDescriptor descriptor(String xml) {
		when(filePath.getReader()).thenReturn(new StringReader(xml));
		return new ProjectDescriptor(filePath);
	}
}
