package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.io.StringReader;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.FilePath;
import js.wood.IReferenceHandler;

@RunWith(MockitoJUnitRunner.class)
public class ComponentDescriptorTest {
	@Mock
	private FilePath filePath;
	@Mock
	private IReferenceHandler referenceHandler;

	private ComponentDescriptor descriptor;

	@Before
	public void beforeTest() {
		when(filePath.exists()).thenReturn(true);
	}

	@Test
	public void properties() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<version>1.0</version>" + //
				"	<security-role>admin</security-role>" + //
				"</component>";
		descriptor = descriptor(xml);

		assertThat(descriptor.getVersion(), equalTo("1.0"));
		assertThat(descriptor.getSecurityRole(), equalTo("admin"));
	}

	@Test
	public void properties_Missing() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"</component>";
		descriptor = descriptor(xml);

		assertThat(descriptor.getVersion(), nullValue());
		assertThat(descriptor.getSecurityRole(), nullValue());
	}

	@Test
	public void properties_Empty() {
		String xml = "<?xml version='1.0' encoding='UTF-8'?>" + //
				"<component>" + //
				"	<version></version>" + //
				"	<security-role></security-role>" + //
				"</component>";
		descriptor = descriptor(xml);

		assertThat(descriptor.getVersion(), nullValue());
		assertThat(descriptor.getSecurityRole(), nullValue());
	}

	private ComponentDescriptor descriptor(String xml) {
		when(filePath.getReader()).thenReturn(new StringReader(xml));
		return new ComponentDescriptor(filePath, referenceHandler);
	}
}
