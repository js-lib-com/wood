package js.wood.cli;

import static java.lang.String.format;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Paths;
import java.util.Map;
import java.util.Properties;
import java.util.SortedMap;
import java.util.TreeMap;

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

	public SortedMap<String, String> properties(boolean includeGlobal) throws IOException {
		SortedMap<String, String> properties = new TreeMap<>();
		for (Map.Entry<Object, Object> entry : projectProperties().entrySet()) {
			properties.put(entry.getKey().toString(), entry.getValue().toString());
		}
		if (includeGlobal) {
			for (Map.Entry<Object, Object> entry : globalProperties().entrySet()) {
				properties.put(entry.getKey().toString(), entry.getValue().toString());
			}
		}
		return properties;
	}

	public void set(String key, Object value) throws IOException {
		projectProperties().put(key, converter.asString(value));
		try (Writer writer = new FileWriter(projectPropertiesFile())) {
			projectProperties().store(writer, "project properties");
		}
	}

	public void unset(String key) throws IOException {
		projectProperties().remove(key);
		try (Writer writer = new FileWriter(projectPropertiesFile())) {
			projectProperties().store(writer, "project properties");
		}
	}

	public <T> T get(String key, Class<T> type, String... defaultValue) throws IOException {
		Object value = projectProperties().get(key);
		if (value == null) {
			value = globalProperties().get(key);
			if (value == null) {
				if (defaultValue.length == 1) {
					value = defaultValue[0];
				} else {
					throw new BugError("Property not found |%s|.", key);
				}
			}
		}
		return converter.asObject(Strings.injectProperties(value.toString()), type);
	}

	public String get(String key, String... defaultValue) throws IOException {
		return get(key, String.class, defaultValue);
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

	private static File projectPropertiesFile() throws IOException {
		File workingDir = Paths.get("").toAbsolutePath().toFile();
		File propertiesFile = new File(workingDir, PROJECT_PROPERTIES_FILE);
		if (!propertiesFile.exists() && !propertiesFile.createNewFile()) {
			throw new IOException(format("Cannot create project properties file |%s|.", propertiesFile));
		}
		return propertiesFile;
	}
}
