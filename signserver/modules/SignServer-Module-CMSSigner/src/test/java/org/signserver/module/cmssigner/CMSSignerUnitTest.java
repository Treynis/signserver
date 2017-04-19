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

import java.security.KeyPair;
import java.security.Security;
import java.security.cert.Certificate;
import java.util.Arrays;
import java.util.Date;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.cert.jcajce.JcaX509CertificateConverter;
import org.bouncycastle.cms.CMSProcessableByteArray;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.jce.provider.BouncyCastleProvider;
import org.bouncycastle.util.encoders.Hex;
import static org.junit.Assert.*;
import org.junit.BeforeClass;
import org.junit.Test;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.RequestContext;
import org.signserver.common.RequestMetadata;
import org.signserver.common.SignServerException;
import org.signserver.common.WorkerConfig;
import org.signserver.common.data.SignatureRequest;
import org.signserver.common.data.SignatureResponse;
import org.signserver.server.SignServerContext;
import org.signserver.server.data.impl.CloseableReadableData;
import org.signserver.server.data.impl.CloseableWritableData;
import org.signserver.test.utils.builders.CertBuilder;
import org.signserver.test.utils.builders.CryptoUtils;
import org.signserver.test.utils.mock.MockedCryptoToken;
import org.signserver.test.utils.mock.MockedServicesImpl;
import org.signserver.testutils.ModulesTestCase;

