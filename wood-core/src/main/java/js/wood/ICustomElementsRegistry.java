package js.wood;

/**
 * Registry for WOOD components based on W3C custom elements. These components support a simplified usage syntax where WOOD
 * operators can be omitted. For example <code>&ltglist-view w:compo="compo/list-view" ...</code> can be reduced to
 * <code>&ltglist-view ...</code>.
 * 
 * @author Iulian Rotaru
 */
public interface ICustomElementsRegistry {

	void register(String tagName, CompoPath compoPath, String operator);

	ICustomElement getByTagName(String tagName);

}