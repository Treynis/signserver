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

package org.ejbca.core.ejb.services;

import java.io.Serializable;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Random;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.Timer;

import org.apache.commons.lang.StringUtils;
import org.ejbca.core.ejb.BaseSessionBean;
import org.ejbca.core.ejb.log.ILogSessionLocal;
import org.ejbca.core.ejb.log.ILogSessionLocalHome;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.core.model.services.IInterval;
import org.ejbca.core.model.services.IWorker;
import org.ejbca.core.model.services.ServiceConfiguration;
import org.ejbca.core.model.services.ServiceExecutionFailedException;


/**
 * Uses JNDI name for datasource as defined in env 'Datasource' in ejb-jar.xml.
 *
 * @ejb.bean description="Timed Object Session bean running the services"
 *   display-name="ServiceTimerSessionSB"
 *   name="ServiceTimerSession"
 *   jndi-name="ServiceTimerSession"
 *   local-jndi-name="ServiceTimerSessionLocal"
 *   view-type="both"
 *   type="Stateless"
 *   transaction-type="Bean"
 *
 * @weblogic.enable-call-by-reference True
 * 
 * @ejb.transaction type="Supports"
 *
 * @ejb.env-entry name="DataSource"
 *   type="java.lang.String"
 *   value="${datasource.jndi-name-prefix}${datasource.jndi-name}"
 *   
 *
 * @ejb.env-entry
 *   description="Defines the JNDI name of the mail service used"
 *   name="MailJNDIName"
 *   type="java.lang.String"
 *   value="${mail.jndi-name}"
 *
 * @ejb.home extends="javax.ejb.EJBHome"
 *   local-extends="javax.ejb.EJBLocalHome"
 *   local-class="org.ejbca.core.ejb.services.IServiceTimerSessionLocalHome"
 *   remote-class="org.ejbca.core.ejb.services.IServiceTimerSessionHome"
 *
 * @ejb.interface extends="javax.ejb.EJBObject"
 *   local-extends="javax.ejb.EJBLocalObject"
 *   local-class="org.ejbca.core.ejb.services.IServiceTimerSessionLocal"
 *   remote-class="org.ejbca.core.ejb.services.IServiceTimerSessionRemote"
 *
 * @ejb.ejb-external-ref description="The Service session bean"
 *   view-type="local"
 *   ref-name="ejb/ServiceSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.services.IServiceSessionLocalHome"
 *   business="org.ejbca.core.ejb.services.IServiceSessionLocal"
 *   link="ServiceSession"
 *
 * @ejb.ejb-external-ref description="The Service Timer session bean"
 *   view-type="local"
 *   ref-name="ejb/ServiceTimerSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.services.IServiceTimerSessionLocalHome"
 *   business="org.ejbca.core.ejb.services.IServiceTimerSessionLocal"
 *   link="ServiceTimerSession"

 * @ejb.ejb-external-ref description="The Certificate entity bean used to store and fetch certificates"
 *   view-type="local"
 *   ref-name="ejb/CertificateDataLocal"
 *   type="Entity"
 *   home="org.ejbca.core.ejb.ca.store.CertificateDataLocalHome"
 *   business="org.ejbca.core.ejb.ca.store.CertificateDataLocal"
 *   link="CertificateData"
 *
 * @ejb.ejb-external-ref description="The CRL entity bean used to store and fetch CRLs"
 *   view-type="local"
 *   ref-name="ejb/CRLDataLocal"
 *   type="Entity"
 *   home="org.ejbca.core.ejb.ca.store.CRLDataLocalHome"
 *   business="org.ejbca.core.ejb.ca.store.CRLDataLocal"
 *   link="CRLData"
 *   
 * @ejb.ejb-external-ref description="The Authorization Session Bean"
 *   view-type="local"
 *   ref-name="ejb/AuthorizationSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.authorization.IAuthorizationSessionLocalHome"
 *   business="org.ejbca.core.ejb.authorization.IAuthorizationSessionLocal"
 *   link="AuthorizationSession"
 *   
 * @ejb.ejb-external-ref description="The User Admin Session Bean"
 *   view-type="local"
 *   ref-name="ejb/UserAdminSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ra.IUserAdminSessionLocalHome"
 *   business="org.ejbca.core.ejb.ra.IUserAdminSessionLocal"
 *   link="UserAdminSession"
 *
 * @ejb.ejb-external-ref description="The Certificate store used to store and fetch certificates"
 *   view-type="local"
 *   ref-name="ejb/CertificateStoreSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocal"
 *   link="CertificateStoreSession"
 *
 * @ejb.ejb-external-ref description="The log session bean"
 *   view-type="local"
 *   ref-name="ejb/LogSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.log.ILogSessionLocalHome"
 *   business="org.ejbca.core.ejb.log.ILogSessionLocal"
 *   link="LogSession"
 *   
 * @ejb.ejb-external-ref description="The CAAdmin Session Bean"
 *   view-type="local"
 *   ref-name="ejb/CAAdminSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionLocal"
 *   link="CAAdminSession"
 *
 * @ejb.ejb-external-ref description="The CRL Create bean"
 *   view-type="local"
 *   ref-name="ejb/CreateCRLSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.crl.ICreateCRLSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.crl.ICreateCRLSessionLocal"
 *   link="CreateCRLSession"
 *
 * @ejb.ejb-external-ref
 *   description="ProtectedLogSessionBean"
 *   view-type="local"
 *   ref-name="ejb/ProtectedLogSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.log.IProtectedLogSessionLocalHome"
 *   business="org.ejbca.core.ejb.log.IProtectedLogSessionLocal"
 *   link="ProtectedLogSession"
 *   
 * @ejb.ejb-external-ref description="The Approval Session Bean"
 *   view-type="local"
 *   ref-name="ejb/ApprovalSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.approval.IApprovalSessionLocalHome"
 *   business="org.ejbca.core.ejb.approval.IApprovalSessionLocal"
 *   link="ApprovalSession"
 *   
 * @ejb.ejb-external-ref
 *   description="The Key Recovery session bean"
 *   view-type="local"
 *   ref-name="ejb/KeyRecoverySessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.keyrecovery.IKeyRecoverySessionLocalHome"
 *   business="org.ejbca.core.ejb.keyrecovery.IKeyRecoverySessionLocal"
 *   link="KeyRecoverySession"
 *
 * @ejb.ejb-external-ref
 *   description="The Hard token session bean"
 *   view-type="local"
 *   ref-name="ejb/HardTokenSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.hardtoken.IHardTokenSessionLocalHome"
 *   business="org.ejbca.core.ejb.hardtoken.IHardTokenSessionLocal"
 *   link="HardTokenSession"
 *
 * @ejb.ejb-external-ref
 *   description="The signing session used to create CRL"
 *   view-type="local"
 *   ref-name="ejb/RSASignSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.sign.ISignSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.sign.ISignSessionLocal"
 *   link="RSASignSession"
 *
 * @ejb.ejb-external-ref
 *   description="The publisher bean"
 *   view-type="local"
 *   ref-name="ejb/PublisherSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.publisher.IPublisherSessionLocal"
 *   link="PublisherSession"
 *
 * @ejb.ejb-external-ref
 *   description="The publisher queue bean"
 *   view-type="local"
 *   ref-name="ejb/PublisherQueueSessionLocal"
 *   type="Session"
 *   home="org.ejbca.core.ejb.ca.publisher.IPublisherQueueSessionLocalHome"
 *   business="org.ejbca.core.ejb.ca.publisher.IPublisherQueueSessionLocal"
 *   link="PublisherQueueSession"
 *
 *  @version $Id: ServiceTimerSessionBean.java 8391 2009-12-03 10:01:08Z anatom $
 */
