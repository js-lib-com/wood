package com.jslib.wood.test;

import java.io.File;

public class TestFile extends File {
    private static final long serialVersionUID = -5975578621510948684L;

    public TestFile(String pathname) {
        super(pathname);
    }

    @Override
    public boolean isFile() {
        return true;
    }
}
