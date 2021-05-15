package js.wood.cli.atref;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import js.util.Strings;
import picocli.CommandLine.ITypeConverter;

public class VariableReference {
	private static final Pattern PATTERN = Pattern.compile("^\\@([a-z]+)\\/([a-z\\-]+)$");

	private final String value;
	private final String type;
	private final String name;

	private VariableReference(String value) {
		this.value = value;
		Matcher matcher = PATTERN.matcher(value);
		if (!matcher.find()) {
			this.type = null;
			this.name = null;
			return;
		}
		this.type = matcher.group(1);
		this.name = matcher.group(2);
	}

	public boolean isValid() {
		return type != null && name != null;
	}

	public String value() {
		return value;
	}

	public String type() {
		return type;
	}

	public String name() {
		return name;
	}

	public VariableReference clone(String newname) {
		return new VariableReference(Strings.concat('@', type, '/', newname));
	}

	public static class TypeConverter implements ITypeConverter<VariableReference> {
		@Override
		public VariableReference convert(String value) throws Exception {
			return new VariableReference(value);
		}
	}
}
