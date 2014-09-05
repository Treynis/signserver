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
package org.signserver.server.timedservices.hsmkeepalive;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import org.apache.commons.io.IOUtils;
import org.apache.log4j.Logger;
import org.junit.Before;
import org.junit.FixMethodOrder;
import org.junit.runners.MethodSorters;
import org.signserver.common.SignServerUtil;
import org.signserver.ejb.interfaces.IWorkerSession;
import org.signserver.ejb.interfaces.IGlobalConfigurationSession;
import org.signserver.testutils.ModulesTestCase;

/**
 * System test for the HSM keep-alive timed service.
 * 
 * @author Marcus Lundblad
 * @version $Id$
 */
@FixMethodOrder(MethodSorters.NAME_ASCENDING)
public class HSMKeepAliveTimedServiceTest extends ModulesTestCase {
    
    private static final Logger LOG = Logger.getLogger(HSMKeepAliveTimedServiceTest.class);
    
    private static int WORKERID_SERVICE = 5800;
    private static int WORKERID_CRYPTOWORKER1 = 5801;
    private static String WORKERNAME_CRYPTOWORKER1 = "CryptoWorker1";
    private static int WORKERID_CRYPTOWORKER2 = 5802;
    private static String WORKERNAME_CRYPTOWORKER2 = "CryptoWorker2";
    
    private final IWorkerSession workerSession = getWorkerSession();
    private final IGlobalConfigurationSession globalSession = getGlobalSession();

    private File signServerHome;
    
    @Before
    public void setUp() throws Exception {
        SignServerUtil.installBCProvider();
        signServerHome = getSignServerHome();
        deleteDebugFile(WORKERID_CRYPTOWORKER1);
        deleteDebugFile(WORKERID_CRYPTOWORKER2);
    }
    
    private File getDebugFile(final int workerId) {
        return new File(signServerHome,
                         "~testkey-" + workerId);
    }
    
    private void deleteDebugFile(final int workerId) {
        final File debugFile = getDebugFile(workerId);
 
        if (!debugFile.delete()) {
            LOG.error("Could not delete debug file: " + debugFile.getAbsolutePath());
        }
    }
    
    private boolean debugFileExists(final int workerId) {
        final File debugFile = getDebugFile(workerId);

        return debugFile.exists() && debugFile.isFile();
    }
    
    private String getDebugKeyAlias(final int workerId) {
        final File debugFile = getDebugFile(workerId);
 
        try {
            final FileInputStream fis = new FileInputStream(debugFile);
            
            return IOUtils.toString(fis);
        } catch (FileNotFoundException e) {
            return null;
        } catch (IOException e) {
            LOG.error("Error reading debug file: " + e.getMessage());
            return null;
        }
    }
    
