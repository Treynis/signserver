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

import java.util.Collection;
import java.util.Iterator;

import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;





/**
 * List users with status NEW in the database.
 *
 * @version $Id: RaListNewUsersCommand.java 6158 2008-08-27 14:39:37Z jeklund $
 *
 * @see org.ejbca.core.ejb.ra.UserDataLocal
 */
public class RaListNewUsersCommand extends BaseRaAdminCommand {
    /**
     * Creates a new instance of RaListNewUsersCommand
     *
     * @param args command line arguments
     */
    public RaListNewUsersCommand(String[] args) {
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
            Collection coll = getUserAdminSession().findAllUsersByStatus(administrator,
                    UserDataConstants.STATUS_NEW);
            Iterator iter = coll.iterator();

            while (iter.hasNext()) {
                UserDataVO data = (UserDataVO) iter.next();
                getOutputStream().println("New User: " + data.getUsername() + ", \"" + data.getDN() +
                    "\", \"" + data.getSubjectAltName() + "\", " + data.getEmail() + ", " +
                    data.getStatus() + ", " + data.getType() + ", " + data.getTokenType());
            }
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }

    // execute
}
