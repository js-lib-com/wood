package js.wood;

import java.util.HashMap;
import java.util.Locale;
import java.util.Map;

/**
 * Variable caches used to speed up preview process. Cache life spans for a component preview session.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
class VariablesCache {
	private final Project project;

	/** Project asset variables. Asset variables are used when requested variable name miss from source variables. */
	private final Variables assetVariables;

	/**
	 * Source variables instances mapped by source directory where variables files are defined. Asset variables are not
	 * included.
	 */
	private final Map<FilePath, Variables> sourceVariablesMap = new HashMap<>();

	public VariablesCache(Project project) {
		this.project = project;
		this.assetVariables = this.project.createVariables(project.getAssetDir());
	}

	/**
	 * Initialize variables cache by cleaning component variables hash and rescanning assets and site styles directories.
	 * 
	 * @param project parent project.
	 */
	public synchronized void update() {
		assetVariables.reload(project.getAssetDir());
		sourceVariablesMap.clear();
	}

	/**
	 * Return cached variable value for requested variable reference. First attempt to retrieve variable value from source
	 * variables and if not found attempt retrieving it from asset. If variables are not already on source variables map create
	 * new instance and store before return it.
	 * <p>
	 * Return null if variable not found or if is empty.
	 * 
	 * @param local current locale,
	 * @param reference variable reference to resolve,
	 * @param sourceFile source file containing the variable reference,
	 * @param handler variable reference handler for specific processing.
	 * @return variable value or null if variable not defined or if is empty.
	 */
	public String get(Locale locale, Reference reference, FilePath sourceFile, IReferenceHandler handler) {
		FilePath sourceDir = sourceFile.getParentDir();
		Variables sourceVariables = sourceVariablesMap.get(sourceDir);
		if (sourceVariables == null) {
			synchronized (this) {
				if (sourceVariables == null) {
					sourceVariables = project.createVariables(sourceDir);
					sourceVariablesMap.put(sourceDir, sourceVariables);
				}
			}
		}

		String value = sourceVariables.get(locale, reference, sourceFile, handler);
		if (value == null) {
			value = assetVariables.get(locale, reference, sourceFile, handler);
		}
		return value;
	}

	// --------------------------------------------------------------------------------------------
	// Test support

	Map<FilePath, Variables> getSourceVariablesMap() {
		return sourceVariablesMap;
	}
}