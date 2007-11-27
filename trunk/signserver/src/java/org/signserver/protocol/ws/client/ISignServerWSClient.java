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

package org.signserver.protocol.ws.client;

import java.util.List;

import org.signserver.protocol.ws.ProcessResponseWS;
import org.signserver.protocol.ws.ProcessRequestWS;

/**
 * Interface that should be implemented by all SignService WebService
 * clients. It contains two main methods, the first one used to signData
 * the other one to initialize the client.
 * 
 * It's up to the implementing class to take care of high-availability
 * related functionality according to a policy that should be added to
 * the SignServerWSClientFactory
 * 
 * 
 * @author Philip Vendil
 *
 * @version $Id: ISignServerWSClient.java,v 1.1 2007-11-27 06:05:11 herrvendil Exp $
 */
public interface ISignServerWSClient {
   
	/**
     * Method used to initialize a SignServer client with a given
     * high availablity policy.
     * 
     * @param host to connect to
     * @param port to connect to
     * @param timeOut in milliseconds
     * @param wSDLURI the URL to the WSDL of the service appended to the host and port.
     * @param useHTTPS if HTTPS should be used to connect to the server. 
     */
	void init(String[] hosts, int port, int timeOut, 
         String wSDLURI, boolean useHTTPS);

	/**
	 * The main method used to send process requests to a sign server.
	 * 
	 * It's up the implementing class to take care of the High-Availability according to
	 * the policy.
	 * 
	 * @param requests a list of requests to process
	 * @param errorCallback an interface to which all the problems are reported
	 * this is mainly used to report problems when connecting to nodes. 
	 * @return list of ProcessResponse or null if all nodes are down or didn't respond in time.
	 */
	List<ProcessResponseWS> process(String workerId, List<ProcessRequestWS> requests, IFaultCallback errorCallback);
	


}