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
import java.util.Iterator;
import java.util.List;

import org.ejbca.core.protocol.xkms.common.XKMSConstants;
import org.w3._2002._03.xkms_.KeyBindingType;
import org.w3._2002._03.xkms_.ValidateRequestType;
import org.w3._2002._03.xkms_.ValidateResultType;

/**
 * Class generating a response for a locate call
 * 
 * 
 * @author Philip Vendil 2006 sep 27
 *
 * @version $Id: ValidateResponseGenerator.java 5585 2008-05-01 20:55:00Z anatom $
 */

public class ValidateResponseGenerator extends
		KISSResponseGenerator {
	

	public ValidateResponseGenerator(String remoteIP, ValidateRequestType req) {
		super(remoteIP, req);
	}
	
	/**
	 * Returns a locate response
	 */
	public ValidateResultType getResponse(boolean requestVerifies){
		ValidateResultType result = xkmsFactory.createValidateResultType();		
		super.populateResponse(result, requestVerifies);		
		ValidateRequestType req = (ValidateRequestType) this.req;

		
		if(resultMajor == null){
			if(!checkValidRespondWithRequest(req.getRespondWith())){
				resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
				resultMinor = XKMSConstants.RESULTMINOR_MESSAGENOTSUPPORTED;
			}

			if(resultMajor == null){ 
				List<X509Certificate> queryResult = processRequest(req.getQueryKeyBinding());

				if(resultMajor == null){ 		
					Iterator<X509Certificate> iter = queryResult.iterator();
					while(iter.hasNext()){
						X509Certificate nextCert = iter.next();
						result.getKeyBinding().add((KeyBindingType) getResponseValues(req.getQueryKeyBinding(),nextCert,true,false));

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
	

    
    


}
