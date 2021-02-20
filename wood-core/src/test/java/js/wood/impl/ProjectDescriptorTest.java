package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import js.wood.impl.NamingStrategy;
import js.wood.impl.ProjectDescriptor;

public class ProjectDescriptorTest {
	private ProjectDescriptor descriptor;

	@Before
	public void beforeTest() {
		descriptor = new ProjectDescriptor(new File("src/test/resources/project-descriptor.xml"));
	}

	@Test
	public void properties() {
		assertThat(descriptor.getAuthor(), equalTo("j(s)-lib"));
		assertThat(descriptor.getName(null), equalTo("project"));
		assertThat(descriptor.getTheme(), equalTo("material"));
		assertThat(descriptor.getNamingStrategy(), equalTo(NamingStrategy.XMLNS));
	}

	@Test
	public void locales() {
		List<Locale> locales = descriptor.getLocales();
		assertThat(locales, notNullValue());
		assertThat(locales, hasSize(4));
		assertThat(locales, contains(new Locale("en"), new Locale("de"), new Locale("fr"), new Locale("ro")));
	}

	@Test
	public void excludes() {
		List<String> excludes = descriptor.getExcludes();
		assertThat(excludes, notNullValue());
		assertThat(excludes, hasSize(1));
		assertThat(excludes.get(0), equalTo("page/about"));
	}

	@Test
	public void defaults() {
		descriptor = new ProjectDescriptor(new File("src/test/resources/empty-project-descriptor.xml"));
		assertThat(descriptor.getAuthor(), equalTo("WOOD"));
		assertThat(descriptor.getName(null), nullValue());
		assertThat(descriptor.getTheme(), nullValue());
		assertThat(descriptor.getNamingStrategy(), equalTo(NamingStrategy.XMLNS));
		assertThat(descriptor.getExcludes(), emptyIterable());
	}
}
