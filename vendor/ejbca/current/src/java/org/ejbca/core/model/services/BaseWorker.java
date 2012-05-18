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

import java.util.ArrayList;
import java.util.Collection;
import java.util.Properties;

import org.apache.log4j.Logger;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.services.intervals.DummyInterval;

/**
 * Abstract base class that initializes the worker and its interval and action.
 * 
 * @author Philip Vendil 2006 sep 27
 *
 * @version $Id: BaseWorker.java 8367 2009-11-30 12:05:35Z anderspki $
 */
public abstract class BaseWorker extends BaseServiceComponent implements IWorker {

	private static final Logger log = Logger.getLogger(BaseWorker.class);
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();
    
	/** Should be a ';' separated string of CAIds. */
	public static final String PROP_CAIDSTOCHECK     = "worker.caidstocheck";
	
	/** The time in 'timeunit' that a user is allowed to have status 'new' since last modification date */
	public static final String PROP_TIMEBEFOREEXPIRING = "worker.timebeforeexpiring";
	
	/** Unit in days, hours or seconds */
	public static final String PROP_TIMEUNIT           = "worker.timeunit";

	public static final String UNIT_SECONDS = "SECONDS";
	public static final String UNIT_MINUTES = "MINUTES";
	public static final String UNIT_HOURS = "HOURS";
	public static final String UNIT_DAYS = "DAYS";
	
	public static final int UNITVAL_SECONDS = 1;
	public static final int UNITVAL_MINUTES = 60;
	public static final int UNITVAL_HOURS = 3600;
	public static final int UNITVAL_DAYS = 86400;

	public static final String[] AVAILABLE_UNITS = {UNIT_SECONDS, UNIT_MINUTES, UNIT_HOURS, UNIT_DAYS};
	public static final int[] AVAILABLE_UNITSVALUES = {UNITVAL_SECONDS, UNITVAL_MINUTES, UNITVAL_HOURS, UNITVAL_DAYS};
	

    protected Properties properties = null;
    protected String serviceName = null;
    protected ServiceConfiguration serviceConfiguration = null;
    private IAction action = null;
    private IInterval interval = null;
    
    private Admin admin = null;

	private transient Collection cAIdsToCheck = null;
	private transient long timeBeforeExpire = -1;

	/**
	 * @see org.ejbca.core.model.services.IWorker#init(org.ejbca.core.model.services.ServiceConfiguration, java.lang.String)
	 */
	public void init(Admin admin, ServiceConfiguration serviceConfiguration,
			String serviceName) {
		this.admin = admin;
		this.serviceName = serviceName;
		this.properties = serviceConfiguration.getWorkerProperties();
		this.serviceConfiguration = serviceConfiguration;
		
		String actionClassPath = serviceConfiguration.getActionClassPath();
		if(actionClassPath != null){
			try {
				action = (IAction) this.getClass().getClassLoader().loadClass(actionClassPath).newInstance();
				action.init(serviceConfiguration.getActionProperties(), serviceName);
			} catch (Exception e) {
				String msg = intres.getLocalizedMessage("services.erroractionclasspath", serviceName);
				log.error(msg,e);
			}       
		}else{
			log.debug("Warning no action class i defined for the service " + serviceName);
		}
		
		String intervalClassPath = serviceConfiguration.getIntervalClassPath();
		if(intervalClassPath != null){
			try {
				interval = (IInterval) this.getClass().getClassLoader().loadClass(intervalClassPath).newInstance();
				interval.init(serviceConfiguration.getIntervalProperties(), serviceName);
			} catch (Exception e) {
				String msg = intres.getLocalizedMessage("services.errorintervalclasspath", serviceName);
				log.error(msg,e);
			}       
		}else{
			String msg = intres.getLocalizedMessage("services.errorintervalclasspath", serviceName);
			log.error(msg);
		}
		
		if(interval == null){
			interval = new DummyInterval();
		}

	}

	
	/**
	 * @see org.ejbca.core.model.services.IWorker#getNextInterval()
	 */
	public long getNextInterval() {		
		return interval.getTimeToExecution();
	}
	
