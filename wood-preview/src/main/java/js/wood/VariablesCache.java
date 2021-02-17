package js.wood;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

import js.wood.impl.Variables;

/**
 * Variable caches used to speed up preview process. Cache life span last for a component preview session.
 * 
 * @author Iulian Rotaru
 */
public class VariablesCache {
	private final PreviewProject project;

	/** Project asset variables. */
	private final Variables assetVariables;

	/** Components variables. */
	private final Map<DirPath, Variables> variablesMap;

	public VariablesCache(PreviewProject project) {
		this.project = project;
		this.assetVariables = new Variables(project.getAssetsDir());
		this.variablesMap = new HashMap<>();
	}

	/**
	 * Initialize variables cache by cleaning component variables hash and rescanning assets and site styles directories.
	 * 
	 * @param project parent project.
	 */
	public synchronized void update() {
		assetVariables.reload(project.getAssetsDir());
		variablesMap.clear();
	}

	/**
	 * Return cached component variables. If variables are not already on hash create new instance and store before return it.
	 * Component to return variables for is identified by given source file.
	 * 
	 * @param sourcePath component source file.
	 * @return component variables.
	 */
	public String get(Locale locale, Reference reference, FilePath sourcePath, IReferenceHandler handler) {
		Variables variables = variablesMap.get(sourcePath.getParentDirPath());
		if (variables == null) {
			synchronized (this) {
				if (variables == null) {
					variables = new Variables(sourcePath.getParentDirPath());
					variablesMap.put(sourcePath.getParentDirPath(), variables);
				}
			}
		}

		String value = variables.get(locale, reference, sourcePath, handler);
		if (value == null) {
			value = assetVariables.get(locale, reference, sourcePath, handler);
		}
		return value;
	}
}