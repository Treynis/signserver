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

import org.ejbca.core.model.ca.catoken.CATokenConstants;



/**
 * Base class for CAServiceInfo used by extended services that does signing 
 * 
 * 
 * @version $Id: BaseSigningCAServiceInfo.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class BaseSigningCAServiceInfo extends ExtendedCAServiceInfo implements Serializable {    
       
    private String subjectdn      = null;
    private String subjectaltname = null;   
	private String keyspec        = "1024"; // Default key length
    private String keyalgorithm   = CATokenConstants.KEYALGORITHM_RSA; // Default key algo
    private List   certchain  = null;
    
    private boolean renew = false;
           
    /**
     * Used when creating new service.
     */
       
    public BaseSigningCAServiceInfo(int status,
                             String subjectdn, 
                             String subjectaltname, 
                             String keyspec, 
                             String keyalgorithm){
      super(status);                       	
      this.subjectdn = subjectdn;
      this.subjectaltname = subjectaltname;    	
      this.keyspec = keyspec;
      this.keyalgorithm = keyalgorithm; 	 
    }
    
	/**
	 * Used when returning information from service
	 */
       
	public BaseSigningCAServiceInfo(int status,
							 String subjectdn, 
							 String subjectaltname, 
							 String keyspec, 
							 String keyalgorithm,
							 List certpath){
	  super(status);                       	
	  this.subjectdn = subjectdn;
	  this.subjectaltname = subjectaltname;    	
	  this.keyspec = keyspec;
	  this.keyalgorithm = keyalgorithm; 	 
	  this.certchain = certpath;
	}    
    
    /*
     * Used when updating existing services, only status is used.
     */
    public BaseSigningCAServiceInfo(int status, boolean renew){
      super(status);	
      this.renew = renew;
    }
    
    public String getSubjectDN(){ return this.subjectdn; }
    public String getSubjectAltName(){ return this.subjectaltname; }
    public String getKeySpec(){ return this.keyspec; }
    public String getKeyAlgorithm(){ return this.keyalgorithm; }
    public boolean getRenewFlag(){ return this.renew; } 
    public List getCertificatePath(){ return this.certchain;}   
    
    

}
