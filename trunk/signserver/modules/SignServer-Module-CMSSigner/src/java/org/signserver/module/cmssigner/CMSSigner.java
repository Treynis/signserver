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

import java.io.IOException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.cms.*;
import org.bouncycastle.cms.jcajce.JcaSignerInfoGeneratorBuilder;
import org.bouncycastle.operator.ContentSigner;
import org.bouncycastle.operator.OperatorCreationException;
import org.bouncycastle.operator.jcajce.JcaContentSignerBuilder;
import org.bouncycastle.operator.jcajce.JcaDigestCalculatorProviderBuilder;
import org.signserver.common.*;
import org.signserver.server.WorkerContext;
import org.signserver.server.cryptotokens.ICryptoToken;
import org.signserver.server.signers.BaseSigner;

/**
 * A Signer signing arbitrary content and produces the result in
 * Cryptographic Message Syntax (CMS) - RFC 3852.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class CMSSigner extends BaseSigner {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(CMSSigner.class);

    /** Content-type for the produced data. */
    private static final String CONTENT_TYPE = "application/pkcs7-signature";
    
    @Override
    public void init(final int workerId, final WorkerConfig config,
            final WorkerContext workerContext, final EntityManager workerEM) {
        super.init(workerId, config, workerContext, workerEM);
    }

    public ProcessResponse processData(final ProcessRequest signRequest,
            final RequestContext requestContext) throws IllegalRequestException,
            CryptoTokenOfflineException, SignServerException {

        ProcessResponse signResponse;
        ISignRequest sReq = (ISignRequest) signRequest;

        // Check that the request contains a valid GenericSignRequest object
        // with a byte[].
        if (!(signRequest instanceof GenericSignRequest)) {
            throw new IllegalRequestException(
                    "Recieved request wasn't a expected GenericSignRequest.");
        }
        if (!(sReq.getRequestData() instanceof byte[])) {
            throw new IllegalRequestException(
                    "Recieved request data wasn't a expected byte[].");
        }

        byte[] data = (byte[]) sReq.getRequestData();
        final String archiveId = createArchiveId(data, (String) requestContext.get(RequestContext.TRANSACTION_ID));

        // Get certificate chain and signer certificate
        Collection<Certificate> certs = this.getSigningCertificateChain();
        if (certs == null) {
            throw new IllegalArgumentException(
                    "Null certificate chain. This signer needs a certificate.");
        }
        List<X509Certificate> x509CertChain = new LinkedList<X509Certificate>();
        for (Certificate cert : certs) {
            if (cert instanceof X509Certificate) {
                x509CertChain.add((X509Certificate) cert);
                LOG.debug("Adding to chain: "
                        + ((X509Certificate) cert).getSubjectDN());
            }
        }
        Certificate cert = this.getSigningCertificate();
        LOG.debug("SigningCert: " + ((X509Certificate) cert).getSubjectDN());

        // Private key
        PrivateKey privKey
                = getCryptoToken().getPrivateKey(ICryptoToken.PURPOSE_SIGN);

        try {
            final CMSSignedDataGenerator generator
                    = new CMSSignedDataGenerator();
            final ContentSigner contentSigner = new JcaContentSignerBuilder(getDefaultSignatureAlgorithm(cert.getPublicKey())).setProvider(getCryptoToken().getProvider(ICryptoToken.PROVIDERUSAGE_SIGN)).build(privKey);
            generator.addSignerInfoGenerator(new JcaSignerInfoGeneratorBuilder(
                     new JcaDigestCalculatorProviderBuilder().setProvider("BC").build())
                     .build(contentSigner, (X509Certificate) cert));
                      
            generator.addCertificates(new JcaCertStore(certs));
            final CMSTypedData content = new CMSProcessableByteArray(data);
            final CMSSignedData signedData = generator.generate(content, true);

            final byte[] signedbytes = signedData.getEncoded();

            if (signRequest instanceof GenericServletRequest) {
                signResponse = new GenericServletResponse(sReq.getRequestID(),
                        signedbytes, getSigningCertificate(), archiveId,
                        new ArchiveData(signedbytes),
                        CONTENT_TYPE);
            } else {
                signResponse = new GenericSignResponse(sReq.getRequestID(),
                        signedbytes, getSigningCertificate(), archiveId,
                        new ArchiveData(signedbytes));
            }
            
            // Suggest new file name
            final Object fileNameOriginal = requestContext.get(RequestContext.FILENAME);
            if (fileNameOriginal instanceof String) {
                requestContext.put(RequestContext.RESPONSE_FILENAME, fileNameOriginal + ".p7s");
            }
            
            return signResponse;
        } catch (OperatorCreationException ex) {
            LOG.error("Error initializing signer", ex);
            throw new SignServerException("Error initializing signer", ex);
        } catch (CertificateEncodingException ex) {
            LOG.error("Error constructing cert store", ex);
            throw new SignServerException("Error constructing cert store", ex);
        } catch (CMSException ex) {
            LOG.error("Error constructing CMS", ex);
            throw new SignServerException("Error constructing CMS", ex);
        } catch (IOException ex) {
            LOG.error("Error constructing CMS", ex);
            throw new SignServerException("Error constructing CMS", ex);
        }
    }

    private String getDefaultSignatureAlgorithm(final PublicKey publicKey) {
        final String result;
        if (publicKey instanceof ECPublicKey) {
            result = "SHA1withECDSA";
        }  else if (publicKey instanceof DSAPublicKey) {
            result = "SHA1withDSA";
        } else {
            result = "SHA1withRSA";
        }
        return result;
    }
}
