package js.wood.unit;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import js.wood.impl.FileType;

public class FileTypeTest {
	@Test
	public void forExtension() {
		assertThat(FileType.forExtension("htm"), equalTo(FileType.LAYOUT));
		assertThat(FileType.forExtension("css"), equalTo(FileType.STYLE));
		assertThat(FileType.forExtension("js"), equalTo(FileType.SCRIPT));
		assertThat(FileType.forExtension("xml"), equalTo(FileType.XML));
		assertThat(FileType.forExtension("jpg"), equalTo(FileType.MEDIA));
		assertThat(FileType.forExtension("jpeg"), equalTo(FileType.MEDIA));
		assertThat(FileType.forExtension("png"), equalTo(FileType.MEDIA));
		assertThat(FileType.forExtension("avi"), equalTo(FileType.MEDIA));
	}

	/** Not recognized extensions are considered media file. Also if extension is null or empty. */
	@Test
	public void nullOrBadExtension() {
		assertThat(FileType.forExtension("fake"), equalTo(FileType.MEDIA));
		assertThat(FileType.forExtension(null), equalTo(FileType.MEDIA));
		assertThat(FileType.forExtension(""), equalTo(FileType.MEDIA));
	}

	@Test
	public void equals() {
		assertTrue(FileType.LAYOUT.equals(new File("path/file.htm")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file.css")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file.js")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file.xml")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file")));
		assertTrue(FileType.STYLE.equals(new File("path/file.css")));
		assertTrue(FileType.SCRIPT.equals(new File("path/file.js")));
		assertTrue(FileType.XML.equals(new File("path/file.xml")));
		assertTrue(FileType.MEDIA.equals(new File("path/file")));
		assertTrue(FileType.MEDIA.equals(new File("path/file.png")));
		assertTrue(FileType.MEDIA.equals(new File("path/file.avi")));
	}
}