public class ServiceTimerSessionBean extends BaseSessionBean implements javax.ejb.TimedObject {



    /**
     * The remote interface of  log session bean
     */
    private transient ILogSessionLocal logsession = null;

    /**
     * The remote interface of service session bean
     */
    private transient IServiceSessionLocal servicesession = null;

    /**
     * The remote interface of service timer session bean
     */
    IServiceTimerSessionLocal servicetimersession = null;
    
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();


    /**
     * The administrator that the services should be runned as.
     */
    Admin intAdmin = new Admin(Admin.TYPE_INTERNALUSER);
    
    
    
    /**
     * Default create for SessionBean without any creation Arguments.
     *
     * @throws CreateException if bean instance can't be created
     */
    public void ejbCreate() throws CreateException {    	    
    	
    }
    
    /**
     * Constant indicating the Id of the "service loader" service.
     * Used in a clustered environment to perodically load available
     * services
     */
    private static final Integer SERVICELOADER_ID = Integer.valueOf(0);
    
    private static final long SERVICELOADER_PERIOD = 5 * 60 * 1000;

    /**
     * Method implemented from the TimerObject and is the main method of this
     * session bean. It calls the work object for each object.
     * 
     * @param timer
     */
	public void ejbTimeout(Timer timer) {
		trace(">ejbTimeout");    		
		Integer timerInfo = (Integer) timer.getInfo();
		if(timerInfo.equals(SERVICELOADER_ID)){
			log.debug("Running the internal Service loader.");
			load();
		}else{		
			ServiceConfiguration serviceData = null;
			IWorker worker = null;
			String serviceName = null;
			boolean run = false;
			try{
				serviceData = getServiceSession().getServiceConfiguration(intAdmin, timerInfo.intValue());
				if(serviceData != null){
					serviceName = getServiceSession().getServiceName(intAdmin, timerInfo.intValue());
					worker = getWorker(serviceData,serviceName);
					run = getServiceTimerSession().checkAndUpdateServiceTimeout(worker.getNextInterval(), timerInfo, serviceData, serviceName);
					log.debug("Service will run: "+run);
				} else {
					log.debug("Service was null and will not run, neither will it be rescheduled, so it will never run. Id: "+timerInfo.intValue());
				}
			} catch (Throwable e) {
			    // We need to catch wide here in order to continue even if there is some error
				log.info("Error getting and running service, we must see if we need to re-schedule: "+ e.getMessage());
				if (log.isDebugEnabled()) {
					// Don't spam log with stacktraces in normal production cases
					log.debug("Exception: ", e);
				}
				
				// Check if we have scheduled this time to run again, or if this exception would stop the service from running for ever.
				// If we can't find any current timer we will try to create a new one.
				boolean isScheduledToRun = false;
				Collection timers = getSessionContext().getTimerService().getTimers();
				for (Iterator iterator = timers.iterator(); iterator.hasNext();) {
					Timer t = (Timer) iterator.next();
					Integer tInfo = (Integer) timer.getInfo();
					if (tInfo.intValue() == timerInfo.intValue()) {
						Date nextTimeOut = timer.getNextTimeout();
						Date now = new Date();
						if (log.isDebugEnabled()) {
							log.debug("Next timeout for existing timer is '"+nextTimeOut+"' now is '"+now);
						}
						if (nextTimeOut.after(now)) {
							// Yes we found a timer that is scheduled for this service
							isScheduledToRun = true;							
						}
					}
				}
				if (!isScheduledToRun) {
					long nextInterval = 60; // Default try to run again in 60 seconds in case of error
					if (worker != null) {
						nextInterval = worker.getNextInterval();
					}
					long intervalMillis = getNextIntervalMillis(nextInterval);
					getSessionContext().getTimerService().createTimer(intervalMillis, timerInfo);
					String msg = intres.getLocalizedMessage("services.servicefailedrescheduled", intervalMillis);
					log.info(msg);
				}
			}
			if(run){
				if(serviceData != null){
					try{
						if(serviceData.isActive() && worker.getNextInterval() != IInterval.DONT_EXECUTE){				
							worker.work();			  							
							getLogSession().log(intAdmin, intAdmin.getCaId(), LogConstants.MODULE_SERVICES, new java.util.Date(), null, null, LogConstants.EVENT_INFO_SERVICEEXECUTED, intres.getLocalizedMessage("services.serviceexecuted", serviceName));
						}
					}catch (ServiceExecutionFailedException e) {
						getLogSession().log(intAdmin, intAdmin.getCaId(), LogConstants.MODULE_SERVICES, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_SERVICEEXECUTED, intres.getLocalizedMessage("services.serviceexecutionfailed", serviceName));
					}
				} else {
					getLogSession().log(intAdmin, intAdmin.getCaId(), LogConstants.MODULE_SERVICES, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_SERVICEEXECUTED, intres.getLocalizedMessage("services.servicenotfound", timerInfo));
				} 
			}else{
				Object o = timerInfo;
				if (serviceName != null) {
					o = serviceName;
				}
				getLogSession().log(intAdmin, intAdmin.getCaId(), LogConstants.MODULE_SERVICES, new java.util.Date(), null, null, LogConstants.EVENT_INFO_SERVICEEXECUTED, intres.getLocalizedMessage("services.servicerunonothernode", o));
			}
		}
		trace("<ejbTimeout");		
	}    
    /**
     * Internal method should not be called from external classes, method is public to get automatic transaction handling.
     * 
     * This method need "RequiresNew" transaction handling, because we want to make sure that the timer
     * runs the next time even if the execution fails.
     * 
     * @return true if the service should run, false if the service should not run
     * 
     * @ejb.interface-method view-type="local"
     * @ejb.transaction type="RequiresNew"
     */
	public boolean checkAndUpdateServiceTimeout(long nextInterval, int timerInfo, ServiceConfiguration serviceData, String serviceName) {
		boolean ret = false;
		// Add a random delay within 30 seconds to the interval, just to make sure nodes in a cluster is
		// not scheduled to run on the exact same second. If the next scheduled run is less than 40 seconds away, 
		// in which case we only randomize on 5 seconds.
		long intervalMillis = getNextIntervalMillis(nextInterval);
		getSessionContext().getTimerService().createTimer(intervalMillis, timerInfo);
		// Calculate the nextRunTimeStamp, since we set a new timer
		Date runDateCheck = serviceData.getNextRunTimestamp(); // nextRunDateCheck will typically be the same (or just a millisecond earlier) as now here
		Date currentDate = new Date();
		Date nextRunDate = new Date(currentDate.getTime() + intervalMillis);
		if (log.isDebugEnabled()) {
			log.debug("nextRunDate is: "+nextRunDate);
			log.debug("runDateCheck is: "+runDateCheck);
			log.debug("currentDate is: "+currentDate);
		}
		// Check if the current date is after when the service should run.
		// If a service on another cluster node has updated this timestamp already, then it will return false and
		// this service will not run.
		// This is a semaphor (not the best one admitted) so that services in a cluster only runs on one node and don't compete with each other.
		// If a worker on one node for instance runs for a very long time, there is a chance that another worker on another node will break this semaphore and
		// run as well.
		if(currentDate.after(runDateCheck)){
			// We only update the nextRunTimeStamp if the service will be running otherwise it will, in theory, be a race to exclude each other between the nodes.
			serviceData.setNextRunTimestamp(nextRunDate);
			getServiceSession().changeService(intAdmin, serviceName, serviceData, true); 
			ret=true;
		}		
		return ret;
	}

