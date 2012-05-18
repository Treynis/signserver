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

import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Iterator;

import org.apache.commons.lang.StringUtils;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ca.store.CertificateInfo;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.util.CertTools;





/**
 * Re-publishes the certificates of all users beloinging to a particular CA.
 *
 * @version $Id: CARepublishCommand.java 8422 2009-12-10 13:18:39Z primelars $
 */
public class CARepublishCommand extends BaseCaAdminCommand {
    /**
     * Creates a new instance of RaListUsersCommand
     *
     * @param args command line arguments
     */
    public CARepublishCommand(String[] args) {
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
            if (args.length < 2) {
                getOutputStream().println("Usage: CA republish <CA name> [-all]");
                return;
            }

            String caname = args[1];
            boolean addAll = false;
            if (args.length == 3) {
                String all = args[2];
                if (StringUtils.equalsIgnoreCase(all, "-all")) {
                	addAll = true;
                }
            }
                        
            // Get the CAs info and id
            CAInfo cainfo = getCAAdminSession().getCAInfo(administrator, caname);
            if ( cainfo == null ) {
            	getOutputStream().println("CA with name '" + caname + "' does not exist.");
            	return;
            }
            // Publish the CAns certificate and CRL
            Collection cachain = cainfo.getCertificateChain();
            Iterator caiter = cachain.iterator();
            if (caiter.hasNext()) {
                X509Certificate cacert = (X509Certificate)caiter.next();
                int crlNumber = getCertificateStoreSession().getLastCRLNumber(administrator, cainfo.getSubjectDN(), false);
                byte[] crlbytes = getCertificateStoreSession().getLastCRL(administrator, cainfo.getSubjectDN(), false);
                Collection capublishers = cainfo.getCRLPublishers();
                // Store cert and CRL in ca publishers.
                if(capublishers != null) {
                    String fingerprint = CertTools.getFingerprintAsString(cacert);
                    String username = getCertificateStoreSession().findUsernameByCertSerno(administrator, cacert.getSerialNumber(), cacert.getIssuerDN().getName());
        		    CertificateInfo certinfo = getCertificateStoreSession().getCertificateInfo(administrator, fingerprint);
        		    getPublisherSession().storeCertificate(administrator, capublishers, cacert, username, null, cainfo.getSubjectDN(), fingerprint, certinfo.getStatus(), certinfo.getType(), certinfo.getRevocationDate().getTime(), certinfo.getRevocationReason(), certinfo.getTag(), certinfo.getCertificateProfileId(), certinfo.getUpdateTime().getTime(), null);                                
                    getOutputStream().println("Certificate published for "+caname);
                    if ( (crlbytes != null) && (crlbytes.length > 0) && (crlNumber > 0) ) {
                        getPublisherSession().storeCRL(administrator, capublishers, crlbytes, fingerprint, crlNumber, cainfo.getSubjectDN());                        
                        getOutputStream().println("CRL published for "+caname);
                    } else {
                        getOutputStream().println("CRL not published, no CRL createed for CA?");
                    }
                } else {
                    getOutputStream().println("No publishers configured for the CA, no CA certificate or CRL published.");
                }
            } else {
                getOutputStream().println("CA does not have a certificate, no certificate or CRL published!");
            }
            
            // Get all users for this CA
            Collection coll = getUserAdminSession().findAllUsersByCaId(administrator, cainfo.getCAId());
            Iterator iter = coll.iterator();
            while (iter.hasNext()) {
                UserDataVO data = (UserDataVO) iter.next();
                getOutputStream().println("User: " + data.getUsername() + ", \"" + data.getDN() +
                    "\", \"" + data.getSubjectAltName() + "\", " + data.getEmail() + ", " +
                    data.getStatus() + ", " + data.getType() + ", " + data.getTokenType() + ", " + data.getHardTokenIssuerId()+", "+data.getCertificateProfileId());

                if (data.getCertificateProfileId() > 0) { // only if we find a certificate profile
                    CertificateProfile certProfile = getCertificateStoreSession().getCertificateProfile(administrator, data.getCertificateProfileId());
                    if (certProfile == null) {
                        error("Can not get certificate profile with id: "+data.getCertificateProfileId());
                        continue;
                    }
                    Collection certCol = getCertificateStoreSession().findCertificatesByUsername(administrator, data.getUsername());
                    Iterator certIter = certCol.iterator();
                    X509Certificate cert = null;
                    if (certIter.hasNext()) {
                        cert = (X509Certificate)certIter.next();
                    }
                    X509Certificate tmpCert = null;
                    while (certIter.hasNext())
                    {
                        // Make sure we get the latest certificate of them all (if there are more than one for this user).
                        tmpCert = (X509Certificate)certIter.next();
                        if (tmpCert.getNotBefore().compareTo(cert.getNotBefore()) > 0) {
                            cert = tmpCert;
                        }
                    }
                    if (cert != null) {
                        if(certProfile.getPublisherList() != null) {
                            getOutputStream().println("Re-publishing user "+data.getUsername());
                            if (addAll) {
                                getOutputStream().println("Re-publishing all certificates ("+certCol.size()+").");
                            	Iterator i = certCol.iterator();
                            	while (i.hasNext()) {
                            		X509Certificate c = (X509Certificate)i.next();
                                    publishCert(data, certProfile, c);
                            	}
                            }
                            // Publish the latest again, last to make sure that is the one stuck in LDAP for example
                            publishCert(data, certProfile, cert);
                        } else {
                            getOutputStream().println("Not publishing user "+data.getUsername()+", no publisher in certificate profile.");
                        }
                    } else {
                        getOutputStream().println("No certificate to publish for user "+data.getUsername());
                    }
                } else {
                    getOutputStream().println("No certificate profile id exists for user "+data.getUsername());
                }
            }
        } catch (Exception e) {
            throw new ErrorAdminCommandException(e);
        }
    } // execute

	private void publishCert(UserDataVO data, CertificateProfile certProfile, X509Certificate cert) {
		try {
		    String fingerprint = CertTools.getFingerprintAsString(cert);
		    CertificateInfo certinfo = getCertificateStoreSession().getCertificateInfo(administrator, fingerprint);
            final String userDataDN = data.getDN();
		    getPublisherSession().storeCertificate(administrator, certProfile.getPublisherList(), cert, data.getUsername(), data.getPassword(), userDataDN, fingerprint, certinfo.getStatus(), certinfo.getType(), certinfo.getRevocationDate().getTime(), certinfo.getRevocationReason(), certinfo.getTag(), certinfo.getCertificateProfileId(), certinfo.getUpdateTime().getTime(), null);                                
		} catch (Exception e) {
		    // catch failure to publish one user and continue with the rest
		    error("Failed to publish certificate for user "+data.getUsername()+", continuing with next user.");
		}
	}
}
