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
package org.ejbca.core.model.services;

import java.io.Serializable;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.UpgradeableDataHashMap;
import org.ejbca.core.model.services.workers.EmailSendingWorker;

/**
 * Value class used for persist the worker, interval and action configurations
 * to database
 * 
 * @author Philip Vendil 2006 sep 27
 *
 * @version $Id: ServiceConfiguration.java 8382 2009-11-30 15:44:20Z anderspki $
 */
public class ServiceConfiguration extends UpgradeableDataHashMap implements Serializable, Cloneable {

    private static final Logger log = Logger.getLogger(ServiceConfiguration.class);
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();
    
	private static final float LATEST_VERSION = 3;
	
	private static final String INTERVALCLASSPATH = "INTERVALCLASSPATH";
	private static final String INTERVALPROPERTIES = "INTERVALPROPERTIES";
	private static final String WORKERCLASSPATH = "WORKERCLASSPATH";
	private static final String WORKERPROPERTIES = "WORKERPROPERTIES";
	private static final String ACTIONCLASSPATH = "ACTIONCLASSPATH";
	private static final String ACTIONPROPERTIES = "ACTIONPROPERTIES";
	private static final String DESCRIPTION = "DESCRIPTION";
	private static final String ACTIVE = "ACTIVE";
	private static final String NEXTRUNTIMESTAMP = "NEXTRUNTIMESTAMP";
	private static final String OLDRUNTIMESTAMP = "OLDRUNTIMESTAMP";
	private static final String HIDDEN = "HIDDEN";
	
	/**
	 * Constructor used to create a new service configuration.
	 */
	public ServiceConfiguration(){
		setActive(false);
		setHidden(false);
		setDescription("");
		setActionClassPath("");
		setActionProperties(new Properties());
		setWorkerClassPath("");
		setWorkerProperties(new Properties());
		setIntervalClassPath("");
		setIntervalProperties(new Properties());
		setNextRunTimestamp(new Date(0));
	}
	
	
	/**
	 * @return the Action Class Path
	 */
	public String getActionClassPath() {
		return (String) data.get(ACTIONCLASSPATH);
	}

	/**
	 * @param actionClassPath the actionClassPath to set
	 */
	public void setActionClassPath(String actionClassPath) {
		data.put(ACTIONCLASSPATH,actionClassPath);
	}

	/**
	 * @return the actionProperties
	 */
	public Properties getActionProperties() {
		return (Properties) data.get(ACTIONPROPERTIES);
	}

	/**
	 * @param actionProperties the actionProperties to set
	 */
	public void setActionProperties(Properties actionProperties) {
		data.put(ACTIONPROPERTIES, actionProperties);
	}

	/**
	 * @return the active
	 */
	public boolean isActive() {
		return ((Boolean) data.get(ACTIVE)).booleanValue();
	}

	/**
	 * @param active the active to set
	 */
	public void setActive(boolean active) {
		data.put(ACTIVE, new Boolean(active));
	}
	
	public boolean isHidden() {
		return ((Boolean) data.get(HIDDEN)).booleanValue();
	}
	
	public void setHidden(boolean b) {
		data.put(HIDDEN, new Boolean(b));
	}
	
	/**
	 * @return the date of the next time this service should run.
	 * This is a special service flag ensuring that not two nodes
	 * runs the service at the same time.
	 * 
	 */
	public Date getNextRunTimestamp() {
		if(data.get(NEXTRUNTIMESTAMP) == null){
			return new Date(0);
		}
		
		return new Date(((Long) data.get(NEXTRUNTIMESTAMP)).longValue());
	}

	/**
	 * @return the last value of the previous method.
	 * @see setNextRunTimestap
	 * 
	 */
	public Date getOldRunTimestamp() {
		if(data.get(OLDRUNTIMESTAMP) == null){
			return new Date(0);
		}
		
		return new Date(((Long) data.get(OLDRUNTIMESTAMP)).longValue());
	}

	/**
	 * @param nextRunTimeStamp the active time to set
	 * This method saves the previous value so that workers can access
	 * when they are to be run in the future as well as when they
	 * should have been run.
	 */
	public void setNextRunTimestamp(Date nextRunTimeStamp) {
		data.put (OLDRUNTIMESTAMP, new Long(getNextRunTimestamp ().getTime()));
		data.put(NEXTRUNTIMESTAMP, new Long(nextRunTimeStamp.getTime()));
	}


	/**
	 * @return the description
	 */
	public String getDescription() {
		return (String) data.get(DESCRIPTION);
	}

	/**
	 * @param description the description to set
	 */
	public void setDescription(String description) {
		data.put(DESCRIPTION, description);
	}

	/**
	 * @return the intervalClassPath
	 */
	public String getIntervalClassPath() {
		return (String) data.get(INTERVALCLASSPATH);
	}

	/**
	 * @param intervalClassPath the intervalClassPath to set
	 */
	public void setIntervalClassPath(String intervalClassPath) {
		data.put(INTERVALCLASSPATH,intervalClassPath);
	}

