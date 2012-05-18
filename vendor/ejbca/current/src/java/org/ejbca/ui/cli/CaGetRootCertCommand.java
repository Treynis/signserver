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

import java.io.FileOutputStream;
import java.security.cert.Certificate;
import java.util.ArrayList;

import org.ejbca.util.CertTools;


/**
 * Export root CA certificate.
 *
 * @version $Id: CaGetRootCertCommand.java 6180 2008-08-28 14:47:14Z anatom $
 */
public class CaGetRootCertCommand extends BaseCaAdminCommand {
    /**
     * Creates a new instance of CaGetRootCertCommand
     *
     * @param args command line arguments
     */
    public CaGetRootCertCommand(String[] args) {
        super(args);
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
		
        if (args.length < 3) {		
            String msg = "Save root CA certificate (PEM- or DER-format) to file.\n";
            msg += "Usage: CA getrootcert <caname> <filename> <-der>";
            throw new IllegalAdminCommandException(msg);
        }		
		
        String caname = args[1];
        String filename = args[2];
        boolean pem = true;
        if (args.length > 3) {
            if (("-der").equals(args[3])) {
                pem = false;
            }
        }
        	
		getOutputStream().flush();
        try {
            ArrayList chain = new ArrayList(getCertChain(caname));
            if (chain.size() > 0) {
                Certificate rootcert = (Certificate)chain.get(chain.size()-1);
 
                FileOutputStream fos = new FileOutputStream(filename);
                if (pem) {		
                    fos.write(CertTools.getPEMFromCerts(chain));
                } else {					
                    fos.write(rootcert.getEncoded());
                }				
                fos.close();
				getOutputStream().println("Wrote Root CA certificate to '" + filename + "'");
            } else {
                getOutputStream().println("No CA certificate found.");
            }
        } catch (Exception e) {			
            throw new ErrorAdminCommandException(e);
        }        
    } // execute
}
