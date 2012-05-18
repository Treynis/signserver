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
 
package org.ejbca.core.protocol.ws.client;

import java.math.BigInteger;


import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.protocol.ws.client.gen.AlreadyRevokedException_Exception;
import org.ejbca.core.protocol.ws.client.gen.ApprovalException_Exception;
import org.ejbca.core.protocol.ws.client.gen.AuthorizationDeniedException_Exception;
import org.ejbca.core.protocol.ws.client.gen.RevokeStatus;
import org.ejbca.core.protocol.ws.client.gen.WaitingForApprovalException_Exception;
import org.ejbca.ui.cli.ErrorAdminCommandException;
import org.ejbca.ui.cli.IAdminCommand;
import org.ejbca.ui.cli.IllegalAdminCommandException;
import org.ejbca.util.CertTools;

/**
 * Revokes a given certificate
 *
 * @version $Id: RevokeCertCommand.java 6428 2008-10-06 09:15:14Z anatom $
 */
public class RevokeCertCommand extends EJBCAWSRABaseCommand implements IAdminCommand{

	
	private static final int ARG_ISSUERDN                 = 1;
	private static final int ARG_CERTSN                   = 2;
	private static final int ARG_REASON                   = 3;
	
	
    /**
     * Creates a new instance of RevokeCertCommand
     *
     * @param args command line arguments
     */
    public RevokeCertCommand(String[] args) {
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
           
            if(args.length != 4){
            	usage();
            	System.exit(-1);
            }
            
            String issuerdn = CertTools.stringToBCDNString(args[ARG_ISSUERDN]);            
            String certsn = getCertSN(args[ARG_CERTSN]);
            int reason = getRevokeReason(args[ARG_REASON]);
            
            if(reason == RevokedCertInfo.NOT_REVOKED){
        		getPrintStream().println("Error : Unsupported reason " + reason);
        		usage();
        		System.exit(-1);
            }
                        
            try{
            	RevokeStatus status =  getEjbcaRAWS().checkRevokationStatus(issuerdn,certsn);
            	if (status != null) {
                	if(status.getReason() != RevokedCertInfo.NOT_REVOKED){
                		getPrintStream().println("Error : Certificate is already revoked");
                		System.exit(-1);
                	}
                	getEjbcaRAWS().revokeCert(issuerdn,certsn,reason);            	         
                    getPrintStream().println("Certificate revoked successfully");            		
            	} else {
            		getPrintStream().println("Certificate does not exist");
            	}
            } catch (AuthorizationDeniedException_Exception e) {
            	getPrintStream().println("Error : " + e.getMessage());            
            } catch (AlreadyRevokedException_Exception e) {
            	getPrintStream().println("The certificate was already revoked.");            
			} catch (WaitingForApprovalException_Exception e) {
            	getPrintStream().println("The revocation request has been sent for approval.");            
			} catch (ApprovalException_Exception e) {
            	getPrintStream().println("This revocation has already been requested.");            
            }
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }


	private String getCertSN(String certsn) {
		try{
			new BigInteger(certsn,16);
		}catch(NumberFormatException e){
			getPrintStream().println("Error in Certificate SN");
			usage();
			System.exit(-1);
		}
		return certsn;
	}

	protected void usage() {
		getPrintStream().println("Command used to revoke a certificate");
		getPrintStream().println("Usage : revokecert <issuerdn> <certificatesn (HEX)>  <reason>  \n\n");
		getPrintStream().println("Reason should be one of : ");
		for(int i=1; i< REASON_TEXTS.length-1;i++){
			getPrintStream().print(REASON_TEXTS[i] + ", ");
		}
		getPrintStream().print(REASON_TEXTS[REASON_TEXTS.length-1]);
   }


}
