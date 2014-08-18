package com.helloworld;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.apache.commons.lang.ObjectUtils;
import org.irods.jargon.core.connection.*;
import org.irods.jargon.core.exception.DataNotFoundException;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.exception.JargonFileOrCollAlreadyExistsException;
import org.irods.jargon.core.exception.OverwriteException;
import org.irods.jargon.core.packinstr.Tag;
import org.irods.jargon.core.packinstr.TransferOptions;
import org.irods.jargon.core.pub.*;
import org.irods.jargon.core.pub.domain.Collection;
import org.irods.jargon.core.pub.domain.Resource;
import org.irods.jargon.core.pub.io.IRODSFile;
import org.irods.jargon.core.pub.io.IRODSFileFactory;
import org.irods.jargon.core.query.AVUQueryElement;
import org.irods.jargon.core.query.AVUQueryOperatorEnum;
import org.irods.jargon.core.query.CollectionAndDataObjectListingEntry;
import org.irods.jargon.core.query.JargonQueryException;
import org.irods.jargon.core.query.MetaDataAndDomainData;
import org.irods.jargon.core.query.RodsGenQueryEnum;
import org.irods.jargon.core.rule.IRODSRuleExecResult;
import org.irods.jargon.core.transfer.TransferControlBlock;
import org.irods.jargon.core.transfer.TransferStatus;
import org.irods.jargon.core.transfer.TransferStatusCallbackListener;
import org.slf4j.LoggerFactory;

import java.io.File;
import javax.swing.JOptionPane;
/**
 * Created by Bogdan on 28.07.2014.
 */
public class FileService {

    public FileService()
    {

        irodsAccount = new IRODSAccount("192.168.6.128",1247,"rods","rods","","tempZone","demoResc");

        try {
            irodsFileSystem = new IRODSFileSystem();
        }
        catch (JargonException ex) {
        }
        try {
            this.dataTransferOps = irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(irodsAccount);
        }
        catch (JargonException ex) {
        }

    }

    private IRODSAccount irodsAccount;

    private IRODSFileSystem irodsFileSystem;

    private TransferControlBlock transferControlBlock;

    private DataTransferOperations dataTransferOps = null;

    public IRODSFile getIRODSFileForPath(String irodsFilePath) throws Exception {
        if (irodsFilePath == null || irodsFilePath.isEmpty()) {
            throw new Exception("null or empty irodsFilePath");
        }
        try {
            return irodsFileSystem.getIRODSFileFactory(irodsAccount).instanceIRODSFile(irodsFilePath);
        }
        catch (JargonException ex) {
            throw new Exception("aaa");
        }
        finally {
            irodsFileSystem.close(irodsAccount);
        }
    }

    public void putFile(UploadDataObj uploadData) {
        // this is just a regular local file or folder
        //String localSourceAbsolutePath = transferFile.getAbsolutePath();
        //JOptionPane.showMessageDialog(null, "hui");
        String localSourceAbsolutePath = uploadData.getFile().getAbsolutePath();

        String sourceResource = irodsAccount.getDefaultStorageResource();

        // need to create new Transfer Control Block for each transfer since it needs to be reset
        // on how many files there are to transfer and how many have been transferred so far
        try {
            this.transferControlBlock = irodsFileSystem.getIRODSAccessObjectFactory().buildDefaultTransferControlBlockBasedOnJargonProperties();
            transferControlBlock.getTransferOptions().setIntraFileStatusCallbacks(true);
            //idropGui.getiDropCore().setTransferControlBlock(transferControlBlock);
        } catch (JargonException ex) {
            //java.util.logging.Logger.getLogger(LocalFileTree.class.getName()).log(
            //java.util.logging.Level.SEVERE, null, ex);
            //idropGui.showIdropException(ex);
        }
        try {
            //System.out.print(sourceResource);
            dataTransferOps.putOperation(localSourceAbsolutePath,
                    "/tempZone/home/rods/" + uploadData.getFile().getName(), sourceResource, new TransferStatusCallbackListener() {
                        @Override
                        public FileStatusCallbackResponse statusCallback(TransferStatus transferStatus) throws JargonException {
                            return null;
                        }

                        @Override
                        public void overallStatusCallback(TransferStatus transferStatus) throws JargonException {

                        }

                        @Override
                        public CallbackResponse transferAsksWhetherToForceOperation(String s, boolean b) {
                            return null;
                        }
                    }, transferControlBlock);

        } catch (JargonException ex) {
            //java.util.logging.Logger.getLogger(LocalFileTree.class.getName()).log(
            // java.util.logging.Level.SEVERE, null, ex);
            //idropGui.showIdropException(ex);
            System.out.print("gg");

        } finally {
            irodsFileSystem.closeAndEatExceptions();
        }
    }

