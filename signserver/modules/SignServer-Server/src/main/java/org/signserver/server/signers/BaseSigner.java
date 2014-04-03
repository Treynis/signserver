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
package org.signserver.server.signers;

import java.io.IOException;
import java.security.InvalidAlgorithmParameterException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.CertStoreException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Collection;
import java.util.LinkedList;
import java.util.List;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.bouncycastle.cert.X509CertificateHolder;
import org.bouncycastle.cert.jcajce.JcaCertStore;
import org.bouncycastle.util.Selector;
import org.bouncycastle.util.Store;
import org.signserver.common.*;
import org.signserver.server.BaseProcessable;
import org.signserver.server.KeyUsageCounterHash;
import org.signserver.server.ValidityTimeUtils;
import org.signserver.server.WorkerContext;
import org.signserver.server.cryptotokens.ICryptoToken;
import org.signserver.server.entities.KeyUsageCounter;

/**
 * Base class that all signers can extend to cover basic in common
 * functionality.
 *
 * @author Philip Vendil
 * @version $Id$
 */
public abstract class BaseSigner extends BaseProcessable implements ISigner {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(BaseSigner.class);
   
    protected int includeCertificateLevels;
    protected boolean hasSetIncludeCertificateLevels;
    
    private List<String> configErrors = new LinkedList<String>();

    
    @Override
    public void init(int workerId, WorkerConfig config,
            WorkerContext workerContext, EntityManager workerEM) {
        super.init(workerId, config, workerContext, workerEM);
        
        final String includeCertificateLevelsProperty = config.getProperties().getProperty(WorkerConfig.PROPERTY_INCLUDE_CERTIFICATE_LEVELS);

        if (includeCertificateLevelsProperty != null) {
            hasSetIncludeCertificateLevels = true;
            
            try {
                includeCertificateLevels = Integer.parseInt(includeCertificateLevelsProperty);
                if (includeCertificateLevels < 0) {
                    configErrors.add("Illegal value for property " + WorkerConfig.PROPERTY_INCLUDE_CERTIFICATE_LEVELS + ". Only numbers >= 0 supported.");
                }
            } catch (NumberFormatException ex) {
                configErrors.add("Unable to parse property " + WorkerConfig.PROPERTY_INCLUDE_CERTIFICATE_LEVELS + ". Only numbers >= 0 supported: " + ex.getLocalizedMessage());
            }
        }
    }

    /**
     * @see org.signserver.server.IProcessable#getStatus()
     */
    @Override
    public WorkerStatus getStatus(final List<String> additionalFatalErrors) {
        SignerStatus retval;
        final List<String> fatalErrors = new LinkedList<String>(additionalFatalErrors);
        fatalErrors.addAll(getFatalErrors());

        final boolean keyUsageCounterDisabled = config.getProperty(SignServerConstants.DISABLEKEYUSAGECOUNTER, "FALSE").equalsIgnoreCase("TRUE");
        
        ICryptoToken token = null;
        try {
            token = getCryptoToken();
        } catch (SignServerException ex) {
            // getFatalErrors will pick up crypto token errors gathered
            // during creation of the crypto token
        }
        
        try {
            final Certificate cert = getSigningCertificate();
            final long keyUsageLimit = Long.valueOf(config.getProperty(SignServerConstants.KEYUSAGELIMIT, "-1"));
            final int status;
            if (token == null) {
                status = SignerStatus.STATUS_OFFLINE;
            } else {
                status = token.getCryptoTokenStatus();
            }

            if (cert != null) { 
                KeyUsageCounter counter = getSignServerContext().getKeyUsageCounterDataService().getCounter(KeyUsageCounterHash.create(cert.getPublicKey()));
                if ((counter == null && !keyUsageCounterDisabled) 
                        || (keyUsageLimit != -1 && status == CryptoTokenStatus.STATUS_ACTIVE && (counter == null || counter.getCounter() >= keyUsageLimit))) {
                    fatalErrors.add("Key usage limit exceeded or not initialized");
                }

                if (counter != null) {
                    retval = new SignerStatus(workerId, status, fatalErrors, new ProcessableConfig(config), cert, counter.getCounter());
                } else {
                    retval = new SignerStatus(workerId, status, fatalErrors, new ProcessableConfig(config), cert);
                }
            } else {
                retval = new SignerStatus(workerId, status, fatalErrors, new ProcessableConfig(config), cert);
            }
        } catch (CryptoTokenOfflineException e) {
            try {
                retval = new SignerStatus(workerId, getCryptoToken().getCryptoTokenStatus(),
                        fatalErrors, new ProcessableConfig(config), null);
            } catch (SignServerException e2) {
                // the error will have been picked up by getCryptoTokenFatalErrors already
                retval = new SignerStatus(workerId, SignerStatus.STATUS_OFFLINE, fatalErrors, new ProcessableConfig(config), null);
            }
        } catch (NumberFormatException ex) {
            try {
                fatalErrors.add("Incorrect value in worker property " +
                        SignServerConstants.KEYUSAGELIMIT + ": " + ex.getMessage());
                retval = new SignerStatus(workerId, getCryptoToken().getCryptoTokenStatus(),
                        fatalErrors, new ProcessableConfig(config), null);
            } catch (SignServerException e) {
                fatalErrors.add("Failed to get crypto token: " + e.getMessage());
                retval = new SignerStatus(workerId, SignerStatus.STATUS_OFFLINE, fatalErrors, new ProcessableConfig(config), null);
            }
        }
        retval.setKeyUsageCounterDisabled(keyUsageCounterDisabled);
        return retval;
    }

