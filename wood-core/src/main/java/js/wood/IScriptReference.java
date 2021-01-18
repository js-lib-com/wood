package js.wood;

public interface IScriptReference {
	String getSource();

	String getType();

	boolean isAsync();
	
	boolean isDefer();

	boolean isNoModule();
	
	String getNonce();
	
	String getReferrerPolicy();
	
	String getIntegrity();

	String getCrossOrigin();
	
	boolean isEmbedded();
}
