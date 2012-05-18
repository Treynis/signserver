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
 
package org.ejbca.ui.cli;

import java.rmi.UnmarshalException;

import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.catoken.ICAToken;
import org.ejbca.ui.cli.util.ConsolePasswordReader;





/**
 * Activates the specified HSM CA.
 *
 * @version $Id: CaActivateCACommand.java 7227 2009-04-01 20:26:38Z anatom $
 */
public class CaActivateCACommand extends BaseCaAdminCommand {
    /**
     * Creates a new instance of RaListUsersCommand
     *
     * @param args command line arguments
     */
    public CaActivateCACommand(String[] args) {
        super(args);
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
        try {
            if (args.length < 2) {
                getOutputStream().println("Usage: CA activateca <CA name> [<authorization code>]");
                getOutputStream().println("Leaving out authorization code will prompt for it.");
                return;
            }

            String caname = args[1];
            String authorizationcode = null;
            if (args.length > 2) {
            	authorizationcode = args[2];
            } else {
                getOutputStream().print("Enter authorization code: ");
                // Read the password, but mask it so we don't display it on the console
                ConsolePasswordReader r = new ConsolePasswordReader();
                authorizationcode = String.valueOf(r.readPassword());            	
            }
                        
            // Get the CAs info and id
            CAInfo cainfo = getCAAdminSession().getCAInfo(administrator, caname);
            if(cainfo == null){
            	getOutputStream().println("Error: CA " + caname + " cannot be found");	
            	return;            	
            }
                                    
            // Check that CA has correct status.
            if ( (cainfo.getStatus() == SecConst.CA_OFFLINE) || 
            		(cainfo.getStatus() == SecConst.CA_ACTIVE) && (cainfo.getCATokenInfo().getCATokenStatus() == ICAToken.STATUS_OFFLINE) ) {
            	try {
                	getCAAdminSession().activateCAToken(administrator, cainfo.getCAId(), authorizationcode);            		
            	} catch (UnmarshalException e) {
            		// If we gat a classnotfound we are probably getting an error back from the token, 
            		// with a class we don't have here at the CLI. It is probably invalid PIN
            		getOutputStream().println("Error returned, did you enter the correct PIN?");
            		getOutputStream().println(e.getMessage());
            	} catch (ApprovalException e){
            		getOutputStream().println("Error: CA Token activation approval request already exists.");
            	} catch (WaitingForApprovalException e){
            		getOutputStream().println("CA requires an approval to be activated. A request have been sent to authorized administrators." );
            	}
            }else{
            	getOutputStream().println("Error: CA or CAToken must be offline to be activated.");
            }
            
 
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    } // execute
}
