package com.jslib.wood.api;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.StringReader;
import java.io.Writer;
import java.util.Properties;

import org.junit.Test;

public class JavaTest {
	/** {@link File#getPath()} removes trailing name-separator. */
	@Test
	public void filePathTrailingSeparator() {
		assertThat(new File("file/path").getPath().replace('\\', '/'), equalTo("file/path"));
		assertThat(new File("file/path/").getPath().replace('\\', '/'), equalTo("file/path"));
	}

	/** {@link File#getParent()} has no trailing name-separator. */
	@Test
	public void fileParentTrailingSeparator() {
		assertThat(new File("file/parent/path").getParent().replace('\\', '/'), equalTo("file/parent"));
		assertThat(new File("file/parent/path/").getParent().replace('\\', '/'), equalTo("file/parent"));
	}

	/** {@link File#getName()} has no trailing name-separator. */
	@Test
	public void fileNameTrailingSeparator() {
		assertThat(new File("file/path").getName().replace('\\', '/'), equalTo("path"));
		assertThat(new File("file/path/").getName().replace('\\', '/'), equalTo("path"));
	}

	@Test
	public void removeNotWrittenFile() throws IOException {
		File file = new File("src/test/resources/file");
		Writer writer = new FileWriter(file);

		// file writer constructor creates the file
		assertTrue(file.exists());
		// delete fails because file is still opened by writer
		assertFalse(file.delete());

		writer.close();
		assertTrue(file.delete());
		assertFalse(file.exists());
	}

	/**
	 * Property load method discard backslash before not escaping characters. In this example \\ backslashes are loaded as a
	 * single one and '\r' from \repository begin is loaded as new line.
	 */
	@Test
	public void propertiesLoadBackSlash() throws IOException {
		Properties properties = new Properties();
		try (Reader reader = new StringReader("repository.dir=D:\\\\wood-1.0\repository")) {
			properties.load(reader);
		}
		assertThat(properties.get("repository.dir"), equalTo("D:\\wood-1.0"));
	}
}
