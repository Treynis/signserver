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
package org.signserver.module.cmssigner;

import java.io.File;
import java.io.FileOutputStream;
import java.security.cert.CertSelector;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.util.Collection;

import org.apache.log4j.Logger;
import org.bouncycastle.x509.AttributeCertificateHolder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.jce.X509Principal;
import org.signserver.common.GenericSignRequest;
import org.signserver.common.GenericSignResponse;
import org.signserver.common.RequestContext;
import org.signserver.common.SignServerUtil;
import org.signserver.testutils.ModulesTestCase;
import org.signserver.testutils.TestUtils;
import org.signserver.testutils.TestingSecurityManager;

/**
 * Tests for CMSSigner.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class CMSSignerTest extends ModulesTestCase {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(CMSSignerTest.class);
	
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SignServerUtil.installBCProvider();
    }

    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
        TestingSecurityManager.remove();
    }	
	
    public void test00SetupDatabase() throws Exception {
        addSigner("org.signserver.module.cmssigner.CMSSigner");
    }

    /**
     * Tests that the signer can produce a CMS structure and that it returns
     * the signer's certficate and that it is included in the structure and
     * that it can be used to verify the signature and that the signed content
     * also is included. Also test that the default signature algorithm is SHA1withRSA
     * @throws Exception In case of error.
     */
    public void test01BasicCMSSignRSA() throws Exception {
        LOG.debug(">test01BasicCMSSignRSA");

        testBasicCMSSign(null, "1.3.14.3.2.26", "1.2.840.113549.1.1.1");
        
        LOG.debug("<test01BasicCMSSignRSA");
    }
    
    /**
     * Test setting SIGNATUREALGORITHM to a non-default value.
     * @throws Exception
     */
    public void test02BasicCMSSignSHA256withRSA() throws Exception {
        testBasicCMSSign("SHA256withRSA", "2.16.840.1.101.3.4.2.1", "1.2.840.113549.1.1.1");
    }
    
    private void testBasicCMSSign(final String sigAlg, final String expectedDigAlgOID,
            final String expectedEncAlgOID) throws Exception {
        final int reqid = 37;

        final String testDocument = "Something to sign...123";

        final GenericSignRequest signRequest =
                new GenericSignRequest(reqid, testDocument.getBytes());

        // override signature algorithm if set
        if (sigAlg != null) {
            workerSession.setWorkerProperty(getSignerIdDummy1(), CMSSigner.SIGNATUREALGORITHM, sigAlg);
            workerSession.reloadConfiguration(getSignerIdDummy1());
        }
        
        final GenericSignResponse res =
                (GenericSignResponse) workerSession.process(getSignerIdDummy1(),
                    signRequest, new RequestContext());
        final byte[] data = res.getProcessedData();
   
        // Answer to right question
        assertSame("Request ID", reqid, res.getRequestID());

        // Output for manual inspection
        final FileOutputStream fos = new FileOutputStream(
                new File(getSignServerHome(),
                "tmp" + File.separator + "signedcms_" + sigAlg + ".p7s"));
        fos.write((byte[]) data);
        fos.close();

        // Check certificate returned
        final Certificate signercert = res.getSignerCertificate();
        assertNotNull("Signer certificate", signercert);

        // Check that the signed data contains the document (i.e. not detached)
        final CMSSignedData signedData = new CMSSignedData(data);
        final byte[] content = (byte[]) signedData.getSignedContent()
                .getContent();
        assertEquals("Signed document", testDocument, new String(content));

        // Get signers
        final Collection signers = signedData.getSignerInfos().getSigners();
        final SignerInformation signer
                = (SignerInformation) signers.iterator().next();

        // Verify using the signer's certificate
        assertTrue("Verification using signer certificate",
                signer.verify(signercert.getPublicKey(), "BC"));

        // Check that the signer's certificate is included
        CertStore certs = signedData.getCertificatesAndCRLs("Collection", "BC");
        X509Principal issuer = new X509Principal(signer.getSID().getIssuer());
        CertSelector cs = new AttributeCertificateHolder(issuer, signer.getSID().getSerialNumber());
        Collection<? extends Certificate> signerCerts
                = certs.getCertificates(cs);
        assertEquals("One certificate included", 1, signerCerts.size());
        assertEquals(signercert, signerCerts.iterator().next());

        // check that the default signature algorithm is SHA1withRSA
        assertEquals("Digest algorithm", expectedDigAlgOID, signer.getDigestAlgorithmID().getAlgorithm().getId());
        assertEquals("Encryption algorithm", expectedEncAlgOID, signer.getEncryptionAlgOID());   
    }

    /**
     * Remove the workers created etc.
     * @throws Exception in case of error
     */
    public void test99TearDownDatabase() throws Exception {
        removeWorker(getSignerIdDummy1());
    }
}
