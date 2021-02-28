package js.wood.api;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;

import java.io.File;

import org.junit.Test;

public class JavaTest {
	/** {@link File#getPath()} removes trailing name-separator.  */
	@Test
	public void filePathTrailingSeparator() {
		assertThat(new File("file/path").getPath().replace('\\', '/'), equalTo("file/path"));
		assertThat(new File("file/path/").getPath().replace('\\', '/'), equalTo("file/path"));
	}

	/** {@link File#getParent()} has no trailing name-separator.  */
	@Test
	public void fileParentTrailingSeparator() {
		assertThat(new File("file/parent/path").getParent().replace('\\', '/'), equalTo("file/parent"));
		assertThat(new File("file/parent/path/").getParent().replace('\\', '/'), equalTo("file/parent"));
	}

	/** {@link File#getName()} has no trailing name-separator.  */
	@Test
	public void fileNameTrailingSeparator() {
		assertThat(new File("file/path").getName().replace('\\', '/'), equalTo("path"));
		assertThat(new File("file/path/").getName().replace('\\', '/'), equalTo("path"));
	}
}
