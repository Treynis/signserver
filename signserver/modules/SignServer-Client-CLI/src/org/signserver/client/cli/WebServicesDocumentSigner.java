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
package org.signserver.client.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import org.apache.log4j.Logger;
import org.signserver.client.api.ISignServerWorker;
import org.signserver.client.api.SigningAndValidationWS;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.GenericSignRequest;
import org.signserver.common.GenericSignResponse;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.ProcessResponse;
import org.signserver.common.RequestContext;
import org.signserver.common.SignServerException;


/**
 * DocumentSigner using the Web Services interface.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class WebServicesDocumentSigner extends AbstractDocumentSigner {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(WebServicesDocumentSigner.class);

    private String workerName;
    private String pdfPassword;

    private ISignServerWorker signServer;

    private Random random = new Random();

    public WebServicesDocumentSigner(final String host, final int port,
            final String workerName, final boolean useHTTPS, 
            final String username, final String password, final String pdfPassword) {
        this.signServer = new SigningAndValidationWS(host, port, useHTTPS, 
                username, password);
        this.workerName = workerName;
        this.pdfPassword = pdfPassword;
    }

    protected void doSign(final byte[] data, final String encoding,
            final OutputStream out, final Map<String, Object> requestContext)
            throws IllegalRequestException,
                CryptoTokenOfflineException, SignServerException,
                IOException {

        final int requestId = random.nextInt();

        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending sign request with id " + requestId
                    + " containing data of length " + data.length + " bytes"
                    + " to worker " + workerName);
        }

        // Take start time
        final long startTime = System.nanoTime();

        // RequestContext is used by this API to transfer the metadata
        RequestContext context = new RequestContext();
        Map<String, String> metadata = new HashMap<String, String>();
        metadata.put(RequestContext.METADATA_PDFPASSWORD, pdfPassword);
        metadata.put(RequestContext.FILENAME, (String) requestContext.get(RequestContext.FILENAME));
        context.put(RequestContext.REQUEST_METADATA, metadata);
        
        final ProcessResponse response = signServer.process(workerName,
                new GenericSignRequest(requestId, data), context);

        // Take stop time
        final long estimatedTime = System.nanoTime() - startTime;

        if(response instanceof GenericSignResponse) {
            final GenericSignResponse signResponse =
                    (GenericSignResponse) response;

            if (LOG.isDebugEnabled()) {
                LOG.debug(String.format("Got sign response with id %d, "
                        + "archive id %d, signed data of length %d bytes "
                        + "signed by signer with certificate:\n%s.",
                        signResponse.getRequestID(),
                        signResponse.getArchiveId(),
                        signResponse.getProcessedData().length,
                        signResponse.getSignerCertificate()));
            }

            // Write the signed data
            out.write(signResponse.getProcessedData());

            LOG.info("Processing took "
                    + TimeUnit.NANOSECONDS.toMillis(estimatedTime) + " ms");
        } else {
            throw new SignServerException("Unexpected response type: "
                    + response.getClass().getName());
        }
    }
}
