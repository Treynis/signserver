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

import org.ejbca.core.model.ra.UserDataVO;


/**
 * Find details of a user in the database.
 *
 * @version $Id: RaKeyRecoverNewestCommand.java 6986 2009-02-19 16:21:35Z anatom $
 */
public class RaKeyRecoverNewestCommand extends BaseRaAdminCommand {
    /**
     * Creates a new instance of RaFindUserCommand
     *
     * @param args command line arguments
     */
    public RaKeyRecoverNewestCommand(String[] args) {
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
            if (args.length != 2) {
                getOutputStream().println("Usage: RA keyrecovernewest <username>");

                return;
            }

            String username = args[1];

             boolean usekeyrecovery = getRaAdminSession().loadGlobalConfiguration(administrator).getEnableKeyRecovery();  
             if(!usekeyrecovery){
               getOutputStream().println("Keyrecovery have to be enabled in the system configuration in order to use this command.");
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
             
             getKeyRecoverySession().markNewestAsRecoverable(administrator, username,userdata.getEndEntityProfileId());
                     
             getOutputStream().println("Key corresponding to users newest certificate has been marked for recovery.");             
 

        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }

    // execute
}
