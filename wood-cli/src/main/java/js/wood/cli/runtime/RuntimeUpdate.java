package js.wood.cli.runtime;

import java.net.URI;
import java.nio.file.Path;
import java.util.regex.Pattern;

import com.jslib.commons.cli.ExitCode;
import com.jslib.commons.cli.Task;
import com.jslib.commons.cli.WebsUtil;

import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "update", description = "Update runtime WOOD libraries.")
public class RuntimeUpdate extends Task {
	private static final URI DISTRIBUTION_URI = URI.create("http://maven.js-lib.com/com/js-lib/");
	private static final Pattern DIRECTORY_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d*(?:-[a-z0-9]+)?/$", Pattern.CASE_INSENSITIVE);
	// wood-core-1.1.0-20210422.044635-1.jar
	private static final Pattern FILE_PATTERN = Pattern.compile("^wood-(?:core|preview)-(?:[0-9.-]+).jar$");

	@Option(names = { "-y", "--yes" }, description = "Auto-confirm update.")
	private boolean yes;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about processed files.")
	private boolean verbose;

	@Override
	protected ExitCode exec() throws Exception {
		Path runtimeDir = files.getPath(config.get("runtime.home")).resolve(config.get("runtime.name"));
		if (!assertValid(() -> files.exists(runtimeDir), "Missing runtime directory %s", runtimeDir)) {
			return ExitCode.ABORT;
		}
		Path libDir = runtimeDir.resolve("lib");
		if (!assertValid(() -> files.exists(libDir), "Missing runtime library directory %s", libDir)) {
			return ExitCode.ABORT;
		}

		if (verbose) {
			console.print("Checking WOOD libraries repository...");
		}

		// is critical to end the path with separator
		URI coreURI = DISTRIBUTION_URI.resolve("wood-core/");
		WebsUtil.File coreDir = webs.latestVersion(coreURI, DIRECTORY_PATTERN, verbose);
		if (!assertValid(() -> coreDir != null, "Empty WOOD core library repository %s.", coreURI)) {
			return ExitCode.ABORT;
		}
		WebsUtil.File coreFile = webs.latestVersion(coreDir.getURI(), FILE_PATTERN, verbose);
		if (!assertValid(() -> coreFile != null, "Invalid WOOD core version %s. No archive found.", coreDir.getURI())) {
			return ExitCode.ABORT;
		}

		// is critical to end the path with separator
		URI previewURI = DISTRIBUTION_URI.resolve("wood-preview/");
		WebsUtil.File previewDir = webs.latestVersion(previewURI, DIRECTORY_PATTERN, verbose);
		if (!assertValid(() -> previewDir != null, "Empty WOOD preview library repository %s.", previewURI)) {
			return ExitCode.ABORT;
		}
		WebsUtil.File previewFile = webs.latestVersion(previewDir.getURI(), FILE_PATTERN, verbose);
		if (!assertValid(() -> previewFile != null, "Invalid WOOD preview version %s. No archive found.", previewDir.getURI())) {
			return ExitCode.ABORT;
		}

		console.print("Updating wood-core library from %s", coreFile.getName());
		console.print("Updating wood-preview library from %s", previewFile.getName());
		if (!yes && !console.confirm("Please confirm: yes | [no]", "yes")) {
			console.print("User cancel.");
			return ExitCode.CANCEL;
		}

		console.print("Downloading WOOD core archive %s", coreFile.getName());
		Path downloadedCoreFile = webs.download(coreFile, libDir.resolve("~" + coreFile.getName()), verbose);

		console.print("Downloading WOOD preview archive %s", previewFile.getName());
		Path downloadedPreviewFile = webs.download(previewFile, libDir.resolve("~" + previewFile.getName()), verbose);

		console.print("Replacing WOOD core archive %s", coreFile.getName());
		files.deleteIfExists(files.getFileByNamePattern(libDir, Pattern.compile("^wood-core-\\d+\\.\\d+.+\\.jar$")));
		files.move(downloadedCoreFile, libDir.resolve(coreFile.getName()));

		console.print("Replacing WOOD preview archive %s", previewFile.getName());
		files.deleteIfExists(files.getFileByNamePattern(libDir, Pattern.compile("^wood-preview-\\d+\\.\\d+.+\\.jar$")));
		files.move(downloadedPreviewFile, libDir.resolve(previewFile.getName()));

		return ExitCode.SUCCESS;
	}

	@FunctionalInterface
	private interface Predicate {
		boolean test();
	}

	private boolean assertValid(Predicate predicate, String message, Object... args) {
		if (predicate.test()) {
			return true;
		}
		console.print(message, args);
		console.print("Command abort.");
		return false;
	}
}
