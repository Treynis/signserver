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

import java.io.BufferedReader;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStreamReader;
import java.rmi.RemoteException;
import java.security.SecureRandom;
import java.util.Properties;

/**
 * 
 * @version $Id: LogResetProtectedLogCommand.java 6103 2008-08-20 19:24:43Z anatom $
 *
 */
public class LogResetProtectedLogCommand extends BaseLogAdminCommand  {
	
	public static final String COMMAND_NAME = "resetprotected";

	private static final SecureRandom seeder = new SecureRandom();

	public LogResetProtectedLogCommand(String[] args) {
        super(args);
	}

	public void execute() throws IllegalAdminCommandException,	ErrorAdminCommandException {
		if (args.length < 3) {
			String msg = "Usage: LOG resetprotected <export | noexport> <export handler properties-file>\n" +
			"Tries to set the log to a consistent state, by removing almost all log posts. Use with care.\n";
			throw new IllegalAdminCommandException(msg);
		}
		boolean export = "export".equalsIgnoreCase(args[1]);
		Properties properties = new Properties();
		try {
			properties.load(new FileInputStream(args[2]));
		} catch (FileNotFoundException e1) {
			getOutputStream().print("Connot find "+args[2]+"\n");
	        return;
		} catch (IOException e1) {
			getOutputStream().print("Connot load "+args[2]+"\n");
	        return;
		}
		// 4 chars should be enough to make the user think at least once..
        String randomString = ""+(seeder.nextInt(9000)+1000);
        getOutputStream().print("\nYOU ARE ABOUT TO DELETE THE PROTECTED LOG!\n\n"+
        							"This should only be used for recovery when everything else fails.\n"+
        							"Confirm the delete by entering \""+randomString+"\": ");
        BufferedReader br = new BufferedReader(new InputStreamReader(System.in));
        try {
           if (!randomString.equals(br.readLine().trim())) {
        	   getOutputStream().println("\nNot correct. Exiting.\n");
        	   return;
           }
        } catch (IOException e) {
        	getOutputStream().println("IO error: "+e.getMessage());
           return;
        }
        getOutputStream().print("\nForcing the protected log to a consistent state...\n");
        try {
			if (getProtectedLogSession().resetEntireLog(export, properties)) {
				getOutputStream().print("SUCCESS!\n");
			} else {
				getOutputStream().print("FAILED!\n");
			}
		} catch (RemoteException e) {
			e.printStackTrace();
		} catch (Exception e) {
			e.printStackTrace();
		}
	}
}
