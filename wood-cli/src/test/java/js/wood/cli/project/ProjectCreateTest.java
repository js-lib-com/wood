package js.wood.cli.project;

import java.io.IOException;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;

@Ignore
public class ProjectCreateTest {
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
		task.exec();
	}
}
