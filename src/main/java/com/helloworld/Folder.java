package com.helloworld;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

/**
 * Created by Bogdan on 26.09.2014.
 */
public class Folder {
    private String name;
    private Date downloadedTime;
    private long modified;
    private List<Object> productFiles = new ArrayList<Object>();
    private String path;

    public Folder(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
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

    public void setTimeToUpdate() {
        Calendar cal = Calendar.getInstance();
        cal.setTime(this.getDownloadedTime());
        cal.add(Calendar.SECOND, -11);
        Date elevenMinutesBack = cal.getTime();
        this.setDownloadedTime(elevenMinutesBack);
    }

    public boolean checkTime(Date time) {
        long a = ((time.getTime() - this.getDownloadedTime().getTime()) / 1000 % 60);
        boolean b = a < 10;
        return (((time.getTime() - this.getDownloadedTime().getTime()) / 1000 % 60) < 10);
    }
}