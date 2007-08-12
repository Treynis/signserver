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

import java.io.File;
import java.io.FileOutputStream;
import java.math.BigInteger;
import java.util.Iterator;
import java.util.List;

import org.signserver.common.ArchiveDataVO;



  
/**
 * Returns all archive datas requested from given IP
 *
 * @version $Id: FindFromRequestCertCommand.java,v 1.1 2007-02-27 16:18:07 herrvendil Exp $
 */
public class FindFromRequestCertCommand extends BaseCommand {
	
	
	
    /**
     * Creates a new instance of FindFromRequestIPCommand
     *
     * @param args command line arguments
     */
    public FindFromRequestCertCommand(String[] args) {
        super(args);
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute(String hostname) throws IllegalAdminCommandException, ErrorAdminCommandException {
        if (args.length != 6) {
	       throw new IllegalAdminCommandException("Usage: signserver archive findfromrequestcert <signerid> <certificatesn (hex)> <issuerd> <outputpath>\n" + 
	       		                                  "Example: signserver archive findfromrequestcert 1 EF34242D2324 \"CN=Test Root CA\" /tmp/archivedata \n\n");	       
	    }	
        try {                    	
        	int signerid = getWorkerId(args[1], hostname);
        	checkThatWorkerIsSigner(signerid,hostname);
        	
        	String certsn = args[3];
        	String issuerdn = args[4];
        	BigInteger sn = new BigInteger(certsn,16);         	
            File outputPath = new File(args[5]);
            if(!outputPath.exists()){
            	throw new IllegalAdminCommandException("Error output path " + args[4] + " doesn't exist\n\n");	 
            }
            if(!outputPath.isDirectory()){
            	throw new IllegalAdminCommandException("Error output path " + args[4] + " isn't a directory\n\n");	 
            }            
            
        	this.getOutputStream().println("Trying to find archive datas requested from client with certificate " + certsn + " issued by " + issuerdn + "\n");
		                               	
        	List result = getSignSession(hostname).findArchiveDatasFromRequestCertificate(signerid,sn,issuerdn);        	        	
        	
            if(result.size() != 0){
            	Iterator iter = result.iterator();
            	while (iter.hasNext()){
            	  ArchiveDataVO next = (ArchiveDataVO) iter.next();            	
            	  String filename = outputPath.getAbsolutePath() + "/"+ next.getArchiveId();
            	  FileOutputStream os = new FileOutputStream(filename);
            	  os.write(next.getArchiveData().getData());
            	  os.close();
            	  this.getOutputStream().println("Archive data with archiveid " + next.getArchiveId() + " written to file : " +filename + "\n\n");
            	}
            }else{
            	this.getOutputStream().println("Couldn't find any archive data from client with certificate " + certsn + " issued by " + issuerdn + " from signer " +signerid + "\n\n");
            }        	
        	
    		this.getOutputStream().println("\n\n");
        	
        } catch (Exception e) {
        	throw new ErrorAdminCommandException(e);            
        }
    }

	public int getCommandType() {
		return TYPE_EXECUTEONMASTER;
	}

    // execute
}
