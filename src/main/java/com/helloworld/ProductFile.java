package com.helloworld;

import java.io.File;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
/**
 * Created by Bogdan on 26.09.2014.
 */
public class ProductFile {
    private String name;
    private String IRODSPath = "";

    public Date getLastModified() {
        return lastModified;
    }

    public void setLastModified(Date lastModified) {
        this.lastModified = lastModified;
    }

    private Date lastModified;

    public long getLength() {
        return length;
    }

    public void setLength(long length) {
        this.length = length;
    }

    private long length;

    public ProductFile(String name, String path) {
        this.name = name;
        this.IRODSPath = path;
    }

    public String getName() {
        return name;
    }

    public void setIRODSPath(String path) {
        this.IRODSPath = path;
    }

    public String getIRODSPath() {
        return IRODSPath;
    }
}