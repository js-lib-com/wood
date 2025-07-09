package com.jslib.wood.test;

import com.jslib.wood.FilePath;
import com.jslib.wood.impl.BaseDescriptor;

public class TestDescriptor extends BaseDescriptor {
    public TestDescriptor(FilePath descriptorFile) {
        super(descriptorFile, descriptorFile.exists() ? descriptorFile.getReader() : null);
    }
}
