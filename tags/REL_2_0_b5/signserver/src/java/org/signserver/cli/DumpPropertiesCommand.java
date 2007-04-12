/*************************************************************************
 *                                                                       *
 *  SignServer: The OpenSource Automated Signing Server                  *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/


package org.signserver.cli;

import java.io.FileOutputStream;
import java.rmi.RemoteException;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Properties;

import org.signserver.common.GlobalConfiguration;
import org.signserver.common.WorkerConfig;

 

/**
 * Command used to dump all configured properties for a worker or all workers
 *
 * @version $Id: DumpPropertiesCommand.java,v 1.1 2007-03-07 07:41:19 herrvendil Exp $
 */
public class DumpPropertiesCommand extends BaseCommand {
	
	
    /**
     * Creates a new instance of SetPropertyCommand
     *
     * @param args command line arguments
     */
    public DumpPropertiesCommand(String[] args) {
        super(args);
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute(String hostname) throws IllegalAdminCommandException, ErrorAdminCommandException {
        if (args.length != 3) {
	       throw new IllegalAdminCommandException("Usage: signserver dumpproperties <-host hostname (optional)> < all | workerid > <outfile>\n" + 
	       		                                  "Example 1: signserver dumpproperties 1 myworkerbackup.properties\n" +
	       		                                  "Example 2: signserver dumpproperties all singserverbackup.properties\n" +
	    		                                  "Example 3: signserver dumpproperties -host node3.someorg.com all singserverbackup.properties\n\n");	       
	    }	
        try {            
        	
        	String outfile = args[2];        	
        	String workerid = args[1];
        	
        	Properties outProps = new Properties();

        	if(workerid.substring(0, 1).matches("\\d")){ 
        		dumpWorkerProperties(hostname, Integer.parseInt(workerid),outProps);        		            		
        	}else{
        		if(workerid.trim().equalsIgnoreCase("ALL")){
        			dumpAllProperties(hostname, outProps);            			
        		}else{
        			// named worker is requested
        			int id = getSignSession(hostname).getSignerId(workerid);
        			if(id == 0){
        				throw new IllegalAdminCommandException("Error: No worker with the given name could be found");
        			}
            		dumpWorkerProperties(hostname, id,outProps);        			
        		}
        	}
        	
        	FileOutputStream fos = new FileOutputStream(outfile);
        	outProps.store(fos, null);
        	fos.close();
        	getOutputStream().println("Properties successfully dumped into file " + outfile);

        	this.getOutputStream().println("\n\n");

        } catch (Exception e) {
        	throw new ErrorAdminCommandException(e);            
        }
    }

	private void dumpAllProperties(String hostname, Properties outProps) throws RemoteException, Exception {		
		List workers = getGlobalConfigurationSession(hostname).getWorkers(GlobalConfiguration.WORKERTYPE_ALL);
		
		// First output all global properties
		GlobalConfiguration gc = getGlobalConfigurationSession(hostname).getGlobalConfiguration();
		Iterator iter = gc.getKeyIterator();
		while(iter.hasNext()){
			String next = (String) iter.next();
			outProps.put(next, gc.getProperty(next));
		}
		iter = workers.iterator();
		while(iter.hasNext()){
			Integer next = (Integer) iter.next();
			WorkerConfig workerConfig = getSignSession(hostname).getCurrentSignerConfig(next.intValue());
			Enumeration e = workerConfig.getProperties().keys();
			Properties workerProps = workerConfig.getProperties();
			while(e.hasMoreElements()){
				String key = (String) e.nextElement();
				outProps.setProperty("WORKER" + next +"."+key, workerProps.getProperty(key));
			}
		}				
	}

	private void dumpWorkerProperties(String hostname, int signerId, Properties outProps) throws RemoteException, Exception {
		GlobalConfiguration gc = getGlobalConfigurationSession(hostname).getGlobalConfiguration();
		Iterator iter = gc.getKeyIterator();
		while(iter.hasNext()){
			String next = (String) iter.next();
			if(next.substring(5).startsWith("WORKER")){
			  outProps.put(next, gc.getProperty(next));
			}
		}	
		
		WorkerConfig workerConfig = getSignSession(hostname).getCurrentSignerConfig(signerId);
		Enumeration e = workerConfig.getProperties().keys();
		Properties workerProps = workerConfig.getProperties();
		while(e.hasMoreElements()){
			String key = (String) e.nextElement();
			outProps.setProperty("WORKER" + signerId +"."+key, workerProps.getProperty(key));
		}
	}

	// execute
    
	public int getCommandType() {
		return TYPE_EXECUTEONMASTER;
	}
	

}