	private long getNextIntervalMillis(long nextIntervalSecs) {
		Date currentDate = new Date();
		Date nextApproxTime = new Date(currentDate.getTime()+nextIntervalSecs*1000);
		Date fourtysec = new Date(currentDate.getTime()+40100); // add 100 milliseconds
		Date threesec = new Date(currentDate.getTime()+3100); // add 100 milliseconds
		/*
		if (log.isDebugEnabled()) {
			log.info("nextApproc: "+nextApproxTime);			
			log.info("currentDate: "+currentDate);			
			log.info("forty: "+fourtysec);			
			log.info("three: "+threesec);			
		}*/
		int randInterval = 30000;
		if (fourtysec.after(nextApproxTime)) {
			// If we are running with less than 40 second intervale we only randomize 5 seconds
			randInterval = 5000;
			// And if we are running with a very short interval we only randomize on one second
			if (threesec.after(nextApproxTime)) {
				randInterval = 1000;
			}
		}
		Random rand = new Random();
		int randMillis = rand.nextInt(randInterval);
		if (log.isDebugEnabled()) {
			log.debug("Adding random delay: "+randMillis);			
		}
		
		long intervalMillis = nextIntervalSecs*1000+randMillis;
		return intervalMillis;
	}
	
    /**
     * Loads and activates all the services from database that are active
     *
     * @throws EJBException if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */
	public void load(){
		// Get all services

		Collection currentTimers = getSessionContext().getTimerService().getTimers();
		Iterator iter = currentTimers.iterator();
		HashSet existingTimers = new HashSet();
		while(iter.hasNext()){
			Timer timer = (Timer) iter.next();
			try {
				Serializable info = timer.getInfo();
				existingTimers.add(info);    			
			} catch (Throwable e) {
				// We need this try because weblogic seems to suck...
				log.debug("Error invoking timer.getInfo(): ", e);
			}
		}

		HashMap idToNameMap = getServiceSession().getServiceIdToNameMap(intAdmin);
		Collection allServices = idToNameMap.keySet();
		iter = allServices.iterator();
		while(iter.hasNext()){
			Integer id = (Integer) iter.next();
			ServiceConfiguration serviceConfiguration = getServiceSession().getServiceConfiguration(intAdmin, id.intValue());
			if(!existingTimers.contains(id)){
				IWorker worker = getWorker(serviceConfiguration, (String) idToNameMap.get(id));
				if(worker != null && serviceConfiguration.isActive()  && worker.getNextInterval() != IInterval.DONT_EXECUTE){
					getSessionContext().getTimerService().createTimer((worker.getNextInterval()) *1000, id);
				}
			}
		}

		if(!existingTimers.contains(SERVICELOADER_ID)){
			// load the service timer
			getSessionContext().getTimerService().createTimer(SERVICELOADER_PERIOD, SERVICELOADER_ID);
		}
	}
	
