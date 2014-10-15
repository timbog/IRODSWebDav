package com.helloworldTests;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import com.helloworld.FileService;
import java.io.File;
import org.junit.Before;
import org.junit.Test;
import static org.junit.Assert.*;

public class FileServiceTest {

    private FileService fs;

    @org.junit.Before
    public void setUp() throws Exception {
        fs =  FileService.getInstance();
        IRODSAccount irodsAccount = new IRODSAccount("192.168.6.138", 1247, "rods", "rods", "", "tempZone", "demoResc");
        IRODSFileSystem irodsFileSystem = new IRODSFileSystem();
        DataTransferOperations dataTransferOps = irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(irodsAccount);
        //fs.setInitialFolders(irodsAccount.getZone());
        fs.setAccount(irodsAccount);
        fs.setDataTransferOps(dataTransferOps);
        fs.setIRODSFileSystem(irodsFileSystem);
    }

    @org.junit.Test
    public void testGetFile() throws Exception {
        IRODSFile f = fs.getIRODSFileForPath("/tempZone/home/rods/olu.txt");
        //fs.getFile((File) f, System.getProperty("java.io.tmpdir"));
    }
}