package js.wood.script;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public class ScriptScanner  {
	public ScriptScanner() {
	}

	public Set<String> scanClassDefinition(File scriptFile) throws IOException {
		return ClassDefinitionScanner.getClasses(scriptFile);
	}

	public Collection<Dependency> scanDependencies(File scriptFile, Set<String> definedClasses) throws FileNotFoundException {
		DependencyScanner scanner = new DependencyScanner();
		// defined classes are excluded from dependencies scanning
		return scanner.getDependencies(scriptFile, definedClasses);
	}
}
