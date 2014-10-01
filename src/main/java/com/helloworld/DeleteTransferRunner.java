package com.helloworld;

import java.io.File;

/**
 * Created by Bogdan on 01.10.2014.
 */
public class DeleteTransferRunner implements Runnable{

    private FileService fs;
    private String path;

    public DeleteTransferRunner(FileService fls, String path)
    {
        this.fs = fls;
        this.path = path;
    }

    public void run()		//Этот метод будет выполняться в побочном потоке
    {
        try {
            fs.deleteFileOrFolderNoForce(path);
        } catch (Exception ex) {

        }
    }
}