    /**
     * Cancels all existing timers a unload
     *
     * @throws EJBException if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */
	public void unload(){
		// Get all servicess
		Collection currentTimers = getSessionContext().getTimerService().getTimers();
		Iterator iter = currentTimers.iterator();
		while(iter.hasNext()){
			try {
				Timer timer = (Timer) iter.next();			
				timer.cancel(); 							
			} catch (Exception e) {
				// We need to catch this because Weblogic 10 throws an exception if we
				// have not scheduled this timer, so we don't have anything to cancel.
				// Only weblogic though...
				log.info("Caught exception canceling timer: "+e.getMessage());
			}
		}
	}
	
	
    /**
     * Adds a timer to the bean, and cancels all existing timeouts for this id.
     *
     * @param id the id of the timer
     * @throws EJBException if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */
	public void addTimer(long interval, Integer id){
		// Cancel old timers before adding new one
		cancelTimer(id);
		getSessionContext().getTimerService().createTimer(interval, id);
	}
	
    /**
     * cancels a timer with the given Id
     *
     * @throws EJBException             if a communication or other error occurs.
     * @ejb.interface-method view-type="both"
     */
	public void cancelTimer(Integer id){
		  Collection timers = getSessionContext().getTimerService().getTimers();
		  Iterator iter = timers.iterator();
		  while(iter.hasNext()){
			  try {
				  Timer next = (Timer) iter.next();
				  if(id.equals(next.getInfo())){
					  next.cancel();
				  }
			  } catch (Exception e) {
				  // We need to catch this because Weblogic 10 throws an exception if we
				  // have not scheduled this timer, so we don't have anything to cancel.
				  // Only weblogic though...
				  log.info("Caught exception canceling timer: "+e.getMessage());
			  }
		  }
	}
	
	

