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

package org.signserver.protocol.validationservice.ws;

import org.signserver.common.IllegalRequestException;
import org.signserver.common.SignServerException;
import org.signserver.validationservice.common.ValidationServiceConstants.CertType;



/**
 * WebService interface for the Validation Service
 * 
 *
 * @version $Id: IValidationWS.java,v 1.1 2007-12-02 20:35:18 herrvendil Exp $
 */
public interface IValidationWS {

	/**
	 * WebService method used to check the revocation status of a certificate. It performs
	 * calls to underlying validation services depending on CA issued the certificate.
	 * 
	 * The call also checks verification and validity of the certificate.
	 * 
	 * @param serviceName id or name of the validation service to validate the certificate
	 * @param base64Cert the certificate to check in base64 encoding. 
	 * @param certType the intended purposes that the client want to use the certificate for. 
	 * @return A response containing the status of the certificate.
	 * @throws IllegalRequestException if the request contains illegal data.
	 * @throws OperationFailedException if operation couldn't be performed due to application error or communication problems
	 * with underlying systems.
	 */
	ValidationResponse isValid(String serviceName, String base64Cert, CertType certType)
			throws IllegalRequestException, SignServerException;

	/**
	 * Method used to check the status of the current node of the service. The method checks
	 * the availability of this and all underlying systems.
	 * 
	 * @param serviceName id or name of the validation service to check
	 * 
	 * @return a status message of the status of the system, returns the string "ALLOK" if the system
	 * is running without any problems, otherwise will the message describing the error be returned.
	 * 
	 * @throws IllegalRequestException if the caller isn't authorized to perform this call.
	 */
	String getStatus(String serviceName) throws IllegalRequestException;

}