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

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigInteger;
import java.net.URL;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.Properties;
import javax.xml.namespace.QName;
import junit.framework.TestCase;
import org.apache.log4j.Logger;

/**
 * Tests for the Admin WS interface. Currently only tests that each operation
 * requires authentication.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class AdminWebServiceTest extends TestCase {
    
    /** Logger for this class. **/
    private static final Logger LOG = Logger.getLogger(AdminWebServiceTest.class);

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

    private AdminWS adminWS;


    public AdminWebServiceTest(String testName) {
        super(testName);
        setupKeystores();
    }

    /** Setup keystores for SSL. **/
    private void setupKeystores() {
        Properties config = new Properties();
        File confFile1 = new File("../../signserver_build.properties");
        File confFile2 = new File("../../conf/signserver_build.properties");
        try {
            if (confFile1.exists()) {
                config.load(new FileInputStream(confFile1));
            } else {
                config.load(new FileInputStream(confFile2));
            }
        } catch (FileNotFoundException ignored) {
            LOG.debug("No signserver_build.properties");
        } catch (IOException ex) {
            LOG.error("Not using signserver_build.properties: " + ex.getMessage());
        }
        System.setProperty("javax.net.ssl.trustStore", "../../p12/truststore.jks");
        System.setProperty("javax.net.ssl.trustStorePassword",
                config.getProperty("java.trustpassword", "changeit"));
        //System.setProperty("javax.net.ssl.keyStore", "../../p12/testadmin.jks");
        //System.setProperty("javax.net.ssl.keyStorePassword", "foo123");
    }

    @Override
    protected void setUp() throws Exception {
        super.setUp();
        final AdminWSService service = new AdminWSService(
                new URL("https://localhost:8442/signserver/AdminWSService/AdminWS?wsdl"),
                new QName("http://adminws.signserver.org/",
                    "AdminWSService"));
        adminWS = service.getAdminWSPort();
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

    public void testGetKeyUsageCounterValue() throws Exception {
        try {
            adminWS.getKeyUsageCounterValue(ANY_WORKERID);
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }

    public void testProcess() throws Exception {
        try {
            final List<byte[]> requests = Collections.emptyList();
            adminWS.process(String.valueOf(ANY_WORKERID), requests);
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }
    
    public void testQueryAuditLog() throws Exception {
        try {
            adminWS.queryAuditLog(0, 10, Collections.<QueryCondition>emptyList(), Collections.<QueryOrdering>emptyList());
            fail("Access should have been denied!");
        } catch (AdminNotAuthorizedException_Exception ignored) {
            // OK
        }
    }

    // TODO add test methods here. The name must begin with 'test'. For example:
    // public void testHello() {}

}
