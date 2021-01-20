package js.wood.unit;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;

import org.junit.Test;

import js.wood.impl.FileType;

public class FileTypeTest extends WoodTestCase {
	@Test
	public void fileTypeForExtension() {
		assertEquals(FileType.LAYOUT, FileType.forExtension("htm"));
		assertEquals(FileType.STYLE, FileType.forExtension("css"));
		assertEquals(FileType.SCRIPT, FileType.forExtension("js"));
		assertEquals(FileType.XML, FileType.forExtension("xml"));
		assertEquals(FileType.MEDIA, FileType.forExtension(null));
		assertEquals(FileType.MEDIA, FileType.forExtension(""));
		assertEquals(FileType.MEDIA, FileType.forExtension("png"));
		assertEquals(FileType.MEDIA, FileType.forExtension("avi"));
	}

	@Test
	public void fileTypeEquals() {
		assertTrue(FileType.LAYOUT.equals(new File("path/file.htm")));
		assertTrue(FileType.STYLE.equals(new File("path/file.css")));
		assertTrue(FileType.SCRIPT.equals(new File("path/file.js")));
		assertTrue(FileType.XML.equals(new File("path/file.xml")));
		assertTrue(FileType.MEDIA.equals(new File("path/file")));
		assertTrue(FileType.MEDIA.equals(new File("path/file.png")));
		assertTrue(FileType.MEDIA.equals(new File("path/file.avi")));

		assertFalse(FileType.LAYOUT.equals(new File("path/file.css")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file.js")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file.xml")));
		assertFalse(FileType.LAYOUT.equals(new File("path/file")));
	}
}