    public void test00setupDatabase() throws Exception {
        setProperties(new File(getSignServerHome(), "res/test/test-hsmkeepalive-configuration.properties"));
        
        // set debug outpaths
        workerSession.setWorkerProperty(WORKERID_CRYPTOWORKER1,
                TestKeyDebugCryptoToken.TESTKEY_DEBUG_OUTPATH,
                getDebugFile(WORKERID_CRYPTOWORKER1).getAbsolutePath());
        workerSession.setWorkerProperty(WORKERID_CRYPTOWORKER2,
                TestKeyDebugCryptoToken.TESTKEY_DEBUG_OUTPATH,
                getDebugFile(WORKERID_CRYPTOWORKER2).getAbsolutePath());
        
        
        workerSession.reloadConfiguration(WORKERID_SERVICE);
        workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER1);
        workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER2);
    }
    
    /**
     * Test a basic configuration with two crypto workers set up with the
     * TESTKEY key alias property.
     * 
     * @throws Exception 
     */
    public void test01runServiceWithTwoWorkers() throws Exception {
        try {
            // make sure the service had time to run
            Thread.sleep(2000);
            // check that the service has run and tested keys for both configured workers
            assertTrue("testKey run on worker 1",
                        debugFileExists(WORKERID_CRYPTOWORKER1));
            assertTrue("testKey run on worker 2",
                        debugFileExists(WORKERID_CRYPTOWORKER2));
            assertEquals("TESTKEY alias used for worker 1",
                         "TestKey1", getDebugKeyAlias(WORKERID_CRYPTOWORKER1));
            assertEquals("TESTKEY alias used for worker 2",
                         "TestKey2", getDebugKeyAlias(WORKERID_CRYPTOWORKER2));
        } finally {
            deleteDebugFile(WORKERID_CRYPTOWORKER1);
            deleteDebugFile(WORKERID_CRYPTOWORKER2);
        }
    }
    
    /**
     * Test that when setting DEFAULTKEY, TESTKEY is still used.
     * 
     * @throws Exception 
     */
    public void test02runServiceWithTestAndDefaultKey() throws Exception {
        try {
            workerSession.setWorkerProperty(WORKERID_CRYPTOWORKER1,
                    "DEFAULTKEY", "DefaultKey1");
            workerSession.setWorkerProperty(WORKERID_CRYPTOWORKER2,
                    "DEFAULTKEY", "DefaultKey2");
            workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER1);
            workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER2);
            
            // make sure the service had time to run
            Thread.sleep(2000);
            // check that the service has run and tested keys for both configured workers
            assertTrue("testKey run on worker 1",
                        debugFileExists(WORKERID_CRYPTOWORKER1));
            assertTrue("testKey run on worker 2",
                        debugFileExists(WORKERID_CRYPTOWORKER2));
            assertEquals("TESTKEY alias used for worker 1",
                         "TestKey1", getDebugKeyAlias(WORKERID_CRYPTOWORKER1));
            assertEquals("TESTKEY alias used for worker 2",
                         "TestKey2", getDebugKeyAlias(WORKERID_CRYPTOWORKER2));
        } finally {
            workerSession.removeWorkerProperty(WORKERID_CRYPTOWORKER1, "DEFAULTKEY");
            workerSession.removeWorkerProperty(WORKERID_CRYPTOWORKER2, "DEFAULTKEY");
            workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER1);
            workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER2);

            deleteDebugFile(WORKERID_CRYPTOWORKER1);
            deleteDebugFile(WORKERID_CRYPTOWORKER2);
        }
    }
    
    /**
     * Test that DEFAULTKEY is used if TESTKEY is missing.
     * 
     * @throws Exception 
     */
    public void test03runServiceWithOnlyDefaultKey() throws Exception {
        try {
            workerSession.setWorkerProperty(WORKERID_CRYPTOWORKER1,
                    "DEFAULTKEY", "DefaultKey1");
            workerSession.setWorkerProperty(WORKERID_CRYPTOWORKER2,
                    "DEFAULTKEY", "DefaultKey2");
            workerSession.removeWorkerProperty(WORKERID_CRYPTOWORKER1,
                    "TESTKEY");
            workerSession.removeWorkerProperty(WORKERID_CRYPTOWORKER2,
                    "TESTKEY");
            workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER1);
            workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER2);
            
            // make sure the service had time to run
            Thread.sleep(2000);
            // check that the service has run and tested keys for both configured workers
            assertTrue("testKey run on worker 1",
                        debugFileExists(WORKERID_CRYPTOWORKER1));
            assertTrue("testKey run on worker 2",
                        debugFileExists(WORKERID_CRYPTOWORKER2));
            assertEquals("DEFAULTKEY alias used for worker 1",
                         "DefaultKey1", getDebugKeyAlias(WORKERID_CRYPTOWORKER1));
            assertEquals("DEFAULTKEY alias used for worker 2",
                         "DefaultKey2", getDebugKeyAlias(WORKERID_CRYPTOWORKER2));
        } finally {
            workerSession.removeWorkerProperty(WORKERID_CRYPTOWORKER1, "DEFAULTKEY");
            workerSession.removeWorkerProperty(WORKERID_CRYPTOWORKER2, "DEFAULTKEY");
            workerSession.setWorkerProperty(WORKERID_CRYPTOWORKER1, "TESTKEY",
                    "TestKey1");
            workerSession.setWorkerProperty(WORKERID_CRYPTOWORKER2, "TESTKEY",
                    "TestKey2");
            workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER1);
            workerSession.reloadConfiguration(WORKERID_CRYPTOWORKER2);

            deleteDebugFile(WORKERID_CRYPTOWORKER1);
            deleteDebugFile(WORKERID_CRYPTOWORKER2);
        }
    }
    
    public void test99tearDownDatabase() throws Exception {
        removeWorker(WORKERID_SERVICE);
        removeWorker(WORKERID_CRYPTOWORKER1);
        removeWorker(WORKERID_CRYPTOWORKER2);
    } 
}
