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

package org.signserver.rmiClient;
 
import org.signserver.common.ISignRequest;
import org.signserver.common.ISignResponse;

/**
 * @author lars
 *
 */
public interface ISigner {
    /**
     * @param signerID
     * @param request
     * @return
     */
    ISignResponse signData(int signerID, ISignRequest request);
    /**
     * @return
     */
    String ping();
}
