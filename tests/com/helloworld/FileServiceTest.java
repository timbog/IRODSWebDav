package com.helloworld;

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
        fs = new FileService();
    }

    @org.junit.Test
    public void testGetFile() throws Exception {
        IRODSFile f = fs.getIRODSFileForPath("\\tempZone\\home\\rods\\po.txt");
        fs.getFile((File) f, System.getProperty("java.io.tmpdir"));
    }
}