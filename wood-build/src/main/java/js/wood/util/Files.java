package js.wood.util;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;

import js.util.Strings;

public class Files extends js.util.Files {
	public static boolean isXML(File file, String... roots) throws IOException {
		try (BufferedReader reader = new BufferedReader(new FileReader(file))) {
			String line = reader.readLine();
			if (line.startsWith("<?")) {
				line = reader.readLine();
			}
			for (String root : roots) {
				if (line.startsWith(Strings.concat('<', root, '>'))) {
					return true;
				}
			}
		}
		return false;
	}
}
