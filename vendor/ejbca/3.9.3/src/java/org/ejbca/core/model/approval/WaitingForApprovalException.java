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
 * Excception thrown from actions that stop to wait for approvals
 * 
 * @author Philip Vendil
 * @version $Id: WaitingForApprovalException.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class WaitingForApprovalException extends Exception {

	public int approvalId = 0;

	public WaitingForApprovalException(String message, Throwable cause) {
		super(message, cause);
	}

	public WaitingForApprovalException(String message) {
		super(message);
	}
	
	public WaitingForApprovalException(String message, int approvalId) {
		super(message);
		this.approvalId = approvalId;
	}
	
	public int getApprovalId(){
		return approvalId;
	}
	
	public void setApprovalId(int approvalId){
		this.approvalId = approvalId;
	}

}
