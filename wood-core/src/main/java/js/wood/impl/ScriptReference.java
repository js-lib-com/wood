package js.wood.impl;

import js.wood.IScriptReference;

/**
 * Third party script descriptor contains script source and flag to append to document head. This class is loaded from
 * <code>scripts</code> section of the component descriptor, see snippet below. It is used to declare third party scripts.
 * <p>
 * 
 * <pre>
 * &lt;scripts&gt;
 *    &lt;script&gt;http://code.jquery.com/jquery-1.7.min.js&lt;/script&gt;
 *    &lt;script&gt;http://sixqs.com/site/js/lib/qtip.js&lt;/script&gt;
 * &lt;/scripts&gt;
 * </pre>
 * 
 * @author Iulian Rotaru
 */
public class ScriptReference implements IScriptReference {
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

	public ScriptReference(String source) {
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
		ScriptReference other = (ScriptReference) obj;
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
}