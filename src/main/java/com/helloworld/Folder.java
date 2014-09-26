package com.helloworld;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;

/**
 * Created by Bogdan on 26.09.2014.
 */
public class Folder {

    private String name;
    private Date downloadedTime;
    private long modified;
    private List<Object> productFiles;
    private String path;

    public Folder(String name) {
        this.name = name;
        this.productFiles = new ArrayList<Object>();
    }

    public String getName() {
        return name;
    }

    public List<Object> getProductFiles() {
        return productFiles;
    }

    public void setProductFiles(List<Object> pf) {
        this.productFiles = pf;
    }

    public Date getDownloadedTime() {
        return downloadedTime;
    }

    public void setDownloadedTime(Date time) {
        this.downloadedTime = time;
    }

    public void setPath(String path) {
        this.path = path;
    }

    public String getPath(){
        return path;
    }

    public long getModified() {
        return modified;
    }

    public void setModified(long modified) {
        this.modified = modified;
    }
}