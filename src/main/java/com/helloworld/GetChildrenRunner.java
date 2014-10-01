package com.helloworld;

import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;

import java.io.File;
import java.io.*;
import java.text.ParseException;
import java.util.*;
/**
 * Created by Bogdan on 27.09.2014.
 */
public class GetChildrenRunner implements Runnable {

    private FileService fs;
    private Folder folder;
    private ArrayList<Object> productFiles;

    public GetChildrenRunner(FileService fls, Folder folder)
    {
        this.fs = fls;
        this.folder = folder;
    }

    public ArrayList<Object> getProductFiles() {
        return productFiles;
    }

    public void run()
    {
        try {
            List<CollectionAndDataObjectListingEntry> files = fs.getFilesAndCollectionsUnderParentCollection(folder.getPath());
            productFiles = new ArrayList<Object>(files.size());
            for (CollectionAndDataObjectListingEntry entry: files) {
                IRODSFile f = fs.getIRODSFileForPath(entry.getFormattedAbsolutePath());
                if (f.isDirectory()) {
                    Folder tmp = new Folder(f.getName());
                    tmp.setModified(f.lastModified());
                    tmp.setPath(folder.getPath() + "/" + tmp.getName());
                    productFiles.add(tmp);
                }
                else
                    productFiles.add(new ProductFile(f.getName(), (File) f));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        folder.setProductFiles(productFiles);
    }
}