package js.wood.impl;

import js.dom.Element;
import js.wood.IScriptReference;
import js.wood.WoodException;

public class ScriptReferenceFactory {
	public static ScriptReference create(Element scriptElement, IScriptReference defaults) {
		final String src = scriptElement.getAttr("src");
		if (src == null) {
			throw new WoodException("Invalid descriptor file. Missing 'src' attribute from <script> element.");
		}
		ScriptReference script = new ScriptReference(src);

		script.setType(value(scriptElement.getAttr("type"), defaults.getType()));
		script.setAsync(value(scriptElement.getAttr("async"), defaults.getAsync()));
		script.setDefer(value(scriptElement.getAttr("defer"), defaults.getDefer()));
		script.setNoModule(value(scriptElement.getAttr("nomodule"), defaults.getNoModule()));
		script.setNonce(value(scriptElement.getAttr("nonce"), defaults.getNonce()));
		script.setReferrerPolicy(value(scriptElement.getAttr("referrerpolicy"), defaults.getReferrerPolicy()));
		script.setIntegrity(value(scriptElement.getAttr("integrity"), defaults.getIntegrity()));
		script.setCrossOrigin(value(scriptElement.getAttr("crossorigin"), defaults.getCrossOrigin()));
		script.setEmbedded(Boolean.parseBoolean(scriptElement.getAttr("embedded")));

		return script;
	}

	private static String value(String base, String defaults) {
		return base != null ? base : defaults;
	}
}
