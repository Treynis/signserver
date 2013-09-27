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
package org.signserver.module.xades.signer;

import java.io.ByteArrayInputStream;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.NoSuchAlgorithmException;
import java.security.Security;
import java.security.cert.CertStore;
import java.security.cert.Certificate;
import java.security.cert.CollectionCertStoreParameters;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Date;
import java.util.HashSet;
import java.util.Set;

import javax.persistence.EntityManager;
import javax.xml.crypto.dsig.SignatureMethod;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import org.apache.log4j.Logger;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.junit.BeforeClass;
import org.junit.Test;
import static org.junit.Assert.*;
import org.signserver.common.GenericSignRequest;
import org.signserver.common.GenericSignResponse;
import org.signserver.common.RequestContext;
import org.signserver.common.WorkerConfig;
import org.signserver.module.xades.signer.MockedTimeStampTokenProvider.MockedTimeStampVerificationProvider;
import org.signserver.server.WorkerContext;
import org.signserver.server.cryptotokens.ICryptoToken;
import org.signserver.test.utils.builders.CertBuilder;
import org.signserver.test.utils.builders.CryptoUtils;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

import xades4j.properties.AllDataObjsCommitmentTypeProperty;
import xades4j.properties.QualifyingProperties;
import xades4j.properties.SignedDataObjectProperty;
import xades4j.properties.SignedProperties;
import xades4j.providers.CertificateValidationProvider;
import xades4j.providers.impl.PKIXCertificateValidationProvider;
import xades4j.verification.SignatureSpecificVerificationOptions;
import xades4j.verification.XAdESVerificationResult;
import xades4j.verification.XadesVerificationProfile;
import xades4j.verification.XadesVerifier;

