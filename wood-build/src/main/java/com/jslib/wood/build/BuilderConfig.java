package com.jslib.wood.build;

import java.io.File;

public class BuilderConfig {
    private File projectDir;
    private int buildNumber;

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
}
