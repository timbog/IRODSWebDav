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
import java.text.ParseException;
import java.util.*;

import org.apache.commons.io.FileUtils;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import java.text.SimpleDateFormat;

@ResourceController
public class HelloWorldController  {

    private FileService service = FileService.getInstance();
    private List<Folder> folders = new ArrayList<Folder>();
    private HashSet<String> files = new HashSet<String>();

    public HelloWorldController() {
        service.setController(this);
    }

    public void setInitialFolders(String zoneDirName)
    {
        Folder zoneDir = new Folder(zoneDirName);
        zoneDir.setPath("/" + zoneDirName);
        try {
            folders.add(zoneDir);
        } catch (Exception e) {
            System.out.print("");
        }
    }

    @Root
    public HelloWorldController getRoot() {
        return this;
    }

    @ChildrenOf
    public List<Folder> getProducts(HelloWorldController root) {
        return folders;
    }

    @ChildrenOf
    public List<Object> getProductFiles(Folder folder) {
        List<Object> productFiles = null;
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        Date now = new Date();
        if (folder.getDownloadedTime() == null)
            folder.setDownloadedTime(now);
        if ((((folder.getDownloadedTime().getTime() - now.getTime()) / (60 * 1000) % 60) < 10) &&
        (folder.getProductFiles().size() != 0))
            return folder.getProductFiles();
        try {
            List<CollectionAndDataObjectListingEntry> files = service.getFilesAndCollectionsUnderParentCollection(folder.getPath());
            productFiles = new ArrayList<Object>(files.size());
            for (CollectionAndDataObjectListingEntry entry: files) {
                IRODSFile f = service.getIRODSFileForPath(entry.getFormattedAbsolutePath());
                if (f.isDirectory()) {
                    Folder tmp = new Folder(f.getName());
                    tmp.setModified(f.lastModified());
                    tmp.setPath(folder.getPath() + "/" + tmp.getName());
                    productFiles.add(tmp);
                }
                else
                    productFiles.add(new ProductFile(f.getName(), (File) f));
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        folder.setProductFiles(productFiles);
        return productFiles;
    }

    @Get
    public InputStream getFile(ProductFile file) throws IOException {
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        try {
            service.getFile((File) file.getFile(), targetIrodsFileAbsolutePath);
        }
        catch (Throwable t) {
            t.printStackTrace();
            System.out.println(t);
        }
        return FileUtils.openInputStream(new File(targetIrodsFileAbsolutePath + file.getName()));
    }

    @PutChild
    public ProductFile upload(Folder product, String newName, byte[] bytes){
        File file = new File(System.getProperty("java.io.tmpdir") + newName);
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

        service.putFile(new UploadDataObj(file), product.getPath());
        product.getProductFiles().add(pf);
        return pf;
    }

    @MakeCollection
    public Folder createFolder(Folder zone, String newName) {
        Folder newZone = new Folder(newName);
        newZone.setPath(zone.getPath() + "/" + newName);
        zone.getProductFiles().add(newZone);
        try {
            service.createNewFolder(newZone.getPath());
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
    public void pretendToDeleteImagesFolder(Folder zone) {
        try {
            service.deleteFileOrFolderNoForce(zone.getPath());
        }
        catch (Exception ex){

        }
    }

    @Move
    public void move(ProductFile pf, Folder newZone, String newName) {
        String str = pf.getFile().getParent();
        if (str.equals(newZone.getPath())) {
            try {
                service.renameIRODSFileOrDirectory(str +  '/' + pf.getName(), newName);
            }
            catch (Exception ex) {
            }
            return;
        }
        newZone.getProductFiles().add(pf);
        try {
            service.moveIRODSFileUnderneathNewParent(pf.getFile().getAbsolutePath(), newZone.getPath() + "/" + newName);
        }
        catch (Exception ex) {
        }
    }

    @Move
    public void move(Folder zn, Folder newZone, String newName) {

        String str = zn.getPath();
        int temp = 0;
        for (int i = 0; i < str.length(); i++) {
            if (str.charAt(str.length() - 1 - i) == '/') {
                temp = str.length() - 1 - i;
                break;
            }
        }
        String parentPath = str.substring(0, temp);
        if (parentPath.equals(newZone.getPath())) {
            try {
                service.renameIRODSFileOrDirectory(zn.getPath(), newName);
            }
            catch (Exception ex) {
            }
            return;
        }
        try {
            service.createNewFolder(newZone.getPath() + "/" + newName);
            moveFiles(zn, newZone.getPath() + "/" + newName);
            service.deleteFileOrFolderNoForce(zn.getPath());
        }
        catch (Exception ex) {
        }
    }

    @ModifiedDate
    public Date getModifiedDate(ProductFile pf) {
       SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        try {
            return sdf.parse(sdf.format(pf.getFile().lastModified()));
        }
        catch (ParseException ex) {
        }
        return null;
    }

    @ModifiedDate
    public Date getModifiedDate(Folder zn) {
        SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        try {
            return sdf.parse(sdf.format(zn.getModified()));
        }
        catch (ParseException ex) {
        }
        return null;
    }

    private void moveFiles(Folder oldZone, String newZonePath) throws Exception
    {
        List<Object> list = this.getProductFiles(oldZone);
        for (Object entry:list) {
            String str = entry.getClass().getName();
            if (entry.getClass().getName() == "com.helloworld.HelloWorldController$ProductFile") {
                ProductFile pf = (ProductFile) entry;
                this.getFile(pf);
                service.putFile(new UploadDataObj(new File(System.getProperty("java.io.tmpdir") + pf.getFile().getName())), newZonePath);
            }
            if (entry.getClass().getName() == "com.helloworld.HelloWorldController$Folder") {
                Folder zn = (Folder) entry;
                try {
                    service.createNewFolder(newZonePath + "/" + zn.getName());
                }
                catch (Exception ex) {
                }
                moveFiles(zn, newZonePath + "/" + zn.getName());
            }
        }
    }


    /*public List<Object> getChildren (IRODSZone zn) {
        List<Object> productFiles = null;
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
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
                    tmp.modified = f.lastModified();
                    tmp.path = zn.path + "/" + tmp.getName();
                    productFiles.add(tmp);
                }
                else
                    productFiles.add(new ProductFile(f.getName(), (File) f));
                //break;
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
        zn.productFiles = productFiles;
        return productFiles;
    }*/

}