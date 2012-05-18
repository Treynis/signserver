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
 
package org.ejbca.ui.web.admin.cainterface;


import java.util.HashMap;

import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocal;
import org.ejbca.core.model.log.Admin;

/**
 * A class used to improve performance by proxying certificateprofile id to certificate name mappings by minimizing the number of needed lockups over rmi.
 * 
 * @version $Id: CertificateProfileNameProxy.java 6997 2009-02-20 12:03:58Z anatom $
 */
public class CertificateProfileNameProxy implements java.io.Serializable {
    
    /** Creates a new instance of ProfileNameProxy */
    public CertificateProfileNameProxy(Admin administrator, ICertificateStoreSessionLocal certificatestoresession){
      this.certificatestoresession = certificatestoresession;
      
      certificateprofilenamestore = new HashMap(); 
      this.admin= administrator;
        
    }
    
    /**
     * Method that first tries to find certificateprofile name in local hashmap and if it doesn't exists looks it up over RMI.
     *
     * @param certificateprofileid the certificateprofile id number to look up.
     * @return the certificateprofilename or null if no certificateprofilename is relatied to the given id
     */
    public String getCertificateProfileName(int certificateprofileid)  {
      String returnval = null;  
      // Check if name is in hashmap
      returnval = (String) certificateprofilenamestore.get(new Integer(certificateprofileid));
      
      if(returnval==null){
        // Retreive profilename 
        returnval = certificatestoresession.getCertificateProfileName(admin, certificateprofileid);
        if(returnval != null) {
          certificateprofilenamestore.put(new Integer(certificateprofileid),returnval);
        }
      }    
       
      return returnval;
    }
    
    // Private fields
    private HashMap                        certificateprofilenamestore;
    private ICertificateStoreSessionLocal  certificatestoresession;
    private Admin                          admin;

}
