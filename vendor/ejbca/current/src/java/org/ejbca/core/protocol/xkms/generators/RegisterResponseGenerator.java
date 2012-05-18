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

package org.ejbca.core.protocol.xkms.generators;

import java.security.PublicKey;
import java.security.cert.X509Certificate;

import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.protocol.xkms.common.XKMSConstants;
import org.w3._2002._03.xkms_.KeyBindingAbstractType;
import org.w3._2002._03.xkms_.KeyBindingType;
import org.w3._2002._03.xkms_.RegisterRequestType;
import org.w3._2002._03.xkms_.RegisterResultType;
import org.w3c.dom.Document;

/**
 * Class generating a response for a register call
 * 
 * 
 * @author Philip Vendil 
 *
 * @version $Id: RegisterResponseGenerator.java 5585 2008-05-01 20:55:00Z anatom $
 */

public class RegisterResponseGenerator extends
		KRSSResponseGenerator {
	//private static Logger log = Logger.getLogger(RegisterResponseGenerator.class);

	public RegisterResponseGenerator(String remoteIP, RegisterRequestType req, Document requestDoc) {
		super(remoteIP, req,requestDoc);
	}
	
	/**
	 * Returns a register response
	 */
	public RegisterResultType getResponse(boolean requestVerifies){
		RegisterResultType result = xkmsFactory.createRegisterResultType();		
		super.populateResponse(result, requestVerifies);		
		RegisterRequestType req = (RegisterRequestType) this.req;
		

		if(resultMajor == null){ 		
			if(!checkValidRespondWithRequest(req.getRespondWith(),false)){
				resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
				resultMinor = XKMSConstants.RESULTMINOR_MESSAGENOTSUPPORTED;
			}

			if(resultMajor == null){ 
				

				if(resultMajor == null){ 
					PublicKey publicKey = getPublicKey(req);					
					if(confirmPOP(publicKey)){
						String subjectDN = getSubjectDN(req);
						UserDataVO userData = findUserData(subjectDN);
						if(userData != null){
							String password = "";	
							boolean encryptedPassword = isPasswordEncrypted(req);
							if(encryptedPassword){
								password = getEncryptedPassword(requestDoc, userData.getPassword());
							}else{
								password = getClearPassword(req, userData.getPassword());
							}
							String revocationCode = getRevocationCode(req);
							if(password != null ){
								X509Certificate cert = registerReissueOrRecover(false,false, result, userData,password, publicKey, revocationCode);
								if(cert != null){
									KeyBindingAbstractType keyBinding = getResponseValues(req.getPrototypeKeyBinding(), cert, false, true);
									result.getKeyBinding().add((KeyBindingType) keyBinding);
								}
							}
						}
					}
				}
			}
		}
		
		if(resultMajor == null){ 
			resultMajor = XKMSConstants.RESULTMAJOR_SUCCESS;
		}
		  		   
		setResult(result);		
		
		return result;
	}

	/**
	 * Method extracting the public key from the message.
	 * @param req the request
	 * @return the public key or null if no public key could be found.
	 */
	protected PublicKey getPublicKey(RegisterRequestType req){
		Object retval = getPublicKeyInfo(req, true);
		if(retval instanceof X509Certificate){
			retval = ((X509Certificate) retval).getPublicKey();
		}

		
		return (PublicKey) retval;
	}
	




	
}
