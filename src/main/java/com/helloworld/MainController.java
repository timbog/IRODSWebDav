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
import org.apache.commons.io.input.NullInputStream;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.text.SimpleDateFormat;
import java.lang.Object;

@ResourceController
public class MainController {

    static Logger logger = LoggerFactory.getLogger(MainController.class);

    private FileService service = FileService.getInstance();
    private List<Folder> folders = new ArrayList<Folder>();
    private Folder temporaryFolder = new Folder("");
    private Folder zoneDir = new Folder("");
    private List<String> filesToPutLocalPaths = new ArrayList<String>();
    private List<String> filesToPutIRODSPaths = new ArrayList<String>();

    public MainController() {
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
        //GetChildrenRunner runner = new GetChildrenRunner(service, zoneDir);
        //Thread getChildrenThread = new Thread(runner);
        //getChildrenThread.start();
    }

    @Root
    public MainController getRoot() {
        return this;
    }

    @ChildrenOf
    public List<Folder> getProducts(MainController root) {
        return folders;
    }

    @ChildrenOf
    public List<Object> getProductFiles(Folder folder) {
        temporaryFolder = folder;
        if (filesToPutLocalPaths.size() != 0) {
            putEmptyFiles();
        }
        List<Object> productFiles = null;
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        Date now = new Date();
        if (folder.getDownloadedTime() == null)
            folder.setDownloadedTime(now);
        else if (folder.checkTime(now)) {
            return folder.getProductFiles();
        }
        try {
            List<CollectionAndDataObjectListingEntry> files = service.getFilesAndCollectionsUnderParentCollection(folder.getPath());
            productFiles = new ArrayList<Object>(files.size());
            for (CollectionAndDataObjectListingEntry entry: files) {
                if (entry.isCollection()) {
                    Folder tmp = new Folder(entry.getPathOrName().substring(entry.getPathOrName().lastIndexOf("/") + 1));
                    tmp.setModified(entry.getModifiedAt().getTime());
                    tmp.setPath(folder.getPath() + "/" + tmp.getName());
                    productFiles.add(tmp);
                }
                else {
                    ProductFile newFile = new ProductFile(entry.getPathOrName().substring(entry.getPathOrName().lastIndexOf("/") + 1), entry.getFormattedAbsolutePath());
                    newFile.setLength(entry.getDataSize());
                    newFile.setLastModified(entry.getModifiedAt());
                    //newFile.setIRODSPath(newFile.getFile().getPath());
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
        if (filesToPutLocalPaths.size() != 0) {
            putEmptyFiles();
        }
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        ArrayList<String> ls = getFolderNames(file.getIRODSPath().substring(file.getIRODSPath().lastIndexOf(getSlashForTemporaryOS()) + 1));
        targetIrodsFileAbsolutePath = makeDirectories(ls, targetIrodsFileAbsolutePath);
        File targetFile = new File(targetIrodsFileAbsolutePath, file.getName());
        if (targetFile.exists() && !targetFile.delete()) {
            logger.error("Cannot delete file {}", targetFile);
            return new NullInputStream(0);
        }
        try {
            service.getFile(file.getIRODSPath(), targetIrodsFileAbsolutePath);
        }
        catch (Throwable t) {
            t.printStackTrace();
            logger.error("Cannot retrieve file {}; \n---  {}", targetFile, t.getMessage());
        }
        return FileUtils.openInputStream(targetFile);
    }

    @PutChild
    public ProductFile upload(Folder product, String newName, byte[] bytes){
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        ArrayList<String> ls = getFolderNames(product.getPath());
        targetIrodsFileAbsolutePath = makeDirectories(ls, targetIrodsFileAbsolutePath);
        File file = new File(targetIrodsFileAbsolutePath + getSlashForTemporaryOS() + newName);
        ProductFile pf = new ProductFile(newName, product.getPath() + getSlashForTemporaryOS() + newName);
        pf.setLastModified(new Date());
        pf.setLength(bytes.length);
        try {
            file.createNewFile();
        }
        catch (IOException ex) {
            int a =56;
        }
        if ((bytes == null || bytes.length == 0) && (!filesToPutLocalPaths.contains(targetIrodsFileAbsolutePath))) {
            filesToPutIRODSPaths.add(product.getPath());
            filesToPutLocalPaths.add(targetIrodsFileAbsolutePath + getSlashForTemporaryOS() + newName);
            product.getProductFiles().add(pf);
            return pf;
        }
        filesToPutLocalPaths.remove(targetIrodsFileAbsolutePath + getSlashForTemporaryOS() + newName);
        filesToPutIRODSPaths.remove(product.getPath());
        try {
            FileOutputStream fos = new FileOutputStream(file.getAbsolutePath());
            fos.write(bytes);
            fos.close();
        }
        catch (Exception e) {
        }
        PutTransferRunner runner = new PutTransferRunner(service, targetIrodsFileAbsolutePath + "/" + newName, product);
        Thread putThread = new Thread(runner);
        putThread.start();
        product.getProductFiles().remove(pf);
        pf.setLength(bytes.length);
        product.getProductFiles().add(pf);
        pf.setIRODSPath(product.getPath() + getSlashForTemporaryOS() + newName);
        return pf;
    }

    /*@PutChild
    public ProductFile upload(ProductFile pf, byte[] bytes) {
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        ArrayList<String> ls = getFolderNames(pf.getIRODSPath());
        targetIrodsFileAbsolutePath = makeDirectories(ls, targetIrodsFileAbsolutePath);
        byte[] test = bytes;
        try {
            FileOutputStream fos = new FileOutputStream(pf.getIRODSPath());
            fos.write(bytes);
            fos.close();
        }
        catch (Exception e) {
        }
        String s = pf.getIRODSPath().substring(0, pf.getIRODSPath().lastIndexOf("/"));
        Folder product = getFolderForPath(pf.getIRODSPath().substring(0, pf.getIRODSPath().lastIndexOf("/")));
        PutTransferRunner runner = new PutTransferRunner(service, targetIrodsFileAbsolutePath + "/" + pf.getName(), product);
        Thread putThread = new Thread(runner);
        putThread.start();
        pf.setLength(bytes.length);

        product.getProductFiles().add(pf);
        return pf;
    }*/

    @MakeCollection
    public Folder createFolder(Folder zone, String newName) {
        Folder newZone = new Folder(newName);
        newZone.setPath(zone.getPath() + "/" + newName);
        newZone.setDownloadedTime(new Date());
        zone.getProductFiles().add(newZone);
        try {
            service.createNewFolder(newZone.getPath());
        }
        catch (Exception ex) {
        }
        //zone.setTimeToUpdate();
        //getProductFiles(zone);
        return newZone;
    }

    @ContentLength
    public Long getContentLength(ProductFile file) {
        return file.getLength();
    }

    @Delete
    public void pretendToDeleteImagesFolder(ProductFile file) {
        try {
            service.deleteFileOrFolderNoForce(file.getIRODSPath());
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

    /*@Delete
    public void pretendToDeleteImagesFolder(ProductFile file) {
        DeleteTransferRunner runner = new DeleteTransferRunner(service, file.getIRODSPath());
        Thread deleteThread = new Thread(runner);
        deleteThread.start();
//service.deleteFileOrFolderNoForce(file.getFile().getPath());
        temporaryFolder.getProductFiles().remove(file);
    }

    @Delete
    public void pretendToDeleteImagesFolder(Folder zone) {
        DeleteTransferRunner runner = new DeleteTransferRunner(service, zone.getPath());
        Thread deleteThread = new Thread(runner);
        deleteThread.start();
        temporaryFolder.getProductFiles().remove(zone);
        getProductFiles(temporaryFolder);
    }*/

    @Move
    public void move(ProductFile pf, Folder newZone, String newName) {
        Folder parentFolder = getFolderForPath(pf.getIRODSPath());
        if (parentFolder.getPath().equals(newZone.getPath())) {
            try {
                service.renameIRODSFileOrDirectory(pf.getIRODSPath(), newName);
                parentFolder.setTimeToUpdate();
            }
            catch (Exception ex) {
            }
            return;
        }
        newZone.getProductFiles().add(pf);
        try {
            service.moveIRODSFileUnderneathNewParent(pf.getIRODSPath(), newZone.getPath() + "/" + newName);
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
        String parentPath = str.substring(0, zn.getPath().lastIndexOf("/"));
        Folder parentZone = getFolderForPath(parentPath);
        if (parentPath.equals(newZone.getPath())) {
            try {
                service.renameIRODSFileOrDirectory(zn.getPath(), newName);
                zn.setName(newName);
            }
            catch (Exception ex) {
            }
            return;
        }
        List<Object> l = parentZone.getProductFiles();
        parentZone.getProductFiles().remove(zn);
        try {
            service.createNewFolder(newZone.getPath() + "/" + newName);
            Folder newFold = new Folder(newName);
            newFold.setPath(newZone.getPath() + "/" + newName);
            newFold.setModified(zn.getModified());
            newFold.setDownloadedTime(new Date());
            newZone.getProductFiles().add(newFold);
            moveFiles(zn, newZone.getPath() + "/" + newName);
            service.deleteFileOrFolderNoForce(zn.getPath());
        }
        catch (Exception ex) {
            System.out.print("fdfdf");
        }
        newZone.setTimeToUpdate();
    }

    @ModifiedDate
    public Date getModifiedDate(ProductFile pf) {
       SimpleDateFormat sdf = new SimpleDateFormat("MM/dd/yyyy HH:mm:ss");
        try {
            return sdf.parse(sdf.format(pf.getLastModified()));
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
        List<Object> list = getProductFiles(oldZone);
        for (Object entry:list) {
            String str = entry.getClass().getName();
            if (entry.getClass().getName() == "com.helloworld.ProductFile") {
                ProductFile pf = (ProductFile) entry;
                ArrayList<String> ls = getFolderNames(newZonePath);
                String path = makeDirectories(ls, System.getProperty("java.io.tmpdir"));
                service.getFile(pf.getIRODSPath(), path);
                Folder fold = getFolderForPath(newZonePath);
                PutTransferRunner runner = new PutTransferRunner(service, path + getSlashForTemporaryOS() + pf.getName(), fold);
                Thread putThread = new Thread(runner);
                putThread.start();
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
                boolean bool = new File(basePath + getSlashForTemporaryOS() + nameArray.get(i)).mkdirs();
                basePath = basePath + getSlashForTemporaryOS() + nameArray.get(i);
            }
        }
        return basePath;
    }

    private Folder getFolderForPath (String path) {
        String temp = "";
        Folder folder = zoneDir;
        for (int i = 0; i < path.length(); i++) {
            String str = path.substring(i,i + 1);
            int y = path.length() - 1;
            if (path.charAt(i) != '/')
                temp = temp + path.charAt(i);
            if (((!temp.equals(zoneDir.getName()) && (!temp.equals("")) && (path.charAt(i) == '/'))) || (i == path.length() - 1)) {
                for (int j = 0; j < folder.getProductFiles().size(); j++) {
                    if (folder.getProductFiles().get(j).getClass().getName().equals("com.helloworld.Folder")) {
                        Folder tempFold = (Folder) folder.getProductFiles().get(j);
                        if (tempFold.getName().equals(temp)) {
                            folder = tempFold;
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

    private void putEmptyFiles() {
        List<Folder> parentFolders = new ArrayList<Folder>();
        for (int i = 0; i < filesToPutIRODSPaths.size(); i++) {
            parentFolders.add(getFolderForPath(filesToPutIRODSPaths.get(i)));
            Folder fold = getFolderForPath(filesToPutIRODSPaths.get(i));
            /*PutTransferRunner runner = new PutTransferRunner(service, filesToPutLocalPaths.get(i), fold);
            //boolean bl = new File(filesToPutLocalPaths.get(0)).exists();
            Thread putThread = new Thread(runner);
            putThread.start();*/
        }
        PutFilesControl control = new PutFilesControl(service, parentFolders, filesToPutLocalPaths);
        filesToPutIRODSPaths.clear();
        filesToPutLocalPaths.clear();
    }

    private String getSlashForTemporaryOS() {
        if (System.getProperty("java.io.tmpdir").contains("/")) {
            return "/";
        }
        else return "\\";
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
