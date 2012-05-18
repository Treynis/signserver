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
 
/*
 * HardTokenIssuer.java
 *
 * Created on den 19 januari 2003, 12:53
 */
package org.ejbca.core.model.hardtoken;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;

import org.ejbca.core.model.UpgradeableDataHashMap;

/**
 * HardTokenIssuer V3 is a class representing the data saved for each HardTokenIssuer.
 * it isn't back compatible with the old version.
 *
 * @author  TomSelleck
 * @version $Id: HardTokenIssuer.java 5585 2008-05-01 20:55:00Z anatom $
 */
public  class HardTokenIssuer extends UpgradeableDataHashMap implements Serializable, Cloneable {

    // Default Values
    public static final float LATEST_VERSION = 1;

    // Protexted Constants, must be overloaded by all deriving classes.   
    protected static final String AVAILABLEHARDTOKENSPROFILES  = "availablehardtokensprofiles"; 
	protected static final String DESCRIPTION                  = "description"; 
    // Public Constructors.
    
    public HardTokenIssuer(){
      data.put(AVAILABLEHARDTOKENSPROFILES,new ArrayList());
      data.put(DESCRIPTION,"");         
    }
    
    // Public Methods
    
    // Availablehardtokens defines which hard tokens the issuer is able to issue. 
    public ArrayList getAvailableHardTokenProfiles(){
      return  (ArrayList) data.get(AVAILABLEHARDTOKENSPROFILES); 
    }
    
    public void setAvailableHardTokenProfiles(ArrayList availablehardtokens){
      data.put(AVAILABLEHARDTOKENSPROFILES,availablehardtokens); 
    }    
    
    public String getDescription(){
      return  (String) data.get(DESCRIPTION);	
    }

	public void setDescription(String description){
	  data.put(DESCRIPTION, description);	
	}    
    
    public void setField(String field, Object value){ 
       data.put(field,value);   
    }
    
    /** Implemtation of UpgradableDataHashMap function getLatestVersion */
    public float getLatestVersion(){
       return LATEST_VERSION;
    }

    /** Implemtation of UpgradableDataHashMap function upgrade. */

    public void upgrade(){	
    }
    
    public Object clone() throws CloneNotSupportedException {
      HardTokenIssuer clone = new HardTokenIssuer();
      HashMap clonedata = (HashMap) clone.saveData();
      
      Iterator i = (data.keySet()).iterator();
      while(i.hasNext()){
        Object key = i.next();  
        clonedata.put(key,data.get(key)); 
      }
      
      clone.loadData(clonedata);
      return clone;
    }    

}
