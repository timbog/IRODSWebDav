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
import java.lang.Object;

@ResourceController
public class HelloWorldController {

    private FileService service = FileService.getInstance();
    private List<Folder> folders = new ArrayList<Folder>();
    private HashSet<String> files = new HashSet<String>();
    private Folder temporaryFolder = new Folder("");
    private Folder zoneDir = new Folder("");

    public HelloWorldController() {
        service.setController(this);
    }

    public void setInitialFolders(String zoneDirName)
    {
        zoneDir = new Folder(zoneDirName);
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
        this.temporaryFolder = folder;
        List<Object> productFiles = null;
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        Date now = new Date();
        if (folder.getDownloadedTime() == null)
            folder.setDownloadedTime(now);
        if ((((now.getTime() - folder.getDownloadedTime().getTime()) / (60 * 1000) % 60) < 10) &&
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
                else {
                    ProductFile newFile = new ProductFile(f.getName(), (File) f);
                    newFile.setIRODSPath(newFile.getFile().getPath());
                    productFiles.add(newFile);
                }
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
       folder.setProductFiles(productFiles);
       folder.setDownloadedTime(new Date());
        return productFiles;
    }

    @Get
    public InputStream getFile(ProductFile file) throws IOException {
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        ArrayList<String> ls = this.getFolderNames(file.getFile().getPath());
        targetIrodsFileAbsolutePath = this.makeDirectories(ls, targetIrodsFileAbsolutePath);
        try {
            /*GetTransferRunner runner = new GetTransferRunner(service, (File) file.getFile(), targetIrodsFileAbsolutePath);
            Thread getThread = new Thread(runner);
            getThread.start();*/
            service.getFile((File) file.getFile(), targetIrodsFileAbsolutePath);
        }
        catch (Throwable t) {
            t.printStackTrace();
            System.out.println(t);
        }
        return FileUtils.openInputStream(new File(targetIrodsFileAbsolutePath + "\\" + file.getName()));
    }

    @PutChild
    public ProductFile upload(Folder product, String newName, byte[] bytes){
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        ArrayList<String> ls = this.getFolderNames(product.getPath());
        targetIrodsFileAbsolutePath = this.makeDirectories(ls, targetIrodsFileAbsolutePath);
        File file = new File(targetIrodsFileAbsolutePath + "\\" + newName);

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
        PutTransferRunner runner = new PutTransferRunner(service, file, product);
        Thread putThread = new Thread(runner);
        putThread.start();
        product.getProductFiles().add(pf);
        pf.setIRODSPath(product.getPath() + "/" + newName);
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
            temporaryFolder.getProductFiles().remove(file);
        }
        catch (Exception ex){

        }
    }

    @Delete
    public void pretendToDeleteImagesFolder(Folder zone) {
        try {
            service.deleteFileOrFolderNoForce(zone.getPath());
            temporaryFolder.getProductFiles().remove(zone);
        }
        catch (Exception ex){

        }
    }

    @Move
    public void move(ProductFile pf, Folder newZone, String newName) {
        String str = pf.getFile().getParent();
        Folder parentFolder = this.getFolderForPath(pf.getIRODSPath());
        if (str.equals(newZone.getPath())) {
            try {
                service.renameIRODSFileOrDirectory(str +  '/' + pf.getName(), newName);
                parentFolder.setTimeToUpdate();
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
        newZone.setTimeToUpdate();
        parentFolder.getProductFiles().remove(pf);
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
        Folder parentZone = this.getFolderForPath(parentPath);
        if (parentPath.equals(newZone.getPath())) {
            try {
                service.renameIRODSFileOrDirectory(zn.getPath(), newName);
                parentZone.setTimeToUpdate();
            }
            catch (Exception ex) {
            }
            return;
        }
        //parentZone.getProductFiles().remove(zn);
        try {
            service.createNewFolder(newZone.getPath() + "/" + newName);
            moveFiles(zn, newZone.getPath() + "/" + newName);
            service.deleteFileOrFolderNoForce(zn.getPath());
        }
        catch (Exception ex) {
            System.out.print("fdfdf");
        }

        parentZone.setTimeToUpdate();
        newZone.setTimeToUpdate();
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

    private void moveFiles(Folder oldZone, String newZonePath) throws Exception {
        List<Object> list = this.getProductFiles(oldZone);
        for (Object entry:list) {
            String str = entry.getClass().getName();
            if (entry.getClass().getName() == "com.helloworld.ProductFile") {
                ProductFile pf = (ProductFile) entry;
                ArrayList<String> ls = this.getFolderNames(newZonePath);
                String path = this.makeDirectories(ls, System.getProperty("java.io.tmpdir"));
                service.getFile(pf.getFile(), path);
                String se =  path + "/" + pf.getFile().getName();
                service.putFile(new UploadDataObj(new File(path + "/" + pf.getFile().getName())), newZonePath);
            }
            if (entry.getClass().getName() == "com.helloworld.Folder") {
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

    private ArrayList<String> getFolderNames (String path) {
        ArrayList<String> list = new ArrayList<String>();
        String temp = "";
        for (int i = 0; i < path.length(); i++) {
            if (path.charAt(i) != '/')
                temp = temp + path.charAt(i);
            else {
                if (temp.length() != 0)
                    list.add(temp);
                temp = "";
            }
        }
        return list;
    }

    private String makeDirectories(ArrayList<String> nameArray, String basePath)
    {
        for (int i = 0; i < nameArray.size(); i++) {
            if (i == 0) {
                boolean bool = new File(basePath + nameArray.get(i)).mkdirs();
                basePath = basePath + nameArray.get(i);
            }
            else {
                boolean bool = new File(basePath + "\\" + nameArray.get(i)).mkdirs();
                basePath = basePath + "\\" + nameArray.get(i);
            }
        }
        return basePath;
    }

    private Folder getFolderForPath (String path) {
        String temp = "";
        Folder folder = zoneDir;
        for (int i = 0; i < path.length(); i++) {
            String str = path.substring(i,i + 1);
            boolean x = (path.substring(i,i + 1).equals( "\\"));
            if ((path.charAt(i) != '/') && (!path.substring(i,i + 1).equals( "\\")))
                temp = temp + path.charAt(i);
            else if ((!temp.equals(zoneDir.getName()) && (!temp.equals("")))) {
                for (int j = 0; j < folder.getProductFiles().size(); j++) {
                    if (folder.getProductFiles().get(j).getClass().getName().equals("com.helloworld.Folder")) {
                        Folder tempFold = (Folder) folder.getProductFiles().get(j);
                        if (tempFold.getName().equals(temp)) {
                            folder = tempFold;
                            //break;
                        }
                    }
                }
                temp = "";
            }
           else if (temp.equals(zoneDir.getName()))
               temp = "";
        }
        return folder;
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