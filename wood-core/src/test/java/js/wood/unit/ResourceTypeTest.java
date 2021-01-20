package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import js.wood.impl.ResourceType;

public class ResourceTypeTest {
	@Test
	public void resourceTypeValueOf() {
		assertEquals(ResourceType.STRING, ResourceType.getValueOf("string"));
		assertEquals(ResourceType.STRING, ResourceType.getValueOf("STRING"));
		assertEquals(ResourceType.UNKNOWN, ResourceType.getValueOf("STRINGx"));
	}

	@Test
	public void resourceTypePredicates() {
		assertTrue(ResourceType.STRING.isVariable());
		assertTrue(ResourceType.TEXT.isVariable());
		assertTrue(ResourceType.COLOR.isVariable());
		assertTrue(ResourceType.DIMEN.isVariable());
		assertTrue(ResourceType.STYLE.isVariable());

		assertFalse(ResourceType.IMAGE.isVariable());
		assertFalse(ResourceType.AUDIO.isVariable());
		assertFalse(ResourceType.VIDEO.isVariable());
		assertFalse(ResourceType.UNKNOWN.isVariable());

		assertTrue(ResourceType.IMAGE.isMedia());
		assertTrue(ResourceType.AUDIO.isMedia());
		assertTrue(ResourceType.VIDEO.isMedia());

		assertFalse(ResourceType.STRING.isMedia());
		assertFalse(ResourceType.TEXT.isMedia());
		assertFalse(ResourceType.COLOR.isMedia());
		assertFalse(ResourceType.DIMEN.isMedia());
		assertFalse(ResourceType.STYLE.isMedia());
		assertFalse(ResourceType.UNKNOWN.isMedia());
	}
}
