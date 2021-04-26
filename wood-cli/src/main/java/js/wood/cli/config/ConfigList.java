package js.wood.cli.config;

import static java.lang.String.format;

import java.util.Map;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "list", description = "Project properties list.")
public class ConfigList extends Task {
	@Option(names = { "-a", "--all" }, description = "List both project and global properties.")
	private boolean all;

	@Override
	protected ExitCode exec() throws Exception {
		Map<String, String> properties = config.getProperties(all);
		int keyWidth = 0;
		for (String key : properties.keySet()) {
			if (keyWidth < key.length()) {
				keyWidth = key.length();
			}
		}
		String message = format(" - %%-%ds : %%s", keyWidth);

		console.crlf();
		for (Map.Entry<String, String> entry : config.getProperties(all).entrySet()) {
			console.print(message, entry.getKey(), entry.getValue());
		}

		return ExitCode.SUCCESS;
	}
}
