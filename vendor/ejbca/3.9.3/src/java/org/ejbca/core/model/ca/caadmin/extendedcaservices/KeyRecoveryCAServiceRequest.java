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
import java.security.KeyPair;


/**
 * Class used when requesting key recovery related services from a CA.  
 *
 * @version $Id: KeyRecoveryCAServiceRequest.java 7018 2009-02-23 18:13:02Z anatom $
 */
public class KeyRecoveryCAServiceRequest extends ExtendedCAServiceRequest implements Serializable {    
 
	public static final int COMMAND_ENCRYPTKEYS = 1;
	public static final int COMMAND_DECRYPTKEYS = 2;
	
    private int command;
    private byte[] keydata;
    private KeyPair keypair;
    /** Constructor for KeyRecoveryCAServiceRequest
     */                   
    public KeyRecoveryCAServiceRequest(int command, byte[] keydata) {
        this.command = command;
        this.keydata = keydata;
    }

    /** Constructor for KeyRecoveryCAServiceRequest
     */                   
    public KeyRecoveryCAServiceRequest(int command, KeyPair keypair) {
    	this.command = command;
    	this.keypair = keypair;
    }
    
    public int getCommand(){
    	return command;    	
    }
    
    /**
     *  Returns data beloning to the decrypt keys request, returns null oterwise.
     */
    
    public  byte[] getKeyData(){
    	byte[] ret = null;
    	if(command == COMMAND_DECRYPTKEYS) {
        	ret = keydata;
    	}
    	return ret;
    }

    /**
     *  Returns data beloning to the encrypt keys request, returns null oterwise.
     */
    
    public  KeyPair getKeyPair(){
    	KeyPair ret = null;
    	if(command == COMMAND_ENCRYPTKEYS) {
        	ret = keypair;
    	}
    	return ret;
    }
    
}
