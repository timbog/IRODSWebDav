
package com.helloworld;

import io.milton.common.Utils;
import io.milton.context.RootContext;
import io.milton.dns.utils.base64;
import io.milton.http.*;
import io.milton.http.annotated.AnnotationResourceFactory;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.http.fs.FileSystemResourceFactory;
import io.milton.http.fs.SimpleSecurityManager;
import io.milton.http.http11.auth.*;
import io.milton.principal.DiscretePrincipal;
import io.milton.resource.Resource;
import org.irods.jargon.core.connection.IRODSAccount;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

import io.milton.common.Utils;
import io.milton.dns.utils.base64;
import io.milton.http.*;
import io.milton.http.exceptions.BadRequestException;
import io.milton.http.exceptions.NotAuthorizedException;
import io.milton.principal.DiscretePrincipal;
import io.milton.resource.Resource;

import java.util.List;
import java.util.concurrent.ConcurrentHashMap;
import java.io.File;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * This wraps a bunch of other authentication handlers, so if any of those
 * successfully login the user then this will generate a cookie which can be
 * used subsequently.
 * <p/>
 * Note that it is usually not correct to wrap a Digest auth handler because
 * that would then defeat the purpose of digest authentication. However, this
 * can and should wrap Basic and Form authentication handlers
 *
 * @author brad
 */
public class IRODSAuthHandler implements AuthenticationHandler {

    private static final Logger log = LoggerFactory.getLogger(IRODSAuthHandler.class);
    private static final String HANDLER_ATT_NAME = "_delegatedAuthenticationHandler";
    public static final int SECONDS_PER_YEAR = 60 * 60 * 24 * 365;
    private final String requestParamLogout = "miltonLogout";
    private final String cookieUserUrlValue = "miltonUserUrl";
    private final String cookieUserUrlHash = "miltonUserUrlHash";
    private final List<AuthenticationHandler> handlers;
    private final ResourceFactory principalResourceFactory;
    private final NonceProvider nonceProvider;
    private String userUrlAttName = "userUrl";
    private boolean useLongLivedCookies = true;
    private final List<String> keys;
    protected io.milton.http.SecurityManager securityManager;
    protected Map<String, String> mapOfNameAndPasswords;
    protected String defaultUser = "user";
    protected String defaultPassword = "password";
    protected String fsRealm = "milton";
    private RootContext rootContext = new RootContext();

    public IRODSAuthHandler() {
        this.nonceProvider = new SimpleMemoryNonceProvider(86400, new ExpiredNonceRemover( new ConcurrentHashMap<UUID, Nonce>(),86400), new ConcurrentHashMap<UUID, Nonce>());
        this.handlers = new ArrayList<AuthenticationHandler>();
        handlers.add(new DigestAuthenticationHandler(nonceProvider));
        this.principalResourceFactory = new AnnotationResourceFactory();
        this.keys = new ArrayList<String>();
        this.keys.add("b78cda01-e1a7-4e5b-b3ce-a9f16a9e33e5");
    }

    protected io.milton.http.SecurityManager securityManager() {
        if (securityManager == null) {
            if (mapOfNameAndPasswords == null) {
                mapOfNameAndPasswords = new HashMap<String, String>();
                mapOfNameAndPasswords.put(defaultUser, defaultPassword);
                log.info("Configuring default user and password: " + defaultUser + "/" + defaultPassword + " for SimpleSecurityManager");
            }
            if (fsRealm == null) {
                fsRealm = "milton";
            }
            securityManager = new SimpleSecurityManager(fsRealm, mapOfNameAndPasswords);
        }
        log.info("Using securityManager: " + securityManager.getClass());
        rootContext.put(securityManager);
        return securityManager;
    }

    @Override
    public boolean credentialsPresent(Request request) {
        String userUrl = getUserUrlFromRequest(request);
        if (userUrl != null && userUrl.length() > 0) {
            return true;
        }
        for (AuthenticationHandler h : handlers) {
            if (h.credentialsPresent(request)) {
                return true;
            }
        }
        return false;
    }

    @Override
    public boolean supports(Resource r, Request request) {
// find the authId, if any, from the request
        String userUrl = getUserUrl(request);

// check for a logout command, if so logout
        if (isLogout(request)) {
            log.info("Is LogOut request, clear cookie");
            if (userUrl != null && userUrl.length() > 0) {
                clearCookieValue(HttpManager.response());
            }
        }

        for (AuthenticationHandler hnd : handlers) {
            if (hnd.supports(r, request)) {
                log.info("Found child handler who supports this request {}", hnd);
                request.getAttributes().put(HANDLER_ATT_NAME, hnd);
                return true;
            }
        }

// We will support it if there is either a auth id request param
        if (userUrl != null) {
            return true;
        } else {
            return false;
        }
    }

