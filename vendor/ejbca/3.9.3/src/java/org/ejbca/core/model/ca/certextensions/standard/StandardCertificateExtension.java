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

import org.ejbca.core.model.ca.certextensions.CertificateExtension;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;

/**
 * Base class for a standard certificate extension.
 * All standard extensions should inherit this class.
 * 
 * The methods that need implementation is init frmm here and getValue from the super class.
 * init should call setOID and setCriticalFlag from the super class.
 * Implementing class must have a default constructor, calling super constructor.
 * 
 * @version $Id: StandardCertificateExtension.java 5585 2008-05-01 20:55:00Z anatom $
 */
public abstract class StandardCertificateExtension extends CertificateExtension {
	
	/**
	 * Constuctor for creating a Certificate Extension. 
	 */
	public StandardCertificateExtension() {
		super();
	}
	
	/**
	 * Method that initialises the CertificateExtension
	 * 
	 * @param certProf certificateprofile that defines if this extension is used and critical
	 */
	public abstract void init(CertificateProfile certProf);
	
}
