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

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ra.IUserAdminSessionLocal;
import org.ejbca.core.ejb.ra.IUserAdminSessionLocalHome;
import org.ejbca.core.model.approval.ApprovalDataText;
import org.ejbca.core.model.approval.ApprovalDataVO;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.ApprovalRequest;
import org.ejbca.core.model.approval.ApprovalRequestExecutionException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.UserDataConstants;

/**
 * Approval Request created when trying to edit an end entity.
 *  
 * 
 * 
 * @author Philip Vendil
 * @version $Id: ChangeStatusEndEntityApprovalRequest.java 6798 2009-01-05 17:25:19Z anatom $
 */
public class ChangeStatusEndEntityApprovalRequest extends ApprovalRequest {

	private static final long serialVersionUID = -1L;

	private static final Logger log = Logger.getLogger(ChangeStatusEndEntityApprovalRequest.class);
	
	private static final int LATEST_VERSION = 1;
	

	
	
	private String username;
	private int oldstatus;
	private int newstatus;
	
	/**
	 * Constuctor used in externaliziation only
	 */
	public ChangeStatusEndEntityApprovalRequest() {}


	public ChangeStatusEndEntityApprovalRequest( String username, int oldstatus, int newstatus, Admin requestAdmin, String requestSignature, int numOfReqApprovals, int cAId, int endEntityProfileId) {
		super(requestAdmin, requestSignature, REQUESTTYPE_COMPARING,
				numOfReqApprovals, cAId, endEntityProfileId);
		this.username = username;
		this.oldstatus = oldstatus;
		this.newstatus = newstatus;
	}


	/** Overrides ApprovalRequest.isAllowedTransition()
	 * @see ApprovalRequest#isAllowedTransition()
	 */
	public boolean isAllowedTransition() {
		boolean ret = false;
		
		// Allow Keyrecovery transitions until keyrecovery approvals have been implemented.
		if(newstatus == UserDataConstants.STATUS_KEYRECOVERY){
			return true;
		}
		
		
		if (oldstatus == UserDataConstants.STATUS_GENERATED) {
			switch (newstatus) {
			case UserDataConstants.STATUS_GENERATED:
				ret = true;
				break;
			default:
				break;
			}			
		}
		if (oldstatus == UserDataConstants.STATUS_NEW) {
			switch (newstatus) {
			case UserDataConstants.STATUS_NEW:
				ret = true;
				break;
			case UserDataConstants.STATUS_INPROCESS:
				ret = true;
				break;
			case UserDataConstants.STATUS_GENERATED:
				ret = true;
				break;
			case UserDataConstants.STATUS_FAILED:
				ret = true;
				break;
			default:
				break;
			}			
		}
		if (oldstatus == UserDataConstants.STATUS_FAILED) {
			switch (newstatus) {
			case UserDataConstants.STATUS_FAILED:
				ret = true;
				break;
			case UserDataConstants.STATUS_INPROCESS:
				ret = true;
				break;
			case UserDataConstants.STATUS_GENERATED:
				ret = true;
				break;
			default:
				break;
			}			
		}
		if (oldstatus == UserDataConstants.STATUS_INPROCESS) {
			switch (newstatus) {
			case UserDataConstants.STATUS_INPROCESS:
				ret = true;
				break;
			case UserDataConstants.STATUS_GENERATED:
				ret = true;
				break;
			case UserDataConstants.STATUS_FAILED:
				ret = true;
				break;
			default:
				break;
			}			
		}
		if (oldstatus == UserDataConstants.STATUS_KEYRECOVERY) {
			switch (newstatus) {
			case UserDataConstants.STATUS_KEYRECOVERY:
				ret = true;
				break;
			case UserDataConstants.STATUS_INPROCESS:
				ret = true;
				break;
			case UserDataConstants.STATUS_GENERATED:
				ret = true;
				break;
			case UserDataConstants.STATUS_FAILED:
				ret = true;
				break;
			default:
				break;
			}			
		}
		return ret;
	}

	public void execute() throws ApprovalRequestExecutionException {
		log.debug("Executing Change Status  for user:" + username);
		try{
			ServiceLocator locator = ServiceLocator.getInstance();
			IUserAdminSessionLocalHome userdatahome = (IUserAdminSessionLocalHome) locator.getLocalHome(IUserAdminSessionLocalHome.COMP_NAME);
			IUserAdminSessionLocal usersession = userdatahome.create();
		
		    usersession.setUserStatus(getRequestAdmin(), username, newstatus);
		}catch (CreateException e) {
			throw new ApprovalRequestExecutionException("Error creating new userdata session", e);
		} catch (AuthorizationDeniedException e) {
			throw new ApprovalRequestExecutionException("Authorization Denied :" + e.getMessage(), e);
		} catch (FinderException e) {
			throw new ApprovalRequestExecutionException("User with username + " + username  + " doesn't exist.", e);
		} catch (ApprovalException e) {
			throw new EJBException("This should never happen",e);
		} catch (WaitingForApprovalException e) {
			throw new EJBException("This should never happen",e);
		} 

	}

    /**
     * Approval Id is genereated of This approval type (i.e AddEndEntityApprovalRequest) and UserName
     */
	public int generateApprovalId() {		
		return new String(getApprovalType() + ";" + username).hashCode();
	}


	public int getApprovalType() {		
		return ApprovalDataVO.APPROVALTYPE_CHANGESTATUSENDENTITY;
	}


	public List getNewRequestDataAsText(Admin admin) {
		ArrayList retval = new ArrayList();
		retval.add(new ApprovalDataText("USERNAME",username,true,false));
		retval.add(new ApprovalDataText("STATUS",UserDataConstants.getTranslatableStatusText(newstatus),true,true));		
		return retval;
	}
	
	public List getOldRequestDataAsText(Admin admin) {
		ArrayList retval = new ArrayList();
		retval.add(new ApprovalDataText("USERNAME",username,true,false));
		retval.add(new ApprovalDataText("STATUS",UserDataConstants.getTranslatableStatusText(oldstatus),true,true));		
		return retval;
	}


	public boolean isExecutable() {		
		return true;
	}
	
	public void writeExternal(ObjectOutput out) throws IOException {
		super.writeExternal(out);
		out.writeInt(LATEST_VERSION);
		out.writeObject(username);
		out.writeInt(newstatus);
		out.writeInt(oldstatus);		
		
	}

	public void readExternal(ObjectInput in) throws IOException, ClassNotFoundException {        
		super.readExternal(in);
        int version = in.readInt();
        if(version == 1){
    		username = (String) in.readObject();
    		newstatus = in.readInt();
    		oldstatus = in.readInt();    		
        }

	}

}
