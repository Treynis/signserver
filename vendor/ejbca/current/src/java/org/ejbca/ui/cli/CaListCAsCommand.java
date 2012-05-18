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

import java.security.cert.Certificate;
import java.util.Collection;
import java.util.Iterator;

import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.util.CertTools;

/**
 * Lists the names of all available CAs.
 *
 * @version $Id: CaListCAsCommand.java 6158 2008-08-27 14:39:37Z jeklund $
 */
public class CaListCAsCommand extends BaseCaAdminCommand {
    /**
     * Creates a new instance of CaListCAsCommand
     *
     * @param args command line arguments
     */
    public CaListCAsCommand(String[] args) {
        super(args);
    }

    /**
     * Runs the command
     *
     * @throws IllegalAdminCommandException Error in command args
     * @throws ErrorAdminCommandException Error running command
     */
    public void execute() throws IllegalAdminCommandException, ErrorAdminCommandException {
            
        if (args.length > 1) {
           String msg = "Lists the names of all available CAs.\nUsage: CA listcas";               
           throw new IllegalAdminCommandException(msg);
        }            
        try {
            Collection caids = getCAAdminSession().getAvailableCAs(administrator);
            Iterator iter = caids.iterator();
            while (iter.hasNext()) {
                int caid = ((Integer)iter.next()).intValue();
                CAInfo ca = getCAAdminSession().getCAInfo(administrator,caid);
                Collection certs = ca.getCertificateChain();
                Iterator ci = certs.iterator();
                Certificate cacert = null;
                if (ci.hasNext()) {
                    cacert = (Certificate)ci.next();                	
                }
                getOutputStream().println();
                getOutputStream().println("CA Name: "+ca.getName());
                getOutputStream().println("Id: "+ca.getCAId());
                if (cacert != null) {
                    getOutputStream().println("Issuer DN: "+CertTools.getIssuerDN(cacert));                	
                }
                getOutputStream().println("Subject DN: "+ca.getSubjectDN());
                getOutputStream().println("Type: "+ca.getCAType());
                getOutputStream().println("Expire time: "+ca.getExpireTime());
                getOutputStream().println("Signed by: "+ca.getSignedBy());
            }
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    } // execute
}
