package js.wood;

import java.util.Locale;

public interface IVariables {
	void setAssetVariables(IVariables assetVariables);

	void setThemeVariables(IVariables themeVariables);

	String get(Locale locale, IReference reference, FilePath source, IReferenceHandler handler) throws WoodException;
}
