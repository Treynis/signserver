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

package org.ejbca.core.model.ca.certextensions.standard;

import java.security.PublicKey;

import org.bouncycastle.asn1.DEREncodable;
import org.bouncycastle.asn1.x509.BasicConstraints;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.ejbca.core.model.ca.caadmin.CA;
import org.ejbca.core.model.ca.certextensions.CertificateExtensionException;
import org.ejbca.core.model.ca.certextensions.CertificateExtentionConfigurationException;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ra.UserDataVO;

/**
 * Class for standard X509 certificate extension. 
 * See rfc3280 or later for spec of this extension.
 * 
 * @author: Tomas Gustavsson
 * @version $Id: BasicConstraint.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class BasicConstraint extends StandardCertificateExtension {
	
	/**
	 * Constructor for creating the certificate extension 
	 */
	public BasicConstraint() {
		super();
	}

	/**
	 * @see StandardCertificateExtension#init(CertificateProfile)
	 */
	public void init(CertificateProfile certProf) {
		super.setOID(X509Extensions.BasicConstraints.getId());
		super.setCriticalFlag(certProf.getBasicConstraintsCritical());
	}
	/**
	 * Method that should return the DEREncodable value used in the extension
	 * this is the method at all implementors must implement.
	 * 
	 * @param userData the userdata of the issued certificate.
	 * @param ca the CA data with access to all the keys etc
	 * @param certProfile the certificate profile
	 * @return a DEREncodable or null.
	 */
	public DEREncodable getValue(UserDataVO subject, CA ca, CertificateProfile certProfile, PublicKey userPublicKey ) throws CertificateExtentionConfigurationException, CertificateExtensionException {
		// Default value, end entity 
    	BasicConstraints bc = new BasicConstraints(false);
        if ((certProfile.getType() == CertificateProfile.TYPE_SUBCA)
            || (certProfile.getType() == CertificateProfile.TYPE_ROOTCA)){            	
        	if(certProfile.getUsePathLengthConstraint()){
        		bc = new BasicConstraints(certProfile.getPathLengthConstraint());
        	}else{
        		bc =  new BasicConstraints(true);
        	}            	
        }
		return bc;
	}	
}