    @Override
    public Object authenticate(Resource resource, Request request) {
// If there is a delegating handler which supports the request then we MUST use it
// This would have been selected in the supports method
        /*
        AuthenticationHandler delegateHandler = (AuthenticationHandler) request.getAttributes().get(HANDLER_ATT_NAME);
        if (delegateHandler != null) {
            if (log.isTraceEnabled()) {
                log.trace("authenticate: use delegateHandler: " + delegateHandler);
            }
            Object tag =
                    delegateHandler.authenticate(resource, request);
            if (tag != null) {
                if (tag instanceof DiscretePrincipal) {
                    DiscretePrincipal p = (DiscretePrincipal) tag;
                    setLoginCookies(p, request);
                    log.trace("authenticate: authentication passed by delegated handler, persisted userUrl to cookie");
                } else {
                    log.warn("authenticate: auth.tag is not an instance of " + DiscretePrincipal.class + ", is: " + tag.getClass() + " so is not compatible with cookie authentication");
// If form auth returned a non principal object then there is no way to
// persist the authentication state, so subsequent requests will fail. To prevent
// this we disable form auth and reject the login, this will result in a Basic/Digest
// authentication challenge
                    if (delegateHandler instanceof FormAuthenticationHandler) {
                        LoginResponseHandler.setDisableHtmlResponse(request);
                        return null;
                    }
                }
                return tag;
            } else {
                log.info("Login failed by delegated handler: " + delegateHandler.getClass());
                return null;
            }
        } else {
            log.trace("no delegating handler");
// No delegating handler means that we expect either to get a previous login token
// via a cookie, or this is an anonymous request
            if (isLogout(request)) {
                log.trace("authenticate: is logout");
                return null;
            } else {
                String userUrl = getUserUrl(request);
                if (userUrl == null) {
                    log.trace("authenticate: no userUrl in request or cookie, nothing to do");
// no token in request, so is anonymous
                    return null;
                } else {
                    if (log.isTraceEnabled()) {
                        log.trace("authenticate: userUrl=" + userUrl);
                    }
// we found a userUrl
                    String host = request.getHostHeader();
                    Resource r;
                    try {
                        r = principalResourceFactory.getResource(host, userUrl);
                        log.trace("found current user: " + r);
                    } catch (NotAuthorizedException ex) {
                        log.error("Couldnt check userUrl in cookie", ex);
                        r = null;
                    } catch (BadRequestException ex) {
                        log.error("Couldnt check userUrl in cookie", ex);
                        r = null;
                    }
                    if (r == null) {
                        log.warn("User not found host: " + host + " userUrl: " + userUrl + " with resourcefactory: " + principalResourceFactory);
                        clearCookieValue(HttpManager.response());
                    } else {
// Logged in ok with details. Check if details came from request parameter, in
// which case we need to set cookies
                        if (request.getParams() != null && request.getParams().containsKey(cookieUserUrlValue)) {
                            if (r instanceof DiscretePrincipal) {
                                DiscretePrincipal dp = (DiscretePrincipal) r;
                                setLoginCookies(dp, request);
                            } else {
                                log.warn("Found user from request, but user object is not expected type. Should be " + DiscretePrincipal.class + " but is " + r.getClass());
                            }
                        } else {
                            log.trace("Do not set cookies, because token did not come from request variable");
                        }
                    }
                    return r;
                }
            }
        }
        */
        return "ok";
    }

    /**
     * Sets cookies to make the given user the currently logged in user for any
     * subsequent requests.
     * <p/>
     * And also makes that user the current on-behalf-of user in
     * CurrentUserService
     *
     * @param user
     * @param request
     */
    public void setLoginCookies(DiscretePrincipal user, Request request) {
        log.trace("setLoginCookies");
        if (user == null) {
            throw new NullPointerException("user object is null");
        }
        if (user.getIdenitifer() == null) {
            throw new NullPointerException("getIdenitifer object is null");
        }
        String userUrl = user.getIdenitifer().getValue();
        if (userUrl == null) {
            throw new NullPointerException("user identifier returned a null value");
        }
        setLoginCookies(userUrl, request);
    }

