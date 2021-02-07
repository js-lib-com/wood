package js.wood;

public interface IScriptReference {
	String getSource();

	String getType();

	String getAsync();
	
	String getDefer();

	String getNoModule();
	
	String getNonce();
	
	String getReferrerPolicy();
	
	String getIntegrity();

	String getCrossOrigin();
	
	boolean isEmbedded();
}
