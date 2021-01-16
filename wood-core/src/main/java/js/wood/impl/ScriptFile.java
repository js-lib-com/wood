package js.wood.impl;

import java.io.FileNotFoundException;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import js.util.Classes;
import js.wood.Dependency;
import js.wood.DirPath;
import js.wood.FilePath;
import js.wood.IScriptFile;
import js.wood.IScriptScanner;
import js.wood.Project;
import js.wood.WoodException;

/**
 * Meta data about a script file. This class wraps script source path, qualified names for classes defined by script file and
 * lists of dependencies: strong, weak and third party. Relies on {@link DependencyScanner} to actually detect dependencies.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public final class ScriptFile implements IScriptFile {
	/** Script source file. */
	private FilePath sourceFile;

	/** Fully qualified names for classes defined by this script file. */
	private Set<String> definedClasses = new HashSet<>();

	/**
	 * Strong dependencies are those susceptible to be used on script loading. These files need to be loaded before this script
	 * file.
	 */
	private Set<IScriptFile> strongDependencies = new HashSet<>();

	/** Weak dependencies are runtime dependencies used after script loaded. */
	private Set<IScriptFile> weakDependencies = new HashSet<>();

	/** Third party dependencies are URLs to scripts stored on foreign servers, like google maps API. */
	private Set<String> thirdPartyDependencies = new HashSet<>();

	/** Cached value for instance hash code. Cache is possible because is based on source path that is immutable. */
	private int hashCode;

	private final IScriptScanner scriptScanner;

	/**
	 * Unit test constructor.
	 */
	private ScriptFile() {
		this.scriptScanner = Classes.loadOptionalService(IScriptScanner.class);
	}

	/**
	 * Create script file instance. This constructor initializes only defined classes list; dependencies are solved after
	 * project completes first scanning step, see {@link #scanDependencies(Map)}.
	 * <p>
	 * Implementation note. Script file instance initialization is performed in two steps:
	 * <ol>
	 * <li>create instance that scan for defined script classes and fill project script classes map,
	 * <li>scan for dependencies and uses project script classes to determine dependency script file.
	 * </ol>
	 * Script AST scanning is heavy and is performed twice but I do not see reasonable alternative.
	 * 
	 * @param project script parent project,
	 * @param sourceFile script source file.
	 */
	public ScriptFile(Project project, FilePath sourceFile) {
		this();
		this.sourceFile = sourceFile;
		this.hashCode = sourceFile.hashCode();
		System.out.println(sourceFile.toString());

		/*
		// scan for script file dependencies only if discovery is enabled
		if (project.hasScriptDiscovery()) {
			assertScriptScanner();
			try {
				this.definedClasses = scriptScanner.scanClassDefinition(sourceFile.toFile());
			} catch (IOException e) {
				throw new WoodException(e);
			}
		}
		*/
	}

	/**
	 * Get package directory path.
	 * 
	 * @return package directory path.
	 */
	public DirPath getPackagePath() {
		return sourceFile.getDirPath();
	}

	/**
	 * Get script file path.
	 * 
	 * @return script file path.
	 * @see #sourceFile
	 */
	@Override
	public FilePath getSourceFile() {
		return sourceFile;
	}

	/**
	 * Get the set of fully qualified names for classes defined by this script file.
	 * 
	 * @return defined classes.
	 * @see #definedClasses
	 */
	public Set<String> getDefinedClasses() {
		return definedClasses;
	}

	/**
	 * Return strong dependencies in no particular order. This getter returns meaningful value only if called after
	 * {@link #scanDependencies(Map)}. Otherwise returns empty iterator.
	 * 
	 * @return strong dependencies.
	 * @see #strongDependencies
	 */
	@Override
	public Iterable<IScriptFile> getStrongDependencies() {
		return strongDependencies;
	}

	/**
	 * Return weak dependencies, in no particular order. This getter should be invoked after {@link #scanDependencies(Map)};
	 * otherwise returns empty iterator.
	 * 
	 * @return weak dependencies.
	 * @see #weakDependencies
	 */
	@Override
	public Iterable<IScriptFile> getWeakDependencies() {
		return weakDependencies;
	}

	/**
	 * Return third party dependencies. This getter should be called after {@link #scanDependencies(Map)}; otherwise returns
	 * empty iterator.
	 * 
	 * @return third party dependencies.
	 * @see #thirdPartyDependencies
	 */
	@Override
	public Iterable<String> getThirdPartyDependencies() {
		return thirdPartyDependencies;
	}

	/**
	 * Scan this script dependencies and update internal dependencies lists. This method is invoked after project completes
	 * scripts scanning first step; it uses project script classes map to find out where a particular script class is defined.
	 * If script file not found throws exception.
	 * 
	 * @param classScripts project class scripts map.
	 * @throws WoodException if dependencies scanning fails or dependency script file not found.
	 */
	public void scanDependencies(Map<String, ScriptFile> classScripts) throws WoodException {
		assertScriptScanner();
		Collection<Dependency> dependencies = null;
		try {
			dependencies = scriptScanner.scanDependencies(sourceFile.toFile(), definedClasses);
		} catch (FileNotFoundException e) {
			throw new WoodException(e);
		}

		for (Dependency dependency : dependencies) {
			if (dependency.isThirdParty()) {
				thirdPartyDependencies.add(dependency.getName());
				continue;
			}

			ScriptFile scriptFile = classScripts.get(dependency.getName());
			if (scriptFile == null) {
				throw new WoodException("Broken reference. Missing script file for dependency |%s| required by |%s|", dependency, sourceFile);
			}

			// takes care a script to not be stored on both strong and weak dependencies list
			if (dependency.isStrong()) {
				weakDependencies.remove(scriptFile);
				strongDependencies.add(scriptFile);
			} else {
				if (!strongDependencies.contains(scriptFile)) {
					weakDependencies.add(scriptFile);
				}
			}
		}
	}

	// ------------------------------------------------------
	// Object support

	/**
	 * Returns cached value for instance hash code.
	 * 
	 * @return instance hash code.
	 * @see #hashCode
	 */
	@Override
	public int hashCode() {
		return hashCode;
	}

	/**
	 * This method uses cached hash code to compare instances, hash code that is based on source file path.
	 * 
	 * @param obj another script file instance.
	 * @return true if this instance has the same source file path as given one.
	 */
	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScriptFile other = (ScriptFile) obj;
		return this.hashCode == other.hashCode;
	}

	/**
	 * Return string representation of the wrapped source file.
	 * 
	 * @return instance string representation.
	 */
	@Override
	public String toString() {
		return sourceFile.toString();
	}

	private void assertScriptScanner() {
		if (this.scriptScanner == null) {
			throw new WoodException("Project configured with script discovery but no implementation found.");
		}
	}
}
