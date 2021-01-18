package js.wood.script;

import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import js.dom.Document;
import js.dom.Element;
import js.wood.FilePath;
import js.wood.IOperatorsHandler;
import js.wood.IReferenceHandler;
import js.wood.WoodException;
import js.wood.impl.ComponentDescriptor;
import js.wood.impl.Operator;

public class ScriptDependencyHanlder  {
	/** Aggregated set of script classes declared by this component, in no particular order. */
	private final Set<String> scriptClasses = new HashSet<>();

	/** Script file where certain script class is defined. */
	private Map<String, ScriptFile> classScripts = new HashMap<>();

	public void onLayoutLoaded(Document layoutDoc, FilePath layoutPath, IOperatorsHandler operators) {
		collectScriptClasses(layoutPath, layoutDoc.getRoot(), operators);
	}

	public ComponentDescriptor getComponentDescriptor(FilePath layoutPath, IReferenceHandler referenceHandler) {
		// TODO Auto-generated method stub
		return null;
	}

	/**
	 * Scan given layout fragment for script classes and add to component script classes set.
	 * 
	 * @param layoutFile layout source file, for error reporting,
	 * @param layout layout fragment to scan for script classes.
	 * @throws WoodException if layout contains script classes declaration for which project has not a definition file.
	 * @see #scriptClasses
	 */
	private void collectScriptClasses(FilePath layoutFile, Element layout, IOperatorsHandler operators) {
		Element pageClassEl = operators.getByOperator(layout, Operator.SCRIPT);
		if (pageClassEl != null) {
			addScriptClass(layoutFile, operators.getOperand(pageClassEl, Operator.SCRIPT));
		}

		// collect all script classes used for custom elements and formatting
		// first include custom classes then formatting
		// both class and formatting operators are declared by template engine and prefixed with data-

		for (Element scriptClassEl : layout.findByXPath("//*[@data-class]")) {
			addScriptClass(layoutFile, scriptClassEl.getAttr("data-class"));
		}
		for (Element formatClassEl : layout.findByXPath("//*[@data-format]")) {
			addScriptClass(layoutFile, formatClassEl.getAttr("data-format"));
		}
	}


	/**
	 * Add script class to component script classes set, {@link #scriptClasses}. Given script class is collected from layout
	 * source file created by developer. Throws exception if requested script class is not defined in a project script file; a
	 * reason may be class name misspelling.
	 * 
	 * @param layoutFile layout file declaring script class, for error reporting,
	 * @param scriptClass qualified script class name.
	 * @throws WoodException if script class definition file is not found.
	 */
	private void addScriptClass(FilePath layoutFile, String scriptClass) {
		if (scriptClass == null) {
			throw new WoodException("Empty script reference on |%s|. Please check wood:script, data-class and data-format attributes.", layoutFile);
		}
		if (!scriptFileExists(scriptClass)) {
			throw new WoodException("Broken script reference. No script file found for class |%s| requested from |%s|.", scriptClass, layoutFile);
		}
		scriptClasses.add(scriptClass);
	}

	/**
	 * Test if a script class is indeed defined into a script file. This predicate is used when scanning layout files to ensure
	 * that declared script classes are valid.
	 * 
	 * @param scriptClass script qualified class name.
	 * @return true if script class is indeed defined into a script file.
	 */
	private boolean scriptFileExists(String scriptClass) {
		return classScripts.containsKey(scriptClass);
	}
}
