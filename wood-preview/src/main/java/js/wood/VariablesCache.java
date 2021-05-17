package js.wood;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Variable caches used to speed up preview process. Cache life span last for a component preview session.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class VariablesCache {
	private final Project project;

	private final Factory factory;

	/** Project asset variables. */
	private final Variables assetVariables;

	/** Components variables. */
	private final Map<FilePath, Variables> variablesMap = new HashMap<>();

	public VariablesCache(Project project) {
		this.project = project;
		this.factory = project.getFactory();
		this.assetVariables = this.factory.createVariables(project.getAssetsDir());
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
		Variables sourceVariables = variablesMap.get(sourcePath.getParentDir());
		if (sourceVariables == null) {
			synchronized (this) {
				if (sourceVariables == null) {
					sourceVariables = factory.createVariables(sourcePath.getParentDir());
					variablesMap.put(sourcePath.getParentDir(), sourceVariables);
				}
			}
		}

		String value = sourceVariables.get(locale, reference, sourcePath, handler);
		if (value == null) {
			value = assetVariables.get(locale, reference, sourcePath, handler);
		}
		return value;
	}

	// --------------------------------------------------------------------------------------------
	// Test support
	
	Map<FilePath, Variables> getVariablesMap() {
		return variablesMap;
	}
}