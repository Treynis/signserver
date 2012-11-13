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
package org.signserver.server.cryptotokens;

import java.io.BufferedInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.io.IOException;
import java.math.BigInteger;
import java.security.KeyPair;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.concurrent.TimeUnit;
import org.bouncycastle.asn1.cmp.PKIStatus;
import org.bouncycastle.asn1.x500.X500Name;
import org.bouncycastle.asn1.x509.ExtendedKeyUsage;
import org.bouncycastle.asn1.x509.KeyPurposeId;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.X509v3CertificateBuilder;
import org.bouncycastle.cert.jcajce.JcaX509v3CertificateBuilder;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.cms.SignerInformation;
import org.bouncycastle.cms.SignerInformationStore;
import org.bouncycastle.cms.jcajce.JcaSimpleSignerInfoVerifierBuilder;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.pkcs.PKCS10CertificationRequest;
import org.bouncycastle.tsp.TSPAlgorithms;
import org.bouncycastle.tsp.TimeStampRequest;
import org.bouncycastle.tsp.TimeStampRequestGenerator;
import org.bouncycastle.tsp.TimeStampResponse;
import org.bouncycastle.util.Store;
import org.bouncycastle.util.encoders.Base64;
import org.signserver.common.Base64SignerCertReqData;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.GenericSignRequest;
import org.signserver.common.GenericSignResponse;
import org.signserver.common.GlobalConfiguration;
import org.signserver.common.PKCS10CertReqInfo;
import org.signserver.common.RequestContext;
import org.signserver.common.SODSignRequest;
import org.signserver.common.SODSignResponse;
import org.signserver.common.SignServerUtil;
import org.signserver.test.utils.builders.CryptoUtils;
import org.signserver.testutils.ModulesTestCase;

