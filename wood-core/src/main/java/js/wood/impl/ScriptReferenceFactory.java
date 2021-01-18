package js.wood.impl;

import js.dom.Element;
import js.wood.WoodException;

public class ScriptReferenceFactory {
	public static ScriptReference create(Element scriptElement) {
		final String src = scriptElement.getAttr("src");
		if (src == null) {
			throw new WoodException("Invalid descriptor file. Missing 'src' attribute from <script> element.");
		}
		ScriptReference script = new ScriptReference(src);

		script.setType(scriptElement.getAttr("type"));
		script.setAsync(Boolean.parseBoolean(scriptElement.getAttr("async")));
		script.setDefer(Boolean.parseBoolean(scriptElement.getAttr("defer")));
		script.setNoModule(Boolean.parseBoolean(scriptElement.getAttr("nomodule")));
		script.setNonce(scriptElement.getAttr("nonce"));
		script.setReferrerPolicy(scriptElement.getAttr("referrerpolicy"));
		script.setIntegrity(scriptElement.getAttr("integrity"));
		script.setCrossOrigin(scriptElement.getAttr("crossorigin"));
		script.setEmbedded(Boolean.parseBoolean(scriptElement.getAttr("embedded")));

		return script;
	}
}
