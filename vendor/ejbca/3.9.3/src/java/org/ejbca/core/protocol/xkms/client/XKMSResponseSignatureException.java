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

package org.ejbca.core.protocol.xkms.client;

/**
 * Exception throws in a XKMS service signature
 * cannot be verified.
 * 
 * 
 * @author Philip Vendil 2006 dec 20
 *
 * @version $Id: XKMSResponseSignatureException.java 5585 2008-05-01 20:55:00Z anatom $
 */

public class XKMSResponseSignatureException extends Exception {

	public XKMSResponseSignatureException() {
		super();
	}

	public XKMSResponseSignatureException(String arg0, Throwable arg1) {
		super(arg0, arg1);
	}

	public XKMSResponseSignatureException(String arg0) {
		super(arg0);
	}

	public XKMSResponseSignatureException(Throwable arg0) {
		super(arg0);
	}

}
