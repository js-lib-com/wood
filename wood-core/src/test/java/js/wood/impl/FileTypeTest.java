package js.wood.impl;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;

import java.io.File;

import org.junit.Test;

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
	public void forExtension_BadExtension() {
		assertThat(FileType.forExtension("fake"), equalTo(FileType.MEDIA));
		assertThat(FileType.forExtension(""), equalTo(FileType.MEDIA));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forExtension_NullExtension() {
		FileType.forExtension(null);
	}

	@Test
	public void forFile() {
		assertThat(FileType.forFile(new File("path/file.htm")), equalTo(FileType.LAYOUT));
		assertThat(FileType.forFile(new File("path/file.css")), equalTo(FileType.STYLE));
		assertThat(FileType.forFile(new File("path/file.js")), equalTo(FileType.SCRIPT));
		assertThat(FileType.forFile(new File("path/file.xml")), equalTo(FileType.XML));
		assertThat(FileType.forFile(new File("path/file")), equalTo(FileType.MEDIA));
		assertThat(FileType.forFile(new File("path/file.png")), equalTo(FileType.MEDIA));
		assertThat(FileType.forFile(new File("path/file.avi")), equalTo(FileType.MEDIA));
	}

	@Test(expected = IllegalArgumentException.class)
	public void forFile_NullFile() {
		FileType.forFile(null);
	}
}
