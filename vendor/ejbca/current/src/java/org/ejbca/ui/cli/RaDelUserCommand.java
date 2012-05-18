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

import org.ejbca.core.model.authorization.AuthorizationDeniedException;


/**
 * Deletes a user from the database.
 *
 * @version $Id: RaDelUserCommand.java 6158 2008-08-27 14:39:37Z jeklund $
 */
public class RaDelUserCommand extends BaseRaAdminCommand {
    /**
     * Creates a new instance of RaDelUserCommand
     *
     * @param args command line arguments
     */
    public RaDelUserCommand(String[] args) {
        super(args);
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
        if (args.length < 2) {
            throw new IllegalAdminCommandException("Usage: RA deluser <username>");
        }

        try {
            String username = args[1];
            getOutputStream().print("Have you revoked the user [y/N]? ");

            int inp = System.in.read();

            if ((inp == 121) || (inp == 89)) {
                try {
                    getUserAdminSession().deleteUser(administrator, username);
                    getOutputStream().println("Deleted user " + username);
                } catch (AuthorizationDeniedException e) {
                    getOutputStream().println("Error : Not authorized to remove user.");
                }
            } else {
                getOutputStream().println("Delete aborted!");
                getOutputStream().println("Please run 'ra revokeuser " + username + "'.");
            }
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }

    // execute
}
