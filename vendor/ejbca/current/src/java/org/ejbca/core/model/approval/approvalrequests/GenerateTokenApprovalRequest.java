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
package org.ejbca.core.model.approval.approvalrequests;

import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectOutput;
import java.util.ArrayList;
import java.util.List;

import org.ejbca.core.model.approval.ApprovalDataText;
import org.ejbca.core.model.approval.ApprovalDataVO;
import org.ejbca.core.model.approval.ApprovalRequest;
import org.ejbca.core.model.approval.ApprovalRequestExecutionException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.util.CertTools;

/**
 * Special Approval Request created when an adminsitrator wants
 * to generate a token through the Web Service interface.
 *  
 * It is a two step approval request were the first step is
 * a view hard token puk data and the second is the actual
 * hard token generation
 * 
 * @author Philip Vendil
 * @version $Id: GenerateTokenApprovalRequest.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class GenerateTokenApprovalRequest extends ApprovalRequest {

	private static final long serialVersionUID = -1L;

	//private static final Logger log = Logger.getLogger(ViewHardTokenDataApprovalRequest.class);
	
	public static final int STEP_0_VIEWHARDTOKENDATA = 0;
	public static final int STEP_1_GENERATETOKEN     = 1;
	
	private static final int LATEST_VERSION = 1;
		
	private String dn;
	private String username;
	private String tokenTypeLabel;
	

	/**
	 * Constuctor used in externaliziation only
	 */
	public GenerateTokenApprovalRequest() {}


	public GenerateTokenApprovalRequest(String username, String userDN, String tokenTypeLabel, Admin requestAdmin, String requestSignature, int numOfReqApprovals, int cAId, int endEntityProfileId) {
		super(requestAdmin, requestSignature, REQUESTTYPE_SIMPLE,
				numOfReqApprovals, cAId, endEntityProfileId,2);
		this.username = username;
		this.dn = userDN;
		this.tokenTypeLabel = tokenTypeLabel;
	}


	public void execute() throws ApprovalRequestExecutionException {
		// This is a non-executable approval

	}

    /**
     * Approval Id is genereated of This approval type (i.e AddEndEntityApprovalRequest) and UserName
     */
	public int generateApprovalId() {					
		return new String(getApprovalType() + ";" + username + ";" + CertTools.getFingerprintAsString(getRequestAdminCert())).hashCode();
	}


	public int getApprovalType() {		
		return ApprovalDataVO.APPROVALTYPE_GENERATETOKEN;
	}


	public List getNewRequestDataAsText(Admin admin) {
		ArrayList retval = new ArrayList();
		retval.add(new ApprovalDataText("USERNAME",username,true,false));		
		retval.add(new ApprovalDataText("SUBJECTDN",dn,true,false));
		retval.add(new ApprovalDataText("LABEL",tokenTypeLabel,true,true));
		
		return retval;
	}
	
	public List getOldRequestDataAsText(Admin admin) {
		return null;
	}


	public boolean isExecutable() {		
		return false;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(LATEST_VERSION);
		out.writeObject(username);
		out.writeObject(dn);
		out.writeObject(tokenTypeLabel);
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {        
		super.readExternal(in);
        int version = in.readInt();
        if(version == 1){
    		username = (String) in.readObject();
            dn = (String) in.readObject();
            tokenTypeLabel = (String) in.readObject();            
        }
	}


	/**
	 * @return the subject dn used in the request
	 */
	public String getDN() {
		return dn;
	}

}
