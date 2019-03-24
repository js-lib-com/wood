package js.wood;

/**
 * Strategy used for script dependencies resolving for inclusion into build and preview. Currently, production ready solution is
 * to list script dependencies into component descriptor. It is developer responsibility to keep scripts list updated. There is
 * also an experimental, automatic solution: scan HTM and script sources, discover dependencies and sort in the right oder. But
 * due to limited <code>rhino</code> parser support of ES2015, automatic scan is limited only to standard j(s)-script dialect.
 * <p>
 * This value is configured into {@link Project} via <code>script-dependency-strategy</code> element, with default to
 * {@link #DESCRIPTOR}.
 * 
 * @author Iulian Rotaru
 */
public enum ScriptDependencyStrategy {
	/**
	 * Script dependencies is listed into component descriptor. This is default strategy used when
	 * <code>script-dependency-strategy</code> element is missing from project configuration.
	 */
	DESCRIPTOR,
	/** Experimental script dependencies discovery based on source files scanning. */
	DISCOVERY
}
