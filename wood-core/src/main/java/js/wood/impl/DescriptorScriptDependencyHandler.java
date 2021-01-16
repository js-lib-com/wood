package js.wood.impl;

import js.dom.Document;
import js.wood.CT;
import js.wood.FilePath;
import js.wood.IOperatorsHandler;
import js.wood.IReferenceHandler;
import js.wood.IScriptDependecyHandler;

public class DescriptorScriptDependencyHandler implements IScriptDependecyHandler {

	@Override
	public void onLayoutLoaded(Document layoutDoc, FilePath layoutPath, IOperatorsHandler operators) {
		// TODO Auto-generated method stub

	}

	@Override
	public ComponentDescriptor getComponentDescriptor(FilePath layoutPath, IReferenceHandler referenceHandler) {
		FilePath descriptorFile = layoutPath.getDirPath().getFilePath(layoutPath.getDirPath().getName() + CT.DOT_XML_EXT);
		// return new ComponentDescriptor(descriptorFile, referenceHandler);
		return new ComponentDescriptor(descriptorFile, referenceHandler);
	}

}