    public void setLoginCookies(String userUrl, Request request) {
        if (request == null) {
            return;
        }

        Response response = HttpManager.response();
        String signing = getUrlSigningHash(userUrl, request);
        setCookieValues(response, userUrl, signing);
        request.getAttributes().put(userUrlAttName, userUrl);
    }

    @Override
    public void appendChallenges(Resource resource, Request request, List<String> challenges) {
        for (AuthenticationHandler h : handlers) {
            if (h.isCompatible(resource, request)) {
                h.appendChallenges(resource, request,
                        challenges);
            }
        }
    }

    @Override
    public boolean isCompatible(Resource resource, Request request) {
        for (AuthenticationHandler h : handlers) {
            if (h.isCompatible(resource, request)) {
                return true;
            }
        }
        return false;
    }

    private boolean isLogout(Request request) {
        if (request.getParams() == null) {
            return false;
        }

        String logoutCommand = request.getParams().get(requestParamLogout);
        return (logoutCommand != null && logoutCommand.length() > 0);
    }

    /**
     * Find a previous login token in the request, and if present verify its
     * authenticity via a signing cookie
     *
     * @param request
     * @return
     */
    public String getUserUrl(Request request) {
        if (request == null) {
            return null;
        }
        String userUrl = getUserUrlFromRequest(request);

        if (userUrl != null) {
            userUrl = userUrl.trim();
            if (userUrl.length() > 0) {
                if (verifyHash(userUrl, request)) {
                    return userUrl;
                } else {
                    log.error("Invalid userUrl hash, possible attempted hacking attempt. userUrl=" + userUrl);
                }
            }
        }
        return null;
    }

    public String getUserUrlFromRequest(Request request) {
        String encodedUserUrl = getCookieOrParam(request, cookieUserUrlValue);
        if (encodedUserUrl == null) {
            log.trace("getUserUrlFromRequest: Null encodedUserUrl");
            return null;
        }
        if (log.isDebugEnabled()) {
            log.debug("getUserUrlFromRequest: Raw:" + encodedUserUrl);
        }
        if (!encodedUserUrl.startsWith("b64")) {
            log.trace("Looks like a plain path, return as is");
            return encodedUserUrl;
        } else {
            log.trace("Looks like a base64 encoded string");
            encodedUserUrl = encodedUserUrl.substring(3);
        }
        encodedUserUrl = Utils.decodePath(encodedUserUrl);
        if (log.isDebugEnabled()) {
            log.debug("getUserUrlFromRequest: Percent decoded:" + encodedUserUrl);
        }

        byte[] arr = base64.fromString(encodedUserUrl);
        if (arr == null) {
            log.debug("Failed to decode encodedUserUrl, so maybe its not encoded, return as it is");
            return encodedUserUrl; // its just not encoded
        }
        String s = new String(arr);
        if (log.isDebugEnabled()) {
            log.debug("getUserUrlFromRequest: Decoded user url:" + s);
        }
        return s;
    }

    public String getHashFromRequest(Request request) {
        String signing = getCookieOrParam(request, cookieUserUrlHash);
        return signing;
    }

    private boolean verifyHash(String userUrl, Request request) {
        String signing = getHashFromRequest(request);
        if (signing == null) {
            return false;
        }
        signing = signing.replace("\"", "");
        signing = signing.trim();
        if (signing.length() == 0) {
            log.warn("cookie signature is not present in cookie: " + cookieUserUrlHash);
            return false;
        }

        for (String key : keys) {
            if (key != null && key.length() > 0) {
                if (verifyHash(userUrl, key, signing, request)) {
                    return true;
                }
            }
        }
        return false;
    }

    private boolean verifyHash(String userUrl, String key, String signing, Request request) {
// split the signing into nonce and hmac
        int pos = signing.indexOf(":");
        if (pos < 1) {
            log.warn("Invalid cookie signing format, no semi-colon: " + signing + " Should be in form - nonce:hmac");
            return false;
        }
        String host = getDomain(request);
        String nonce = signing.substring(0, pos);
        String hmac = signing.substring(pos + 1);
        String message = nonce + ":" + userUrl + ":" + host;

// Check that the hmac is a valid signature
        String expectedHmac = HmacUtils.calcShaHash(message, key);
        if (log.isTraceEnabled()) {
            log.trace("Message:" + message);
            log.trace("Key:" + key);
            log.trace("Hash:" + expectedHmac);
            log.trace("Given Signing:" + signing);
        }
        boolean ok = expectedHmac.equals(hmac);
        if (!ok) {
            if (log.isDebugEnabled()) {
                log.debug("Cookie sig does not match expected. Given=" + hmac + " Expected=" + expectedHmac);
            }
            return false;
        } else {
// signed ok, check to see if nonce is still valid
            NonceProvider.NonceValidity val = nonceProvider.getNonceValidity(nonce, null);
            if (val == NonceProvider.NonceValidity.OK) {
                return true;
            } else if (val == NonceProvider.NonceValidity.EXPIRED) {
// Hopefully the nonce provider will have a time
// for recently expired nonces. So we will accept these but replace with a refreshed nonce
                log.warn("Nonce is valid, but expired. We will accept it but reset it");
                setLoginCookies(userUrl, request);
                return true;
            } else if (val == NonceProvider.NonceValidity.INVALID) {
                log.warn("Received an invalid nonce: " + nonce + " not found in provider: " + nonceProvider);
                return false;
            } else {
                throw new RuntimeException("Unhandled nonce validity value");
            }
        }
    }

