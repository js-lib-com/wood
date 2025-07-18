package com.jslib.wood.impl;

import com.jslib.wood.FilePath;
import com.jslib.wood.IScriptDescriptor;
import com.jslib.wood.dom.Element;

/**
 * Descriptor for page script element. This class is loaded from <code>script</code> element of project or page descriptor. All
 * standard attributes are supported.
 * 
 * @author Iulian Rotaru
 * @since 1.0
 */
public class ScriptDescriptor implements IScriptDescriptor {
	/** Script source is local file path or the URL from where third party script is to be loaded. */
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
	private boolean dynamic;

	private ScriptDescriptor(String source) {
		assert source != null: "Script source argument is null";
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

	public void setDynamic(boolean dynamic) {
		this.dynamic = dynamic;
	}

	@Override
	public boolean isDynamic() {
		return dynamic;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + source.hashCode();
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
        return source.equals(other.source);
    }

	@Override
	public String toString() {
		return source;
	}

	private static final String DEF_SCRIPT_TYPE = "text/javascript";
	private static final String DEF_SCRIPT_DEFER = "true";

	public static ScriptDescriptor create(FilePath scriptFile) {
		ScriptDescriptor script = new ScriptDescriptor(scriptFile.value());
		script.setType(DEF_SCRIPT_TYPE);
		script.setDefer(DEF_SCRIPT_DEFER);
		return script;
	}

	public static ScriptDescriptor create(FilePath scriptFile, boolean embedded) {
		ScriptDescriptor script = new ScriptDescriptor(scriptFile.value());
		script.setType(DEF_SCRIPT_TYPE);
		script.setEmbedded(embedded);
		return script;
	}

	public static ScriptDescriptor create(Element scriptElement) {
		final String src = scriptElement.getAttr("src");
		assert src != null;
		ScriptDescriptor script = new ScriptDescriptor(src);

		script.setType(value(scriptElement.getAttr("type"), DEF_SCRIPT_TYPE));
		script.setAsync(scriptElement.getAttr("async"));
		script.setDefer(value(scriptElement.getAttr("defer"), DEF_SCRIPT_DEFER));
		script.setNoModule(scriptElement.getAttr("nomodule"));
		script.setNonce(scriptElement.getAttr("nonce"));
		script.setReferrerPolicy(scriptElement.getAttr("referrerpolicy"));
		script.setIntegrity(scriptElement.getAttr("integrity"));
		script.setCrossOrigin(scriptElement.getAttr("crossorigin"));
		script.setEmbedded(Boolean.parseBoolean(value(scriptElement.getAttr("embedded"), "false")));
		script.setDynamic(Boolean.parseBoolean(value(scriptElement.getAttr("dynamic"), "false")));

		return script;
	}

	private static String value(String base, String defaults) {
		return base != null ? base : defaults;
	}
}
