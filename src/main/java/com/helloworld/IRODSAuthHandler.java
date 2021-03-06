
package com.helloworld;

import io.milton.http.Auth;
import io.milton.http.Auth.Scheme;
import io.milton.http.AuthenticationHandler;
import io.milton.http.Request;
import io.milton.resource.Resource;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;

/**
 *
 * @author brad
 */
public class IRODSAuthHandler implements AuthenticationHandler {

    private boolean first = true;

    @Override
    public boolean credentialsPresent(Request request) {
        return request.getAuthorization() != null;
    }

    @Override
    public boolean supports( Resource r, Request request ) {
        Auth auth = request.getAuthorization();
        if( auth == null ) {

            return false;
        }
        boolean b = auth.getScheme().equals( Scheme.BASIC );
        return b;
    }

    @Override
    public Object authenticate( Resource resource, Request request ) {
        Auth auth = request.getAuthorization();
        try {
            IRODSAccount irodsAccount = new IRODSAccount("192.168.1.199",1247, auth.getUser(), auth.getPassword(), "", "tempZone", "");
            IRODSFileSystem irodsFileSystem = new IRODSFileSystem();
            DataTransferOperations dataTransferOps = irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(irodsAccount);
            FileService fs = FileService.getInstance();
            if (first) {
                fs.setInitialFolders(irodsAccount.getZone());
                fs.setAccount(irodsAccount);
                fs.setDataTransferOps(dataTransferOps);
                fs.setIRODSFileSystem(irodsFileSystem);
            }
            first = false;
            return "ok";
        }
        catch (Exception ex) {
            System.out.print("Exception");
        }
        return resource.authenticate("1","1");
    }

    @Override
    public void appendChallenges( Resource resource, Request request, List<String> challenges ) {
        if( resource == null ) {
            throw new RuntimeException("Can't generate challenge because resource is null, so can't get realm");
        }
        challenges.add("Basic realm=\"" + resource.getRealm() + "\"");
    }

    @Override
    public boolean isCompatible( Resource resource, Request request ) {
        return true;
    }
}