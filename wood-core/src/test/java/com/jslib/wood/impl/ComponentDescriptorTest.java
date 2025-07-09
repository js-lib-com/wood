package com.jslib.wood.impl;

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

import com.jslib.wood.FilePath;
import com.jslib.wood.IReferenceHandler;
import com.jslib.wood.Reference;

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
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<group>admin</group>" + //
				"</component>";
		
		// WHEN
		descriptor = descriptor(xml);

		// THEN
		assertThat(descriptor.getDescriptorFile(), equalTo(descriptorFile));
		assertThat(descriptor.getResourcesGroup(), equalTo("admin"));
	}

	@Test
	public void GivenPropertyWithVariable_ThenResolve() throws IOException {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<group>@string/role</group>" + //
				"</component>";
		when(referenceHandler.onResourceReference(eq(new Reference(Reference.Type.STRING, "role")), any(FilePath.class))).thenReturn("admin");

		// WHEN
		descriptor = descriptor(xml);

		// THEN
		assertThat(descriptor.getResourcesGroup(), equalTo("admin"));
	}

	@Test
	public void GivenMissingProperty_ThenNull() {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"</component>";
		
		// WHEN
		descriptor = descriptor(xml);

		// THEN
		assertThat(descriptor.getResourcesGroup(), nullValue());
	}

	@Test
	public void GivenEmptyProperty_ThenNull() {
		// GIVEN
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<version></version>" + //
				"	<group></group>" + //
				"</component>";
		
		// WHEN
		descriptor = descriptor(xml);

		// THEN
		assertThat(descriptor.getResourcesGroup(), nullValue());
	}

	// --------------------------------------------------------------------------------------------
	
	private ComponentDescriptor descriptor(String xml) {
		when(descriptorFile.getReader()).thenReturn(new StringReader(xml));
		return new ComponentDescriptor(descriptorFile, referenceHandler);
	}
}
