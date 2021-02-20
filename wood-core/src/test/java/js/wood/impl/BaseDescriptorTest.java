package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.io.FileNotFoundException;
import java.io.StringReader;
import java.util.List;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.FilePath;
import js.wood.ILinkDescriptor;
import js.wood.IMetaDescriptor;
import js.wood.IScriptDescriptor;

@RunWith(MockitoJUnitRunner.class)
public class BaseDescriptorTest {
	@Mock
	private FilePath filePath;

	@Before
	public void beforeTest() throws FileNotFoundException {
		when(filePath.exists()).thenReturn(true);
	}

	@Test
	public void properties() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<display>Test Project</display>" + //
				"	<description>Test project description.</description>" + //
				"</component>";
		BaseDescriptor descriptor = descriptor(xml);

		assertThat(descriptor.getDisplay(null), equalTo("Test Project"));
		assertThat(descriptor.getDescription(null), equalTo("Test project description."));
	}

	@Test
	public void metas() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<metas>" + //
				"		<meta http-equiv='X-UA-Compatible' content='IE=9; IE=8; IE=7; IE=EDGE' />" + //
				"		<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0' />" + //
				"	</metas>" + //
				"</component>";
		BaseDescriptor descriptor = descriptor(xml);

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
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<links>" + //
				"		<link href='http://fonts.googleapis.com/css?family=Great+Vibes' rel='stylesheet' type='text/css' />" + //
				"		<link href='https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css' integrity='sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T' crossorigin='anonymous' rel='stylesheet' />" + //
				"	</links>" + //
				"</component>";
		BaseDescriptor descriptor = descriptor(xml);

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
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<scripts>" + //
				"		<script src='lib/js-lib/js-lib.js' type='text/javascript'></script>" + //
				"	</scripts>" + //
				"</component>";
		BaseDescriptor descriptor = descriptor(xml);

		List<IScriptDescriptor> scripts = descriptor.getScriptDescriptors();
		assertThat(scripts, notNullValue());
		assertThat(scripts, hasSize(1));
		assertThat(scripts.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
		assertThat(scripts.get(0).getType(), equalTo("text/javascript"));
	}

	@Test
	public void defaults() throws FileNotFoundException {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"</component>";
		BaseDescriptor descriptor = descriptor(xml);

		assertThat(descriptor.getDisplay(null), nullValue());
		assertThat(descriptor.getDescription(null), nullValue());
		assertThat(descriptor.getMetaDescriptors(), emptyIterable());
		assertThat(descriptor.getLinkDescriptors(), emptyIterable());
		assertThat(descriptor.getScriptDescriptors(), emptyIterable());
	}

	private BaseDescriptor descriptor(String xml) {
		when(filePath.getReader()).thenReturn(new StringReader(xml));
		return new TestDescriptor(filePath);
	}

	private static class TestDescriptor extends BaseDescriptor {
		public TestDescriptor(FilePath descriptorFile) {
			super(descriptorFile);
		}
	}
}
