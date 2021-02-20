package js.wood;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import org.junit.Test;

import js.wood.Reference;
import js.wood.WoodException;
import js.wood.impl.ResourceType;

public class ReferenceTest {
	@Test
	public void constructor() {
		assertThat(new Reference(ResourceType.STRING, "string-value").toString(), equalTo("@string/string-value"));
		assertThat(new Reference(ResourceType.TEXT, "text-value").toString(), equalTo("@text/text-value"));
		assertThat(new Reference(ResourceType.COLOR, "color-value").toString(), equalTo("@color/color-value"));
		assertThat(new Reference(ResourceType.DIMEN, "dimen-value").toString(), equalTo("@dimen/dimen-value"));
		assertThat(new Reference(ResourceType.AUDIO, "audio-value").toString(), equalTo("@audio/audio-value"));
		assertThat(new Reference(ResourceType.IMAGE, "image-value").toString(), equalTo("@image/image-value"));
		assertThat(new Reference(ResourceType.IMAGE, "ext/java").toString(), equalTo("@image/ext/java"));
		assertThat(new Reference(ResourceType.VIDEO, "video-value").toString(), equalTo("@video/video-value"));
		assertThat(new Reference(ResourceType.UNKNOWN, "unknown-value").toString(), equalTo("@unknown/unknown-value"));
	}

	@Test
	public void path() {
		Reference reference = new Reference(ResourceType.IMAGE, "action/close-icon");
		assertThat(reference.getPath(), equalTo("action"));
		assertThat(reference.getName(), equalTo("close-icon"));
	}

	@Test(expected = WoodException.class)
	public void path_OnVariable() {
		new Reference(ResourceType.STRING, "action/description");
	}

	@Test
	public void isValid() {
		assertTrue(new Reference(ResourceType.STRING, "string-value").isValid());
		assertFalse(new Reference(ResourceType.UNKNOWN, "game-value").isValid());
	}

	@Test
	public void equals() {
		Reference r1 = new Reference(ResourceType.STRING, "string-value");
		Reference r2 = new Reference(ResourceType.STRING, "string-value");
		assertFalse(r1 == r2);
		assertThat(r1, equalTo(r2));
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullResourceType() {
		new Reference(null, "value");
	}

	@Test(expected = IllegalArgumentException.class)
	public void nullResourceName() {
		new Reference(ResourceType.STRING, null);
	}

	@Test(expected = IllegalArgumentException.class)
	public void emptyResourceName() {
		new Reference(ResourceType.STRING, "");
	}
}
