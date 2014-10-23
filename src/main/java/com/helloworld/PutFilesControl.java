package com.helloworld;
import static java.util.concurrent.TimeUnit.*;

import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;

/**
 * Created by bogdan on 23.10.14.
 */
public class PutFilesControl {
    private final ScheduledExecutorService scheduler =
            Executors.newScheduledThreadPool(1);
    private FileService fs;

    private List<Folder> foldList;

    private List<String> localPaths;

    private Date startDate;

    private int index() {
        return (int)((new Date().getTime() - startDate.getTime()) / 1000 % 60);
    }

    public PutFilesControl(FileService fs, List<Folder> parentFolds, List<String> localPaths) {
        this.fs = fs;
        this.foldList = parentFolds;
        this.localPaths = localPaths;
        this.startDate = new Date();
        this.put();
    }

    public void put() {

        //PutTransferRunner putRunner = new PutTransferRunner()
        final ScheduledFuture<?> beeperHandle =
                scheduler.scheduleAtFixedRate(new PutTransferRunner(fs, localPaths.get(index()), foldList.get(index())), 10, 10, SECONDS);
        scheduler.schedule(new Runnable() {
            public void run() { beeperHandle.cancel(true); }
        }, localPaths.size() * 10, SECONDS);
    }
}
