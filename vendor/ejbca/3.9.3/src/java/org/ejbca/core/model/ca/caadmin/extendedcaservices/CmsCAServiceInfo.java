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
import java.util.List;



/**
 * Class used mostly when creating service. Also used when info about the services 
 * is neesed
 * 
 * @version $Id: CmsCAServiceInfo.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class CmsCAServiceInfo extends BaseSigningCAServiceInfo implements Serializable {    
       
    /**
     * Used when creating new service.
     */
       
    public CmsCAServiceInfo(int status,
                             String subjectdn, 
                             String subjectaltname, 
                             String keyspec, 
                             String keyalgorithm){
        super(status, subjectdn, subjectaltname, keyspec, keyalgorithm);                       	
    }
    
	/**
	 * Used when returning information from service
	 */
       
	public CmsCAServiceInfo(int status,
							 String subjectdn, 
							 String subjectaltname, 
							 String keyspec, 
							 String keyalgorithm,
							 List certchain){
		super(status, subjectdn, subjectaltname, keyspec, keyalgorithm, certchain);                       	
	}    
    
    /*
     * Used when updating existing services, only status is used.
     */
    public CmsCAServiceInfo(int status, boolean renew){
      super(status, renew);	
    }

}
