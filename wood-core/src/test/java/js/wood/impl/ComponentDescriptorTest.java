package js.wood.impl;


import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;

import java.io.IOException;
import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.FilePath;
import js.wood.IReferenceHandler;
import js.wood.Reference;

@RunWith(MockitoJUnitRunner.class)
public class ComponentDescriptorTest {
	@Mock
	private FilePath descriptorFile;
	@Mock
	private IReferenceHandler referenceHandler;

	private ComponentDescriptor descriptor;

	@Before
	public void beforeTest() {
		when(descriptorFile.exists()).thenReturn(true);
	}

	@Test
	public void GivenProperties_ThenRetrieve() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<version>1.0</version>" + //
				"	<group>admin</group>" + //
				"</component>";
		
		// when
		descriptor = descriptor(xml);

		// then
		assertThat(descriptor.getDescriptorFile(), equalTo(descriptorFile));
		assertThat(descriptor.getVersion(), equalTo("1.0"));
		assertThat(descriptor.getResourcesGroup(), equalTo("admin"));
	}

	@Test
	public void GivenPropertyWithVariable_ThenResolve() throws IOException {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<version>@string/version</version>" + //
				"</component>";
		when(referenceHandler.onResourceReference(eq(new Reference(ResourceType.STRING, "version")), any(FilePath.class))).thenReturn("1.0");

		// when
		descriptor = descriptor(xml);

		// then
		assertThat(descriptor.getVersion(), equalTo("1.0"));
	}

	@Test
	public void GivenMissingProperty_ThenNull() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"</component>";
		
		// when
		descriptor = descriptor(xml);

		// then
		assertThat(descriptor.getVersion(), nullValue());
		assertThat(descriptor.getResourcesGroup(), nullValue());
	}

	@Test
	public void GivenEmptyProperty_ThenNull() {
		// given
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<version></version>" + //
				"	<group></group>" + //
				"</component>";
		
		// when
		descriptor = descriptor(xml);

		// then
		assertThat(descriptor.getVersion(), nullValue());
		assertThat(descriptor.getResourcesGroup(), nullValue());
	}

	// --------------------------------------------------------------------------------------------
	
	private ComponentDescriptor descriptor(String xml) {
		when(descriptorFile.getReader()).thenReturn(new StringReader(xml));
		return new ComponentDescriptor(descriptorFile, referenceHandler);
	}
}
