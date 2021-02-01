package js.wood.impl;

/**
 * Media query definition from project descriptor.
 * 
 * @author Iulian Rotaru
 */
public class MediaQueryDefinition {
	private final String alias;
	private final String expression;
	private final int weight;

	public MediaQueryDefinition(String alias, String expression, int index) {
		this.alias = alias;
		this.expression = expression;
		this.weight = index + 1;
	}

	public String getAlias() {
		return alias;
	}

	public String getExpression() {
		return expression;
	}

	public int getWeight() {
		return weight;
	}

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((alias == null) ? 0 : alias.hashCode());
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
		MediaQueryDefinition other = (MediaQueryDefinition) obj;
		if (alias == null) {
			if (other.alias != null)
				return false;
		} else if (!alias.equals(other.alias))
			return false;
		return true;
	}
}