    private String getDomain(Request request) {
        String host = request.getHostHeader();
        if (host.contains(":")) {
            host = host.substring(0, host.indexOf(":"));
        }
        if (host == null) {
            host = "nohost";
        }
        return host;
    }

    /**
     * The hmac signs a message in the form nonce || userUrl, where the nonce is
     * requested from the nonceProvider
     * <p/>
     * This method returns a signing token in the form nonce || hmac
     *
     * @param userUrl
     * @param request
     * @return
     */
    public String getUrlSigningHash(String userUrl, Request request) {
        String host = getDomain(request);
        return getUrlSigningHash(userUrl, request, host);
    }

    public String getUrlSigningHash(String userUrl, Request request, String host) {
        String nonce = nonceProvider.createNonce(request);
        String message = nonce + ":" + userUrl + ":" + host;
        String key = keys.get(keys.size() - 1); // Use the last key for new cookies
        String hash = HmacUtils.calcShaHash(message, key);
        String signing = nonce + ":" + hash;
        if (log.isTraceEnabled()) {
            log.trace("Message:" + message);
            log.trace("Key:" + key);
            log.trace("Hash:" + hash);
            log.trace("Signing:" + signing);
        }
        return signing;
    }

    private void setCookieValues(Response response, String userUrl, String hash) {
        log.trace("setCookieValues");
        BeanCookie c = new BeanCookie(cookieUserUrlValue);
        String encodedUserUrl = encodeUserUrl(userUrl);
        c.setValue(encodedUserUrl);
        c.setPath("/");
        c.setVersion(1);
        if (useLongLivedCookies) {
            c.setExpiry(SECONDS_PER_YEAR);
        }
        response.setCookie(c);

        c = new BeanCookie(cookieUserUrlHash);
        c.setValue("\"" + hash + "\"");
        c.setHttpOnly(true); // http only so not accessible from JS. Helps prevent XSS attacks
        c.setVersion(1);
        c.setPath("/");
        if (useLongLivedCookies) {
            c.setExpiry(SECONDS_PER_YEAR);
        }
        response.setCookie(c);
    }

    public String encodeUserUrl(String userUrl) {
        String encodedUserUrl = base64.toString(userUrl.getBytes(Utils.UTF8));
        encodedUserUrl = Utils.percentEncode(encodedUserUrl); // base64 uses some chars illegal in cookies, eg equals
        encodedUserUrl = "b64" + encodedUserUrl; // need to distinguish if base64 encoded or not
        return (encodedUserUrl);
    }

    private void clearCookieValue(Response response) {
        log.info("clearCookieValue");
        response.setCookie(cookieUserUrlValue, "");
        response.setCookie(cookieUserUrlHash, "");
    }

    private String getCookieOrParam(Request request, String name) {
        if (request == null) {
            return null;
        }
        if (request.getParams() != null) {
            String v = request.getParams().get(name);
            if (v != null) {
                return v;
            }
        }
        Cookie c = request.getCookie(name);
        if (c != null) {
            return c.getValue();
        }
        return null;
    }

    public String getCookieNameUserUrlHash() {
        return cookieUserUrlHash;
    }

    public String getCookieNameUserUrl() {
        return cookieUserUrlValue;
    }

    public String getUserUrlAttName() {
        return userUrlAttName;
    }

    public void setUserUrlAttName(String userUrlAttName) {
        this.userUrlAttName = userUrlAttName;
    }

    public void setUseLongLivedCookies(boolean useLongLivedCookies) {
        this.useLongLivedCookies = useLongLivedCookies;
    }

    public boolean isUseLongLivedCookies() {
        return useLongLivedCookies;
    }

}