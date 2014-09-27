package com.helloworld;

import java.io.File;

/**
 * Created by Bogdan on 27.09.2014.
 */
public class PutTransferRunner implements Runnable{
    private FileService fs;
    private File file;
    private String path;
    public PutTransferRunner(FileService fls, File file, String path)
    {
        this.fs = fls;
        this.file = file;
        this.path = path;
    }
    public void run()		//Этот метод будет выполняться в побочном потоке
    {
        fs.putFile(new UploadDataObj(file), path);
    }
}
