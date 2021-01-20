package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;

import org.junit.Test;

import js.wood.Project;
import js.wood.impl.ComponentDescriptor;

public class ComponentDescriptorTest extends WoodTestCase {
	private Project project;
	
	@Test
	public void descriptorConstructor() {
		project = project("project");
		ComponentDescriptor descriptor = new ComponentDescriptor(project.getFile("res/page/index/index.xml"), nullReferenceHandler());

		assertNotNull(field(descriptor, "doc"));
		assertEquals("res/page/index/index.xml", field(descriptor, "filePath").toString());
		assertEquals("null reference handler", field(descriptor, "referenceHandler").toString());
		assertNotNull(field(descriptor, "resolver"));
	}
}
