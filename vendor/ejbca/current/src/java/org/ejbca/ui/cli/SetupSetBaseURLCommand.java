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

import org.ejbca.core.model.log.Admin;

/**
 * Sets the base url of the web interface
 *
 * @version $Id: SetupSetBaseURLCommand.java 6986 2009-02-19 16:21:35Z anatom $
 */
public class SetupSetBaseURLCommand extends BaseAdminCommand {
    /**
     * Creates a new instance of CaCreateCrlCommand
     *
     * @param args command line arguments
     */
    public SetupSetBaseURLCommand(String[] args) {
        super(args, Admin.TYPE_CACOMMANDLINE_USER, "cli");
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
        if (args.length < 3) {
	       throw new IllegalAdminCommandException("Usage: SETUP setdefaultbaseurl <computername> <applicationname>\n" + 
	       		                                                               "Example: setup setbaseurl localhost ejbca \n\n");	       
	    }	
        try {            
        	String computername = args[1];
        	String applicationpath = args[2];
        	getRaAdminSession().initGlobalConfigurationBaseURL(new Admin(Admin.TYPE_CACOMMANDLINE_USER), computername, applicationpath);
        } catch (Exception e) {
        	throw new ErrorAdminCommandException(e);            
        }
    }

    // execute
}
