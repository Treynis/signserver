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
package org.signserver.module.signerstatusreport;

import java.io.*;
import java.util.Map;
import org.apache.log4j.Logger;
import org.signserver.common.GlobalConfiguration;
import org.signserver.common.SignServerUtil;
import org.signserver.testutils.ModulesTestCase;

/**
 * Tests for SignerStatusReportTimedService.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class SignerStatusReportTimedServiceTest extends ModulesTestCase {

    /** Logger for this class. */
    private static final Logger LOG
            = Logger.getLogger(SignerStatusReportTimedServiceTest.class);

    /**
     * Worker id for the service.
     */
    private static final int WORKERID_SERVICE = 5701;

    /**
     * WORKERID used in this test case as defined in
     * junittest-part-config.properties.
     */
    private static final int WORKERID_SIGNER1 = 5681;
    private static final String WORKER_SIGNER1 = "TestXMLSigner81";

    /**
     * WORKERID used in this test case as defined in
     * junittest-part-config.properties.
     */
    private static final int WORKERID_SIGNER2 = 5682;
    private static final String WORKER_SIGNER2 = "TestXMLSigner82";

    /**
     * WORKERID used in this test case as defined in
     * junittest-part-config.properties.
     */
    private static final int WORKERID_SIGNER3 = 5676;
    private static final String WORKER_SIGNER3 = "TestXMLSigner";
    
    private static final int[] WORKERS = new int[] {5676, 5679, 5681, 5682, 5683, 5802, 5803};

    private static final long serviceInterval = 10;

    private static File outputFile;
    
    private SignerStatusReportParser parser = new SignerStatusReportParser();
	
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SignServerUtil.installBCProvider();
        
        outputFile = new File(getSignServerHome() + File.separator
                + "~test-outputfile.dat");
        if (outputFile.exists()) {
            if (!outputFile.delete()) {
                fail("Could not remove: " + outputFile.getAbsolutePath());
            }
        }
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }	

    /**
     * Create test workers.
     * @throws Exception
     */
    public void test00SetupDatabase() throws Exception {

        setProperties(new File(getSignServerHome(), "modules/SignServer-Module-XMLSigner/src/conf/junittest-part-config.properties"));
        workerSession.reloadConfiguration(WORKERID_SIGNER1);
        workerSession.reloadConfiguration(WORKERID_SIGNER2);
        workerSession.reloadConfiguration(WORKERID_SIGNER3);

        // Setup service
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL,
            "WORKER" + WORKERID_SERVICE + ".CLASSPATH",
            "org.signserver.server.timedservices.SignerStatusReportTimedService");

        workerSession.setWorkerProperty(WORKERID_SERVICE, "WORKERS",
                WORKER_SIGNER1+","+WORKER_SIGNER2+","+WORKER_SIGNER3);
        workerSession.setWorkerProperty(WORKERID_SERVICE, "OUTPUTFILE",
                outputFile.getAbsolutePath());
        workerSession.setWorkerProperty(WORKERID_SERVICE, "INTERVAL",
                String.valueOf(serviceInterval));
        workerSession.setWorkerProperty(WORKERID_SERVICE, "ACTIVE", "FALSE");

        workerSession.reloadConfiguration(WORKERID_SERVICE);
    }

    public void test01Report() throws Exception {

        if (outputFile.exists()) {
            outputFile.delete();
            assertFalse("Removed outputfile", outputFile.exists());
        }

        // Enable service
        workerSession.setWorkerProperty(WORKERID_SERVICE, "ACTIVE", "TRUE");
        workerSession.reloadConfiguration(WORKERID_SERVICE);

        waitForServiceRun(30);

        Map<String, Map<String, String>> status;

        // Now all three workers should be present and ACTIVE
        status = parser.parseOutputFile(outputFile);

        assertNotNull("Worker 1 present", status.get(WORKER_SIGNER1));
        assertEquals("Worker 1 active", "ACTIVE", status.get(WORKER_SIGNER1).get("status"));
        assertNotNull("Worker 1 signings", status.get(WORKER_SIGNER1).get("signings"));
        
        assertNotNull("Worker 2 present", status.get(WORKER_SIGNER2));
        assertEquals("Worker 2 active", "ACTIVE", status.get(WORKER_SIGNER2).get("status"));
        assertNotNull("Worker 2 signings", status.get(WORKER_SIGNER2).get("signings"));

        assertNotNull("Worker 3 present", status.get(WORKER_SIGNER3));
        assertEquals("Worker 3 active", "ACTIVE", status.get(WORKER_SIGNER3).get("status"));
        assertNotNull("Worker 3 signings", status.get(WORKER_SIGNER3).get("signings"));

        // Disable one worker and check the result
//        workerSession.setWorkerProperty(WORKERID_SIGNER1, "DISABLED", "TRUE");
//        workerSession.reloadConfiguration(WORKERID_SIGNER1);
        workerSession.deactivateSigner(WORKERID_SIGNER1);
        
        outputFile.delete();

        waitForServiceRun(30);

        // Now WORKER1 should be OFFLINE and the other as before
        try {
            status = parser.parseOutputFile(outputFile);
        } catch (FileNotFoundException ex) {
            fail("FileNotFound: " + ex.getMessage());
        } catch (IOException ex) {
            fail("IOException: " + ex.getMessage());
        } 

        

        assertNotNull("Worker 1 present", status.get(WORKER_SIGNER1));
        assertEquals("Worker 1 OFFLINE", "OFFLINE", status.get(WORKER_SIGNER1).get("status"));
        assertNotNull("Worker 1 signings", status.get(WORKER_SIGNER1).get("signings"));

        assertNotNull("Worker 2 present", status.get(WORKER_SIGNER2));
        assertEquals("Worker 2 active", "ACTIVE", status.get(WORKER_SIGNER2).get("status"));
        assertNotNull("Worker 2 signings", status.get(WORKER_SIGNER2).get("signings"));

        assertNotNull("Worker 3 present", status.get(WORKER_SIGNER3));
        assertEquals("Worker 3 active", "ACTIVE", status.get(WORKER_SIGNER3).get("status"));
        assertNotNull("Worker 3 signings", status.get(WORKER_SIGNER3).get("signings"));
    }

    /**
     * Removes all test workers.
     * @throws Exception
     */
    public void test99TearDownDatabase() throws Exception {
        removeWorker(WORKERID_SERVICE);
        for (int workerId : WORKERS) {
            removeWorker(workerId);
        }
    }

    

    private static void waitForServiceRun(final int maxTries) {
        try {
            for (int i = 0; i < maxTries; i++) {
                if (outputFile.exists()) {
                    break;
                }
                Thread.sleep(1000);
            }
        } catch (InterruptedException ex) {
            LOG.error("Interrupted", ex);
        }
    }

}
