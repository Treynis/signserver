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
package org.signserver.test.performance.impl;

import java.io.IOException;
import java.io.OutputStream;
import java.net.URL;
import java.net.URLConnection;
import java.util.Random;
import java.util.concurrent.TimeUnit;

import org.apache.log4j.Logger;
import org.signserver.test.performance.FailedException;
import org.signserver.test.performance.Task;

/**
 * 
 * Invoke a document signer
 * 
 * @author Marcus Lundblad
 * @version $Id: PDFSign.java 3424 2013-04-08 11:33:56Z malu9369 $
 *
 */
public class DocumentSigner implements Task {
    private static Logger LOG = Logger.getLogger(DocumentSigner.class);
    
    private static final String CRLF = "\r\n";
    private static final String BOUNDARY = "------------------signserver";
    
    private String url;

    private String workerNameOrId;
    private byte[] data;

    public DocumentSigner(final String url, final byte[] data, final String workerNameOrId, final Random random) {
        this.url = url;
        this.workerNameOrId = workerNameOrId;
        this.data = data;
    }
    
    @Override
    public long run() throws FailedException {
        try {
            return pdfRequest();
        } catch (IOException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("Could not create request", ex);
            }
            throw new FailedException("Could not create request: " + ex.getMessage());
        }
    }
    
    private long pdfRequest() throws IOException {
        URL url;
        URLConnection urlConn;

        url = new URL(this.url);

        // Take start time
        final long startMillis = System.currentTimeMillis();
        final long startTime = System.nanoTime();
        if (LOG.isDebugEnabled()) {
            LOG.debug("Sending request at: " + startMillis);
        }

        urlConn = url.openConnection();

        urlConn.setDoInput(true);
        urlConn.setDoOutput(true);
        urlConn.setUseCaches(false);
        
        final StringBuilder sb = new StringBuilder();
        sb.append("--" + BOUNDARY);
        sb.append(CRLF);
        
        String workerName = null;
        int workerId = 0;
        
        OutputStream out = null;
        
        try {
            workerId = Integer.parseInt(workerNameOrId);
        } catch (NumberFormatException e) {
            workerName = workerNameOrId;
        }
        
        if (workerName == null) {
            sb.append("Content-Disposition: form-data; name=\"workerId\"");
            sb.append(CRLF);
            sb.append(CRLF);
            sb.append(workerId);
        } else {
            sb.append("Content-Disposition: form-data; name=\"workerName\"");
            sb.append(CRLF);
            sb.append(CRLF);
            sb.append(workerName);
        }
        sb.append(CRLF);
        
        sb.append("\"");
        sb.append(CRLF);
        sb.append("Content-Type: application/octet-stream");
        sb.append(CRLF);
        sb.append("Content-Transfer-Encoding: binary");
        sb.append(CRLF);
        sb.append(CRLF);

        urlConn.addRequestProperty("Content-Type",
                "multipart/form-data; boundary=" + BOUNDARY);
        urlConn.addRequestProperty("Content-Length", String.valueOf(
                sb.toString().length() + BOUNDARY.length() + 8-1));
        
        out = urlConn.getOutputStream();
        
        out.write(sb.toString().getBytes());
        out.write(data);
        
        out.write(("\r\n--" + BOUNDARY + "--\r\n").getBytes());
        out.flush();
        
        // Take stop time
        final long estimatedTime = System.nanoTime() - startTime;
        final long timeInMillis = TimeUnit.NANOSECONDS.toMillis(estimatedTime);
        
        return timeInMillis;
    }

}
