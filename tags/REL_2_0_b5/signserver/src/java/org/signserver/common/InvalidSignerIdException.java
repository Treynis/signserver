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

 
package org.signserver.common;

/**
 * Exception thrown requesting a signer that doesn't exists
 * 
 * @author Philip Vendil
 * $id$
 */

public class InvalidSignerIdException extends Exception {

	private static final long serialVersionUID = 1L;

	public InvalidSignerIdException(String message) {
		super(message);
	}
	
	public String getMessage() {
		return super.getMessage();
	}


}
