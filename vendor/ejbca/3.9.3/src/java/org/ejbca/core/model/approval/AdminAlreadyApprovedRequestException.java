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
package org.ejbca.core.model.approval;

/**
 * Exception thrown when administrator alreeady have approved or is the same one as requesting
 * an action 
 * 
 * @author Philip Vendil
 * @version $Id: AdminAlreadyApprovedRequestException.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class AdminAlreadyApprovedRequestException extends Exception {

	public AdminAlreadyApprovedRequestException(String message) {
		super(message);
	}

}