    @Override
    protected List<String> getFatalErrors() {
        final LinkedList<String> errors = new LinkedList<String>(super.getFatalErrors());
        errors.addAll(getSignerCertificateFatalErrors());
        errors.addAll(configErrors);
        return errors;
    }

    /**
     * Checks that the signer certificate is available and that it matches the 
     * key-pair in the crypto token and that the time is within the signer's 
     * validity.
     * The errors returned from this method is included in the list of errors
     * returned from getFatalErrors().
     * Signer implementation can override this method and just return an empty 
     * list if they don't require a signer certificate to be present.
     *
     * @return List of errors or an empty list in case of no errors
     */
    protected List<String> getSignerCertificateFatalErrors() {
        final LinkedList<String> result = new LinkedList<String>(super.getFatalErrors());
        // Check if certificate matches key
        Certificate certificate = null;
        try {
            certificate = getSigningCertificate();
            final ICryptoToken token = getCryptoToken();
            if (token == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Signer " + workerId + ": No crypto token");
                }
                result.add("No crypto token available");
            } else if (certificate == null) {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Signer " + workerId + ": No certificate");
                }
                result.add("No signer certificate available");
            } else {
                if (Arrays.equals(certificate.getPublicKey().getEncoded(),
                        getCryptoToken().getPublicKey(
                        ICryptoToken.PURPOSE_SIGN).getEncoded())) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Signer " + workerId + ": Certificate matches key");
                    }
                } else {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Signer " + workerId + ": Certificate does not match key");
                    }
                    result.add("Certificate does not match key");
                }
            }
        } catch (CryptoTokenOfflineException ex) {
            result.add("No signer certificate available");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Signer " + workerId + ": Could not get signer certificate: " + ex.getMessage());
            }
        } catch (SignServerException e) {
            result.add("Could not get crypto token");
            if (LOG.isDebugEnabled()) {
                LOG.debug("Signer " + workerId + ": Could not get crypto token: " + e.getMessage());
            }
        }
        
        // add any eventual crypto token fatal errors gathered in BaseProcessable        
        result.addAll(getCryptoTokenFatalErrors());

        // Check signer validity
        if (certificate instanceof X509Certificate) {
            try {
                ValidityTimeUtils.checkSignerValidity(workerId, getConfig(), (X509Certificate) certificate);
            } catch (CryptoTokenOfflineException ex) {
                result.add(ex.getMessage());
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Signer " + workerId + ": Signer certificate validity time check failed: " + ex.getMessage());
                }
            }
        }

        if (!hasSetIncludeCertificateLevels || includeCertificateLevels > 0) {
            // Check that certificiate chain contains the signer certificate
            try {
                getCertStoreWithChain(certificate);
            } catch (NoSuchAlgorithmException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (NoSuchProviderException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (CertStoreException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (IOException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (CertificateEncodingException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (InvalidAlgorithmParameterException ex) {
                result.add("Unable to get certificate chain");
                LOG.error("Signer " + workerId + ": Unable to get certificate chain: " + ex.getMessage());
            } catch (CryptoTokenOfflineException ex) {
                // error messages will be handle by BaseProcessable::getCryptoToken
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Signer " + workerId + ": Could not get signer certificate in chain: " + ex.getMessage());
                }
            }
        }

        return result;
    }
    
    protected Store getCertStoreWithChain(Certificate signingCert) throws InvalidAlgorithmParameterException, NoSuchAlgorithmException, NoSuchProviderException, CryptoTokenOfflineException, CertStoreException, CertificateEncodingException, IOException {
        List<Certificate> signingCertificateChain = getSigningCertificateChain();
        
        if (signingCertificateChain == null) {
            throw new CryptoTokenOfflineException("Certificate chain not available");
        } else {
            JcaCertStore certStore =
                    new JcaCertStore(includedCertificates(signingCertificateChain));

            if (!containsCertificate(certStore, signingCert) &&
                (!hasSetIncludeCertificateLevels || includeCertificateLevels > 0)) {
                throw new CryptoTokenOfflineException("Signer certificate not included in certificate chain");
            }
            return certStore;
        }
    }
    
    /**
     * @return True if the CertStore contained the Certificate
     */
    private boolean containsCertificate(final Store store, final Certificate subject) throws CertStoreException, IOException, CertificateEncodingException {
        final X509CertificateHolder cert = new X509CertificateHolder(subject.getEncoded());
        final Collection<?> matchedCerts = store.getMatches(new Selector() {
            
            @Override
            public boolean match(Object obj) {
                return cert.equals(obj);
            }
            
            @Override
            public Object clone() {
                return this;
            }
        });
        return matchedCerts.size() > 0;
    }

    /**
     * Given a list of certificates, return a list of at most the number given by INCLUDE_CERTIFICATE_LEVELS
     * from the beginning. If the list is shorter than the property specifies, return the entire list.
     * 
     * @param certs List of certificates
     * @return List of at most the desired certificate levels to include.
     */
    protected List<Certificate> includedCertificates(final List<Certificate> certs) {
        if (hasSetIncludeCertificateLevels) {
            return certs.subList(0, Math.min(includeCertificateLevels, certs.size()));
        } else {
            return certs;
        }
    }
}
