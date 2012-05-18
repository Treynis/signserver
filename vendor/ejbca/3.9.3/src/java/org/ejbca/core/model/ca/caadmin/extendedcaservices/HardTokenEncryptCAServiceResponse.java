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
 
package org.ejbca.core.model.ca.caadmin.extendedcaservices;

import java.io.Serializable;


/**
 * Class used when delevering hard token encrypt service response from a CA.  
 *
 * @version $Id: HardTokenEncryptCAServiceResponse.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class HardTokenEncryptCAServiceResponse extends ExtendedCAServiceResponse implements Serializable {    
             
	public static final int TYPE_ENCRYPTRESPONSE = 1;
	public static final int TYPE_DECRYPTRESPONSE = 1;
    
    private int type;
    private byte[] data;
	
    public HardTokenEncryptCAServiceResponse(int type, byte[] data) {
       this.type=type;
       this.data=data;
    } 
    
           
    /**
     * @return type of response, one of the TYPE_ constants.
     */
    public int getType(){
    	return type;
    }
    
    /**
     *  Method returning the  data if the type of response. 
     *  
     */
    
    public byte[] getData(){    	
    	return data;
    }
    
}
