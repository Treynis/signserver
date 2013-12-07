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
package org.signserver.validationservice.server;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import java.net.URLConnection;
import java.security.NoSuchProviderException;
import java.security.cert.*;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DEREnumerated;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.x509.extension.X509ExtensionUtil;
import org.signserver.common.SignServerException;

/**
 * Utility functions used by validators.
 *
 * @author rayback2
 * @version $Id$
 */
public class ValidationUtils {
    
    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(ValidationUtils.class);

    /**
     * retrieve X509CRL from specified URL
     * 
     * @param url
     * @return the downloaded CRL
     * @throws CertificateException
     * @throws NoSuchProviderException
     * @throws CRLException
     * @throws IOException
     * @throws SignServerException
     */
    public static X509CRL fetchCRLFromURL(URL url) throws SignServerException {

        CertificateFactory certFactory = null;
        try {
            certFactory = CertificateFactory.getInstance("X509", "BC");
        } catch (CertificateException e) {
            throw new SignServerException(
                    "Error creating BC CertificateFactory provider", e);
        } catch (NoSuchProviderException e) {
            throw new SignServerException(
                    "Error creating BC CertificateFactory provider", e);
        }
        return fetchCRLFromURLwithRetry(url, certFactory, 3, 100);
    }

    private static X509CRL fetchCRLFromURLwithRetry(URL url, CertificateFactory certFactory, int retries, long waitTime) throws SignServerException {
        X509CRL result = null;
        SignServerException lastException = null;
        for (int i = 0; i < retries && result == null; i++) {
            try {
                if (LOG.isDebugEnabled()) {
                    LOG.debug("Fetching CRL from: " + url);
                }
                result = fetchCRLFromURL(url, certFactory);
            } catch (SignServerException ex) {
                lastException = ex;
                LOG.info("CRL fetch (" + (i + 1) + " of " + retries + ")" + " failed: " + ex.getMessage());
                try {
                    Thread.sleep(waitTime);
                } catch (InterruptedException ignored) {
                    break;
                }
            }
        }
        if (result == null && lastException != null) {
            throw lastException;
        }
        return result;
    }

    /**
     * retrieve X509CRL from specified URL, uses passed in CertificateFactory
     * 
     * @throws SignServerException
     */
    public static X509CRL fetchCRLFromURL(URL url,
            CertificateFactory certFactory) throws SignServerException {
        URLConnection connection;
        try {
                connection = url.openConnection();
            } catch (IOException e) {
                throw new SignServerException(
                        "Error opening connection for fetching CRL from address : "
                        + url.toString(), e);
            }
            connection.setDoInput(true);

            byte[] responsearr = null;
            InputStream reader = null;
            try {
                try {
                    reader = connection.getInputStream();
                } catch (IOException e) {
                    throw new SignServerException(
                            "Error getting input stream for fetching CRL from address : "
                            + url.toString(), e);
                }
                int responselen = connection.getContentLength();

                if (responselen != -1) {

                    // header indicating content-length is present, so go ahead and use
                    // it
                    responsearr = new byte[responselen];

                    int offset = 0;
                    int bread;
                    try {
                        while ((responselen > 0)
                                && (bread = reader.read(responsearr, offset,
                                responselen)) != -1) {
                            offset += bread;
                            responselen -= bread;
                        }
                    } catch (IOException e) {
                        throw new SignServerException(
                                "Error reading CRL bytes from address : "
                                + url.toString(), e);
                    }

                    // read.read returned -1 but we expect inputstream to contain more
                    // data
                    // is it a dreadful unexpected EOF we were afraid of ??
                    if (responselen > 0) {
                        throw new SignServerException(
                                "Unexpected EOF encountered while reading crl from : "
                                + url.toString());
                    }
                } else {
                    // getContentLength() returns -1. no panic , perfect normal value if
                    // header indicating length is missing (javadoc)
                    // try to read response manually byte by byte (small response
                    // expected , no need to buffer)
                    ByteArrayOutputStream baos = new ByteArrayOutputStream();
                    int b;
                    try {
                        while ((b = reader.read()) != -1) {
                            baos.write(b);
                        }
                    } catch (IOException e) {
                        throw new SignServerException(
                                "Error reading input stream for fetching CRL from address (no length header): "
                                + url.toString(), e);
                    }

                    responsearr = baos.toByteArray();
                }
            } finally {
                if (reader != null) {
                    try {
                        reader.close();
                    } catch (IOException ex) {
                        LOG.info("Could not close stream after reading CRL", ex);
                    }
                }
            }
        
            ByteArrayInputStream bis = new ByteArrayInputStream(responsearr);
            X509CRL crl;
            try {
                crl = (X509CRL) certFactory.generateCRL(bis);
            } catch (CRLException e) {
                throw new SignServerException(
                        "Error creating CRL object with bytes from address : "
                        + url.toString(), e);
            }
  
        return crl;
    }

    public static int getReasonCodeFromCRLEntry(X509CRLEntry crlEntry)
            throws IOException {
        // retrieve reason
        byte[] reasonBytes = crlEntry.getExtensionValue(X509Extension.reasonCode.getId());
        if (reasonBytes == null) {
            // if null then unspecified (RFC 3280)
            return CRLReason.unspecified;
        }

        DEREnumerated reasonCode = (DEREnumerated) X509ExtensionUtil.fromExtensionValue(reasonBytes);

        return reasonCode.getValue().intValue();
    }
}