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
package org.signserver.server;

import java.io.File;
import java.security.cert.Certificate;
import java.util.Hashtable;
import javax.naming.Context;
import javax.naming.InitialContext;
import junit.framework.TestCase;
import org.apache.log4j.Logger;
import org.signserver.cli.CommonAdminInterface;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.GenericSignRequest;
import org.signserver.common.GenericSignResponse;
import org.signserver.common.GlobalConfiguration;
import org.signserver.common.RequestContext;
import org.signserver.common.SignServerUtil;
import org.signserver.ejb.interfaces.IGlobalConfigurationSession;
import org.signserver.ejb.interfaces.IWorkerSession;
import org.signserver.testutils.TestUtils;

/**
 * Tests limits for the key usages.
 *
 * @author Markus Kilas
 * @version $Id$
 */
public class TestLimitKeyUsages extends TestCase {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(
            TestLimitKeyUsages.class);

    private static IGlobalConfigurationSession.IRemote confSession;
    private static IWorkerSession.IRemote workSession;
    private static File signServerHome;

    /** WORKERID used in this test case. */
    private static final int WORKERID_1 = 5802;

    /**
     * Test with this number of signings.
     */
    private static final int LIMIT = 10;


    @Override
    protected void setUp() throws Exception {
        SignServerUtil.installBCProvider();
        final Context context = getInitialContext();
        confSession = (IGlobalConfigurationSession.IRemote) context.lookup(
                IGlobalConfigurationSession.IRemote.JNDI_NAME);
        workSession = (IWorkerSession.IRemote) context.lookup(
                IWorkerSession.IRemote.JNDI_NAME);
        TestUtils.redirectToTempOut();
        TestUtils.redirectToTempErr();
        CommonAdminInterface.BUILDMODE = "SIGNSERVER";
    }

    @Override
    protected void tearDown() throws Exception {
    }

    public void test00SetupDatabase() throws Exception {

        confSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL,
                "WORKER" + WORKERID_1 + ".CLASSPATH",
                "org.signserver.module.xmlsigner.XMLSigner");
        confSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL,
                "WORKER" + WORKERID_1 + ".SIGNERTOKEN.CLASSPATH",
                "org.signserver.server.cryptotokens.P12CryptoToken");
        workSession.setWorkerProperty(WORKERID_1, "NAME",
                "TestLimitKeyUsageSigner");
        workSession.setWorkerProperty(WORKERID_1, "AUTHTYPE", "NOAUTH");
        workSession.setWorkerProperty(WORKERID_1, "KEYSTOREPATH", 
                getSignServerHome().getAbsolutePath()
                + File.separator + "src" + File.separator + "test"
                + File.separator + "test_keyusagelimit1.p12");
        workSession.setWorkerProperty(WORKERID_1, "KEYSTOREPASSWORD", "foo123");
        workSession.setWorkerProperty(WORKERID_1, "KEYUSAGELIMIT",
                String.valueOf(LIMIT));

        workSession.reloadConfiguration(WORKERID_1);
    }

    /**
     * Do signings up to KEYUSAGELIMIT and then check that the next signing
     * fails.
     *
     * Assumption 1: The database or atleast the table KeyUsageCounter needs to
     * be cleared.
     * Assumption 2: The configured key (test_keyusagelimit1.p12) is not used by
     * any other tests.
     *
     * @throws Exception in case of exception
     */
    public void test01Limit() throws Exception {

        // Do a number of signings LIMIT
        for (int i = 0; i < LIMIT; i++) {
            LOG.debug("Signing " + i);
            doSign();
        }

        try {
            doSign();
            fail("Should have failed now");

        } catch (CryptoTokenOfflineException ok) {}
    }

    /** Do a dummy sign. */
    private static void doSign() throws Exception {

        final RequestContext context = new RequestContext();
        final GenericSignRequest request = new GenericSignRequest(1,
                "<root/>".getBytes());
        GenericSignResponse res;
        // Send request to dispatcher
        res = (GenericSignResponse) workSession.process(WORKERID_1,
            request, context);
        Certificate cert = res.getSignerCertificate();
        assertNotNull(cert);
    }


    public void test99TearDownDatabase() throws Exception {

        TestUtils.assertSuccessfulExecution(new String[] {
            "removeworker",
            String.valueOf(WORKERID_1)
        });

        workSession.reloadConfiguration(WORKERID_1);
    }

    private File getSignServerHome() throws Exception {
        if (signServerHome == null) {
            final String home = System.getenv("SIGNSERVER_HOME");
            assertNotNull("SIGNSERVER_HOME", home);
            signServerHome = new File(home);
            assertTrue("SIGNSERVER_HOME exists", signServerHome.exists());
        }
        return signServerHome;
    }

    /**
     * Get the initial naming context.
     */
    protected Context getInitialContext() throws Exception {
        Hashtable<String, String> props = new Hashtable<String, String>();
        props.put(
                Context.INITIAL_CONTEXT_FACTORY,
                "org.jnp.interfaces.NamingContextFactory");
        props.put(
                Context.URL_PKG_PREFIXES,
                "org.jboss.naming:org.jnp.interfaces");
        props.put(Context.PROVIDER_URL, "jnp://localhost:1099");
        Context ctx = new InitialContext(props);
        return ctx;
    }
}
