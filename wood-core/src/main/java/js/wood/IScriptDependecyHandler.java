package js.wood;

import js.dom.Document;
import js.wood.impl.ComponentDescriptor;

public interface IScriptDependecyHandler {
	void onLayoutLoaded(Document layoutDoc, FilePath layoutPath, IOperatorsHandler operators);
	
	ComponentDescriptor getComponentDescriptor(FilePath layoutPath, IReferenceHandler referenceHandler);
}
