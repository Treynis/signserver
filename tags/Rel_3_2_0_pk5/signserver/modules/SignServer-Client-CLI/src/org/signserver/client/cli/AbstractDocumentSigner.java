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
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.SignServerException;

/**
 *
 * @author Markus Kilas
 * @version $Id$
 */
public abstract class AbstractDocumentSigner implements DocumentSigner {

    private static final String ENCODING_NONE = "none";
    private static final String ENCODING_BASE64 = "base64";

    public AbstractDocumentSigner() {
    }

    public void sign(final byte[] data, final String encoding, 
            final OutputStream out, final Map<String,Object> requestContext)
            throws IllegalRequestException, CryptoTokenOfflineException,
            SignServerException, IOException {
        doSign(data, encoding, out, requestContext);
    }

    public void sign(final byte[] data, final String encoding,
            final Map<String,Object> requestContext) throws IllegalRequestException, CryptoTokenOfflineException, SignServerException, IOException {
        sign(data, encoding, System.out, requestContext);
    }

    public void sign(final byte[] data) throws IllegalRequestException, CryptoTokenOfflineException, SignServerException, IOException {
        sign(data, ENCODING_NONE, new HashMap<String, Object>());
    }

    public void sign(final byte[] data, final OutputStream out,
            final Map<String, Object> requestContext) throws
            IllegalRequestException, CryptoTokenOfflineException,
            SignServerException, IOException {
        doSign(data, ENCODING_NONE, out, requestContext);
    }

    protected abstract void doSign(final byte[] data, final String encoding,
            final OutputStream out, final Map<String,Object> requestContext)
            throws IllegalRequestException,
                CryptoTokenOfflineException, SignServerException,
                IOException;

}