/**
 * Unit tests for the CMSSigner class.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class CMSSignerUnitTest {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(CMSSignerUnitTest.class);

    private static MockedCryptoToken tokenRSA;

    @BeforeClass
    public static void setUpClass() throws Exception {
        Security.addProvider(new BouncyCastleProvider());
        final KeyPair signerKeyPair;
        final String signatureAlgorithm;
        signerKeyPair = CryptoUtils.generateRSA(1024);
        signatureAlgorithm = "SHA1withRSA";
        final Certificate[] certChain =
                new Certificate[] {new JcaX509CertificateConverter().getCertificate(new CertBuilder().
                        setSelfSignKeyPair(signerKeyPair).
                        setNotBefore(new Date()).
                        setSignatureAlgorithm(signatureAlgorithm)
                        .build())};
        final Certificate signerCertificate = certChain[0];
        tokenRSA = new MockedCryptoToken(signerKeyPair.getPrivate(), signerKeyPair.getPublic(), signerCertificate, Arrays.asList(certChain), "BC");
    }

    /**
     * Test that providing an incorrect value for DETACHEDSIGNATURE
     * gives a fatal error.
     * @throws Exception
     */
    @Test
    public void testInit_incorrectDetachedSignatureValue() throws Exception {
        LOG.info("testInit_incorrectDetachedSignatureValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DETACHEDSIGNATURE", "_incorrect-value--");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        String errors = instance.getFatalErrors(new MockedServicesImpl()).toString();
        assertTrue("conf errs: " + errors, errors.contains("DETACHEDSIGNATURE"));
    }

    /**
     * Test that providing an incorrect value for ALLOW_DETACHEDSIGNATURE_OVERRIDE
     * gives a fatal error.
     * @throws Exception
     */
    @Test
    public void testInit_incorrectAllowDetachedSignatureOverrideValue() throws Exception {
        LOG.info("testInit_incorrectAllowDetachedSignatureOverrideValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("ALLOW_DETACHEDSIGNATURE_OVERRIDE", "_incorrect-value--");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        String errors = instance.getFatalErrors(new MockedServicesImpl()).toString();
        assertTrue("conf errs: " + errors, errors.contains("ALLOW_DETACHEDSIGNATURE_OVERRIDE"));
    }

    /**
     * Test that setting an incorrect OID for content OID is not allowed.
     * @throws Exception 
     */
    @Test
    public void testInit_incorrectContentOID() throws Exception {
        LOG.info("testInit_incorrectContentOID");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("CONTENTOID", "incorrect_oid");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);
        
        String errors = instance.getFatalErrors(new MockedServicesImpl()).toString();
        assertTrue("conf errs: " + errors, errors.contains("Illegal content OID specified: incorrect_oid"));
    }
    
    /**
     * Test that setting an incorrect value for ALLOW_CONTENTOID_OVERRIDE is not
     * allowed (so that it is not implicitly treated as false).
     * @throws Exception 
     */
    @Test
    public void testInit_incorrectAllowContentOIDOverride() throws Exception {
        LOG.info("testInit_incorrectAllowContentOIDOverride");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("ALLOW_CONTENTOID_OVERRIDE", "incorrect");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);
        
        String errors = instance.getFatalErrors(new MockedServicesImpl()).toString();
        assertTrue("conf errs: " + errors, errors.contains("Incorrect value for property ALLOW_CONTENTOID_OVERRIDE"));
    }
    
    /**
     * Tests that no signing is performed when the worker is misconfigured.
     * @throws java.lang.Exception
     */
    @Test(expected = SignServerException.class)
    public void testNoProcessOnFatalErrors() throws Exception {
        LOG.info("testNoProcessOnFatalErrors");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("ALLOW_DETACHEDSIGNATURE_OVERRIDE", "_incorrect-value--");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        sign(data, tokenRSA, config);
        fail("Should have thrown exception");
    }

    /**
     * Tests that not specifying the DETACHEDSIGNATURE property and not
     * saying anything in the request about detached signatures gives a
     * signature with the content encapsulated.
     * @throws java.lang.Exception
     */
    @Test
    public void testDetachedSignatureDefaultValue() throws Exception {
        LOG.info("testDetachedSignatureDefaultValue");
        WorkerConfig config = new WorkerConfig();
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        SimplifiedResponse response = sign(data, tokenRSA, config);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        CMSProcessableByteArray signedContent = (CMSProcessableByteArray) signedData.getSignedContent();
        byte[] actualData = (byte[]) signedContent.getContent();
        assertEquals(Hex.toHexString(data), Hex.toHexString(actualData));
    }

    /**
     * Tests that detached signature is not used if not specified in config and
     * that overriding it is not allowed by default.
     * @throws java.lang.Exception
     */
    @Test(expected = IllegalRequestException.class)
    public void testAllowDetachedSignatureOverrideDefaultValue() throws Exception {
        LOG.info("testAllowDetachedSignatureOverrideDefaultValue");
        WorkerConfig config = new WorkerConfig();
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();

        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("DETACHEDSIGNATURE", "true");
        sign(data, tokenRSA, config, requestContext);
        fail("Should have thrown exception as detached signature option can not be overridden");
    }
    
    /**
     * Test that providing an incorrect value for DER_RE_ENCODE
     * gives a fatal error.
     * @throws Exception
     */
    @Test
    public void testInit_incorrectDERReEncodeValue() throws Exception {
        LOG.info("testInit_incorrectDERReEncodeValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DER_RE_ENCODE", "_incorrect-value--");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        String errors = instance.getFatalErrors(new MockedServicesImpl()).toString();
        assertTrue("conf errs: " + errors, errors.contains("DER_RE_ENCODE"));
    }

    /**
     * Tests that not providing a DER_RE_ENCODE property defaults to not DER.
     * @throws Exception 
     */
    @Test
    public void testDERReEncodeDefaultValue() throws Exception {
        LOG.info("testDERReEncodeDefaultValue");
        WorkerConfig config = new WorkerConfig();
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        SimplifiedResponse response = sign(data, tokenRSA, config);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        assertNotNull(signedData);
        
        // Not in DER format by default
        final byte[] der = new ASN1InputStream(cms).readObject().getEncoded("DER");
        assertNotEquals("do not expect DER format", Hex.toHexString(der), Hex.toHexString(cms));
    }

    /**
     * Tests that setting DER_RE_ENCODE=false does not give DER encoding.
     * @throws Exception 
     */
    @Test
    public void testDERReEncodeFalse() throws Exception {
        LOG.info("testDERReEncodeFalse");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DER_RE_ENCODE", "False");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        SimplifiedResponse response = sign(data, tokenRSA, config);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        assertNotNull(signedData);
        
        // Not in DER format by default
        final byte[] der = new ASN1InputStream(cms).readObject().getEncoded("DER");
        assertNotEquals("do not expect DER format", Hex.toHexString(der), Hex.toHexString(cms));
    }

    /**
     * Tests that setting DER_RE_ENCODE=true gives DER encoding.
     * @throws Exception 
     */
    @Test
    public void testDERReEncodeTrue() throws Exception {
        LOG.info("testDERReEncodeTrue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DER_RE_ENCODE", "TruE");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        SimplifiedResponse response = sign(data, tokenRSA, config);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        assertNotNull(signedData);
        
        // Not in DER format by default
        final byte[] der = new ASN1InputStream(cms).readObject().getEncoded("DER");
        assertEquals("expect DER format", Hex.toHexString(der), Hex.toHexString(cms));
    }

    /**
     * Tests that detached signature is used if specified in config and that
     * overriding it can not be done if not allowed.
     * @throws java.lang.Exception
     */
    @Test(expected = IllegalRequestException.class)
    public void testAllowDetachedSignatureOverrideFalseDetached() throws Exception {
        LOG.info("testAllowDetachedSignatureOverrideFalseDetached");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DETACHEDSIGNATURE", "TRUE");
        config.setProperty("ALLOW_DETACHEDSIGNATURE_OVERRIDE", "FALSE");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();

        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("DETACHEDSIGNATURE", "false");
        sign(data, tokenRSA, config, requestContext);
        fail("Should have thrown exception as detached signature option can not be overridden");
    }

    /**
     * Tests that requesting no detached is okey if no detached is configured 
     * even if allow override is false.
     * @throws java.lang.Exception
     */
    @Test
    public void testDetachedSignatureFalseRequestFalse() throws Exception {
        LOG.info("testDetachedSignatureFalseRequestFalse");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DETACHEDSIGNATURE", "FALSE");
        config.setProperty("ALLOW_DETACHEDSIGNATURE_OVERRIDE", "FALSE");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("DETACHEDSIGNATURE", "false");
        SimplifiedResponse response = sign(data, tokenRSA, config, requestContext);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        CMSProcessableByteArray signedContent = (CMSProcessableByteArray) signedData.getSignedContent();
        byte[] actualData = (byte[]) signedContent.getContent();
        assertEquals(Hex.toHexString(data), Hex.toHexString(actualData));
    }

    /**
     * Tests that requesting detached is okey if detached is configured 
     * even if allow override is false.
     * @throws java.lang.Exception
     */
    @Test
    public void testDetachedSignatureTrueRequestTrue() throws Exception {
        LOG.info("testDetachedSignatureTrueRequestTrue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DETACHEDSIGNATURE", "TRUE");
        config.setProperty("ALLOW_DETACHEDSIGNATURE_OVERRIDE", "FALSE");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("DETACHEDSIGNATURE", "TRUE");
        SimplifiedResponse response = sign(data, tokenRSA, config, requestContext);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        CMSProcessableByteArray signedContent = (CMSProcessableByteArray) signedData.getSignedContent();
        assertNull("detached", signedContent);
    }

    /**
     * Tests that requesting detached is okey if allow override is set to true.
     * @throws java.lang.Exception
     */
    @Test
    public void testDetachedSignatureFalseRequestTrue() throws Exception {
        LOG.info("testDetachedSignatureFalseRequestTrue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DETACHEDSIGNATURE", "FALSE");
        config.setProperty("ALLOW_DETACHEDSIGNATURE_OVERRIDE", "TRUE");

        RequestContext requestContext = new RequestContext();

        final byte[] data = "my-data".getBytes("ASCII");
        
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("DETACHEDSIGNATURE", "TRUE");
        
        SimplifiedResponse response = sign(data, tokenRSA, config, requestContext);
        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        CMSProcessableByteArray signedContent = (CMSProcessableByteArray) signedData.getSignedContent();
        assertNull("detached", signedContent);
    }

    /**
     * Tests that requesting no detached is okey if allow override is true.
     * @throws java.lang.Exception
     */
    @Test
    public void testDetachedSignatureTrueRequestFalse() throws Exception {
        LOG.info("testDetachedSignatureTrueRequestFalse");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DETACHEDSIGNATURE", "TRUE");
        config.setProperty("ALLOW_DETACHEDSIGNATURE_OVERRIDE", "TRUE");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("DETACHEDSIGNATURE", "false");
        
        SimplifiedResponse response = sign(data, tokenRSA, config, requestContext);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        CMSProcessableByteArray signedContent = (CMSProcessableByteArray) signedData.getSignedContent();
        byte[] actualData = (byte[]) signedContent.getContent();
        assertEquals(Hex.toHexString(data), Hex.toHexString(actualData));
    }

    /**
     * Tests that requesting with empty string is the same as not requesting.
     * @throws java.lang.Exception
     */
    @Test
    public void testDetachedSignatureTrueRequestEmpty() throws Exception {
        LOG.info("testDetachedSignatureTrueRequestEmpty");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("DETACHEDSIGNATURE", "TRUE");
        config.setProperty("ALLOW_DETACHEDSIGNATURE_OVERRIDE", "FALSE");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("DETACHEDSIGNATURE", "");
        SimplifiedResponse response = sign(data, tokenRSA, config, requestContext);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        CMSProcessableByteArray signedContent = (CMSProcessableByteArray) signedData.getSignedContent();
        assertNull("detached", signedContent);
    }
    
    /**
     * Test that by default, the PKCS#7 signed data OID is used.
     * @throws java.lang.Exception
     */
    @Test
    public void testContentOIDDefaultValue() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        SimplifiedResponse response = sign(data, tokenRSA, config);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        assertEquals("content OID", "1.2.840.113549.1.7.1",
                     signedData.getSignedContentTypeOID());
    }
    
    /**
     * Test overriding content OID using worker property.
     * @throws java.lang.Exception
     */
    @Test
    public void testContentOIDInConfiguration() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("CONTENTOID", "1.2.3.4");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        SimplifiedResponse response = sign(data, tokenRSA, config);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        assertEquals("content OID", "1.2.3.4",
                     signedData.getSignedContentTypeOID());
    }
    
    /**
     * Test overriding content OID in request.
     * @throws java.lang.Exception
     */
    @Test
    public void testContentOIDOverride() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("ALLOW_CONTENTOID_OVERRIDE", "true");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("CONTENTOID", "1.2.3.4");
        SimplifiedResponse response = sign(data, tokenRSA, config, requestContext);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        assertEquals("content OID", "1.2.3.4",
                     signedData.getSignedContentTypeOID());
    }
    
    /**
     * Test overriding content OID in request has higher priority than specified
     * in configuration.
     * @throws java.lang.Exception
     */
    @Test
    public void testContentOIDOverrideAndInConfiguration() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("CONTENTOID", "1.2.3.4");
        config.setProperty("ALLOW_CONTENTOID_OVERRIDE", "TRUE");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("CONTENTOID", "1.2.3.5");
        SimplifiedResponse response = sign(data, tokenRSA, config, requestContext);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        assertEquals("content OID", "1.2.3.5",
                     signedData.getSignedContentTypeOID());
    }
    
    /**
     * Test overriding content OID is not allowed by default.
     * @throws java.lang.Exception
     */
    @Test
    public void testDefaulDontAllowOverridingContentOID() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("CONTENTOID", "1.2.3.5");
        
        try {
            sign(data, tokenRSA, config, requestContext);
            fail("Should throw IllegalRequestException");
        } catch (IllegalRequestException e) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getName());
        }
    }
    
    /**
     * Test overriding content OID is not allowed by default with a content OID
     * specified in the configuration.
     * @throws java.lang.Exception
     */
    @Test
    public void testDontAllowOverridingContentOIDWithContentOIDInConfig() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("CONTENTOID", "1.2.3.4");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("CONTENTOID", "1.2.3.5");
        
        try {
            sign(data, tokenRSA, config, requestContext);
            fail("Should throw IllegalRequestException");
        } catch (IllegalRequestException e) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getName());
        }
    }
    
    /**
     * Test overriding content OID in request with the default OID value is
     * accepted even when not accepting override.
     * @throws java.lang.Exception
     */
    @Test
    public void testOverrideWithDefaultContentOID() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("CONTENTOID", "1.2.840.113549.1.7.1");
        SimplifiedResponse response = sign(data, tokenRSA, config, requestContext);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        assertEquals("content OID", "1.2.840.113549.1.7.1",
                     signedData.getSignedContentTypeOID());
    }
    
    /**
     * Test overriding content OID in request with the specified value from the
     * configuration is accepted even when not accepting override.
     * @throws java.lang.Exception
     */
    @Test
    public void testOverrideWithSpecifiedContentOIDFromConfiguration() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("CONTENTOID", "1.2.3.4");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("CONTENTOID", "1.2.3.4");
        SimplifiedResponse response = sign(data, tokenRSA, config, requestContext);

        byte[] cms = response.getProcessedData();
        CMSSignedData signedData = new CMSSignedData(cms);
        assertEquals("content OID", "1.2.3.4",
                     signedData.getSignedContentTypeOID());
    }
    
    /**
     * Test overriding content OID is not allowed when explicitly configuring.
     * not allowing override.
     * @throws java.lang.Exception
     */
    @Test
    public void testDontAllowOverridingContentOIDExplicit() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("ALLOW_CONTENTOID_OVERRIDE", "false");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("CONTENTOID", "1.2.3.5");
        
        try {
            sign(data, tokenRSA, config, requestContext);
            fail("Should throw IllegalRequestException");
        } catch (IllegalRequestException e) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getName());
        }
    }
    
    /**
     * Test overriding content OID is not allowed with a content OID
     * specified in the configuration also when explicitly configuring not
     * allowing override.
     * @throws java.lang.Exception
     */
    @Test
    public void testDontAllowOverridingContentOIDWithContentOIDInConfigExplicit() throws Exception {
        LOG.info("testContentOIDDefaultValue");
        WorkerConfig config = new WorkerConfig();
        config.setProperty("CONTENTOID", "1.2.3.4");
        config.setProperty("ALLOW_CONTENTOID_OVERRIDE", "FALSE");
        CMSSigner instance = new MockedCMSSigner(tokenRSA);
        instance.init(1, config, new SignServerContext(), null);

        final byte[] data = "my-data".getBytes("ASCII");
        RequestContext requestContext = new RequestContext();
        RequestMetadata metadata = RequestMetadata.getInstance(requestContext);
        metadata.put("CONTENTOID", "1.2.3.5");
        
        try {
            sign(data, tokenRSA, config, requestContext);
            fail("Should throw IllegalRequestException");
        } catch (IllegalRequestException e) {
            // expected
        } catch (Exception e) {
            fail("Unexpected exception: " + e.getClass().getName());
        }
    }
    
    private SimplifiedResponse sign(final byte[] data, MockedCryptoToken token, WorkerConfig config) throws Exception {
        return sign(data, token, config, null);
    }
    
    private SimplifiedResponse sign(final byte[] data, MockedCryptoToken token, WorkerConfig config, RequestContext requestContext) throws Exception {
        MockedCMSSigner instance = new MockedCMSSigner(token);
        instance.init(1, config, new SignServerContext(), null);

        if (requestContext == null) {
            requestContext = new RequestContext();
        }
        requestContext.put(RequestContext.TRANSACTION_ID, "0000-100-1");

        try (
                CloseableReadableData requestData = ModulesTestCase.createRequestData(data);
                CloseableWritableData responseData = ModulesTestCase.createResponseData(false);
            ) {
            SignatureRequest request = new SignatureRequest(100, requestData, responseData);
            SignatureResponse response = (SignatureResponse) instance.processData(request, requestContext);

            byte[] signedBytes = responseData.toReadableData().getAsByteArray();
            Certificate signerCertificate = response.getSignerCertificate();
            return new SimplifiedResponse(signedBytes, signerCertificate);
        }
    }
}