   /**
    * Method that creates a worker from the service configuration. 
    * 
    * @param serviceConfiguration
    * @param serviceName
    * @return a worker object or null if the worker is missconfigured.
    */
    private IWorker getWorker(ServiceConfiguration serviceConfiguration, String serviceName) {
		IWorker worker = null;
    	try {
    		String clazz = serviceConfiguration.getWorkerClassPath();
    		if (StringUtils.isNotEmpty(clazz)) {
    			worker = (IWorker) this.getClass().getClassLoader().loadClass(clazz).newInstance();
    			worker.init(intAdmin, serviceConfiguration, serviceName);    			
    		} else {
    			log.info("Worker has empty classpath for service "+serviceName);
    		}
		} catch (Exception e) {
			// Only display a real error if it is a worker that we are actually using
			if (serviceConfiguration.isActive()) {
				log.error("Worker is missconfigured, check the classpath",e);
			} else {
				log.info("Worker is missconfigured, check the classpath: "+e.getMessage());
			}
		}    	
    	
		return worker;
	}



	/**
     * Gets connection to log session bean
     *
     * @return Connection
     */
    private ILogSessionLocal getLogSession() {
        if (logsession == null) {
            try {
                ILogSessionLocalHome logsessionhome = (ILogSessionLocalHome) getLocator().getLocalHome(ILogSessionLocalHome.COMP_NAME);
                logsession = logsessionhome.create();
            } catch (CreateException e) {
                throw new EJBException(e);
            }
        }
        return logsession;
    } //getLogSession




    /**
     * Gets connection a service session, used for timed services
     *
     * @return Connection
     */
    private IServiceSessionLocal getServiceSession() {
    	if (servicesession == null) {
    		try {
    			IServiceSessionLocalHome servicesessionhome = (IServiceSessionLocalHome) getLocator().getLocalHome(IServiceSessionLocalHome.COMP_NAME);
    			servicesession = servicesessionhome.create();
    		} catch (CreateException e) {
    			throw new EJBException(e);
    		}
    	}
        return servicesession;
    } //getServiceSession 

    /**
     * Gets connection a service timer session, used for timed services
     *
     * @return Connection
     */
    private IServiceTimerSessionLocal getServiceTimerSession() {
    	if (servicetimersession == null) {
    		try {
    			IServiceTimerSessionLocalHome servicesessionhome = (IServiceTimerSessionLocalHome) getLocator().getLocalHome(IServiceTimerSessionLocalHome.COMP_NAME);
    			servicetimersession = servicesessionhome.create();
    		} catch (CreateException e) {
    			throw new EJBException(e);
    		}
    	}
        return servicetimersession;
    } //getServiceTimerSession

} // LocalServiceSessionBean
