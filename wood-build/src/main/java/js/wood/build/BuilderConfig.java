package js.wood.build;

import java.io.File;

public class BuilderConfig {
	private File projectDir;
	private File buildDir;
	private int buildNumber;
	private BuildFS buildFS;

	public File getProjectDir() {
		return projectDir;
	}

	public void setProjectDir(File projectDir) {
		this.projectDir = projectDir;
	}

	public File getBuildDir() {
		return buildDir;
	}

	public void setBuildDir(File buildDir) {
		this.buildDir = buildDir;
	}

	public int getBuildNumber() {
		return buildNumber;
	}

	public void setBuildNumber(int buildNumber) {
		this.buildNumber = buildNumber;
	}

	public BuildFS getBuildFS() {
		return buildFS;
	}

	public void setBuildFS(BuildFS buildFS) {
		this.buildFS = buildFS;
	}

	public Builder createBuilder() {
		return new Builder(this);
	}
}
