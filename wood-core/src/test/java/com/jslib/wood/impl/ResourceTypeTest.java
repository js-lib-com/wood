package com.jslib.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

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

		assertTrue(ResourceType.IMAGE.isMediaFile());
		assertTrue(ResourceType.AUDIO.isMediaFile());
		assertTrue(ResourceType.VIDEO.isMediaFile());

		assertFalse(ResourceType.STRING.isMediaFile());
		assertFalse(ResourceType.TEXT.isMediaFile());
		assertFalse(ResourceType.COLOR.isMediaFile());
		assertFalse(ResourceType.DIMEN.isMediaFile());
		assertFalse(ResourceType.UNKNOWN.isMediaFile());
	}
}
