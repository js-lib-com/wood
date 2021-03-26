package js.wood.cli.project;

import java.io.IOException;
import java.nio.file.spi.FileSystemProvider;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ProjectCreateTest {
	@Mock
	private FileSystemProvider provider;
	
	private ProjectCreate task;

	@Before
	public void beforeTest() {
		task = new ProjectCreate();
		task.setName("test");
		task.setAuthor("Iulian Rotaru");
		task.setLocale("en");
	}

	@Test
	public void exec() throws IOException {
		// task.exec();

		//System.setProperty("java.nio.file.spi.DefaultFileSystemProvider", provider.get);
	}
}
