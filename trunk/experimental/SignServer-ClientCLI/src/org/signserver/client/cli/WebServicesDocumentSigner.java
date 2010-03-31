/*
 * To change this template, choose Tools | Templates
 * and open the template in the editor.
 */

package org.signserver.client.cli;

import java.io.IOException;
import java.io.OutputStream;
import java.util.Random;
import java.util.concurrent.TimeUnit;
import java.util.logging.Level;
import javax.naming.NamingException;
import org.apache.log4j.Logger;
import org.signserver.client.api.ISignServerWorker;
import org.signserver.client.api.SigningAndValidationEJB;
import org.signserver.client.api.SigningAndValidationWS;
import org.signserver.client.api.SigningAndValidationWSBalanced;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.GenericSignRequest;
import org.signserver.common.GenericSignResponse;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.ProcessResponse;
import org.signserver.common.RequestContext;
import org.signserver.common.SignServerException;


/**
 *
 * @author markus
 */
public class WebServicesDocumentSigner extends AbstractDocumentSigner {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(WebServicesDocumentSigner.class);

    private static final String ENCODING_NONE = "none";
    private static final String ENCODING_BASE64 = "base64";

    private String workerName;

    private ISignServerWorker signServer;

    private Random random = new Random();

    public WebServicesDocumentSigner(final String host, final int port,
            final String workerName) {
        this.signServer = new SigningAndValidationWS(host, port);
        this.workerName = workerName;
    }

    protected void doSign(final byte[] data, final String encoding,
            final OutputStream out) throws IllegalRequestException,
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

        final ProcessResponse response = signServer.process(workerName,
                new GenericSignRequest(requestId, data), new RequestContext());

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

//                LOG.debug("Got sign response with id "
//                        + signResponse.getRequestID() + ", archive id "
//                        + signResponse.getArchiveId()
//                        + ", signed data of length "
//                        + signResponse.getProcessedData().length + " bytes "
//                        + "signed by signer with certificate:\n"
//                        + signResponse.getSignerCertificate().toString() + ".");
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
