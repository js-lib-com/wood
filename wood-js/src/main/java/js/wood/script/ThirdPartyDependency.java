package js.wood.script;

/**
 * Third party dependency.
 * 
 * @author Iulian Rotaru
 * @version final
 */
public final class ThirdPartyDependency extends Dependency {
	/**
	 * Create named dependency with dependency type set to {@link Dependency.Type#THIRD_PARTY}.
	 * 
	 * @param name dependency name.
	 */
	public ThirdPartyDependency(String name) {
		super(name, Type.THIRD_PARTY);
	}
}
