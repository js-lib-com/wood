package com.jslib.wood.test;

import java.io.File;

public class TestDirectory extends File {
    private static final long serialVersionUID = -4499496665524589579L;

    public TestDirectory(String path) {
        super(path);
    }

    @Override
    public boolean isDirectory() {
        return true;
    }
}
