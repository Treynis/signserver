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

import java.lang.reflect.Constructor;

/**
 * 
 * @version $Id: LogAdminCommandFactory.java 6103 2008-08-20 19:24:43Z anatom $
 *
 */
public class LogAdminCommandFactory {

    private LogAdminCommandFactory() {
    }

	private static final Class[] commandClasses = { LogVerifyProtectedLogCommand.class, LogAcceptProtectedLogCommand.class, LogResetExportProtectedLogCommand.class,
			LogResetProtectedLogCommand.class};
	
	private static final String[] commandNames = { LogVerifyProtectedLogCommand.COMMAND_NAME, LogAcceptProtectedLogCommand.COMMAND_NAME,
			LogResetExportProtectedLogCommand.COMMAND_NAME, LogResetProtectedLogCommand.COMMAND_NAME};
	
    public static IAdminCommand getCommand(String[] args) {
        if (args.length >= 1) {
        	for (int i=0; i<commandClasses.length; i++) {
        		if (commandNames[i].equalsIgnoreCase(args[0])) {
        			Class[] paramTypes = new Class[] {String[].class};
        			Constructor constructor;
        			try {
        				constructor = commandClasses[i].getConstructor(paramTypes);
		                Object[] params = new Object[1];
	                    params[0] = args;
        				return (IAdminCommand) constructor.newInstance(params);
        			} catch (Exception e) {
        				throw new RuntimeException(e);
        			}
        		}
        	}
        }
        return null;
    } // getCommand

    public static String getAvailableCommands() {
    	String availableCommands = "";
    	for (int i=0; i<commandNames.length; i++) {
    		availableCommands += commandNames[i] + (commandNames.length -1 != i ? " | " : "");
    	}
    	return availableCommands;
    }
}
