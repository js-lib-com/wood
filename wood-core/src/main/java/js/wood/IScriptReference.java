package js.wood;

public interface IScriptReference {
	String getSource();

	boolean hasIntegrity();

	String getIntegrity();

	boolean hasCrossorigin();

	String getCrossorigin();

	boolean isDefer();
	
	boolean isAppendToHead();
}
