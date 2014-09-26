package com.helloworld;

import  java.io.File;
/**
 * Created by Bogdan on 26.09.2014.
 */
public class GetTransferRunner implements Runnable{

    private FileService fs;
    private File file;
    private String path;
    public GetTransferRunner(FileService fls, File file, String path)
    {
        this.fs = fls;
        this.file = file;
        this.path = path;
    }
    public void run()		//Этот метод будет выполняться в побочном потоке
    {
        fs.getFile(file, path);
    }
}