/**
 * Unit tests for the XAdESSigner class.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class XAdESSignerUnitTest {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(XAdESSignerUnitTest.class);
    
    private static MockedCryptoToken tokenRSA;
    private static MockedCryptoToken tokenDSA;
    private static MockedCryptoToken tokenECDSA;
    
    @BeforeClass
    public static void setUpClass() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        tokenRSA = generateToken(KeyType.RSA);
        tokenDSA = generateToken(KeyType.DSA);
        tokenECDSA = generateToken(KeyType.ECDSA);
    }

    private enum KeyType {
        RSA,
        DSA,
        ECDSA
    };
    
    private static MockedCryptoToken generateToken(final KeyType keyType) throws Exception {
        final KeyPair signerKeyPair;
        final String signatureAlgorithm;
        
        switch (keyType) {
        case RSA:
            signerKeyPair = CryptoUtils.generateRSA(1024);
            signatureAlgorithm = "SHA1withRSA";
            break;
        case DSA:
            signerKeyPair = CryptoUtils.generateDSA(1024);
            signatureAlgorithm = "SHA1withDSA";
            break;
        case ECDSA:
            signerKeyPair = CryptoUtils.generateEcCurve("prime256v1");
            signatureAlgorithm = "SHA1withECDSA";
            break;
        default:
            throw new NoSuchAlgorithmException("Invalid key algorithm");
        }
        
        final Certificate[] certChain =
                new Certificate[] {new JcaX509CertificateConverter().getCertificate(new CertBuilder().
                        setSelfSignKeyPair(signerKeyPair).
                        setNotBefore(new Date(MockedTimeStampTokenProvider.TIMESTAMP)).
                        setSignatureAlgorithm(signatureAlgorithm)
                        .build())};
        final Certificate signerCertificate = certChain[0];
        return new MockedCryptoToken(signerKeyPair.getPrivate(), signerKeyPair.getPublic(), signerCertificate, Arrays.asList(certChain), "BC");
            
    }
    
    /**
     * Test of init method, of class XAdESSigner.
     */
    @Test
    public void testInit_ok() {
        LOG.info("init");
        int signerId = 4711;
        WorkerConfig config = new WorkerConfig();
        config.setProperty("XADESFORM", "T");
        config.setProperty("TSA_URL", "http://example.com/?test=5");
        config.setProperty("TSA_USERNAME", "username123");
        config.setProperty("TSA_PASSWORD", "password123");
        
        WorkerContext workerContext = null;
        EntityManager em = null;
        XAdESSigner instance = new MockedXAdESSigner(tokenRSA);
        instance.init(signerId, config, workerContext, em);
        
        XAdESSignerParameters param = instance.getParameters();
        
        assertEquals("XADESFORM", "T", param.getXadesForm().name());
        assertEquals("TSA_URL", "http://example.com/?test=5", param.getTsaParameters().getUrl());
        assertEquals("TSA_USERNAME", "username123", param.getTsaParameters().getUsername());
        assertEquals("TSA_PASSWORD", "password123", param.getTsaParameters().getPassword());
        
        assertEquals(Collections.EMPTY_LIST, instance.getFatalErrors());
    }
    
    /**
     * Test of init method with incorrect XADESFORM, of class XAdESSigner.
     */
    @Test
    public void testInit_incorrectXADESFORM() {
        LOG.info("init");
        int signerId = 4711;
        WorkerConfig config = new WorkerConfig();
        config.setProperty("XADESFORM", "_NonExisting_");
        config.setProperty("TSA_URL", "http://example.com/?test=5");
        config.setProperty("TSA_USERNAME", "username123");
        config.setProperty("TSA_PASSWORD", "password123");
        
        WorkerContext workerContext = null;
        EntityManager em = null;
        XAdESSigner instance = new MockedXAdESSigner(tokenRSA);
        instance.init(signerId, config, workerContext, em);
        
        String errors = instance.getFatalErrors().toString();
        assertTrue("error: " + errors, errors.contains("XADESFORM"));
    }
    
    /**
     * Test of init method with missing TSA_URL, of class XAdESSigner.
     */
    @Test
    public void testInit_missingTSA_URL() {
        LOG.info("init");
        int signerId = 4711;
        WorkerConfig config = new WorkerConfig();
        config.setProperty("XADESFORM", "T");
        // Not set: config.setProperty("TSA_URL", ...
        config.setProperty("TSA_USERNAME", "username123");
        config.setProperty("TSA_PASSWORD", "password123");
        
        WorkerContext workerContext = null;
        EntityManager em = null;
        XAdESSigner instance = new MockedXAdESSigner(tokenRSA);
        instance.init(signerId, config, workerContext, em);
        
        String errors = instance.getFatalErrors().toString();
        assertTrue("error: " + errors, errors.contains("TSA_URL"));
    }
    
    /**
     * Test of init method default value for XADESFORM, of class XAdESSigner.
     */
    @Test
    public void testInit_defaultXADESFORM() {
        LOG.info("init");
        int signerId = 4711;
        WorkerConfig config = new WorkerConfig();
        // Not set: config.setProperty("XADESFORM", "T");
        
        WorkerContext workerContext = null;
        EntityManager em = null;
        XAdESSigner instance = new MockedXAdESSigner(tokenRSA);
        instance.init(signerId, config, workerContext, em);
        
        XAdESSignerParameters param = instance.getParameters();
        
        assertEquals("XADESFORM", "BES", param.getXadesForm().name());
        
        assertEquals(Collections.EMPTY_LIST, instance.getFatalErrors());
    }

    /**
     * Run a signing test with default form and varying commitment types.
     * 
     * @param keyType Token key type to use
     * @param signatureAlgorithm Signature algorithm property value to test, if null use default
     * @param expectedSignatureAlgorithmUri Expected XML signature algorithm URI
     * @param commitmentTypesProperty COMMITMENT_TYPES property to test with
     *                                if null, doesn't set the property
     * @param expectedCommitmentTypeUris List of expected commitment type URIs
     * @throws Exception
     */
    private void testProcessData_basicSigningInternal(final KeyType keyType, final String signatureAlgorithm,
            final String expectedSignatureAlgorithmUri, final String commitmentTypesProperty,
            final Collection<String> expectedCommitmentTypeUris) throws Exception {
        LOG.info("processData");

        final MockedCryptoToken token;
        
        switch (keyType) {
        case RSA:
            token = tokenRSA;
            break;
        case DSA:
            token = tokenDSA;
            break;
        case ECDSA:
            token = tokenECDSA;
            break;
        default:
            throw new NoSuchAlgorithmException("Unknown key algorithm");
        }
        
        XAdESSigner instance = new MockedXAdESSigner(token);
        WorkerConfig config = new WorkerConfig();
        
        if (commitmentTypesProperty != null) {
            config.setProperty("COMMITMENT_TYPES", commitmentTypesProperty);
        }
        
        if (signatureAlgorithm != null) {
            config.setProperty("SIGNATUREALGORITHM", signatureAlgorithm);
        }
        
        instance.init(4711, config, null, null);
        
        RequestContext requestContext = new RequestContext();
        requestContext.put(RequestContext.TRANSACTION_ID, "0000-100-1");
        GenericSignRequest request = new GenericSignRequest(100, "<test100/>".getBytes("UTF-8"));
        GenericSignResponse response = (GenericSignResponse) instance.processData(request, requestContext);
        
        byte[] data = response.getProcessedData();
        final String signedXml = new String(data);
        LOG.debug("signedXml: " + signedXml);
        
        // Validation: setup
        CertStore certStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(token.getCertificateChain(ICryptoToken.PURPOSE_SIGN)));
        KeyStore trustAnchors = KeyStore.getInstance("JKS");
        trustAnchors.load(null, "foo123".toCharArray());
        trustAnchors.setCertificateEntry("cert", token.getCertificate(ICryptoToken.PURPOSE_SIGN));

        CertificateValidationProvider certValidator = new PKIXCertificateValidationProvider(trustAnchors, false, certStore);

        XadesVerificationProfile p = new XadesVerificationProfile(certValidator);
        XadesVerifier verifier = p.newVerifier();
        
        // Validation: parse
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(new ByteArrayInputStream(data));
        Element node = doc.getDocumentElement();
        
        XAdESVerificationResult r = verifier.verify(node, new SignatureSpecificVerificationOptions());

        assertEquals("BES", r.getSignatureForm().name());
        assertEquals("Unexpected signature algorithm in signature", expectedSignatureAlgorithmUri, r.getSignatureAlgorithmUri());

        final QualifyingProperties qp = r.getQualifyingProperties();
        
        final Set<String> foundUris = new HashSet<String>();
        
        final SignedProperties sp = qp.getSignedProperties();
        
        for (final SignedDataObjectProperty signedObjProp : sp.getDataObjProps()) {
            LOG.debug("object property: " + signedObjProp.getClass().getName() + ": " + signedObjProp.toString());
            
            if (signedObjProp instanceof AllDataObjsCommitmentTypeProperty) {
                final AllDataObjsCommitmentTypeProperty commitmentType =
                        (AllDataObjsCommitmentTypeProperty) signedObjProp;
                
                final String uri = commitmentType.getUri();
                LOG.debug("Found commitment type: " + uri);
                if (expectedCommitmentTypeUris.contains(uri)) {
                    foundUris.add(uri);
                } else {
                    fail("Unexpected commitment type: " + uri);
                }
            }
        }
        
        assertTrue("Should contain expected commitment types: " + expectedCommitmentTypeUris.toString(),
                foundUris.size() == expectedCommitmentTypeUris.size());
    }
    
    /**
     * Test of processData method for basic signing, of class XAdESSigner.
     * Test that by default, no commitment types are included.
     * Also test that the default signature algorithm is SHA256withRSA for an RSA key.
     */
    @Test
    public void testProcessData_basicSigning() throws Exception {
        testProcessData_basicSigningInternal(KeyType.RSA,
                null, XAdESSigner.SIGNATURE_METHOD_RSA_SHA256,
                null, Collections.<String>emptyList());
    }
    
    /**
     * Test with explicitly setting a single commitment type.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningSingleCommitmentType() throws Exception {
        testProcessData_basicSigningInternal(KeyType.RSA, 
                null, XAdESSigner.SIGNATURE_METHOD_RSA_SHA256,
                "PROOF_OF_ORIGIN", Collections.singletonList(AllDataObjsCommitmentTypeProperty.proofOfOrigin().getUri()));
    }
    
    /**
     * Test with explicitly setting multiple commitment types.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningMultipleCommitmentTypes() throws Exception {
        testProcessData_basicSigningInternal(KeyType.RSA, 
                null, XAdESSigner.SIGNATURE_METHOD_RSA_SHA256,
                "PROOF_OF_APPROVAL, PROOF_OF_ORIGIN",
                Arrays.asList(AllDataObjsCommitmentTypeProperty.proofOfApproval().getUri(),
                              AllDataObjsCommitmentTypeProperty.proofOfOrigin().getUri()));
    }
    
    /**
     * Test with explictly setting the value NONE.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningCommitmentTypesNone() throws Exception {
        testProcessData_basicSigningInternal(KeyType.RSA,
                null, XAdESSigner.SIGNATURE_METHOD_RSA_SHA256,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test signing with signature algorithm SHA1withRSA.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningRSASHA1() throws Exception {
        testProcessData_basicSigningInternal(KeyType.RSA,
                "SHA1withRSA", SignatureMethod.RSA_SHA1,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test signing with signature algorithm SHA256withRSA.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningRSASHA256() throws Exception {
        testProcessData_basicSigningInternal(KeyType.RSA,
                "SHA256withRSA", XAdESSigner.SIGNATURE_METHOD_RSA_SHA256,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test signing with signature algorithm SHA384withRSA.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningRSASHA384() throws Exception {
        testProcessData_basicSigningInternal(KeyType.RSA,
                "SHA384withRSA", XAdESSigner.SIGNATURE_METHOD_RSA_SHA384,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test signing with signature algorithm SHA512withRSA.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningRSASHA512() throws Exception {
        testProcessData_basicSigningInternal(KeyType.RSA,
                "SHA512withRSA", XAdESSigner.SIGNATURE_METHOD_RSA_SHA512,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test signing with signature algorithm SHA1withDSA.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningDSASHA1() throws Exception {
        testProcessData_basicSigningInternal(KeyType.DSA,
                "SHA1withDSA", SignatureMethod.DSA_SHA1,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test signing with signature algorithm SHA1withECDSA.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningECDSASHA1() throws Exception {
        testProcessData_basicSigningInternal(KeyType.ECDSA,
                "SHA1withECDSA", XAdESSigner.SIGNATURE_METHOD_ECDSA_SHA1,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test signing with signature algorithm SHA256withECDSA.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningECDSASHA256() throws Exception {
        testProcessData_basicSigningInternal(KeyType.ECDSA,
                "SHA256withECDSA", XAdESSigner.SIGNATURE_METHOD_ECDSA_SHA256,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test signing with signature algorithm SHA384withECDSA.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningECDSASHA384() throws Exception {
        testProcessData_basicSigningInternal(KeyType.ECDSA,
                "SHA384withECDSA", XAdESSigner.SIGNATURE_METHOD_ECDSA_SHA384,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test signing with signature algorithm SHA512withECDSA.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningECDSASHA512() throws Exception {
        testProcessData_basicSigningInternal(KeyType.ECDSA,
                "SHA512withECDSA", XAdESSigner.SIGNATURE_METHOD_ECDSA_SHA512,
                "NONE", Collections.<String>emptyList());
    }
    
    /**
     * Test with an empty COMMITMENT_TYPES list.
     * 
     * @throws Exception
     */
    @Test
    public void testProcessData_basicSigningNoCommitmentType() throws Exception {
        LOG.info("testProcessData_basicSigningNoCommitmentType");
        int signerId = 4711;
        WorkerConfig config = new WorkerConfig();
        config.setProperty("COMMITMENT_TYPES", "");
        
        WorkerContext workerContext = null;
        EntityManager em = null;
        XAdESSigner instance = new MockedXAdESSigner(tokenRSA);
        instance.init(signerId, config, workerContext, em);
        
        String errors = instance.getFatalErrors().toString();
        assertTrue("error: " + errors, errors.contains("can not be empty"));
    }
    
    @Test
    public void testProcessData_basicSigningXAdESFormT() throws Exception {
        LOG.info("testProcessData_basicSigningXAdESFormT");

        XAdESSigner instance = new MockedXAdESSigner(tokenRSA);
        WorkerConfig config = new WorkerConfig();
        
        config.setProperty("XADESFORM", "T");
        config.setProperty("TSA_URL", "http://example.com/?test=5");
        
        instance.init(4711, config, null, null);
        instance.setTimeStampTokenProviderImplementation(MockedTimeStampTokenProvider.class);
        
        // reset mock counters
        MockedTimeStampTokenProvider.reset();
        
        RequestContext requestContext = new RequestContext();
        requestContext.put(RequestContext.TRANSACTION_ID, "0000-100-1");
        GenericSignRequest request = new GenericSignRequest(100, "<test100/>".getBytes("UTF-8"));
        GenericSignResponse response = (GenericSignResponse) instance.processData(request, requestContext);
        
        byte[] data = response.getProcessedData();
        final String signedXml = new String(data);
        LOG.debug("signedXml: " + signedXml);
        
        // Validation: setup
        CertStore certStore = CertStore.getInstance("Collection", new CollectionCertStoreParameters(tokenRSA.getCertificateChain(ICryptoToken.PURPOSE_SIGN)));
        KeyStore trustAnchors = KeyStore.getInstance("JKS");
        trustAnchors.load(null, "foo123".toCharArray());
        trustAnchors.setCertificateEntry("cert", tokenRSA.getCertificate(ICryptoToken.PURPOSE_SIGN));
        
        CertificateValidationProvider certValidator = new PKIXCertificateValidationProvider(trustAnchors, false, certStore);
        
        XadesVerificationProfile p =
                new XadesVerificationProfile(certValidator).withTimeStampTokenVerifier(new MockedTimeStampVerificationProvider());
        XadesVerifier verifier = p.newVerifier();
        
        // Validation: parse
        final DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(true);
        final DocumentBuilder builder = factory.newDocumentBuilder();
        final Document doc = builder.parse(new ByteArrayInputStream(data));
        Element node = doc.getDocumentElement();
        
        XAdESVerificationResult r = verifier.verify(node, new SignatureSpecificVerificationOptions());
        
        LOG.debug("signature form: " + r.getSignatureForm().name());
        assertEquals("T", r.getSignatureForm().name());
        assertEquals("http://www.w3.org/2001/04/xmldsig-more#rsa-sha256", r.getSignatureAlgorithmUri());
        
        // check that a time stamp token was requested
        assertTrue("Should request a time stamp token", MockedTimeStampTokenProvider.hasRequestedTimeStampToken());
        
        // check that the time stamp token was verified
        assertTrue("Should try to verify timestamp", MockedTimeStampTokenProvider.hasPerformedTimeStampVerification());
    }

    /**
     * Test that setting an unknown commitment type results in a configuration error.
     * 
     * @throws Exception
     */
    @Test
    public void testUnknownCommitmentType() throws Exception {
        LOG.info("testUnknownCommitmentType");
        int signerId = 4711;
        WorkerConfig config = new WorkerConfig();
        config.setProperty("COMMITMENT_TYPES", "foobar");
        
        WorkerContext workerContext = null;
        EntityManager em = null;
        XAdESSigner instance = new MockedXAdESSigner(tokenRSA);
        instance.init(signerId, config, workerContext, em);
        
        String errors = instance.getFatalErrors().toString();
        assertTrue("error: " + errors, errors.contains("commitment type"));
    }
    
    /**
     * Test that setting an unknown commitment type in combination with
     * a valid one results in a configuration error.
     * 
     * @throws Exception
     */
    @Test
    public void testUnknownAndKnownCommitmentType() throws Exception {
        LOG.info("testUnknownCommitmentType");
        int signerId = 4711;
        WorkerConfig config = new WorkerConfig();
        config.setProperty("COMMITMENT_TYPES", "PROOF_OF_ORIGIN, foobar");
        
        WorkerContext workerContext = null;
        EntityManager em = null;
        XAdESSigner instance = new MockedXAdESSigner(tokenRSA);
        instance.init(signerId, config, workerContext, em);
        
        String errors = instance.getFatalErrors().toString();
        assertTrue("error: " + errors, errors.contains("commitment type"));
    }
}
