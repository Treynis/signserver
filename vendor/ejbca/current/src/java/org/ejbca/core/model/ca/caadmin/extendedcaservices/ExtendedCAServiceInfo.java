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
 * Should be enherited by all ExtendedCAServiceInfo Value objects.
 * These classes are used to retrive general information about the service
 * and alse used to send parameters to the service when creating it.  
 *
 * @version $Id: ExtendedCAServiceInfo.java 5585 2008-05-01 20:55:00Z anatom $
 */
public abstract class ExtendedCAServiceInfo  implements Serializable {    
       	  
    /**
     * Constants indicating the status of the service.     
     */   	  
    public static final int STATUS_INACTIVE = 1;       	  
	public static final int STATUS_ACTIVE   = 2;
	
	public static final int TYPE_OCSPEXTENDEDSERVICE   = 1; 
	public static final int TYPE_XKMSEXTENDEDSERVICE   = 2; 
	public static final int TYPE_CMSEXTENDEDSERVICE = 3; 
	   
	private int status = STATUS_INACTIVE;  
	   
    public ExtendedCAServiceInfo(int status){
      this.status = status;
    }
    
    public int getStatus(){ return this.status; }

}
