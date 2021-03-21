package js.wood.cli;

import static org.hamcrest.MatcherAssert.*;
import static org.hamcrest.Matchers.*;
import static org.mockito.Mockito.*;

import java.io.IOException;
import java.util.Properties;

import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

@RunWith(MockitoJUnitRunner.class)
public class ConfigTest {
	@Mock
	private Properties globalProperties;
	@Mock
	private Properties projectProperties;

	private Config config;

	@Before
	public void beforeTest() {
		config = new Config(globalProperties, projectProperties);
	}

	@Test
	public void get_SystemPropertyInjection() throws IOException {
		when(globalProperties.get("repository.dir")).thenReturn("${WOOD_HOME}\repository");
		System.setProperty("WOOD_HOME", "D:\\wood-1.0");
		assertThat(config.get("repository.dir"), equalTo("D:\\wood-1.0\repository"));
	}
}
