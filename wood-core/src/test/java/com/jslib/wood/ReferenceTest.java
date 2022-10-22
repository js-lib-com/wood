package com.jslib.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

public class ReferenceTest {
	@Test
	public void constructor() {
		assertThat(new Reference(Reference.Type.STRING, "string-value").toString(), equalTo("@string/string-value"));
		assertThat(new Reference(Reference.Type.TEXT, "text-value").toString(), equalTo("@text/text-value"));
		assertThat(new Reference(Reference.Type.AUDIO, "audio-value").toString(), equalTo("@audio/audio-value"));
		assertThat(new Reference(Reference.Type.IMAGE, "image-value").toString(), equalTo("@image/image-value"));
		assertThat(new Reference(Reference.Type.IMAGE, "ext/java").toString(), equalTo("@image/ext/java"));
		assertThat(new Reference(Reference.Type.VIDEO, "video-value").toString(), equalTo("@video/video-value"));
		assertThat(new Reference(Reference.Type.PROJECT, "value").toString(), equalTo("@project/value"));
	}

	@Test
	public void path() {
		Reference reference = new Reference(Reference.Type.IMAGE, "action/close-icon");
		assertThat(reference.getPath(), equalTo("action"));
		assertThat(reference.getName(), equalTo("close-icon"));
	}

	@Test(expected = WoodException.class)
	public void path_OnVariable() {
		new Reference(Reference.Type.STRING, "action/description");
	}

	@Test
	public void equals() {
		Reference r1 = new Reference(Reference.Type.STRING, "string-value");
		Reference r2 = new Reference(Reference.Type.STRING, "string-value");
		assertFalse(r1 == r2);
		assertThat(r1, equalTo(r2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullResourceType() {
		new Reference(null, "value");
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullResourceName() {
		new Reference(Reference.Type.STRING, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyResourceName() {
		new Reference(Reference.Type.STRING, "");
	}
}
