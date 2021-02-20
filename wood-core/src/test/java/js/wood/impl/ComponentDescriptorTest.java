package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.when;

import java.io.File;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import js.wood.FilePath;
import js.wood.IReferenceHandler;
import js.wood.impl.ComponentDescriptor;

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
		descriptor = descriptor("component-descriptor.xml");
	}

	@Test
	public void properties() {
		assertThat(descriptor.getVersion(), equalTo("1.0"));
		assertThat(descriptor.getSecurityRole(), equalTo("admin"));
	}

	@Test
	public void defaults() {
		descriptor = descriptor("empty-project-descriptor.xml");
		assertThat(descriptor.getVersion(), nullValue());
		assertThat(descriptor.getSecurityRole(), nullValue());
	}

	private ComponentDescriptor descriptor(String descriptorFile) {
		when(filePath.toFile()).thenReturn(new File("src/test/resources/" + descriptorFile));
		return new ComponentDescriptor(filePath, referenceHandler);
	}
}
