package js.wood.cli.compo;

import static js.util.Strings.concat;

import java.io.File;

class CompoCoordinates {
	private final String groupId;
	private final String artifactId;
	private final String version;

	public CompoCoordinates() {
		this.groupId = null;
		this.artifactId = null;
		this.version = null;
	}

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

	@Override
	public int hashCode() {
		final int prime = 31;
		int result = 1;
		result = prime * result + ((artifactId == null) ? 0 : artifactId.hashCode());
		result = prime * result + ((groupId == null) ? 0 : groupId.hashCode());
		result = prime * result + ((version == null) ? 0 : version.hashCode());
		return result;
	}

	@Override
	public boolean equals(Object obj) {
		if (this == obj)
			return true;
		if (obj == null)
			return false;
		if (getClass() != obj.getClass())
			return false;
		CompoCoordinates other = (CompoCoordinates) obj;
		if (artifactId == null) {
			if (other.artifactId != null)
				return false;
		} else if (!artifactId.equals(other.artifactId))
			return false;
		if (groupId == null) {
			if (other.groupId != null)
				return false;
		} else if (!groupId.equals(other.groupId))
			return false;
		if (version == null) {
			if (other.version != null)
				return false;
		} else if (!version.equals(other.version))
			return false;
		return true;
	}

	/**
	 * Parse component coordinates represented as a string value. Return a new component coordinates instance or null if given
	 * value does not respect coordinates syntax. This parser accepts the value if it has three string components separated by
	 * colon (:).
	 * 
	 * @param value component coordinates.
	 * @return component coordinates instance or null if parse fail.
	 */
	public static CompoCoordinates parse(String value) {
		if (value.indexOf(';') != -1) {
			return null;
		}
		int firstSeparator = value.indexOf(':');
		if (firstSeparator == -1) {
			return null;
		}
		int secondSeparator = value.indexOf(':', firstSeparator + 1);
		if (secondSeparator == -1) {
			return null;
		}
		return new CompoCoordinates(value.substring(0, firstSeparator), value.substring(firstSeparator + 1, secondSeparator), value.substring(secondSeparator + 1));
	}
}
