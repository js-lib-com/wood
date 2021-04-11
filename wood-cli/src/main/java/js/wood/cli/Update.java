package js.wood.cli;

import static java.lang.String.format;

import java.io.BufferedOutputStream;
import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Properties;
import java.util.regex.Pattern;
import java.util.zip.ZipEntry;
import java.util.zip.ZipInputStream;

import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpGet;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClientBuilder;

import js.format.FileSize;
import picocli.CommandLine.Command;
import picocli.CommandLine.Option;

@Command(name = "update", description = "Update WOOD install.")
public class Update extends Task {

	private static final URI DISTRIBUTION_URI = URI.create("http://maven.js-lib.com/com/js-lib/wood-assembly/");
	private static final Pattern ARCHIVE_DIRECTORY_PATTERN = Pattern.compile("^\\d+\\.\\d+\\.\\d*(-[a-z0-9]+)?/$", Pattern.CASE_INSENSITIVE);
	private static final Pattern ARCHIVE_FILE_PATTERN = Pattern.compile("^wood-assembly.+\\.zip$");

	@Option(names = { "-f", "--force" }, description = "Force update regardless release date.")
	private boolean force;
	@Option(names = { "-v", "--verbose" }, description = "Verbose printouts about processed files.")
	private boolean verbose;

	private WebsUtil webs = new WebsUtil();

	@Override
	protected ExitCode exec() throws Exception {
		if (verbose) {
			console.print("Checking WOOD assemblies repository...");
		}

		WebsUtil.File assemblyDir = latestVersion(DISTRIBUTION_URI, ARCHIVE_DIRECTORY_PATTERN);
		if (assemblyDir == null) {
			console.print("Empty WOOD assemblies repository %s.", DISTRIBUTION_URI);
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		WebsUtil.File assemblyFile = latestVersion(assemblyDir.getURI(), ARCHIVE_FILE_PATTERN);
		if (assemblyFile == null) {
			console.print("Invalid WOOD assembly version %s. No assembly found.", assemblyDir.getURI());
			console.print("Command abort.");
			return ExitCode.ABORT;
		}

		Path woodHome = files.getPath(getWoodHome());
		// uses wood.properties file to detect last update time
		Path propertiesFile = woodHome.resolve("bin/wood.properties");
		if (files.exists(propertiesFile)) {
			if (!force && !assemblyFile.getModificationTime().isAfter(files.getModificationTime(propertiesFile))) {
				console.print("Current WOOD install is updated.");
				console.print("Command abort.");
				return ExitCode.ABORT;
			}
		}

		console.print("Updating WOOD install from %s...", assemblyFile.getName());
		if (!console.confirm("Please confirm: yes | [no]", "yes")) {
			console.print("User cancel.");
			return ExitCode.CANCEL;
		}

		console.print("Current WOOD home: %s", woodHome);

		HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
		try (CloseableHttpClient client = httpClientBuilder.build()) {
			HttpGet httpGet = new HttpGet(assemblyFile.getURI());
			try (CloseableHttpResponse response = client.execute(httpGet)) {
				if (response.getStatusLine().getStatusCode() != 200) {
					throw new IOException(format("Fail to download %s.", assemblyFile.getURI()));
				}

				try (ZipInputStream zipInputStream = new ZipInputStream(response.getEntity().getContent())) {
					ZipEntry zipEntry;
					while ((zipEntry = zipInputStream.getNextEntry()) != null) {
						String fileName = assemblyFile(zipEntry.getName());
						if (fileName.isEmpty() || fileName.endsWith("/")) {
							continue;
						}

						if (fileName.equals("bin/wood.properties")) {
							mergeProperties(zipInputStream, propertiesFile);
							continue;
						}

						copy(zipInputStream, woodHome.resolve(fileName));
					}
				}
			}
		}

		return ExitCode.SUCCESS;
	}

	private void mergeProperties(ZipInputStream zipInputStream, Path propertiesFile) throws IOException {
		if (verbose) {
			console.print("Download file %s", propertiesFile);
			console.print("Merge global properties %s", propertiesFile);
		}
		Properties assemblyProperties = new Properties();
		assemblyProperties.load(zipInputStream);
		config.updateGlobalProperties(assemblyProperties);
		config.getGlobalProperties().store(files.getOutputStream(propertiesFile), null);
	}

	private void copy(ZipInputStream zipInputStream, Path outputFile) throws IOException {
		if (verbose) {
			console.print("Download file %s", outputFile);
		}
		files.createDirectories(outputFile.getParent().toString());

		byte[] buffer = new byte[2048];
		try (BufferedOutputStream fileOutputStream = new BufferedOutputStream(files.getOutputStream(outputFile), buffer.length)) {
			int len;
			while ((len = zipInputStream.read(buffer)) > 0) {
				fileOutputStream.write(buffer, 0, len);
			}
		}
	}

	private WebsUtil.File latestVersion(URI uri, Pattern filePattern) throws IOException, URISyntaxException {
		WebsUtil.File mostRecentFile = null;
		FileSize fileSizeFormatter = new FileSize();
		DateTimeFormatter modificationTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

		for (WebsUtil.File file : webs.index(uri, filePattern)) {
			if (verbose) {
				console.print("%s %s\t%s", modificationTimeFormatter.format(file.getModificationTime()), fileSizeFormatter.format(file.getSize()), file.getName());
			}
			if (mostRecentFile == null) {
				mostRecentFile = file;
				continue;
			}
			if (file.isAfter(mostRecentFile)) {
				mostRecentFile = file;
			}
		}
		return mostRecentFile;
	}

	private String assemblyFile(String zipEntityName) {
		int index = zipEntityName.indexOf('/') + 1;
		return zipEntityName.substring(index);
	}
}
