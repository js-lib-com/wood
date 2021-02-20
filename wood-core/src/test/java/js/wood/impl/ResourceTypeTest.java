package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import js.wood.impl.ResourceType;

public class ResourceTypeTest {
	@Test
	public void valueOf() {
		assertThat(ResourceType.getValueOf("string"), equalTo(ResourceType.STRING));
		assertThat(ResourceType.getValueOf("STRING"), equalTo(ResourceType.STRING));
		assertThat(ResourceType.getValueOf("STRINGx"), equalTo(ResourceType.UNKNOWN));
	}

	@Test
	public void predicates() {
		assertTrue(ResourceType.STRING.isVariable());
		assertTrue(ResourceType.TEXT.isVariable());
		assertTrue(ResourceType.LINK.isVariable());
		assertTrue(ResourceType.TIP.isVariable());
		assertTrue(ResourceType.COLOR.isVariable());
		assertTrue(ResourceType.DIMEN.isVariable());

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
		assertFalse(ResourceType.UNKNOWN.isMedia());
	}
}
