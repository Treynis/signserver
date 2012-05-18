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
 
package org.ejbca.core.model.ca.publisher;

import java.io.UnsupportedEncodingException;
import java.util.HashMap;
import java.util.Iterator;

import org.apache.log4j.Logger;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.UpgradeableDataHashMap;
import org.ejbca.core.model.ra.ExtendedInformation;
import org.ejbca.core.model.ra.UserDataVO;


/**
 * The model holds additional data needed to be able to republish a certificate or CRL after publishing have failed.
 * This data will be stored in PublicFailQueueData. 
 *
 * @author Tomas Gustavsson
 * @version $Id: PublisherQueueVolatileData.java 8422 2009-12-10 13:18:39Z primelars $
 */
public class PublisherQueueVolatileData extends UpgradeableDataHashMap implements java.io.Serializable, Cloneable {

	private static final Logger log = Logger.getLogger(PublisherQueueVolatileData.class);
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    /**
     * Determines if a de-serialized file is compatible with this class.
     *
     * Maintainers must change this value if and only if the new version
     * of this class is not compatible with old versions. See Sun docs
     * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
     * /serialization/spec/version.doc.html> details. </a>
     *
     */
    private static final long serialVersionUID = 100L;
    
    public static final float LATEST_VERSION = 1;    

    // private fields.
    
    // Because the UserData information may be volatile, usernames can be re-used for several different certificates
    // we will store the actual user data used when we tried to publish so we can be sure to use the same.
    /** Username, links to UserData */
    private static final String USERNAME = "username";
    /** Password if sent to publisher. */
    private static final String PASSWORD = "password";
    /** DN from UserData */
    private static final String USERDN = "userdn";
    /** ExtendedInformation from UserData */
    private static final String EXTENDEDINFORMATION = "extendedinformation";
        
    
    // Public constants

    // Public methods.
    /** Creates a new instance of EndEntity Profile */
    public PublisherQueueVolatileData() {
    }

    public String getUsername(){ 
    	String ret = (String) data.get(USERNAME);
    	if (ret == null) {
    		ret = "";
    	}
    	return ret;
    }
    public void setUsername(String username) {
    	if (username != null) {
    		data.put(USERNAME,username);
    	}
    }
    
    public String getPassword(){ 
    	String ret = (String) data.get(PASSWORD);
    	if (ret == null) {
    		ret = "";
    	}
    	return ret;
    }
    public void setPassword(String password) {
    	if (password != null) {
    		data.put(PASSWORD,password);
    	}
    }
    
    public String getUserDN(){ 
    	String ret = (String) data.get(USERDN);
    	if (ret == null) {
    		ret = "";
    	}
    	return ret;
    }
	public void setUserDN(String userDN) {
    	if (userDN != null) {
    		data.put(USERDN,userDN);
    	}
	}

    public ExtendedInformation getExtendedInformation() {
    	String str = (String)data.get(EXTENDEDINFORMATION);
    	ExtendedInformation ret = new ExtendedInformation();
    	if (str != null) {
    		ret = UserDataVO.getExtendedInformation(str);
    	}
    	return ret;
    }
    
    public void setExtendedInformation(ExtendedInformation ei) {
    	String eidata = null;
    	try {
			eidata = UserDataVO.extendedInformationToStringData(ei);
		} catch (UnsupportedEncodingException e) {
			log.error(e);
		}
    	if (eidata != null) {
    		data.put(EXTENDEDINFORMATION, eidata);
    	}
    }
    
    public Object clone() throws CloneNotSupportedException {
      PublisherQueueVolatileData clone = new PublisherQueueVolatileData();
      HashMap clonedata = (HashMap) clone.saveData();

      Iterator i = (data.keySet()).iterator();
      while(i.hasNext()){
        Object key = i.next();
        clonedata.put(key, data.get(key));
      }

      clone.loadData(clonedata);
      return clone;
    }

    /** Function required by XMLEncoder to do a proper serialization. */
    public void setData( Object hmData ) { loadData(hmData); }
    /** Function required by XMLEncoder to do a proper serialization. */
    public Object getData() {return saveData();}
    
    /** Implementation of UpgradableDataHashMap function getLatestVersion */
    public float getLatestVersion(){
       return LATEST_VERSION;
    }

    /** Implementation of UpgradableDataHashMap function upgrade. */

    public void upgrade(){
    	if(Float.compare(LATEST_VERSION, getVersion()) != 0) {
    		// New version of the class, upgrade
			String msg = intres.getLocalizedMessage("publisher.queuedataupgrade", new Float(getVersion()));
            log.info(msg);
    		
    		data.put(VERSION, new Float(LATEST_VERSION));
    	}
    }
    
}
