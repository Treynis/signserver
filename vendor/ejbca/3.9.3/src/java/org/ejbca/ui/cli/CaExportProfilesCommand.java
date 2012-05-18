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

import java.beans.XMLEncoder;
import java.io.File;
import java.io.FileOutputStream;
import java.util.Collection;
import java.util.Iterator;

import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;



/**
 * Export profiles from the databse to XML-files.
 *
 * @version $Id: CaExportProfilesCommand.java 5867 2008-07-10 14:05:50Z jeklund $
 */
public class CaExportProfilesCommand extends BaseCaAdminCommand {
    /**
     * Creates a new instance of CaExportProfilesCommand
     *
     * @param args command line arguments
     */
    public CaExportProfilesCommand(String[] args) {
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
            Collection certprofids = getCertificateStoreSession().getAuthorizedCertificateProfileIds(administrator,0);                                               
			Collection endentityprofids = getRaAdminSession().getAuthorizedEndEntityProfileIds(administrator);

            if (args.length < 2) {
                getOutputStream().println(
                    "Usage: CA exportprofiles <outpath>");
                getOutputStream().print("\n");
                return;
            }

            String outpath = args[1];
            if (!new File(outpath).isDirectory()) {
                getOutputStream().println("Error: '"+outpath+"' is not a directory.");
                return;
            }
            
            getOutputStream().println("Exporting non-fixed certificate profiles: ");
            Iterator iter = certprofids.iterator();
            while (iter.hasNext()) {
            	int profileid = ((Integer) iter.next()).intValue();
                if (profileid == SecConst.PROFILE_NO_PROFILE) { // Certificate profile not found i database.
                    getOutputStream().println("Error : Couldn't find certificate profile '"+profileid+"' in database.");
                } else if (SecConst.isFixedCertificateProfile(profileid)) {
                    //getOutputStream().println("Debug : Skipping export fixed certificate profile with id '"+profileid+"'.");
                } else {
					String profilename = getCertificateStoreSession().getCertificateProfileName(administrator, profileid);									
                    CertificateProfile profile = getCertificateStoreSession().getCertificateProfile(administrator,profileid);
                    if (profile == null) {
                        getOutputStream().println("Error : Couldn't find certificate profile '"+profilename+"'-"+profileid+" in database.");
                    } else {
                        String outfile = outpath+"/certprofile_"+profilename+"-"+profileid+".xml";
                        getOutputStream().println(outfile+".");
                        XMLEncoder encoder = new XMLEncoder(new  FileOutputStream(outfile));
                        encoder.writeObject(profile.saveData());
                        encoder.close();
                    }
                }
            }

            getOutputStream().println("Exporting non-fixed end entity profiles: ");
            iter = endentityprofids.iterator();
            while (iter.hasNext()){                
                int profileid = ((Integer) iter.next()).intValue();
                if (profileid == SecConst.PROFILE_NO_PROFILE) { // Entity profile not found i database.
                    getOutputStream().println("Error : Couldn't find entity profile '"+profileid+"' in database.");
                } else if (profileid == SecConst.EMPTY_ENDENTITYPROFILE) {
                    //getOutputStream().println("Debug : Skipping export fixed end entity profile with id '"+profileid+"'.");
                } else {
                	String profilename = getRaAdminSession().getEndEntityProfileName(administrator, profileid);
                    EndEntityProfile profile = getRaAdminSession().getEndEntityProfile(administrator, profileid);
                    if (profile == null) {
                        getOutputStream().println("Error : Couldn't find entity profile '"+profilename+"'-"+profileid+" in database.");
                    } else {
                        String outfile = outpath+"/entityprofile_"+profilename+"-"+profileid+".xml";
                        getOutputStream().println(outfile+".");
                        XMLEncoder encoder = new XMLEncoder(new  FileOutputStream(outfile));
                        encoder.writeObject(profile.saveData());
                        encoder.close();
                    }
                }
            }         
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    }
}
