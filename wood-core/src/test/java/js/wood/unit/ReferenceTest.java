package js.wood.unit;

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
	public void valuesConstructor() {
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
	public void referenceParserConstructor() {
		assertThat(new Reference("@string/string-value").toString(), equalTo("@string/string-value"));
		assertThat(new Reference("@text/text-value").toString(), equalTo("@text/text-value"));
		assertThat(new Reference("@color/color-value").toString(), equalTo("@color/color-value"));
		assertThat(new Reference("@dimen/dimen-value").toString(), equalTo("@dimen/dimen-value"));
		assertThat(new Reference("@style/style-value").toString(), equalTo("@unknown/style-value"));
		assertThat(new Reference("@audio/audio-value").toString(), equalTo("@audio/audio-value"));
		assertThat(new Reference("@image/image-value").toString(), equalTo("@image/image-value"));
		assertThat(new Reference("@video/video-value").toString(), equalTo("@video/video-value"));
		assertThat(new Reference("@game/game-value").toString(), equalTo("@unknown/game-value"));
	}

	@Test
	public void isValid() {
		assertTrue(new Reference("@string/string-value").isValid());
		assertFalse(new Reference("@game/game-value").isValid());
	}

	@Test
	public void equals() {
		Reference r1 = new Reference("@string/string-value");
		Reference r2 = new Reference("@string/string-value");
		assertFalse(r1 == r2);
		assertThat(r1, equalTo(r2));
	}

	@Test(expected = WoodException.class)
	public void badFormat() {
		new Reference("res/exception/bad-type");
	}
}
