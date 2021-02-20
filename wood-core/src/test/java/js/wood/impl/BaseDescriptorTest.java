package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

import java.io.File;
import java.io.FileNotFoundException;
import java.util.List;

import org.junit.Before;
import org.junit.Test;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.util.Classes;
import js.wood.ILinkDescriptor;
import js.wood.IMetaDescriptor;
import js.wood.IScriptDescriptor;
import js.wood.impl.BaseDescriptor;

public class BaseDescriptorTest {
	private BaseDescriptor descriptor;

	@Before
	public void beforeTest() throws FileNotFoundException {
		descriptor = new TestDescriptor(new File("src/test/resources/project-descriptor.xml"));
	}

	@Test
	public void properties() {
		assertThat(descriptor.getDisplay(null), equalTo("Test Project"));
		assertThat(descriptor.getDescription(null), equalTo("Test project description."));
	}

	@Test
	public void metas() {
		List<IMetaDescriptor> metas = descriptor.getMetaDescriptors();
		assertThat(metas, notNullValue());
		assertThat(metas, hasSize(2));
		assertThat(metas.get(0).getHttpEquiv(), equalTo("X-UA-Compatible"));
		assertThat(metas.get(0).getContent(), equalTo("IE=9; IE=8; IE=7; IE=EDGE"));
		assertThat(metas.get(1).getName(), equalTo("viewport"));
		assertThat(metas.get(1).getContent(), equalTo("width=device-width, initial-scale=1.0, maximum-scale=1.0"));
	}

	@Test
	public void links() {
		List<ILinkDescriptor> links = descriptor.getLinkDescriptors();
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
		List<IScriptDescriptor> scripts = descriptor.getScriptDescriptors();
		assertThat(scripts, notNullValue());
		assertThat(scripts, hasSize(1));
		assertThat(scripts.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
		assertThat(scripts.get(0).getType(), equalTo("text/javascript"));
	}

	@Test
	public void defaults() throws FileNotFoundException {
		descriptor = new TestDescriptor(new File("src/test/resources/empty-descriptor.xml"));
		assertThat(descriptor.getDisplay(null), nullValue());
		assertThat(descriptor.getDescription(null), nullValue());
		assertThat(descriptor.getMetaDescriptors(), emptyIterable());
		assertThat(descriptor.getLinkDescriptors(), emptyIterable());
		assertThat(descriptor.getScriptDescriptors(), emptyIterable());
	}

	private static class TestDescriptor extends BaseDescriptor {
		public TestDescriptor(File descriptorFile) throws FileNotFoundException {
			super(document(descriptorFile));
		}

		private static Document document(File descriptorFile) throws FileNotFoundException {
			DocumentBuilder builder = Classes.loadService(DocumentBuilder.class);
			return builder.loadXML(descriptorFile);
		}
	}
}
