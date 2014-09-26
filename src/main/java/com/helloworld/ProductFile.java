package com.helloworld;

import java.io.File;

/**
 * Created by Bogdan on 26.09.2014.
 */
public class ProductFile {
    private String name;
    private File file;

    public ProductFile(String name, File file) {
        this.name = name;
        this.file = file;
    }

    public String getName() {
        return name;
    }

    public File getFile() {
        return file;
    }
}