	protected IAction getAction(){
		if(action == null){
			String msg = intres.getLocalizedMessage("services.erroractionclasspath", serviceName);
			log.error(msg);
		}
		return action;
	}
	
	/**
	 * Returns the admin that should be used for other calls.
	 */
	protected Admin getAdmin(){
		return admin;
	}
	
	/** Returns the amount of time, in milliseconds that the expire time of configured for */
	protected long getTimeBeforeExpire()
	throws ServiceExecutionFailedException {
		if(timeBeforeExpire == -1){
			String unit = properties.getProperty(PROP_TIMEUNIT);
			if(unit == null){				
				String msg = intres.getLocalizedMessage("services.errorexpireworker.errorconfig", serviceName, "UNIT");
				throw new ServiceExecutionFailedException(msg);
			}
			int unitval = 0;
			for(int i=0;i<AVAILABLE_UNITS.length;i++){
				if(AVAILABLE_UNITS[i].equalsIgnoreCase(unit)){
					unitval = AVAILABLE_UNITSVALUES[i];
					break;
				}
			}
			if(unitval == 0){				
				String msg = intres.getLocalizedMessage("services.errorexpireworker.errorconfig", serviceName, "UNIT");
				throw new ServiceExecutionFailedException(msg);
			}
						
		    String value =  properties.getProperty(PROP_TIMEBEFOREEXPIRING);
		    int intvalue = 0;
		    try{
		      intvalue = Integer.parseInt(value);
		    }catch(NumberFormatException e){
				String msg = intres.getLocalizedMessage("services.errorexpireworker.errorconfig", serviceName, "VALUE");
		    	throw new ServiceExecutionFailedException(msg);
		    }
			
			if(intvalue == 0){
				String msg = intres.getLocalizedMessage("services.errorexpireworker.errorconfig", serviceName, "VALUE");
				throw new ServiceExecutionFailedException(msg);
			}
			timeBeforeExpire = intvalue * unitval;			
		}
	
		return timeBeforeExpire * 1000;
	}

	/** returns a collection of String with CAIds as gotten from the property  BaseWorker.PROP_CAIDSTOCHECK.
	 * @param includeAllCAsIfNonce set to true if the 'catch all' SecConst.ALLCAS should be included in the list IF there does not exist a list. This CAId is not recognized by all recipients...
     * This is due to that the feature of selecting CAs was enabled in EJBCA 3.9.1, and we want the service to keep working even after an upgrade from an earlier version.
	 * 
	 * @return Collection<String> of integer CA ids in String form, use Integer.valueOf to convert to int.
	 */
	protected Collection getCAIdsToCheck(boolean includeAllCAsIfNull) throws ServiceExecutionFailedException {
		if(cAIdsToCheck == null){
			cAIdsToCheck = new ArrayList();
			String cas = properties.getProperty(PROP_CAIDSTOCHECK);
		    if (log.isDebugEnabled()) {
		    	log.debug("CAIds to check: "+cas);
		    }
			if (cas != null) {
				String[] caids = cas.split(";");
				for(int i=0;i<caids.length;i++ ){
					try {
						Integer.valueOf(caids[i]);
					} catch (Exception e) {
						String msg = intres.getLocalizedMessage("services.errorexpireworker.errorconfig", serviceName, PROP_CAIDSTOCHECK);
						throw new ServiceExecutionFailedException(msg, e);						
					}
					cAIdsToCheck.add(Integer.valueOf(caids[i]));
				}				
			} else if (includeAllCAsIfNull) {
				cAIdsToCheck.add(Integer.valueOf(SecConst.ALLCAS));
			}
		}
		return cAIdsToCheck;
	}

}
