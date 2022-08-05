package com.jslib.wood.build;

import java.io.File;
import java.io.IOException;

public class BuilderConfig {
	private File projectDir;
	private int buildNumber;
	private BuildFS buildFS;

	public File getProjectDir() {
		return projectDir;
	}

	public void setProjectDir(File projectDir) {
		this.projectDir = projectDir;
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

	public Builder createBuilder() throws IOException {
		return new Builder(this);
	}
}
