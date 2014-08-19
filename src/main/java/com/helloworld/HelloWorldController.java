/*
 * Licensed to the Apache Software Foundation (ASF) under one
 * or more contributor license agreements.  See the NOTICE file
 * distributed with this work for additional information
 * regarding copyright ownership.  The ASF licenses this file
 * to you under the Apache License, Version 2.0 (the
 * "License"); you may not use this file except in compliance
 * with the License.  You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied.  See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */
package com.helloworld;

import io.milton.annotations.*;

import java.io.*;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;

import org.apache.commons.io.FileUtils;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;

import javax.swing.JOptionPane;

@ResourceController
public class HelloWorldController  {
    private FileService service = new FileService();

    private static org.apache.log4j.Logger log = org.apache.log4j.Logger.getLogger(HelloWorldController.class);

    private List<IRODSZone> IRODSZones = new ArrayList<IRODSZone>();

    public HelloWorldController(){
        IRODSZone pro = new IRODSZone("tempZone");
        pro.path = "/tempZone/home/rods";
        try {

            //File fl = new File("C:\\Users\\Bogdan\\Documents\\newTest.txt");
            //fl.createNewFile();
            //UploadDataObj success = new UploadDataObj(fl);
            //service.putFile(success);
            //ProductFile file = new ProductFile(f.getName(),(File) f);
            //pro.productFiles.add(file);
            IRODSZones.add(pro);
            //IRODSZones.add(pro2);

        } catch (Exception e) {
            System.out.print("Some shit2");
        }
    }

    @Root
    public HelloWorldController getRoot() {
        return this;
    }

    @ChildrenOf
    public List<IRODSZone> getProducts(HelloWorldController root) {
        return IRODSZones;
    }

    // how to get subfolders????

    @ChildrenOf
    public List<Object> getProductFiles(IRODSZone IRODSZone) {
        List<Object> productFiles = null;
        String targetIrodsFileAbsolutePath = "C:\\Users\\Bogdan\\Documents\\";
        try {
            //if (IRODSZone.getName() == "tempZone")
            List<CollectionAndDataObjectListingEntry> files = service.getFilesAndCollectionsUnderParentCollection(IRODSZone.path);
            //if (IRODSZone.getName() != "tempZone")
            //    files = service.getFilesAndCollectionsUnderParentCollection("/tempZone/home/rods/" + IRODSZone.getName());
            productFiles = new ArrayList<Object>(files.size());
            for (CollectionAndDataObjectListingEntry entry: files) {
                //JOptionPane.showMessageDialog(null, "no");
                IRODSFile f = service.getIRODSFileForPath(entry.getFormattedAbsolutePath());
                //service.sourceFiles.add((File) f);
                //service.getFile((File) f, "C:\\Users\\Bogdan\\Documents\\");
                if (f.isDirectory()) {
                    IRODSZone tmp = new IRODSZone(f.getName());
                    tmp.path = IRODSZone.path + "/" + tmp.getName();
                    productFiles.add(tmp);
                }
                else
                    productFiles.add(new ProductFile(f.getName(), (File) f));
                //break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        IRODSZone.productFiles = productFiles;
        return productFiles;
    }

    @Get
    public InputStream getFile(ProductFile file) throws IOException {
        String targetIrodsFileAbsolutePath = "C:\\Users\\Bogdan\\Documents\\";
        try {
            service.getFile((File) file.file, "C:\\Users\\Bogdan\\Documents\\");
        }
        catch (Throwable t) {
            t.printStackTrace();
            System.out.println(t);
        }
        return FileUtils.openInputStream(new File(targetIrodsFileAbsolutePath + file.getName()));
    }

    private HashSet<String> files = new HashSet<String>();

    @PutChild
    public ProductFile upload(IRODSZone product, String newName, byte[] bytes){
        File file = new File("C:\\Users\\Bogdan\\Documents\\tmp\\" + newName);
        ProductFile pf = new ProductFile(newName, file);
        if (bytes == null || bytes.length == 0 || files.contains(newName)) {
            return pf;
        }
        files.add(newName);

        try {
            file.createNewFile();
        }
        catch (IOException ex) {
        }

        try {
            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
            fos.write(bytes);
            fos.close();
        }
        catch (Exception e) {
        }

        service.putFile(new UploadDataObj(file), product.path);
        product.getProductFiles().add(pf);
        return pf;
    }


    @PutChild
    public ProductFile upload(ProductFile pf, byte[] bytes) {
        byte[] test = bytes;
        try {
            FileOutputStream fos = new FileOutputStream(pf.getFile().getAbsolutePath());
            fos.write(bytes);
            fos.close();
        }
        catch (Exception e) {
        }
        UploadDataObj obj = new UploadDataObj(pf.getFile());
        //service.putFile(obj);
        return pf;
    }


    @MakeCollection
    public IRODSZone createFolder(IRODSZone zone, String newName) {
        IRODSZone newZone = new IRODSZone(newName);
        newZone.path = zone.path + "/" + newName;
        zone.productFiles.add(newZone);
        try {
            service.createNewFolder(newZone.path);
        }
        catch (Exception ex) {
        }
        return newZone;
    }

    @ContentLength
    public Long getContentLength(ProductFile file) {
        return file.getFile().length();
    }

    @Delete
    public void pretendToDeleteImagesFolder(ProductFile file) {
        try {
            service.deleteFileOrFolderNoForce(file.getFile().getPath());
        }
        catch (Exception ex){

        }
    }
    @Delete
    public void pretendToDeleteImagesFolder(IRODSZone zone) {
        try {
            service.deleteFileOrFolderNoForce(zone.path);
        }
        catch (Exception ex){

        }
    }


    public class IRODSZone {
        private String name;

        public List<Object> productFiles = new ArrayList<Object>();

        public IRODSZone(String name) {
            this.name = name;
        }

        public String path;

        public String getName() {
            return name;
        }

        public List<Object> getProductFiles() {
            return productFiles;
        }
    }

    public class ProductFile {
        private String name;
        private File file;

        public ProductFile(String name, File file) {
            this.name = name;
            this.file = file;
        }

        public String getName() {
            return name;
        }

        public File getFile() {
            return file;
        }
    }
}