	/**
	 * @return the intervalProperties
	 */
	public Properties getIntervalProperties() {
		return (Properties) data.get(INTERVALPROPERTIES);
	}

	/**
	 * @param intervalProperties the intervalProperties to set
	 */
	public void setIntervalProperties(Properties intervalProperties) {
		data.put(INTERVALPROPERTIES, intervalProperties);
	}

	/**
	 * @return the workerClassPath
	 */
	public String getWorkerClassPath() {
		return (String) data.get(WORKERCLASSPATH);
	}

	/**
	 * @param workerClassPath the workerClassPath to set
	 */
	public void setWorkerClassPath(String workerClassPath) {
		data.put(WORKERCLASSPATH,workerClassPath);
	}

	/**
	 * @return the workerProperties
	 */
	public Properties getWorkerProperties() {
		return (Properties) data.get(WORKERPROPERTIES);
	}

	/**
	 * @param workerProperties the workerProperties to set
	 */
	public void setWorkerProperties(Properties workerProperties) {
		data.put(WORKERPROPERTIES, workerProperties);
	}

	public float getLatestVersion() {
		return LATEST_VERSION;
	}

	public void upgrade() {
		if (Float.compare(LATEST_VERSION, getVersion()) > 0) {
            // New version of the class, upgrade
			String msg = intres.getLocalizedMessage("services.upgrade", new Float(getVersion()));
            log.info(msg);

            log.debug(LATEST_VERSION);
			// We changed the names of properties between v1 and v2, so we have to upgrade a few of them
			if (Float.compare(LATEST_VERSION, Float.valueOf(2)) >= 0) {
	            log.debug("Upgrading to version 2");
				Properties prop = getWorkerProperties();
				if (prop != null) {
					String caids = prop.getProperty("worker.emailexpiration.caidstocheck");
					String timebeforexpire = prop.getProperty("worker.emailexpiration.timebeforeexpiring");
					String timeunit = prop.getProperty("worker.emailexpiration.timeunit");
					String sendtousers = prop.getProperty("worker.emailexpiration.sendtoendusers");
					String sendtoadmins = prop.getProperty("worker.emailexpiration.sendtoadmins");
					String usersubject = prop.getProperty("worker.emailexpiration.usersubject");
					String usermessage = prop.getProperty("worker.emailexpiration.usermessage");
					String adminsubject = prop.getProperty("worker.emailexpiration.adminsubject");
					String adminmessage = prop.getProperty("worker.emailexpiration.adminmessage");
					 
					if (caids != null) {
						prop.setProperty(BaseWorker.PROP_CAIDSTOCHECK, caids);
						prop.remove("worker.emailexpiration.caidstocheck");
					}
					if (timebeforexpire != null) {
						prop.setProperty(BaseWorker.PROP_TIMEBEFOREEXPIRING, timebeforexpire);
						prop.remove("worker.emailexpiration.timebeforeexpiring");
					}
					if (timeunit != null) {
						prop.setProperty(BaseWorker.PROP_TIMEUNIT, timeunit);
						prop.remove("worker.emailexpiration.timeunit");
					}
					if (sendtousers != null) {
						prop.setProperty(EmailSendingWorker.PROP_SENDTOENDUSERS, sendtousers);
						prop.remove("worker.emailexpiration.sendtoendusers");
					}
					if (sendtoadmins != null) {
						prop.setProperty(EmailSendingWorker.PROP_SENDTOADMINS, sendtoadmins);
						prop.remove("worker.emailexpiration.sendtoadmins");
					}
					if (usersubject != null) {
						prop.setProperty(EmailSendingWorker.PROP_USERSUBJECT, usersubject);
						prop.remove("worker.emailexpiration.usersubject");
					}
					if (usermessage != null) {
						prop.setProperty(EmailSendingWorker.PROP_USERMESSAGE, usermessage);
						prop.remove("worker.emailexpiration.usermessage");
					}
					if (adminsubject != null) {
						prop.setProperty(EmailSendingWorker.PROP_ADMINSUBJECT, adminsubject);
						prop.remove("worker.emailexpiration.adminsubject");
					}
					if (adminmessage != null) {
						prop.setProperty(EmailSendingWorker.PROP_ADMINMESSAGE, adminmessage);
						prop.remove("worker.emailexpiration.adminmessage");
					}
					setWorkerProperties(prop);
				}
				
				if (Float.compare(LATEST_VERSION, Float.valueOf(3)) >= 0) {
		            log.debug("Upgrading to version 3");
		            // The hidden field was added
		            setHidden(false);
				}
			}
			data.put(VERSION, new Float(LATEST_VERSION));
		}		
	}
	
    public Object clone() throws CloneNotSupportedException {
        ServiceConfiguration clone = new ServiceConfiguration();
        HashMap clonedata = (HashMap) clone.saveData();

        Iterator i = (data.keySet()).iterator();
        while(i.hasNext()){
          Object key = i.next();
          clonedata.put(key, data.get(key));
        }

        clone.loadData(clonedata);
        return clone;
      }


	

}
