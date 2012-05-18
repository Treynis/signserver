/*************************************************************************
 *                                                                       *
 *  EJBCA: The OpenSource Certificate Authority                          *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.ejbca.ui.web.protocol;

import java.security.PrivateKey;
import java.security.interfaces.RSAPublicKey;

/**
 * @author lars
 * @version $Id: CardKeys.java 5585 2008-05-01 20:55:00Z anatom $
 */
public interface CardKeys {

	/**
	 * @param publicKey
	 * @return
	 * @throws Exception
	 */
	PrivateKey getPrivateKey(RSAPublicKey publicKey) throws Exception;
    /**
     * @param authCode
     * @throws InterruptedException
     */
    void autenticate( String authCode) throws InterruptedException;
	/**
	 * Check if key is OK (verifies PIN).
	 * @param publicKey 
	 * @return
	 */
	boolean isOK(RSAPublicKey publicKey);
}
