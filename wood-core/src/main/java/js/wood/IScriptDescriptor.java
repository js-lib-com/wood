package js.wood;

import java.util.List;

public interface IScriptDescriptor {
	String getSource();

	List<IScriptDescriptor> getDependencies();

	String getType();

	String getAsync();

	String getDefer();

	String getNoModule();

	String getNonce();

	String getReferrerPolicy();

	String getIntegrity();

	String getCrossOrigin();

	boolean isEmbedded();

	boolean isDynamic();
}
