package js.wood.cli;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Properties;

import js.converter.Converter;
import js.converter.ConverterRegistry;
import js.lang.BugError;
import js.util.Strings;

public class Config {
	private static final String PROJECT_PROPERTIES_FILE = ".project.properties";

	private final Converter converter;

	private final Properties globalProperties;
	private final Properties projectProperties;

	public Config(Properties globalProperties, Properties projectProperties) {
		this.converter = ConverterRegistry.getConverter();
		this.globalProperties = globalProperties;
		this.projectProperties = projectProperties;
	}

	public void set(String key, Object value) throws IOException {
		projectProperties().put(key, converter.asString(value));
		try (Writer writer = new FileWriter(projectPropertiesFile())) {
			projectProperties().store(writer, "project properties");
		}
	}

	public <T> T get(String key, Class<T> type) throws IOException {
		Object value = projectProperties().get(key);
		if (value == null) {
			value = globalProperties().get(key);
			if (value == null) {
				throw new BugError("Property not found |%s|.", key);
			}
		}
		return converter.asObject(Strings.injectProperties(value.toString()), type);
	}

	public String get(String key) throws IOException {
		return get(key, String.class);
	}

	private Properties globalProperties() throws IOException {
		if (globalProperties.isEmpty()) {
			synchronized (globalProperties) {
				if (globalProperties.isEmpty()) {
					String woodHome = System.getProperty("WOOD_HOME");
					if (woodHome == null) {
						throw new BugError("Invalid Java context. Missing WOOD_HOME property.");
					}
					File woodDir = new File(woodHome);
					if (!woodDir.exists()) {
						throw new BugError("Invalid Java context. Missing wood directory.");
					}
					File binDir = new File(woodDir, "bin");
					if (!binDir.exists()) {
						throw new BugError("Invalid WOOD install. Missing binaries directory |%s|.", binDir);
					}
					File propertiesFile = new File(binDir, "wood.properties");
					if (!propertiesFile.exists()) {
						throw new BugError("Invalid WOOD install. Missing global properties file |%s|.", propertiesFile);
					}
					try (Reader reader = new FileReader(propertiesFile)) {
						globalProperties.load(reader);
					}
				}
			}
		}
		return globalProperties;
	}

	private Properties projectProperties() throws IOException {
		if (projectProperties.isEmpty()) {
			synchronized (projectProperties) {
				if (projectProperties.isEmpty()) {
					try (Reader reader = new FileReader(projectPropertiesFile())) {
						projectProperties.load(reader);
					}
				}
			}
		}
		return projectProperties;
	}

	private static File projectPropertiesFile() {
		File workingDir = Paths.get("").toAbsolutePath().toFile();
		File propertiesFile = new File(workingDir, PROJECT_PROPERTIES_FILE);
		if (!propertiesFile.exists()) {
			throw new BugError("Invalid WOOD project setup. Missing project properties file |%s|.", propertiesFile);
		}
		return propertiesFile;
	}
}
