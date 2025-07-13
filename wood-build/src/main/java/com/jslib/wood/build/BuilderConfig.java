package com.jslib.wood.build;

import java.io.File;

/**
 * Builder config is used by WOOD Maven plugin to configure builder.
 */
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
