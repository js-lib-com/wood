package js.wood.cli.atref;

import static java.lang.String.format;

import java.io.File;
import java.io.IOException;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.List;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;
import com.jslib.commons.cli.TextReplace;

import js.util.Files;
import picocli.CommandLine.Command;
import picocli.CommandLine.Model.CommandSpec;
import picocli.CommandLine.ParameterException;
import picocli.CommandLine.Parameters;
import picocli.CommandLine.Spec;

@Command(name = "variable", description = "Rename at-variable.")
public class RenameVariable extends Task {
	@Spec
	private CommandSpec commandSpec;

	@Parameters(index = "0", description = "At-variable reference.", converter = VariableReference.TypeConverter.class)
	private VariableReference reference;
	@Parameters(index = "1", description = "At-variable new name.")
	private String newname;

	private TextReplace textReplace = new TextReplace();

	@Override
	protected ExitCode exec() throws Exception {
		if (!reference.isValid()) {
			throw new ParameterException(commandSpec.commandLine(), format("Invalid variable reference %s.", reference.value()));
		}

		Path projectDir = files.getProjectDir();

		textReplace.setFilter(file -> isXML(file, reference.type()));
		textReplace.replaceAll(projectDir.toFile(), reference.name(), newname);

		textReplace.setFilter(file -> isTargetFile(file));
		String newreference = reference.clone(newname).value();
		textReplace.replaceAll(projectDir.toFile(), reference.value(), newreference);

		return ExitCode.SUCCESS;
	}

	private static boolean isXML(File file, String root) {
		try {
			return Files.isXML(file, root);
		} catch (IOException e) {
		}
		return false;
	}

	private static boolean isTargetFile(File file) {
		List<String> targetFileExtensions = Arrays.asList("htm", "css", "js", "xml");
		for (String targetFileExtension : targetFileExtensions) {
			if (file.getName().endsWith(targetFileExtension)) {
				return true;
			}
		}
		return false;
	}
}
