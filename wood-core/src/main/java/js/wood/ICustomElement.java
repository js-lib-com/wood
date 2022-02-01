package js.wood;

/**
 * Registry meta-data for components based on W3C custom elements. A component based on W3C custom element has a tag name that
 * contains at least one hyphen. Since tag name is used to identify the component on registry its usage can be simplified, WOOD
 * operator being optional.
 * 
 * Be it an hypothetical tab view with two tabs: a list view and a grid view. Here is canonical syntax using WOOD operators to
 * declare components relations.
 * 
 * <pre>
 * <tab-view w:template="compos/tab-view">
 * 	<list-view w:compo="compos/list-view" />
 * 	<grid-view w:compo="compos/grid-view" />	
 * </tab-view>
 * </pre>
 * 
 * Since we already declare on descriptors the tab view is a template and the other two view are child components, above layout
 * can be written without WOOD operators.
 * 
 * <pre>
 * <tab-view>
 * 	<list-view />
 * 	<grid-view />	
 * </tab-view>
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public interface ICustomElement {

	String compoPath();

	String operator();

}
