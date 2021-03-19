package js.wood.cli.compo;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import picocli.CommandLine.ITypeConverter;

class CompoNameConverter implements ITypeConverter<CompoName> {
	private static final Pattern MAVEN_COORDINATES = Pattern.compile("^([^:]+):([^:]+):([^:]+)$", Pattern.CASE_INSENSITIVE);

	@Override
	public CompoName convert(String value) throws Exception {
		Matcher matcher = MAVEN_COORDINATES.matcher(value);
		if (matcher.find()) {
			return new CompoName(matcher.group(1), matcher.group(2), matcher.group(3));
		}
		return null;
	}
}