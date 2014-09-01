package com.helloworld;

import io.milton.http.Auth;
import io.milton.http.AuthenticationHandler;
import io.milton.http.Request;
import io.milton.resource.Resource;
import org.irods.jargon.core.connection.IRODSAccount;
import org.irods.jargon.core.exception.JargonException;
import org.irods.jargon.core.pub.DataTransferOperations;
import org.irods.jargon.core.pub.IRODSFileSystem;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.List;

/**
 * Created by Bogdan on 27.08.2014.
 */
public class IRODSAuthHandler implements AuthenticationHandler {
    private static final Logger log = LoggerFactory.getLogger(IRODSAuthHandler.class);

    @Override
    public boolean credentialsPresent(Request request) {
        return request.getAuthorization() != null;
    }

    @Override
    public boolean supports( Resource r, Request request ) {
        Auth auth = request.getAuthorization();
        return true;
        /*if( auth == null ) {
            log.trace("supports: no credentials provided");
            return false;
        }
        log.trace( "supports: {}", auth.getScheme() );
        boolean b = auth.getScheme().equals( Auth.Scheme.BASIC );
        if( b ) {
            log.trace("supports: is BASIC auth scheme, supports = true");
        } else {
            log.trace("supports: is BASIC auth scheme, supports = false");
        }
        return b;*/
    }

    @Override
    public Object authenticate( Resource resource, Request request ) {
        log.trace( "authenticate" );
        Auth auth = request.getAuthorization();
        Object o = resource.authenticate( auth.getUser(), auth.getPassword() );
        /*IRODSAccount irodsAccount = new IRODSAccount("192.168.6.129", 1247, auth.getUser(), auth.getPassword(), "", "tempZone", "demoResc");
        try {
            IRODSFileSystem irodsFileSystem = new IRODSFileSystem();
            DataTransferOperations dataTransferOps = irodsFileSystem.getIRODSAccessObjectFactory().getDataTransferOperations(irodsAccount);
            o = resource.authenticate( "user", "password" );
        }
        catch (JargonException ex) {
        }
        log.trace( "result: {}", o );*/
        return o;
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