package com.helloworld;

import java.io.File;

/**
 * Created by Bogdan on 27.09.2014.
 */
public class PutTransferRunner implements Runnable{
    private FileService fs;
    private String localPath;
    private Folder parentFold;

    public PutTransferRunner(FileService fls, String localPath, Folder fold)
    {
        this.fs = fls;
        this.localPath = localPath;
        this.parentFold = fold;
    }
    public void run()
    {
        fs.putFile(localPath, parentFold.getPath());
        parentFold.setTimeToUpdate();
    }
}
