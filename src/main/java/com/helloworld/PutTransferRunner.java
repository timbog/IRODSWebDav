package com.helloworld;

import java.io.File;

/**
 * Created by Bogdan on 27.09.2014.
 */
public class PutTransferRunner implements Runnable{
    private FileService fs;
    private File file;
    private Folder parentFold;

    public PutTransferRunner(FileService fls, File file, Folder fold)
    {
        this.fs = fls;
        this.file = file;
        this.parentFold = fold;
    }
    public void run()
    {
        fs.putFile(new UploadDataObj(file), parentFold.getPath());
        parentFold.setTimeToUpdate();
    }
}
