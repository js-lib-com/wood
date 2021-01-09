package js.wood;

public interface IScriptFile {
	FilePath getSourceFile();

	Iterable<IScriptFile> getStrongDependencies();

	Iterable<IScriptFile> getWeakDependencies();

	Iterable<String> getThirdPartyDependencies();
}
