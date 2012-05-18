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
package org.ejbca.core.protocol.ws.common;

import java.security.cert.CertificateException;

import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;

/**
 * Class used to generate a java.security.Certificate from a 
 * org.ejbca.core.protocol.ws.common.Certificate
 * 
 * @author Philip Vendil
 *
 * $Id: CertificateHelper.java 5631 2008-05-22 11:46:54Z anatom $
 */
public class CertificateHelper {

	/**
	 * Indicates that the requester want a BASE64 encoded certificate in the CertificateResponse object.
	 */
	public static String RESPONSETYPE_CERTIFICATE    = "CERTIFICATE";
	/**
	 * Indicates that the requester want a BASE64 encoded pkcs7 in the CertificateResponse object.
	 */
	public static String RESPONSETYPE_PKCS7          = "PKCS7";
	/**
	 * Indicates that the requester want a BASE64 encoded pkcs7 with the complete chain in the CertificateResponse object.
	 */
	public static String RESPONSETYPE_PKCS7WITHCHAIN = "PKCS7WITHCHAIN";
	
	/**
	 * Method that builds a certificate from the data in the WS response.
	 */
	public static java.security.cert.Certificate getCertificate(byte[] certificateData) throws CertificateException{
        java.security.cert.Certificate retval = CertTools.getCertfromByteArray(Base64.decode(certificateData)); 
        return retval; 
	}
	
	/**
	 * Simple method that just returns raw PKCS7 data instead of the BASE64 encoded contained in
	 * the WS response
	 */
	public static byte[] getPKCS7(byte[] pkcs7Data) {
		return Base64.decode(pkcs7Data);
	}
	
	
}
