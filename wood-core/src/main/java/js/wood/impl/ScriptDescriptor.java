package js.wood.impl;

import js.dom.Element;
import js.wood.IScriptDescriptor;

/**
 * Descriptor for page script element. This class is loaded from <code>script</code> element of project or page descriptor. All
 * standard attributes are supported.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class ScriptDescriptor implements IScriptDescriptor {
	/** Script source is the URL from where third script is to be loaded. */
	private final String source;

	private String type;
	private String async;
	private String defer;
	private String noModule;
	private String nonce;
	private String referrerPolicy;
	private String integrity;
	private String crossOrigin;
	private boolean embedded;

	public ScriptDescriptor(String source) {
		this.source = source;
	}

	@Override
	public String getSource() {
		return source;
	}

	public void setType(String type) {
		this.type = type;
	}

	@Override
	public String getType() {
		return type;
	}

	public void setAsync(String async) {
		this.async = async;
	}

	@Override
	public String getAsync() {
		return async;
	}

	public void setDefer(String defer) {
		this.defer = defer;
	}

	@Override
	public String getDefer() {
		return defer;
	}

	public void setNoModule(String noModule) {
		this.noModule = noModule;
	}

	@Override
	public String getNoModule() {
		return noModule;
	}

	public void setNonce(String nonce) {
		this.nonce = nonce;
	}

	@Override
	public String getNonce() {
		return nonce;
	}

	public void setReferrerPolicy(String referrerPolicy) {
		this.referrerPolicy = referrerPolicy;
	}

	@Override
	public String getReferrerPolicy() {
		return referrerPolicy;
	}

	public void setIntegrity(String integrity) {
		this.integrity = integrity;
	}

	@Override
	public String getIntegrity() {
		return integrity;
	}

	public void setCrossOrigin(String crossOrigin) {
		this.crossOrigin = crossOrigin;
	}

	@Override
	public String getCrossOrigin() {
		return crossOrigin;
	}

	public void setEmbedded(boolean embedded) {
		this.embedded = embedded;
	}

	@Override
	public boolean isEmbedded() {
		return embedded;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((source == null) ? 0 : source.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		ScriptDescriptor other = (ScriptDescriptor) obj;
		if (source == null) {
			if (other.source != null)
				return false;
		} else if (!source.equals(other.source))
			return false;
		return true;
	}

	@Override
	public String toString() {
		return source;
	}

	public static ScriptDescriptor create(Element scriptElement, IScriptDescriptor defaults) {
		final String src = scriptElement.getAttr("src");
		assert src != null;
		ScriptDescriptor script = new ScriptDescriptor(src);

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