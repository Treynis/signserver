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

import java.math.BigInteger;
import java.security.cert.X509Certificate;

import org.ejbca.core.model.ra.UserDataVO;

/**
 * Find details of a user in the database.
 *
 * @version $Id: RaKeyRecoverCommand.java 6158 2008-08-27 14:39:37Z jeklund $
 */
public class RaKeyRecoverCommand extends BaseRaAdminCommand {
    /**
     * Creates a new instance of RaFindUserCommand
     *
     * @param args command line arguments
     */
    public RaKeyRecoverCommand(String[] args) {
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
            if (args.length != 3) {
                getOutputStream().println("Usage: RA keyrecover <CertificateSN (HEX)> <IssuerDN>");

                return;
            }

            BigInteger certificatesn = new BigInteger(args[1], 16);
            String issuerdn = args[2];

             boolean usekeyrecovery = getRaAdminSession().loadGlobalConfiguration(administrator).getEnableKeyRecovery();  
             if(!usekeyrecovery){
               getOutputStream().println("Keyrecovery have to be enabled in the system configuration in order to use this command.");
               return;                   
             }   
              
             X509Certificate cert = (X509Certificate) getCertificateStoreSession().findCertificateByIssuerAndSerno(
                                                                             administrator, issuerdn, 
                                                                             certificatesn);
              
             if(cert == null){
               getOutputStream().println("Certificate couldn't be found in database.");
               return;              
             }
              
             String username = getCertificateStoreSession().findUsernameByCertSerno(administrator, certificatesn, issuerdn);
              
             if(!getKeyRecoverySession().existsKeys(administrator,cert)){
               getOutputStream().println("Specified keys doesn't exist in database.");
               return;                  
             }
              
             if(getKeyRecoverySession().isUserMarked(administrator,username)){
               getOutputStream().println("User is already marked for recovery.");
               return;                     
             }
             
             UserDataVO userdata = getUserAdminSession().findUser(administrator, username);
             if(userdata == null){
                 getOutputStream().println("Error, The user doesn't exist.");
                 return;
             }
  
             getKeyRecoverySession().markAsRecoverable(administrator, 
                                                  cert, userdata.getEndEntityProfileId());
                      
 
             getOutputStream().println("Keys corresponding to given certificate has been marked for recovery.");                           

        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }

    // execute
}
