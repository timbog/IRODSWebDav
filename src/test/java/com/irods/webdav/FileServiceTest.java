package com.irods.webdav;

import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.irods.jargon.core.pub.io.IRODSFile;
import com.helloworld.FileService;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.File;

import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

public class FileServiceTest {
/*
    public static final String TMP_PATH = System.getProperty("java.io.tmpdir");
    public static final String TEST_FILE_NAME_1 = "vooovq";
    public static final String TEST_FILE_NAME_2 = "iuyiui";
    public static final File TMP_FILE_1 = new File(TMP_PATH, TEST_FILE_NAME_1);
    public static final File TMP_FILE_2 = new File(TMP_PATH, TEST_FILE_NAME_2);

    private FileService fs;

    @Before
    public void setUp() throws Exception {
        fs =  FileService.getInstance();
        IRODSAccount irodsAccount = new IRODSAccount("172.16.16.205", 1247, "rods", "rods", "", "tempZone", "");
        IRODSFileSystem irodsFileSystem = new IRODSFileSystem();
        DataTransferOperations dataTransferOps = irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(irodsAccount);
        //fs.setInitialFolders(irodsAccount.getZone());
        fs.setAccount(irodsAccount);
        fs.setDataTransferOps(dataTransferOps);
        fs.setIRODSFileSystem(irodsFileSystem);
    }
    @After
    public void tearDown() {
        fs = null;
        TMP_FILE_1.delete();
        TMP_FILE_2.delete();
    }


    @Test
    public void testGetFileFromDefaultResc() throws Exception {
        assertFalse("tmp file should not exist", TMP_FILE_1.exists());
        IRODSFile f = fs.getIRODSFileForPath("/tempZone/home/rods/" + TEST_FILE_NAME_1);
        fs.getFile(f.getPath(), TMP_PATH);
        assertTrue("tmp file should exist", TMP_FILE_1.exists());
    }

    @Test
    public void testGetFileFromNotDefaultResc() throws Exception {
        assertFalse("tmp file should not exist", TMP_FILE_2.exists());
        IRODSFile f = fs.getIRODSFileForPath("/tempZone/home/rods/" + TEST_FILE_NAME_2);
        fs.getFile(f.getPath(), TMP_PATH);
        assertTrue("tmp file should exist", TMP_FILE_2.exists());
    }*/
}