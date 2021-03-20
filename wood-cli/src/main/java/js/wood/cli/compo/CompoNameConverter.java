package js.wood.cli.compo;

import picocli.CommandLine.ITypeConverter;

class CompoNameConverter implements ITypeConverter<CompoName> {
	@Override
	public CompoName convert(String value) throws Exception {
		return new CompoName(value);
	}
}