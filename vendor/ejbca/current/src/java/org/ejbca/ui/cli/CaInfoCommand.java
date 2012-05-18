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
import java.security.interfaces.ECPublicKey;
import java.util.ArrayList;

import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.bouncycastle.jce.spec.ECNamedCurveSpec;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.util.CertTools;
import org.ejbca.util.keystore.KeyTools;



/**
 * Gets and prints info about the CA.
 *
 * @version $Id: CaInfoCommand.java 8233 2009-10-31 14:37:47Z anatom $
 */
public class CaInfoCommand extends BaseCaAdminCommand {
    /**
     * Creates a new instance of CaInfoCommand
     *
     * @param args command line arguments
     */
    public CaInfoCommand(String[] args) {
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
           String msg = "Usage: CA info <caname>";               
           throw new IllegalAdminCommandException(msg);
        }
        try {            
            String caname = args[1];
            ArrayList chain = new ArrayList(getCertChain(caname));
            CAInfo cainfo = getCAInfo(caname);
                                    
            getOutputStream().println("CA name: " + caname);
            getOutputStream().println("CA type: "+cainfo.getCAType());
            getOutputStream().println("CA ID: " + cainfo.getCAId());
            getOutputStream().println("CA CRL Expiration Period: " + cainfo.getCRLPeriod());
            getOutputStream().println("CA CRL Issue Interval: " + cainfo.getCRLIssueInterval());
            getOutputStream().println("CA Description: " + cainfo.getDescription());
            getOutputStream().println();
            
            if (chain.size() < 2) {
              getOutputStream().println("This is a Root CA.");
            } else {
              getOutputStream().println("This is a subordinate CA.");
            }
              
            getOutputStream().println("Size of chain: " + chain.size());
            if (chain.size() > 0) {
                Certificate rootcert = (Certificate)chain.get(chain.size()-1);
                getOutputStream().println("Root CA DN: "+CertTools.getSubjectDN(rootcert));
                getOutputStream().println("Root CA id: "+CertTools.getSubjectDN(rootcert).hashCode());
                getOutputStream().println("Certificate valid from: "+CertTools.getNotBefore(rootcert));
                getOutputStream().println("Certificate valid to: "+CertTools.getNotAfter(rootcert));
            	getOutputStream().println("Root CA key algorithm: "+rootcert.getPublicKey().getAlgorithm());
            	getOutputStream().println("Root CA key size: "+KeyTools.getKeyLength(rootcert.getPublicKey()));
                if(rootcert.getPublicKey() instanceof ECPublicKey) {
                	if(((ECPublicKey) rootcert.getPublicKey()).getParams() instanceof ECNamedCurveSpec) {
                		getOutputStream().println("Root CA ECDSA key spec: " + ((ECNamedCurveSpec) ((ECPublicKey)rootcert.getPublicKey()).getParams()).getName());
                	}
                }
                for(int i = chain.size()-2; i>=0; i--){                                          
                    Certificate cacert = (Certificate)chain.get(i);
                    getOutputStream().println("CA DN: "+CertTools.getSubjectDN(cacert));
                    getOutputStream().println("Certificate valid from: "+CertTools.getNotBefore(cacert));
                    getOutputStream().println("Certificate valid to: "+CertTools.getNotAfter(cacert));
                	getOutputStream().println("CA key algorithm: "+cacert.getPublicKey().getAlgorithm());
                	getOutputStream().println("CA key size: "+KeyTools.getKeyLength(cacert.getPublicKey()));
                    if(cacert.getPublicKey() instanceof ECPublicKey) {
                    	if(((ECPublicKey) cacert.getPublicKey()).getParams() instanceof ECNamedCurveSpec) {
                    		getOutputStream().println("CA ECDSA key spec: " + ((ECNamedCurveSpec) ((ECPublicKey)cacert.getPublicKey()).getParams()).getName());
                    	}
                    }
                }                
            }
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    } // execute
    
}
