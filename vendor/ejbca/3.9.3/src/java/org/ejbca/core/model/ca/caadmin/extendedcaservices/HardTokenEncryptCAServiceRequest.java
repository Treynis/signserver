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
 * Class used when requesting hard token encrypt related services from a CA.  
 *
 * @version $Id: HardTokenEncryptCAServiceRequest.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class HardTokenEncryptCAServiceRequest extends ExtendedCAServiceRequest implements Serializable {    
 
	public static final int COMMAND_ENCRYPTDATA = 1;
	public static final int COMMAND_DECRYPTDATA = 2;
	
    private int command;
    private byte[] data;

    public HardTokenEncryptCAServiceRequest(int command, byte[] data) {
        this.command = command;
        this.data = data;
    }

    
    public int getCommand(){
    	return command;    	
    }
    
    /**
     *  Returns data beloning to the decrypt keys request, returns null oterwise.
     */
    
    public  byte[] getData(){
    	return data;
    }
    
    
}
