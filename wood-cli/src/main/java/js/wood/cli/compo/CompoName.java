package js.wood.cli.compo;

import js.util.Strings;

class CompoName {
	private final String groupId;
	private final String artifactId;
	private final String version;

	public CompoName(String groupId, String artifactId, String version) {
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

	public String toPath() {
		return Strings.concat(groupId.replace('.', '/'), '/', artifactId, '/', version);
	}

	@Override
	public String toString() {
		return Strings.concat(groupId, ':', artifactId, ':', version);
	}
}
