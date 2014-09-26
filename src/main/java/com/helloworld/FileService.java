package com.helloworld;

import java.io.File;
import java.util.ArrayList;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

//import org.apache.commons.lang.ObjectUtils;
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

    private static FileService instance;
    private HelloWorldController controller;
    private IRODSAccount irodsAccount;
    private IRODSFileSystem irodsFileSystem;
    private TransferControlBlock transferControlBlock;
    private DataTransferOperations dataTransferOps;
    private TransferStatusCallbackListener listener = new TransferStatusCallbackListener() {
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

    private FileService() {}

    private List<File> sourceFiles = new ArrayList<File>();

    public void setAccount (IRODSAccount acc) {
        irodsAccount = acc;
    }

    public void setDataTransferOps(DataTransferOperations opers) {
        dataTransferOps = opers;
    }

    public void setIRODSFileSystem(IRODSFileSystem sys) {
        irodsFileSystem = sys;
    }

    public void setController(HelloWorldController con) {
        controller = con;
    }

    public IRODSAccount getAccount() {return irodsAccount;}

    public void setInitialFolders(String zoneDirName)
    {
        controller.setInitialFolders(zoneDirName);
    }

    public static FileService getInstance()
    {
        if (instance == null)
            instance = new FileService();
        return instance;
    }

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

    public void putFile(UploadDataObj uploadData, String path) {
        String localSourceAbsolutePath = uploadData.getFile().getAbsolutePath();
        String sourceResource = irodsAccount.getDefaultStorageResource();
        try {
            this.transferControlBlock = irodsFileSystem.getIRODSAccessObjectFactory().buildDefaultTransferControlBlockBasedOnJargonProperties();
            transferControlBlock.getTransferOptions().setIntraFileStatusCallbacks(true);
        } catch (JargonException ex) {
        }
        try {
            String s =  path + '/' + uploadData.getFile().getName();
            String s2 = uploadData.getFile().getName();
            dataTransferOps.putOperation(localSourceAbsolutePath,
                    path + "/" + uploadData.getFile().getName(), sourceResource, new TransferStatusCallbackListener() {
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
                    }
                    , transferControlBlock);

        } catch (Exception ex) {
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
            throw new Exception("exception getting collections under: {}" + parentCollectionAbsolutePath, ex);
        } finally {
            try {
                irodsFileSystem.close(irodsAccount);
            } catch (JargonException ex) {
            }
        }

    }

    public void getFile() {
        String targetIrodsFileAbsolutePath = System.getProperty("java.io.tmpdir");
        for (File transferFile : sourceFiles) {
            getFile(transferFile, targetIrodsFileAbsolutePath);
        }
    }

    public void getFile(File transferFile, String targetIrodsFileAbsolutePath) {
        try {
            this.transferControlBlock = irodsFileSystem.getIRODSAccessObjectFactory().buildDefaultTransferControlBlockBasedOnJargonProperties();
            transferControlBlock.getTransferOptions().setIntraFileStatusCallbacks(true);
        } catch (JargonException ex) {
        }
        if (transferFile instanceof IRODSFile) {
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
            transferFile.getAbsolutePath();
        }
    }

    public boolean createNewFolder(final String newFolderAbsolutePath) throws Exception {

        if (newFolderAbsolutePath == null || newFolderAbsolutePath.isEmpty()) {
            throw new Exception("null or empty newFolderAbsolutePath");
        }

        boolean createSuccessful = false;

        try {
            IRODSFile newDirectory = irodsFileSystem.getIRODSFileFactory(irodsAccount).instanceIRODSFile(
                    newFolderAbsolutePath);
            createSuccessful = newDirectory.mkdirs();
        } catch (JargonException ex) {
            throw new Exception("exception creating new dir", ex);
        } finally {
            try {
                irodsFileSystem.close(irodsAccount);
            } catch (JargonException ex) {
            }
        }
        return createSuccessful;
    }

    public void deleteFileOrFolderNoForce(final String deleteFileAbsolutePath) throws Exception {
        if (deleteFileAbsolutePath == null || deleteFileAbsolutePath.isEmpty()) {
            throw new Exception("null or empty deleteFileAbsolutePath");
        }
        try {
            IRODSFile deleteFileOrDir = irodsFileSystem.getIRODSFileFactory(irodsAccount).instanceIRODSFile(
                    deleteFileAbsolutePath);
            deleteFileOrDir.delete();
        } catch (JargonException ex) {
            throw new Exception("exception deleting dir:" + deleteFileAbsolutePath, ex);
        } finally {
            try {
                irodsFileSystem.close(irodsAccount);
            } catch (JargonException ex) {
            }
        }
    }

    public void moveIRODSFileUnderneathNewParent(final String currentAbsolutePath, final String newAbsolutePath)
            throws Exception {
        if (currentAbsolutePath == null || currentAbsolutePath.isEmpty()) {
            throw new Exception("null or empty currentAbsolutePath");
        }
        if (newAbsolutePath == null || newAbsolutePath.isEmpty()) {
            throw new Exception("null or empty newAbsolutePath");
        }
        try {
            DataTransferOperations dataTransferOperations = irodsFileSystem.getIRODSAccessObjectFactory()
                    .getDataTransferOperations(irodsAccount);
            dataTransferOperations.move(currentAbsolutePath, newAbsolutePath);
        } catch (JargonException ex) {
            throw new Exception("exception moving file", ex);
        } finally {
            try {
                irodsFileSystem.close(irodsAccount);
            } catch (JargonException ex) {
            }
        }
    }

    public String renameIRODSFileOrDirectory(final String irodsCurrentAbsolutePath, final String newFileOrCollectionName)
            throws Exception {

        if (irodsCurrentAbsolutePath == null || irodsCurrentAbsolutePath.isEmpty()) {
            throw new Exception("null or empty irodsCurrentAbsolutePath");
        }
        if (newFileOrCollectionName == null || newFileOrCollectionName.isEmpty()) {
            throw new Exception("null or empty newFileOrCollectionName");
        }
        String newPath = "";
        try {
            IRODSFileFactory irodsFileFactory = irodsFileSystem.getIRODSFileFactory(irodsAccount);
            IRODSFile sourceFile = irodsFileFactory.instanceIRODSFile(irodsCurrentAbsolutePath);
            StringBuilder newPathSb = new StringBuilder();
            newPathSb.append(sourceFile.getParent());
            newPathSb.append("/");
            newPathSb.append(newFileOrCollectionName);
            newPath = newPathSb.toString();
            DataTransferOperations dataTransferOperations = irodsFileSystem.getIRODSAccessObjectFactory()
                    .getDataTransferOperations(irodsAccount);
            dataTransferOperations.move(irodsCurrentAbsolutePath, newPath);
        } catch (JargonException ex) {
            throw new Exception("exception moving file", ex);
        } finally {
            try {
                irodsFileSystem.close(irodsAccount);
            } catch (JargonException ex) {
            }
        }
        return newPath;
    }
}



