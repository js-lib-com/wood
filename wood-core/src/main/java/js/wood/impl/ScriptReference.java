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

	private final String integrity;
	private final String crossorigin;
	private final boolean defer;
	
	/**
	 * Usually scripts are inserted into page document at the bottom, after body content. This flag is used to force script
	 * loading on document header.
	 */
	final boolean appendToHead;

	public ScriptReference(String source, String integrity, String crossorigin, boolean defer, boolean appendToHead) {
		this.source = source;
		this.integrity = integrity;
		this.crossorigin = crossorigin;
		this.defer=defer;
		this.appendToHead = appendToHead;
	}

	@Override
	public String getSource() {
		return source;
	}

	@Override
	public boolean hasIntegrity() {
		return integrity != null;
	}

	@Override
	public String getIntegrity() {
		return integrity;
	}

	@Override
	public boolean hasCrossorigin() {
		return crossorigin != null;
	}

	@Override
	public String getCrossorigin() {
		return crossorigin;
	}

	@Override
	public boolean isDefer() {
		return defer;
	}

	@Override
	public boolean isAppendToHead() {
		return appendToHead;
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
}