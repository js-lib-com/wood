package js.wood.cli.compo;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;

/**
 * Value class for component name.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class CompoName {
	private final String value;
	private final String path;

	public CompoName(String value) throws IOException {
		this.value = value;
		this.path = value.endsWith("?") ? search(value.substring(0, value.length() - 1)) : value;
	}

	public String value() {
		return value;
	}

	public String path() {
		return path;
	}

	@Override
	public String toString() {
		return path;
	}

	public boolean isValid() {
		return path != null;
	}

	private static String search(String prefix) throws IOException {
		File workingDir = Paths.get("").toAbsolutePath().toFile();

		int workingDirPathLength = workingDir.getPath().length();
		List<String> paths = new ArrayList<>();
		search(workingDir, prefix, paths);
		switch (paths.size()) {
		case 0:
			return null;

		case 1:
			// component path is relative to working (project) directory; +1 is for path separator
			return paths.get(0).substring(workingDirPathLength + 1);

		default:
			throw new IllegalStateException("Multiple components found. Please refine search criterion.");
		}
	}

	private static String search(File dir, String prefix, List<String> paths) throws IOException {
		assert dir.isDirectory();
		File[] files = dir.listFiles();
		if (files == null) {
			throw new IOException(format("Cannot list directory %s files.", dir));
		}

		for (File file : files) {
			if (file.isDirectory()) {
				if (file.getName().startsWith(prefix)) {
					return file.getPath().replace('\\', '/');
				}
				String path = search(file, prefix, paths);
				if (path != null) {
					paths.add(path);
				}
			}
		}

		return null;
	}
}
