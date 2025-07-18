package com.jslib.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.emptyIterable;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.io.StringReader;
import java.util.List;

import com.jslib.wood.*;
import com.jslib.wood.test.TestDescriptor;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class BaseDescriptorTest {
	@Mock
	private Project project;
	@Mock
	private FilePath descriptorFile;

	@Before
	public void beforeTest() {
		FilePath filePath = mock(FilePath.class);
		when(filePath.exists()).thenReturn(true);
		when(project.createFilePath(any(String.class))).thenReturn(filePath);

		when(descriptorFile.getProject()).thenReturn(project);
		when(descriptorFile.exists()).thenReturn(true);
	}

	@Test
	public void GivenPropertiesAndNullDefaults_ThenGetProperties() {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<title>Test Project</title>" + //
				"	<description>Test project description.</description>" + //
				"</component>";

		// WHEN
		BaseDescriptor descriptor = descriptor(xml);

		// THEN
		assertThat(descriptor.getTitle(), equalTo("Test Project"));
	}

	@Test
	public void GivenMetaElements_ThenMetaDescriptors() {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<meta http-equiv='X-UA-Compatible' content='IE=9; IE=8; IE=7; IE=EDGE' />" + //
				"	<meta name='viewport' content='width=device-width, initial-scale=1.0, maximum-scale=1.0' />" + //
				"</component>";

		// WHEN
		BaseDescriptor descriptor = descriptor(xml);

		// THEN
		List<IMetaDescriptor> metas = descriptor.getMetaDescriptors();
		assertThat(metas, notNullValue());
		assertThat(metas, hasSize(2));

		assertThat(metas.get(0).getHttpEquiv(), equalTo("X-UA-Compatible"));
		assertThat(metas.get(0).getContent(), equalTo("IE=9; IE=8; IE=7; IE=EDGE"));

		assertThat(metas.get(1).getName(), equalTo("viewport"));
		assertThat(metas.get(1).getContent(), equalTo("width=device-width, initial-scale=1.0, maximum-scale=1.0"));
	}

	@Test
	public void GivenLinkElements_ThenLinkDescriptors() {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<link href='http://fonts.googleapis.com/css?family=Great+Vibes' rel='stylesheet' type='text/css' />" + //
				"	<link href='https://stackpath.bootstrapcdn.com/bootstrap/4.3.1/css/bootstrap.min.css' integrity='sha384-ggOyR0iXCbMQv3Xipma34MD+dH/1fQ784/j6cY/iJTQUOhcWr7x9JvoRxT2MZw1T' crossorigin='anonymous' rel='stylesheet' />" + //
				"</component>";

		// WHEN
		BaseDescriptor descriptor = descriptor(xml);

		// THEN
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
	public void GivenScriptElement_ThenScriptDescriptor() {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<script src='lib/js-lib/js-lib.js' type='text/javascript'></script>" + //
				"</component>";

		// WHEN
		BaseDescriptor descriptor = descriptor(xml);

		// THEN
		List<IScriptDescriptor> scripts = descriptor.getScriptDescriptors();
		assertThat(scripts, notNullValue());
		assertThat(scripts, hasSize(1));

		assertThat(scripts.get(0).getSource(), equalTo("lib/js-lib/js-lib.js"));
		assertThat(scripts.get(0).getType(), equalTo("text/javascript"));
	}

	@Test
	public void GivenMissingProperties_ThenEmptyValues() {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"</component>";

		// WHEN
		BaseDescriptor descriptor = descriptor(xml);

		// THEN
		assertThat(descriptor.getTitle(), nullValue());
		assertThat(descriptor.getMetaDescriptors(), emptyIterable());
		assertThat(descriptor.getLinkDescriptors(), emptyIterable());
		assertThat(descriptor.getScriptDescriptors(), emptyIterable());
	}

	// --------------------------------------------------------------------------------------------

	private BaseDescriptor descriptor(String xml) {
		when(descriptorFile.getReader()).thenReturn(new StringReader(xml));
		return new TestDescriptor(descriptorFile);
	}
}
