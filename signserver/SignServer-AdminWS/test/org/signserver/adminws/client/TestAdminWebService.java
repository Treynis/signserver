/*************************************************************************
 *                                                                       *
 *  SignServer: The OpenSource Automated Signing Server                  *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.signserver.adminws.client;

import java.math.BigInteger;
import java.net.URL;
import java.util.Arrays;
import javax.xml.namespace.QName;
import junit.framework.TestCase;
import org.signserver.adminws.client.AdminWebServiceService;

/**
 * Tests for the Admin WS interface.
 * 
 * @author markus
 * @version $Id$
 */
public class TestAdminWebService extends TestCase {
    
    private static final int ANY_WORKERID = 4711;
    private static final String ANY_KEY = "AKEY";
    private static final String ANY_VALUE = "aValue";
    private static final String AUTH_CODE = "foo123";
    private static final AuthorizedClient ANY_AUTHORIZED_CLIENT
            = new AuthorizedClient();

    static {
        ANY_AUTHORIZED_CLIENT.setIssuerDN(
                "CN=AdminCA4711, O=SignServer Testing, C=SE");
        ANY_AUTHORIZED_CLIENT.setCertSN(
                new BigInteger("111114711").toString(16));
    }

    private AdminWebService adminWS;


    public TestAdminWebService(String testName) {
        super(testName);
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final AdminWebServiceService service = new AdminWebServiceService(
                new URL("http://localhost:8080/signserver/adminws/adminws?wsdl"),
                new QName("http://adminws.signserver.org/",
                    "AdminWebServiceService"));
        adminWS = service.getAdminWebServicePort();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    public void testReloadConfiguration() throws Exception {
        try {
            adminWS.reloadConfiguration(ANY_WORKERID);
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }
    
    public void testActivateSigner_auth() throws Exception {
        try {
            adminWS.activateSigner(ANY_WORKERID, AUTH_CODE);
            fail("Access should have been denied!");
        } catch (CryptoTokenAuthenticationFailureException_Exception ex) {
            fail("Wrong exception: " + ex.getMessage());
        } catch (CryptoTokenOfflineException_Exception ex) {
            fail("Wrong exception: " + ex.getMessage());
        } catch (InvalidWorkerIdException_Exception ex) {
            fail("Wrong exception: " + ex.getMessage());
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }
    
    public void testSetWorkerProperty() throws Exception {
        try {
            adminWS.setWorkerProperty(ANY_WORKERID, ANY_KEY, ANY_VALUE);
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }
    
    public void testAddAuthorizedClient() throws Exception {
        try {
            adminWS.addAuthorizedClient(ANY_WORKERID, ANY_AUTHORIZED_CLIENT);
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }
    
    public void testUploadSignerCertificate() throws Exception {
        try {
            adminWS.uploadSignerCertificate(ANY_WORKERID, new byte[0], "GLOB");
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }
    
    public void testUploadSignerCertificateChain() throws Exception {
        try {
            adminWS.uploadSignerCertificateChain(ANY_WORKERID,
                    Arrays.asList(new byte[0]), "GLOB");
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }
    
    public void testSetGlobalProperty() throws Exception {
        try {
            adminWS.setGlobalProperty("GLOB", ANY_KEY, ANY_VALUE);
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }
    
    public void testGlobalResync() throws Exception {
        try {
            adminWS.globalResync();
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }
    
    public void testGlobalReload() throws Exception {
        try {
            adminWS.globalReload();
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }

    // TODO add test methods here. The name must begin with 'test'. For example:
    // public void testHello() {}

}
