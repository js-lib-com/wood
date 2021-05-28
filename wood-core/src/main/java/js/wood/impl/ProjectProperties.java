package js.wood.impl;

import java.io.IOException;
import java.io.Reader;
import java.util.Properties;

import js.wood.FilePath;
import js.wood.Project;
import js.wood.WoodException;

public class ProjectProperties {
	public static final String PROPERTIES_FILE = ".project.properties";

	private static final String BUILD_DIR = "build.dir";
	private static final String ASSET_DIR = "asset.dir";
	private static final String THEME_DIR = "theme.dir";

	private final Properties properties = new Properties();

	public ProjectProperties(Project project) {
		FilePath propertiesFile = project.createFilePath(PROPERTIES_FILE);
		if (!propertiesFile.exists()) {
			throw new WoodException("Invalid WOOD project. Missing %s file.", propertiesFile);
		}

		try (Reader reader = propertiesFile.getReader()) {
			properties.load(reader);
		} catch (IOException e) {
			throw new WoodException("Fail to read %s file: %s", propertiesFile, e.getMessage());
		}
	}

	public String getBuildDir() {
		return getProperty(BUILD_DIR);
	}

	public String getAssetDir(String defaultAssetDir) {
		return getProperty(ASSET_DIR, defaultAssetDir);
	}

	public String getThemeDir(String defaultThemeDir) {
		return getProperty(THEME_DIR, defaultThemeDir);
	}

	public String getProperty(String key, String... defaultValue) {
		String value = properties.getProperty(key);
		if (value == null && defaultValue.length == 1) {
			value = defaultValue[0];
		}
		if (value == null) {
			throw new WoodException("Invalid project properties. Missig %s key.", key);
		}
		return value;
	}
}
