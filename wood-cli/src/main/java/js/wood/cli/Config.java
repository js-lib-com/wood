package js.wood.cli;

import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Reader;
import java.io.Writer;
import java.nio.file.Files;
import java.nio.file.Path;
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
	private final File projectPropertiesFile;

	public Config(Properties globalProperties, Properties projectProperties) throws IOException {
		this.converter = ConverterRegistry.getConverter();
		this.globalProperties = globalProperties;
		this.projectProperties = projectProperties;

		File workingDir = Paths.get("").toAbsolutePath().toFile();
		this.projectPropertiesFile = new File(workingDir, PROJECT_PROPERTIES_FILE);
		if (this.projectPropertiesFile.exists()) {
			try (Reader reader = new FileReader(projectPropertiesFile)) {
				projectProperties.load(reader);
			}
		}

		Path woodDir = Paths.get(Task.getHome());
		Path propertiesFile = woodDir.resolve("bin/wood.properties");
		if (Files.exists(propertiesFile)) {
			try (Reader reader = Files.newBufferedReader(propertiesFile)) {
				globalProperties.load(reader);
			}
		}
	}

	public SortedMap<String, String> getProperties(boolean includeGlobal) throws IOException {
		SortedMap<String, String> properties = new TreeMap<>();
		for (Map.Entry<Object, Object> entry : projectProperties.entrySet()) {
			properties.put(entry.getKey().toString(), entry.getValue().toString());
		}
		if (includeGlobal) {
			for (Map.Entry<Object, Object> entry : globalProperties.entrySet()) {
				properties.put(entry.getKey().toString(), entry.getValue().toString());
			}
		}
		return properties;
	}

	public Properties getGlobalProperties() throws IOException {
		return globalProperties;
	}

	public void updateGlobalProperties(Properties properties) throws IOException {
		properties.forEach((key, value) -> globalProperties.merge(key, value, (oldValue, newValue) -> newValue));
	}

	public void set(String key, Object value) throws IOException {
		if (!projectPropertiesFile.exists()) {
			throw new BugError("Attempt to alter proporties outside project.");
		}
		projectProperties.put(key, converter.asString(value));
		try (Writer writer = new FileWriter(projectPropertiesFile)) {
			projectProperties.store(writer, "project properties");
		}
	}

	public void unset(String key) throws IOException {
		if (!projectPropertiesFile.exists()) {
			throw new BugError("Attempt to alter proporties outside project.");
		}
		projectProperties.remove(key);
		try (Writer writer = new FileWriter(projectPropertiesFile)) {
			projectProperties.store(writer, "project properties");
		}
	}

	public <T> T get(String key, Class<T> type, String... defaultValue) throws IOException {
		Object value = projectProperties.get(key);
		if (value == null) {
			value = globalProperties.get(key);
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

	public boolean has(String key) throws IOException {
		Object value = projectProperties.get(key);
		if (value == null) {
			value = globalProperties.get(key);
			if (value == null) {
				return false;
			}
		}
		return true;
	}
}
