package js.wood;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Collection;
import java.util.Set;

public interface IScriptScanner {
	Set<String> scanClassDefinition(File scriptFile) throws IOException;

	Collection<Dependency> scanDependencies(File scriptFile, Set<String> definedClasses) throws FileNotFoundException;
}
