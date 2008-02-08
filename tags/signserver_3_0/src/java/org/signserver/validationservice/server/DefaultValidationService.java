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

package org.signserver.validationservice.server;

import java.util.List;

import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.SignServerException;
import org.signserver.validationservice.common.ICertificate;
import org.signserver.validationservice.common.ValidateRequest;
import org.signserver.validationservice.common.ValidateResponse;
import org.signserver.validationservice.common.Validation;
import org.signserver.validationservice.common.Validation.Status;

/**
 * Default validation service performing a full verification and
 * iterating between the configured validators for revocation status
 * 
 * 
 * @author Philip Vendil 29 nov 2007
 *
 * @version $Id: DefaultValidationService.java,v 1.1 2007-12-02 20:35:17 herrvendil Exp $
 */

public class DefaultValidationService extends BaseValidationService {

	/**
	 * @see org.signserver.validationservice.server.IValidationService#validate(org.signserver.validationservice.common.ValidateRequest)
	 */
	public ValidateResponse validate(ValidateRequest validationRequest)
			throws IllegalRequestException, CryptoTokenOfflineException,
			SignServerException {
		
		
		// Get Certificate Chain
		List<ICertificate> cAChain = getCertificateChain(validationRequest.getCertificate());
				
		if(cAChain == null){
			throw new IllegalRequestException("Error issuer of given certificate isn't supported.");
		}
		// Verify and check validity
		Validation validation = ICertificateManager.verifyCertAndChain(validationRequest.getCertificate(),cAChain);

		if(validation.getStatus().equals(Status.VALID)){

			// Check Certificate type
			if(!getCertTypeChecker().checkType(validationRequest.getCertificate(), validationRequest.getCertType())){
                validation = new Validation(validationRequest.getCertificate(),cAChain,Validation.Status.BADCERTTYPE,"Error certificate doesn't fulfill the specification of the requested certificate type");
			}else{
				// Check revocation of the certificate and for the entire chain.
				validation = validationCache.get(validationRequest.getCertificate());
				if(validation == null){
					for(IValidator validator : validators.values()){
						validation = validator.validate(validationRequest.getCertificate());
						if(validation != null){
							validationCache.put(validationRequest.getCertificate(), validation);
							break;
						}
					}
				}
				
				if(validation == null){
                   throw new IllegalRequestException("Error no validators in validation service " + workerId + " supports the issuer of given CA " + validationRequest.getCertificate().getIssuer());
				}
				if(validation.getStatus().equals(Validation.Status.VALID)){
					for(ICertificate cacert : cAChain){
						Validation cavalidation = validationCache.get(validationRequest.getCertificate());
						if(cavalidation == null){
							for(IValidator validator : validators.values()){
								cavalidation = validator.validate(cacert);
								if(cavalidation != null){
									validationCache.put(cacert, cavalidation);
									break;
								}
							}
						}
						if(cavalidation == null){
			                   throw new IllegalRequestException("Error no validators in validation service " + workerId + " supports the issuer of given CA " + validationRequest.getCertificate().getIssuer());
						}
						if(cavalidation != null && !cavalidation.getStatus().equals(Validation.Status.VALID)){
							validation = new Validation(validationRequest.getCertificate(),cAChain,Validation.Status.CAREVOKED," Error CA issuing the requested certificate was revoked",cavalidation.getRevokedDate(),cavalidation.getRevokationReason());
							break;	
						}
					}				
				}
			}
		}
		
		
		
		return new ValidateResponse(validation);
	}



	/**
	 * Method returning the entire certificate chain for the given certificate
	 * from the configured validators.
	 * @param certificate to verify
	 * @return a certificate chain with the root CA last.
	 */
	private List<ICertificate> getCertificateChain(ICertificate certificate) {
		List<ICertificate> retval = null; 
		for(IValidator validator : validators.values()){
			retval = validator.getCertificateChain(certificate);
			if(retval != null){
				break;
			}
		}
		return retval;
	}

}
