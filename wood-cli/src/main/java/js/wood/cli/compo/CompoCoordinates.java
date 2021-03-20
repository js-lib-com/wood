package js.wood.cli.compo;

import static js.util.Strings.concat;

import java.io.File;

class CompoCoordinates {
	private final String groupId;
	private final String artifactId;
	private final String version;

	public CompoCoordinates(String groupId, String artifactId, String version) {
		this.groupId = groupId;
		this.artifactId = artifactId;
		this.version = version;
	}

	public String getGroupId() {
		return groupId;
	}

	public String getArtifactId() {
		return artifactId;
	}

	public String getVersion() {
		return version;
	}

	public boolean isValid() {
		return groupId != null && artifactId != null && version != null;
	}

	public String toFile() {
		return concat(groupId.replace('.', File.separatorChar), File.separatorChar, artifactId, File.separatorChar, version);
	}

	public String toPath() {
		return concat(groupId.replace('.', '/'), '/', artifactId, '/', version);
	}

	@Override
	public String toString() {
		return concat(groupId, ':', artifactId, ':', version);
	}
}
