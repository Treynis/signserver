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

import java.security.cert.X509Certificate;

import org.apache.log4j.Logger;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.protocol.xkms.common.XKMSConstants;
import org.ejbca.util.passgen.IPasswordGenerator;
import org.ejbca.util.passgen.PasswordGeneratorFactory;
import org.w3._2002._03.xkms_.KeyBindingAbstractType;
import org.w3._2002._03.xkms_.KeyBindingType;
import org.w3._2002._03.xkms_.ReissueRequestType;
import org.w3._2002._03.xkms_.ReissueResultType;
import org.w3c.dom.Document;

/**
 * Class generating a response for a reissue call
 * 
 * 
 * @author Philip Vendil 
 *
 * @version $Id: ReissueResponseGenerator.java 5585 2008-05-01 20:55:00Z anatom $
 */

public class ReissueResponseGenerator extends
		KRSSResponseGenerator {
	private static Logger log = Logger.getLogger(ReissueResponseGenerator.class);

	private static final InternalResources intres = InternalResources.getInstance();
	
	public ReissueResponseGenerator(String remoteIP, ReissueRequestType req, Document requestDoc) {
		super(remoteIP, req,requestDoc);
	}
	
	/**
	 * Returns a reissue response
	 */
	public ReissueResultType getResponse(boolean requestVerifies){
		ReissueResultType result = xkmsFactory.createReissueResultType();		
		super.populateResponse(result, requestVerifies);		
		ReissueRequestType req = (ReissueRequestType) this.req;
		

		if(resultMajor == null){ 		
			if(!checkValidRespondWithRequest(req.getRespondWith(),false)){
				resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
				resultMinor = XKMSConstants.RESULTMINOR_MESSAGENOTSUPPORTED;
			}

			if(resultMajor == null){ 
				if(resultMajor == null){ 
					X509Certificate cert = (X509Certificate) getPublicKeyInfo(req, false);
					boolean isCertValid = certIsValid(cert);
					if(isCertValid && confirmPOP(cert.getPublicKey())){						
						UserDataVO userData = findUserData(cert);
						if(userData != null){
							String password = "";
							boolean encryptedPassword = isPasswordEncrypted(req);
							if(isCertValid && XKMSConfig.isAutomaticReissueAllowed()){
								password = setUserStatusToNew(userData);
							}else{							
								if(encryptedPassword){
									password = getEncryptedPassword(requestDoc, userData.getPassword());
								}else{
									password = getClearPassword(req, userData.getPassword());
								}
							}

							
							if(password != null ){
								X509Certificate newCert = registerReissueOrRecover(false,true, result, userData,password, cert.getPublicKey(), null);
								if(newCert != null){
									KeyBindingAbstractType keyBinding = getResponseValues(req.getReissueKeyBinding(), newCert, false, true);
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
     * Method that sets the users status to 'new' and a 
     * default password
     * @param the userdata of the user
     * @return the new password or null of operation failed.
     */
	private String setUserStatusToNew(UserDataVO userdata) {
		String retval = null;
		try {
			IPasswordGenerator passwordGenerator = PasswordGeneratorFactory.getInstance(PasswordGeneratorFactory.PASSWORDTYPE_LETTERSANDDIGITS);
			String password= passwordGenerator.getNewPassword(8, 8);

			userdata.setStatus(UserDataConstants.STATUS_NEW);
			userdata.setPassword(password);

			getUserAdminSession().changeUser(raAdmin, userdata, true);
			retval = password;
		} catch (Exception e) {
			log.error(intres.getLocalizedMessage("xkms.errorsettinguserstatus", userdata.getUsername()),e);			
		}
		
		return retval;
	}




}