/**
 * Test signing with all signers using a PKCS11CryptoToken.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class P11SignTest extends ModulesTestCase {
    
    private static final int WORKER_PDF = 20000;
    private static final int WORKER_TSA = 20001;
    private static final int WORKER_SOD = 20002;
    private static final int WORKER_CMS = 20003;
    private static final int WORKER_XML = 20004;
    private static final int WORKER_ODF = 20005;
    private static final int WORKER_OOXML = 20006;
    private static final int WORKER_MSA = 20007;
    
    private static final String MSAUTHCODE_REQUEST_DATA =
    		"MIIBIwYKKwYBBAGCNwMCATCCARMGCSqGSIb3DQEHAaCCAQQEggEAVVSpOKf9zJYc" +
    		"tyvqgeHfO9JkobPYihUZcW9TbYzAUiJGEsElNCnLUaO0+MZG0TS7hlzqKKvrdXc7" +
    		"O/8C7c8YyjYF5YrLiaYS8cw3VbaQ2M1NWsLGzxF1pxsR9sMDJvfrryPaWj4eTi3Y" +
    		"UqRNS+GTa4quX4xbmB0KqMpCtrvuk4S9cgaJGwxmSE7N3omzvERTUxp7nVSHtms5" +
    		"lVMb082JFlABT1/o2mL5O6qFG119JeuS1+ZiL1AEy//gRs556OE1TB9UEQU2bFUm" +
    		"zBD4VHvkOOB/7X944v9lmK5y9sFv+vnf/34catL1A+ZNLwtd1Qq2VirqJxRK/T61" +
    		"QoSWj4rGpw==";
    
    private final String sharedLibrary;
    private final String slot;
    private final String pin;
    private final String existingKey1;
    
    private final File pdfSampleFile;
    private final File odfSampleFile;
    private final File ooxmlSampleFile;

    public P11SignTest() {
        File home = new File(System.getenv("SIGNSERVER_HOME"));
        assertTrue("Environment variable SIGNSERVER_HOME", home.exists());
        pdfSampleFile = new File(home, "res/test/pdf/sample.pdf");
        odfSampleFile = new File(home, "res/signingtest/input/test.odt");
        ooxmlSampleFile = new File(home, "res/signingtest/input/test.docx");
        sharedLibrary = getConfig().getProperty("test.p11.sharedlibrary");
        slot = getConfig().getProperty("test.p11.slot");
        pin = getConfig().getProperty("test.p11.pin");
        existingKey1 = getConfig().getProperty("test.p11.existingkey1");
    }
    
    @Override
    protected void setUp() throws Exception {
        super.setUp();
        SignServerUtil.installBCProvider();
    }

    
    @Override
    protected void tearDown() throws Exception {
        super.tearDown();
    }
    
    private void setPDFSignerProperties(final int workerId) throws Exception {
        // Setup worker
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".CLASSPATH", "org.signserver.module.pdfsigner.PDFSigner");
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".SIGNERTOKEN.CLASSPATH", PKCS11CryptoToken.class.getName());
        workerSession.setWorkerProperty(workerId, "NAME", "PDFSignerP11");
        workerSession.setWorkerProperty(workerId, "AUTHTYPE", "NOAUTH");
        workerSession.setWorkerProperty(workerId, "SHAREDLIBRARY", sharedLibrary);
        workerSession.setWorkerProperty(workerId, "SLOT", slot);
        workerSession.setWorkerProperty(workerId, "PIN", pin);
        workerSession.setWorkerProperty(workerId, "DEFAULTKEY", existingKey1);
    }
    
    /** Tests that the getCertificateRequest method generates a request. */
    public void testGenerateCSR() throws Exception {
        try {
            setPDFSignerProperties(WORKER_PDF);
            workerSession.reloadConfiguration(WORKER_PDF);
            
            // Tests generating a CSR
            PKCS10CertReqInfo certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + WORKER_PDF, null);
            Base64SignerCertReqData csr = (Base64SignerCertReqData) getWorkerSession().getCertificateRequest(WORKER_PDF, certReqInfo, false);
            assertNotNull(csr);
            assertNotNull(csr.getBase64CertReq());
            assertTrue(csr.getBase64CertReq().length > 0);
            
            // Test for an non-existing key label
            setPDFSignerProperties(WORKER_PDF);
            workerSession.setWorkerProperty(WORKER_PDF, "DEFAULTKEY", "NON-EXISTING-KEY-LABEL");
            workerSession.reloadConfiguration(WORKER_PDF);
            try {
                certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + WORKER_PDF, null);
                getWorkerSession().getCertificateRequest(WORKER_PDF, certReqInfo, false);
                fail("Should have thrown exception as the DEFAULTKEY does not exist");
            } catch (CryptoTokenOfflineException ok) { // NOPMD
                // OK
            }
        } finally {
            removeWorker(WORKER_PDF);
        }
    }

    /**
     * Tests setting up a PDF Signer, giving it a certificate and sign a document.
     */
    public void testPDFSigner() throws Exception {
        try {
            setPDFSignerProperties(WORKER_PDF);
            workerSession.reloadConfiguration(WORKER_PDF);
            
            // Generate CSR
            PKCS10CertReqInfo certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + WORKER_PDF, null);
            Base64SignerCertReqData reqData = (Base64SignerCertReqData) getWorkerSession().getCertificateRequest(WORKER_PDF, certReqInfo, false);
            
            // Issue certificate
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.decode(reqData.getBase64CertReq()));
            KeyPair issuerKeyPair = CryptoUtils.generateRSA(512);
            X509CertificateHolder cert = new X509v3CertificateBuilder(new X500Name("CN=TestP11 Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), csr.getSubject(), csr.getSubjectPublicKeyInfo()).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));
            
            // Install certificate and chain
            workerSession.uploadSignerCertificate(WORKER_PDF, cert.getEncoded(), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.uploadSignerCertificateChain(WORKER_PDF, Arrays.asList(cert.getEncoded()), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.reloadConfiguration(WORKER_PDF);
            
            // Test active
            List<String> errors = workerSession.getStatus(WORKER_PDF).getFatalErrors();
            assertEquals("errors: " + errors, 0, errors.size());
            
            // Test signing
            signGenericDocument(WORKER_PDF, readFile(pdfSampleFile));
        } finally {
            removeWorker(WORKER_PDF);
        }
    }
    
    private byte[] readFile(File file) throws IOException {
        BufferedInputStream in = new BufferedInputStream(new FileInputStream(
                file));
        ByteArrayOutputStream bout = new ByteArrayOutputStream();
        int b;
        while ((b = in.read()) != -1) {
            bout.write(b);
        }
        return bout.toByteArray();
    }
    
    private void setTimeStampSignerProperties(final int workerId) throws IOException {
        // Setup worker
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".CLASSPATH", "org.signserver.module.tsa.TimeStampSigner");
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".SIGNERTOKEN.CLASSPATH", PKCS11CryptoToken.class.getName());
        workerSession.setWorkerProperty(workerId, "NAME", "TSSignerP11");
        workerSession.setWorkerProperty(workerId, "AUTHTYPE", "NOAUTH");
        workerSession.setWorkerProperty(workerId, "SHAREDLIBRARY", sharedLibrary);
        workerSession.setWorkerProperty(workerId, "SLOT", slot);
        workerSession.setWorkerProperty(workerId, "PIN", pin);
        workerSession.setWorkerProperty(workerId, "DEFAULTKEY", existingKey1);
        workerSession.setWorkerProperty(workerId, "DEFAULTTSAPOLICYOID", "1.2.3");
    }
    
    /**
     * Tests setting up a TimeStamp Signer, giving it a certificate and request a time-stamp token.
     */
    public void testTSSigner() throws Exception {
        try {
            setTimeStampSignerProperties(WORKER_TSA);
            workerSession.reloadConfiguration(WORKER_TSA);
            
            // Generate CSR
            PKCS10CertReqInfo certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + WORKER_TSA, null);
            Base64SignerCertReqData reqData = (Base64SignerCertReqData) getWorkerSession().getCertificateRequest(WORKER_TSA, certReqInfo, false);
            
            // Issue certificate
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.decode(reqData.getBase64CertReq()));
            KeyPair issuerKeyPair = CryptoUtils.generateRSA(512);
            X509CertificateHolder cert = new X509v3CertificateBuilder(new X500Name("CN=TestP11 Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), csr.getSubject(), csr.getSubjectPublicKeyInfo()).addExtension(org.bouncycastle.asn1.x509.X509Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping)).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));
            
            // Install certificate and chain
            workerSession.uploadSignerCertificate(WORKER_TSA, cert.getEncoded(), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.uploadSignerCertificateChain(WORKER_TSA, Arrays.asList(cert.getEncoded()), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.reloadConfiguration(WORKER_TSA);
            
            // Test active
            List<String> errors = workerSession.getStatus(WORKER_TSA).getFatalErrors();
            assertEquals("errors: " + errors, 0, errors.size());
            
            // Test signing
            TimeStampRequestGenerator timeStampRequestGenerator = new TimeStampRequestGenerator();
            TimeStampRequest timeStampRequest = timeStampRequestGenerator.generate(TSPAlgorithms.SHA1, new byte[20], BigInteger.valueOf(100));
            byte[] requestBytes = timeStampRequest.getEncoded();
            GenericSignRequest signRequest = new GenericSignRequest(567, requestBytes);
            final GenericSignResponse res = (GenericSignResponse) workerSession.process(WORKER_TSA, signRequest, new RequestContext());
            Certificate signercert = res.getSignerCertificate();
            assertNotNull(signercert);
            final TimeStampResponse timeStampResponse = new TimeStampResponse((byte[]) res.getProcessedData());
            timeStampResponse.validate(timeStampRequest);

            assertEquals("Token granted", PKIStatus.GRANTED, timeStampResponse.getStatus());
            assertNotNull("Got timestamp token", timeStampResponse.getTimeStampToken());
        } finally {
            removeWorker(WORKER_TSA);
        }
    }
    
    private void setMRTDSODSignerProperties(final int workerId) throws IOException {
        // Setup worker
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".CLASSPATH", "org.signserver.module.mrtdsodsigner.MRTDSODSigner");
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".SIGNERTOKEN.CLASSPATH", PKCS11CryptoToken.class.getName());
        workerSession.setWorkerProperty(workerId, "NAME", "SODSignerP11");
        workerSession.setWorkerProperty(workerId, "AUTHTYPE", "NOAUTH");
        workerSession.setWorkerProperty(workerId, "SHAREDLIBRARY", sharedLibrary);
        workerSession.setWorkerProperty(workerId, "SLOT", slot);
        workerSession.setWorkerProperty(workerId, "PIN", pin);
        workerSession.setWorkerProperty(workerId, "DEFAULTKEY", existingKey1);
    }
    
    /**
     * Tests setting up a MRTD SOD Signer, giving it a certificate and requests an SOd.
     */
    public void testMRTDSODSigner() throws Exception {
        final int workerId = WORKER_SOD;
        try {
            setMRTDSODSignerProperties(workerId);
            workerSession.reloadConfiguration(workerId);
            
            // Generate CSR
            PKCS10CertReqInfo certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + workerId, null);
            Base64SignerCertReqData reqData = (Base64SignerCertReqData) getWorkerSession().getCertificateRequest(workerId, certReqInfo, false);
            
            // Issue certificate
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.decode(reqData.getBase64CertReq()));
            KeyPair issuerKeyPair = CryptoUtils.generateRSA(512);
            X509CertificateHolder issuerCert = new JcaX509v3CertificateBuilder(new X500Name("CN=TestP11 Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), new X500Name("CN=TestP11 Issuer"), issuerKeyPair.getPublic()).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));
            X509CertificateHolder cert = new X509v3CertificateBuilder(new X500Name("CN=TestP11 Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), csr.getSubject(), csr.getSubjectPublicKeyInfo()).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));
            
            // Install certificate and chain
            workerSession.uploadSignerCertificate(workerId, cert.getEncoded(), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.uploadSignerCertificateChain(workerId, Arrays.asList(cert.getEncoded(), issuerCert.getEncoded()), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.reloadConfiguration(workerId);
            
            // Test active
            List<String> errors = workerSession.getStatus(workerId).getFatalErrors();
            assertEquals("errors: " + errors, 0, errors.size());
            
            // Test signing
            HashMap<Integer, byte[]> dgs = new HashMap<Integer, byte[]>();
            dgs.put(1, "Yy==".getBytes());
            dgs.put(2, "Yy==".getBytes());
            dgs.put(3, "Yy==".getBytes());
            final SODSignRequest signRequest = new SODSignRequest(233, dgs);
            final SODSignResponse res = (SODSignResponse) workerSession.process(workerId, signRequest, new RequestContext());
            Certificate signercert = res.getSignerCertificate();
            assertNotNull(signercert);
        } finally {
            removeWorker(workerId);
        }
    }
    
    private void setCMSSignerProperties(final int workerId) throws IOException {
        // Setup worker
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".CLASSPATH", "org.signserver.module.cmssigner.CMSSigner");
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".SIGNERTOKEN.CLASSPATH", PKCS11CryptoToken.class.getName());
        workerSession.setWorkerProperty(workerId, "NAME", "CMSSignerP11");
        workerSession.setWorkerProperty(workerId, "AUTHTYPE", "NOAUTH");
        workerSession.setWorkerProperty(workerId, "SHAREDLIBRARY", sharedLibrary);
        workerSession.setWorkerProperty(workerId, "SLOT", slot);
        workerSession.setWorkerProperty(workerId, "PIN", pin);
        workerSession.setWorkerProperty(workerId, "DEFAULTKEY", existingKey1);
    }
    
    /**
     * Tests setting up a CMS Signer, giving it a certificate and sign a file.
     */
    public void testCMSSigner() throws Exception {
        final int workerId = WORKER_CMS;
        try {
            setCMSSignerProperties(workerId);
            workerSession.reloadConfiguration(workerId);
            
            // Generate CSR
            PKCS10CertReqInfo certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + workerId, null);
            Base64SignerCertReqData reqData = (Base64SignerCertReqData) getWorkerSession().getCertificateRequest(workerId, certReqInfo, false);
            
            // Issue certificate
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.decode(reqData.getBase64CertReq()));
            KeyPair issuerKeyPair = CryptoUtils.generateRSA(512);
            X509CertificateHolder cert = new X509v3CertificateBuilder(new X500Name("CN=TestP11 Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), csr.getSubject(), csr.getSubjectPublicKeyInfo()).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));
            
            // Install certificate and chain
            workerSession.uploadSignerCertificate(workerId, cert.getEncoded(), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.uploadSignerCertificateChain(workerId, Arrays.asList(cert.getEncoded()), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.reloadConfiguration(workerId);
            
            // Test active
            List<String> errors = workerSession.getStatus(workerId).getFatalErrors();
            assertEquals("errors: " + errors, 0, errors.size());
            
            // Test signing
            signGenericDocument(workerId, "Sample data".getBytes());
        } finally {
            removeWorker(workerId);
        }
    }
    
    private void setXMLSignerProperties(final int workerId) throws IOException {
        // Setup worker
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".CLASSPATH", "org.signserver.module.xmlsigner.XMLSigner");
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".SIGNERTOKEN.CLASSPATH", PKCS11CryptoToken.class.getName());
        workerSession.setWorkerProperty(workerId, "NAME", "XMLSignerP11");
        workerSession.setWorkerProperty(workerId, "AUTHTYPE", "NOAUTH");
        workerSession.setWorkerProperty(workerId, "SHAREDLIBRARY", sharedLibrary);
        workerSession.setWorkerProperty(workerId, "SLOT", slot);
        workerSession.setWorkerProperty(workerId, "PIN", pin);
        workerSession.setWorkerProperty(workerId, "DEFAULTKEY", existingKey1);
    }
    
    /**
     * Tests setting up a XML Signer, giving it a certificate and sign a document.
     */
    public void testXMLSigner() throws Exception {
        final int workerId = WORKER_XML;
        try {
            setXMLSignerProperties(workerId);
            workerSession.reloadConfiguration(workerId);
            
            // Generate CSR
            PKCS10CertReqInfo certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + workerId, null);
            Base64SignerCertReqData reqData = (Base64SignerCertReqData) getWorkerSession().getCertificateRequest(workerId, certReqInfo, false);
            
            // Issue certificate
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.decode(reqData.getBase64CertReq()));
            KeyPair issuerKeyPair = CryptoUtils.generateRSA(512);
            X509CertificateHolder cert = new X509v3CertificateBuilder(new X500Name("CN=TestP11 Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), csr.getSubject(), csr.getSubjectPublicKeyInfo()).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));
            
            // Install certificate and chain
            workerSession.uploadSignerCertificate(workerId, cert.getEncoded(), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.uploadSignerCertificateChain(workerId, Arrays.asList(cert.getEncoded()), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.reloadConfiguration(workerId);
            
            // Test active
            List<String> errors = workerSession.getStatus(workerId).getFatalErrors();
            assertEquals("errors: " + errors, 0, errors.size());
            
            // Test signing
            signGenericDocument(workerId, "<sampledata/>".getBytes());
        } finally {
            removeWorker(workerId);
        }
    }
    
    // TODO: ODFSigner
    private void setODFSignerProperties(final int workerId) throws IOException {
        // Setup worker
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".CLASSPATH", "org.signserver.module.odfsigner.ODFSigner");
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".SIGNERTOKEN.CLASSPATH", PKCS11CryptoToken.class.getName());
        workerSession.setWorkerProperty(workerId, "NAME", "ODFSignerP11");
        workerSession.setWorkerProperty(workerId, "AUTHTYPE", "NOAUTH");
        workerSession.setWorkerProperty(workerId, "SHAREDLIBRARY", sharedLibrary);
        workerSession.setWorkerProperty(workerId, "SLOT", slot);
        workerSession.setWorkerProperty(workerId, "PIN", pin);
        workerSession.setWorkerProperty(workerId, "DEFAULTKEY", existingKey1);
    }
    
    /**
     * Tests setting up a ODF Signer, giving it a certificate and sign a document.
     */
    public void testODFSigner() throws Exception {
        final int workerId = WORKER_ODF;
        try {
            setODFSignerProperties(workerId);
            workerSession.reloadConfiguration(workerId);
            
            // Generate CSR
            PKCS10CertReqInfo certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + workerId, null);
            Base64SignerCertReqData reqData = (Base64SignerCertReqData) getWorkerSession().getCertificateRequest(workerId, certReqInfo, false);
            
            // Issue certificate
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.decode(reqData.getBase64CertReq()));
            KeyPair issuerKeyPair = CryptoUtils.generateRSA(512);
            X509CertificateHolder cert = new X509v3CertificateBuilder(new X500Name("CN=TestP11 Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), csr.getSubject(), csr.getSubjectPublicKeyInfo()).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));
            
            // Install certificate and chain
            workerSession.uploadSignerCertificate(workerId, cert.getEncoded(), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.uploadSignerCertificateChain(workerId, Arrays.asList(cert.getEncoded()), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.reloadConfiguration(workerId);
            
            // Test active
            List<String> errors = workerSession.getStatus(workerId).getFatalErrors();
            assertEquals("errors: " + errors, 0, errors.size());
            
            // Test signing
            signGenericDocument(workerId, readFile(odfSampleFile));
        } finally {
            removeWorker(workerId);
        }
    }
    
    private void setOOXMLSignerProperties(final int workerId) throws IOException {
        // Setup worker
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".CLASSPATH", "org.signserver.module.ooxmlsigner.OOXMLSigner");
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".SIGNERTOKEN.CLASSPATH", PKCS11CryptoToken.class.getName());
        workerSession.setWorkerProperty(workerId, "NAME", "OOXMLSignerP11");
        workerSession.setWorkerProperty(workerId, "AUTHTYPE", "NOAUTH");
        workerSession.setWorkerProperty(workerId, "SHAREDLIBRARY", sharedLibrary);
        workerSession.setWorkerProperty(workerId, "SLOT", slot);
        workerSession.setWorkerProperty(workerId, "PIN", pin);
        workerSession.setWorkerProperty(workerId, "DEFAULTKEY", existingKey1);
    }
    
    /**
     * Tests setting up a OOXML Signer, giving it a certificate and sign a document.
     */
    public void testOOXMLSigner() throws Exception {
        final int workerId = WORKER_OOXML;
        try {
            setOOXMLSignerProperties(workerId);
            workerSession.reloadConfiguration(workerId);
            
            // Generate CSR
            PKCS10CertReqInfo certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + workerId, null);
            Base64SignerCertReqData reqData = (Base64SignerCertReqData) getWorkerSession().getCertificateRequest(workerId, certReqInfo, false);
            
            // Issue certificate
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.decode(reqData.getBase64CertReq()));
            KeyPair issuerKeyPair = CryptoUtils.generateRSA(512);
            X509CertificateHolder cert = new X509v3CertificateBuilder(new X500Name("CN=TestP11 Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), csr.getSubject(), csr.getSubjectPublicKeyInfo()).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));
            
            // Install certificate and chain
            workerSession.uploadSignerCertificate(workerId, cert.getEncoded(), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.uploadSignerCertificateChain(workerId, Arrays.asList(cert.getEncoded()), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.reloadConfiguration(workerId);
            
            // Test active
            List<String> errors = workerSession.getStatus(workerId).getFatalErrors();
            assertEquals("errors: " + errors, 0, errors.size());
            
            // Test signing
            signGenericDocument(workerId, readFile(ooxmlSampleFile));
        } finally {
            removeWorker(workerId);
        }
    }

    private void setMSAuthTimeStampSignerProperties(final int workerId) throws IOException {
        // Setup worker
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".CLASSPATH", "org.signserver.module.tsa.MSAuthCodeTimeStampSigner");
        globalSession.setProperty(GlobalConfiguration.SCOPE_GLOBAL, "WORKER" + workerId + ".SIGNERTOKEN.CLASSPATH", PKCS11CryptoToken.class.getName());
        workerSession.setWorkerProperty(workerId, "NAME", "MSAuthTSSignerP11");
        workerSession.setWorkerProperty(workerId, "AUTHTYPE", "NOAUTH");
        workerSession.setWorkerProperty(workerId, "SHAREDLIBRARY", sharedLibrary);
        workerSession.setWorkerProperty(workerId, "SLOT", slot);
        workerSession.setWorkerProperty(workerId, "PIN", pin);
        workerSession.setWorkerProperty(workerId, "DEFAULTKEY", existingKey1);
        workerSession.setWorkerProperty(workerId, "DEFAULTTSAPOLICYOID", "1.2.3");
    }
    
    /**
     * Tests setting up a MSAuthCodeTimeStamp Signer, giving it a certificate and request a time-stamp token.
     */
    public void testMSAuthTSSigner() throws Exception {
        final int workerId = WORKER_MSA;
        try {
            setMSAuthTimeStampSignerProperties(workerId);
            workerSession.reloadConfiguration(workerId);
            
            // Generate CSR
            PKCS10CertReqInfo certReqInfo = new PKCS10CertReqInfo("SHA1WithRSA", "CN=Worker" + workerId, null);
            Base64SignerCertReqData reqData = (Base64SignerCertReqData) getWorkerSession().getCertificateRequest(workerId, certReqInfo, false);
            
            // Issue certificate
            PKCS10CertificationRequest csr = new PKCS10CertificationRequest(Base64.decode(reqData.getBase64CertReq()));
            KeyPair issuerKeyPair = CryptoUtils.generateRSA(512);
            X509CertificateHolder cert = new X509v3CertificateBuilder(new X500Name("CN=TestP11 Issuer"), BigInteger.ONE, new Date(), new Date(System.currentTimeMillis() + TimeUnit.DAYS.toMillis(365)), csr.getSubject(), csr.getSubjectPublicKeyInfo()).addExtension(org.bouncycastle.asn1.x509.X509Extension.extendedKeyUsage, true, new ExtendedKeyUsage(KeyPurposeId.id_kp_timeStamping)).build(new JcaContentSignerBuilder("SHA256WithRSA").setProvider("BC").build(issuerKeyPair.getPrivate()));
            
            // Install certificate and chain
            workerSession.uploadSignerCertificate(workerId, cert.getEncoded(), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.uploadSignerCertificateChain(workerId, Arrays.asList(cert.getEncoded()), GlobalConfiguration.SCOPE_GLOBAL);
            workerSession.reloadConfiguration(workerId);
            
            // Test active
            List<String> errors = workerSession.getStatus(workerId).getFatalErrors();
            assertEquals("errors: " + errors, 0, errors.size());
            
            // Test signing
            GenericSignRequest signRequest = new GenericSignRequest(678, MSAUTHCODE_REQUEST_DATA.getBytes());
            final GenericSignResponse res = (GenericSignResponse) workerSession.process(workerId, signRequest, new RequestContext());
            Certificate signercert = res.getSignerCertificate();
            assertNotNull(signercert);
            
            byte[] buf = res.getProcessedData();
            CMSSignedData s = new CMSSignedData(Base64.decode(buf));

            int verified = 0;
            Store certStore = s.getCertificates();
            SignerInformationStore signers = s.getSignerInfos();
            Collection c = signers.getSigners();
            Iterator it = c.iterator();

            while (it.hasNext()) {
                SignerInformation signer = (SignerInformation)it.next();
                Collection certCollection = certStore.getMatches(signer.getSID());
                    
                Iterator certIt = certCollection.iterator();
                X509CertificateHolder signerCert = (X509CertificateHolder) certIt.next();

                if (signer.verify(new JcaSimpleSignerInfoVerifierBuilder().setProvider("BC").build(signerCert))) {
                    verified++;
                }
            }
            
            assertEquals("signer verified", 1, verified);
            
        } finally {
            removeWorker(workerId);
        }
    }
}
