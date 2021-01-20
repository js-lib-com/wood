package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;

import java.io.File;
import java.util.List;
import java.util.Locale;

import org.junit.Before;
import org.junit.Test;

import js.wood.ILinkReference;
import js.wood.IMetaReference;
import js.wood.IScriptReference;
import js.wood.impl.NamingStrategy;
import js.wood.impl.ProjectDescriptor;

public class ProjectDescriptorTest extends WoodTestCase {
	private ProjectDescriptor descriptor;

	@Before
	public void beforeTest() {
		descriptor = new ProjectDescriptor(new File("src/test/resources/project-descriptor.xml"));
	}

	@Test
	public void properties() {
		assertThat(descriptor.getAuthor(), equalTo("j(s)-lib"));
		assertThat(descriptor.getName(null), equalTo("project"));
		assertThat(descriptor.getDisplay(null), equalTo("Test Project"));
		assertThat(descriptor.getDescription(null), equalTo("Test project description."));
		assertThat(descriptor.getTheme(), equalTo("material"));
		assertThat(descriptor.getSiteDir(null), equalTo("build/site"));
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
	public void metas() {
		List<IMetaReference> metas = descriptor.getMetas();
		assertThat(metas, notNullValue());
		assertThat(metas, hasSize(2));
		assertThat(metas.get(0).getHttpEquiv(), equalTo("X-UA-Compatible"));
		assertThat(metas.get(0).getContent(), equalTo("IE=9; IE=8; IE=7; IE=EDGE"));
		assertThat(metas.get(1).getName(), equalTo("viewport"));
		assertThat(metas.get(1).getContent(), equalTo("width=device-width, initial-scale=1.0, maximum-scale=1.0"));
	}

	@Test
	public void links() {
		List<ILinkReference> links = descriptor.getLinks();
		assertThat(links, notNullValue());
		assertThat(links, hasSize(2));

		assertThat(links.get(0).getHref(), equalTo("http://fonts.googleapis.com/css?family=Great+Vibes"));
		assertThat(links.get(0).getRelationship(), equalTo("stylesheet"));
		assertThat(links.get(0).getType(), equalTo("text/css"));

		assertThat(links.get(1).getHref(), equalTo("https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css"));
		assertThat(links.get(1).getRelationship(), equalTo("stylesheet"));
		assertThat(links.get(1).getIntegrity(), equalTo("sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T"));
		assertThat(links.get(1).getCrossOrigin(), equalTo("anonymous"));
	}

	@Test
	public void scripts() {
		List<IScriptReference> scripts = descriptor.getScripts();
		assertThat(scripts, notNullValue());
		assertThat(scripts, hasSize(1));
		assertThat(scripts.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
		assertThat(scripts.get(0).getType(), equalTo("text/javascript"));
	}

	@Test
	public void excludes() {
		List<String> excludes = descriptor.getExcludes();
		assertThat(excludes, notNullValue());
		assertThat(excludes, hasSize(1));
		assertThat(excludes.get(0), equalTo("page/about"));
	}
}