    public List<CollectionAndDataObjectListingEntry> getFilesAndCollectionsUnderParentCollection(
            final String parentCollectionAbsolutePath) throws Exception {

        if (parentCollectionAbsolutePath == null || parentCollectionAbsolutePath.isEmpty()) {
            throw new Exception("null parentCollectionAbsolutePath");
        }

        try {
            CollectionAndDataObjectListAndSearchAO collectionAO = irodsFileSystem.getIRODSAccessObjectFactory()
                    .getCollectionAndDataObjectListAndSearchAO(irodsAccount);
            return collectionAO.listDataObjectsAndCollectionsUnderPath(parentCollectionAbsolutePath);
        } catch (JargonException ex) {
            //Logger.getLogger(IRODSFileService.class.getName()).log(Level.SEVERE, null, ex);
            throw new Exception("exception getting collections under: {}" + parentCollectionAbsolutePath, ex);
        } finally {
            try {
                irodsFileSystem.close(irodsAccount);
            } catch (JargonException ex) {
                //Logger.getLogger(IRODSFileService.class.getName()).log(Level.SEVERE, null, ex);
            }
        }

    }

    public List<File> sourceFiles = new ArrayList<File>();

    public void getFile() {
        String targetIrodsFileAbsolutePath = "C:\\Users\\Bogdan\\Documents\\";
        for (File transferFile : sourceFiles) {
            getFile(transferFile, targetIrodsFileAbsolutePath);
        }

    }

    TransferStatusCallbackListener listener = new TransferStatusCallbackListener() {
        @Override
        public FileStatusCallbackResponse statusCallback(TransferStatus transferStatus) throws JargonException {
            return null;
        }

        @Override
        public void overallStatusCallback(TransferStatus transferStatus) throws JargonException {

        }

        @Override
        public CallbackResponse transferAsksWhetherToForceOperation(String s, boolean b) {
            return null;
        }
    };

    void getFile(File transferFile, String targetIrodsFileAbsolutePath) {
        // need to create new Transfer Control Block for each transfer since it needs to be reset
        // on how many files there are to transfer and how many have been transferred so far
        try {
            this.transferControlBlock = irodsFileSystem.getIRODSAccessObjectFactory().buildDefaultTransferControlBlockBasedOnJargonProperties();
            transferControlBlock.getTransferOptions().setIntraFileStatusCallbacks(true);
        } catch (JargonException ex) {
            int a = 5;
            //java.util.logging.Logger.getLogger(LocalFileTree.class.getName()).log(
            //java.util.logging.Level.SEVERE, null, ex);
        }
        if (transferFile instanceof IRODSFile) {
            /*log.info("initiating a transfer of iRODS file:{}", transferFile.getAbsolutePath());
            log.info("transfer to local file:{}", targetIrodsFileAbsolutePath);*/
            try {
                DataTransferOperations dto = irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(
                        irodsAccount);
                String s = transferFile.getAbsolutePath();
                String s1 = targetIrodsFileAbsolutePath;
                dto.getOperation(transferFile.getAbsolutePath(), targetIrodsFileAbsolutePath, irodsAccount.getDefaultStorageResource(),
                    listener, this.transferControlBlock);
            } catch (Throwable ex) {
                int a = 56;
            }
            finally {
                irodsFileSystem.closeAndEatExceptions();
            }
        } else {
            //log.info("process a local to local move with source...not yet implemented : {}",
            transferFile.getAbsolutePath();
        }
    }
}


