package js.wood.script;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

import js.wood.Dependency;
import js.wood.IScriptScanner;

public class ScriptScanner implements IScriptScanner {
	public ScriptScanner() {
	}

	@Override
	public Set<String> scanClassDefinition(File scriptFile) throws IOException {
		return ClassDefinitionScanner.getClasses(scriptFile);
	}

	@Override
	public Collection<Dependency> scanDependencies(File scriptFile, Set<String> definedClasses) throws FileNotFoundException {
		DependencyScanner scanner = new DependencyScanner();
		// defined classes are excluded from dependencies scanning
		return scanner.getDependencies(scriptFile, definedClasses);
	}
}
