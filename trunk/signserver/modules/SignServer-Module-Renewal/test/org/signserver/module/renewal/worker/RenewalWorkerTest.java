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
package org.signserver.module.renewal.worker;

import java.net.URL;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Properties;
import javax.xml.namespace.QName;
import javax.xml.ws.Endpoint;
import org.apache.log4j.Logger;
import org.signserver.common.GenericPropertiesRequest;
import org.signserver.common.GenericPropertiesResponse;
import org.signserver.common.GlobalConfiguration;
import org.signserver.common.RequestContext;
import org.signserver.module.renewal.common.RenewalWorkerProperties;
import org.signserver.module.renewal.ejbcaws.gen.EjbcaWS;
import org.signserver.module.renewal.ejbcaws.gen.EjbcaWSService;
import org.signserver.module.renewal.ejbcaws.gen.UserDataVOWS;
import org.signserver.module.renewal.ejbcaws.gen.UserMatch;

/**
 * Test case for the RenewalWorker.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class RenewalWorkerTest extends AbstractTestCase {
    
    private static final String EJBCAWSURL_PREFIX = "http://localhost:8111/ejbca";
    private static final String EJBCAWSURL_SUFFIX = "/ejbcaws/ejbcaws";
    private static final String EJBCAWSURL_SUFFIX_WSDL = "/ejbcaws/ejbcaws?wsdl";

    public static final int SIGNERID_6102 = 6102;
    public static final String SIGNER_6102 = "Signer_6102";
    public static final String SIGNER_6102_ENDENTITY = "Signer_6102_endentity";

    static final int MATCH_WITH_USERNAME = 0;
    static final int MATCH_TYPE_EQUALS = 0;

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(RenewalWorkerTest.class);

    private static final int WORKERID = 6101;

    private Endpoint ejbcaEndpoint;
    private MockEjbcaWS mockEjbcaWs;
    private EjbcaWS ejbcaws;

    @Override
    protected void setUp() throws Exception {
        super.setUp();

        mockEjbcaWs = new MockEjbcaWS();
        ejbcaEndpoint = Endpoint.publish(EJBCAWSURL_PREFIX + EJBCAWSURL_SUFFIX,
                mockEjbcaWs);

        final EjbcaWSService service = new EjbcaWSService(
                new URL(EJBCAWSURL_PREFIX + EJBCAWSURL_SUFFIX_WSDL),
                new QName("http://ws.protocol.core.ejbca.org/",
                "EjbcaWSService"));
        ejbcaws = service.getEjbcaWSPort();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();

        ejbcaEndpoint.stop();
        removeSigners();
        removeTempFiles();
    }

    public void test01ejbcaWSMockWorking() throws Exception {
        assertTrue(ejbcaws.isAuthorized("Hej"));
    }

    /**
     * Tests renewal of key and certificate for a worker.
     * @throws Exception
     */
    public void test02renewalFirstTime() throws Exception {
        
        // Setup workers
        addWorkers();

        // Setup EJBCA end entity
        final UserDataVOWS user1 = new UserDataVOWS();
        user1.setUsername(SIGNER_6102_ENDENTITY);
        user1.setPassword("some-password-123");
        user1.setSubjectDN("CN=" + SIGNER_6102_ENDENTITY
                + ",O=SignServer Testing,C=SE");
        user1.setEndEntityProfileName("EMPTY");
        user1.setCertificateProfileName("ENDENTITY");
        user1.setCaName("SomeCA");
        final UserMatchEq match1 = new UserMatchEq();
        match1.setMatchwith(MATCH_WITH_USERNAME);
        match1.setMatchtype(MATCH_TYPE_EQUALS);
        match1.setMatchvalue(SIGNER_6102_ENDENTITY);
        final Map<UserMatchEq, List<UserDataVOWS>> findResult
                = new HashMap<UserMatchEq, List<UserDataVOWS>>();
        findResult.put(match1, Arrays.asList(user1));
        mockEjbcaWs.setFindUserResults(findResult);

        final Properties reqProperties = new Properties();
        reqProperties.setProperty(RenewalWorkerProperties.REQUEST_WORKER, SIGNER_6102);
        reqProperties.setProperty(RenewalWorkerProperties.REQUEST_RENEWKEY,
                RenewalWorkerProperties.REQUEST_RENEWKEY_TRUE);
        reqProperties.setProperty("DUMMYUNIQEVALUE",
                String.valueOf(Math.random()));
        final GenericPropertiesRequest request = new GenericPropertiesRequest(
                reqProperties);
        GenericPropertiesResponse response
                = (GenericPropertiesResponse) getWorkerSession().process(
                    WORKERID, request, new RequestContext());

        // OK result
        assertEquals(RenewalWorkerProperties.RESPONSE_RESULT_OK,
                response.getProperties().getProperty(
                RenewalWorkerProperties.RESPONSE_RESULT));

        // Requested certificate
        assertTrue("should have requested certificate",
                mockEjbcaWs.isPkcs10RequestCalled());

        // Should have certificate and chain
        final X509Certificate cert = (X509Certificate) getWorkerSession()
                .getSignerCertificate(SIGNERID_6102);
        assertNotNull(cert);
        final List<java.security.cert.Certificate> chain
                = getWorkerSession().getSignerCertificateChain(SIGNERID_6102);
        assertNotNull(chain);
        assertTrue(chain.contains(cert));

        // Should not be any NEXTCERTSIGNKEY
        assertNull(getWorkerSession().getCurrentWorkerConfig(SIGNERID_6102).getProperty("NEXTCERTSIGNKEY"));

        // Should be an DEFAULTKEY
        assertNotNull(getWorkerSession().getCurrentWorkerConfig(SIGNERID_6102).getProperty("DEFAULTKEY"));
    }

    // TODO: Test Renewal without key generation (ie when NEXTCERTSIGNKEY exists)

    // TODO: Test renewal without key generation (ie when NEXTCERTSIGNKEY exists) but for DEFAULTKEY requested in request

    // TODO: Test failure: No EJBCA end entity

    // TODO: Test failure: Authentication denied


    private void addWorkers() throws Exception {
        addRenewalWorker(6101, "RenewalWorker_6101");
        addSigner(SIGNERID_6102, SIGNER_6102, SIGNER_6102_ENDENTITY);
    }

    private void removeSigners() throws Exception {
        removeWorker(6101);
        removeWorker(6102);
    }

    protected void addRenewalWorker(final int signerId, final String signerName)
            throws Exception {

        // Create keystore TODO: Don't create an empty one
        final String keystorePath = newTempFile().getAbsolutePath();
        final String keystorePassword = "foo123";
        createEmpyKeystore(keystorePath, keystorePassword);

        final String truststorePath = newTempFile().getAbsolutePath();
        final String truststorePassword = "foo123";
        createEmpyKeystore(truststorePath, truststorePassword);

        getGlobalSession().setProperty(GlobalConfiguration.SCOPE_GLOBAL,
            "WORKER" + signerId + ".CLASSPATH",
            "org.signserver.module.renewal.worker.RenewalWorker");
        getGlobalSession().setProperty(GlobalConfiguration.SCOPE_GLOBAL,
            "WORKER" + signerId + ".SIGNERTOKEN.CLASSPATH",
            "org.signserver.server.cryptotokens.P12CryptoToken");

        getWorkerSession().setWorkerProperty(signerId, "NAME", signerName);
        getWorkerSession().setWorkerProperty(signerId, "AUTHTYPE", "NOAUTH");
        getWorkerSession().setWorkerProperty(signerId, "KEYSTOREPATH",
                keystorePath);
        getWorkerSession().setWorkerProperty(signerId, "KEYSTOREPASSWORD",
                keystorePassword);
        getWorkerSession().setWorkerProperty(signerId, "TRUSTSTOREPATH",
                truststorePath);
        getWorkerSession().setWorkerProperty(signerId, "TRUSTSTOREPASSWORD",
                truststorePassword);
        getWorkerSession().setWorkerProperty(signerId, "TRUSTSTORETYPE",
                "PKCS12");
        getWorkerSession().setWorkerProperty(signerId, "EJBCAWSURL",
                EJBCAWSURL_PREFIX);

        getWorkerSession().reloadConfiguration(signerId);
    }

}
