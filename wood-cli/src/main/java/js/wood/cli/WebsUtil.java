package js.wood.cli;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.apache.http.impl.client.HttpClientBuilder;

import js.dom.Document;
import js.dom.DocumentBuilder;
import js.dom.Element;
import js.util.Classes;

public class WebsUtil {
	private HttpClientBuilder httpClientBuilder = HttpClientBuilder.create();
	private DocumentBuilder documentBuilder = Classes.loadService(DocumentBuilder.class);

	public Iterable<File> index(URI uri, Pattern fileNamePattern) throws IOException, URISyntaxException {
		URL indexPageURL = uri.toURL();
		Document indexPageDoc = documentBuilder.loadHTML(indexPageURL);

		DateTimeFormatter modificationTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm");

		List<File> files = new ArrayList<>();
		for (Element linkElement : indexPageDoc.findByXPath("//*[@href]")) {
			String fileName = linkElement.getAttr("href");
			Matcher matcher = fileNamePattern.matcher(fileName);
			if (matcher.find()) {
				URI fileURI = new URL(indexPageURL, fileName).toURI();

				Element linkParent = linkElement.getParent();
				Element dateElement = linkParent.getNextSibling();
				ZonedDateTime modificationTime = LocalDateTime.parse(dateElement.getText().trim(), modificationTimeFormatter).atZone(ZoneId.of("UTC"));

				Element sizeElement = dateElement.getNextSibling();
				long fileSize = fileSize(sizeElement.getText().trim());

				files.add(new File(fileURI, fileName, modificationTime, fileSize));
			}
		}

		return files;
	}

	private static final Pattern FILE_SIZE_PATTERN = Pattern.compile("^(\\d+(?:\\.\\d+)?)(K|M|G|T)?$", Pattern.CASE_INSENSITIVE);

	private static long fileSize(String value) {
		Matcher matcher = FILE_SIZE_PATTERN.matcher(value);
		if (!matcher.find()) {
			return 0L;
		}
		Double fileSize = Double.parseDouble(matcher.group(1));
		if (matcher.group(2) != null) {
			switch (matcher.group(2).toUpperCase()) {
			case "K":
				fileSize *= 1024;
				break;
			case "M":
				fileSize *= 1048576;
				break;
			case "G":
				fileSize *= 1073741824;
				break;
			case "T":
				fileSize *= 1099511627776L;
				break;
			}
		}
		return fileSize.longValue();
	}

	public static class File {
		private final URI uri;
		private final String name;
		private final ZonedDateTime modificationTime;
		private final long size;

		public File(URI uri, String name, ZonedDateTime modificationTime, long size) {
			this.uri = uri;
			this.name = name;
			this.modificationTime = modificationTime;
			this.size = size;
		}

		public URI getURI() {
			return uri;
		}

		public String getName() {
			return name;
		}

		public LocalDateTime getModificationTime() {
			return modificationTime.withZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime();
		}

		public long getSize() {
			return size;
		}

		public boolean isAfter(File other) {
			return this.modificationTime.isAfter(other.modificationTime);
		}
	}
}
