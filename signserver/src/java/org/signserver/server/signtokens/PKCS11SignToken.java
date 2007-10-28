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

package org.signserver.server.signtokens;
 
import java.util.Properties;


import org.apache.log4j.Logger;
import org.ejbca.core.model.ca.catoken.PKCS11CAToken;
import org.signserver.common.WorkerConfig;


/**
 * Class used to connect to a PKCS11 HSM.
 * 
 * Properties:
 *   sharedLibrary
 *   slot
 *   defaultKey
 *   pin
 * 
 * 
 * @see org.signserver.server.signtokens.ISignToken
 * @author Tomas Gustavsson, Philip Vendil
 * @version $Id: PKCS11SignToken.java,v 1.2 2007-10-28 12:27:15 herrvendil Exp $
 */

public class PKCS11SignToken extends CATokenSignTokenBase implements ISignToken{

	private static final Logger log = Logger.getLogger(PKCS11SignToken.class);
	
	public PKCS11SignToken() throws InstantiationException{
		catoken = new PKCS11CAToken(); 
	}

	/**
	 * Method initializing the PKCS11 device 
	 * 
	 */
	public void init(Properties props) {
		log.debug(">init");
		String signaturealgoritm = props.getProperty(WorkerConfig.SIGNERPROPERTY_SIGNATUREALGORITHM);
		props = fixUpProperties(props);
		try { 
			((PKCS11CAToken)catoken).init(props, null, signaturealgoritm);	
		} catch(Exception e) {
			log.error("Error initializing PKCS11SignToken : " + e.getMessage(),e);
		}
		String authCode = props.getProperty("pin");
		if (authCode != null) {
			try { 
				this.activate(authCode);
			} catch(Exception e) {
				log.error("Error auto activating PKCS11SignToken : " + e.getMessage(),e);
			}
		}
		log.debug("<init");
	}

}
