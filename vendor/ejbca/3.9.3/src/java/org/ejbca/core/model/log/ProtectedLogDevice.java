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

package org.ejbca.core.model.log;

import java.io.Serializable;
import java.net.InetAddress;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.SecureRandom;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import javax.ejb.EJBException;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.log.IProtectedLogSessionLocal;
import org.ejbca.core.ejb.log.IProtectedLogSessionLocalHome;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.ca.caadmin.CADoesntExistsException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceNotActiveException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequestException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.IllegalExtendedCAServiceRequestException;
import org.ejbca.util.CertTools;
import org.ejbca.util.query.IllegalQueryException;
import org.ejbca.util.query.Query;

/**
 * Implements a log device using a protected log. Implementes the Singleton pattern.
 * @version $Id: ProtectedLogDevice.java 7936 2009-08-24 09:05:24Z anatom $
 */
public class ProtectedLogDevice implements ILogDevice, Serializable {

	public final static String CONFIG_TOKENREFTYPE						= "protectionTokenReferenceType";
	public final static String CONFIG_TOKENREFTYPE_CANAME		= "CAName"; 
	public final static String CONFIG_TOKENREFTYPE_URI				= "URI"; 
	public final static String CONFIG_TOKENREFTYPE_NONE			= "none"; 
	public final static String CONFIG_TOKENREFTYPE_DATABASE	= "StoredInDatabase"; 
	public final static String CONFIG_TOKENREFTYPE_CONFIG		= "Base64EncodedConfig"; 

	public final static String CONFIG_TOKENREF								= "protectionTokenReference";
	public final static String CONFIG_KEYSTOREALIAS					= "protectionTokenKeyStoreAlias";
	public final static String CONFIG_KEYSTOREPASSWORD			= "protectionTokenKeyStorePassword";
	public final static String CONFIG_HASHALGO							= "protectionHashAlgorithm";
	public final static String CONFIG_NODEIP									= "nodeIP";
	public final static String CONFIG_PROTECTION_INTENSITY		= "protectionIntensity";
	public final static String CONFIG_MAX_VERIFICATION_STEPS	= "maxVerificationsSteps";
	public final static String CONFIG_ALLOW_EVENTSCONFIG		= "allowConfigurableEvents";
	public final static String CONFIG_LINKIN_INTENSITY					= "linkinIntensity";
	public final static String CONFIG_VERIFYOWN_INTENSITY			= "verifyownIntensity";
	public final static String CONFIG_SEARCHWINDOW                    = "searchWindow";
	
	public final static String DEFAULT_NODEIP								= "127.0.0.1";
	public final static String DEFAULT_DEVICE_NAME						= "ProtectedLogDevice";
	public final static String DEFAULT_MAX_VERIFICATION_STEPS	= "0";
	
	private static final Logger log = Logger.getLogger(ProtectedLogDevice.class);
    private static final InternalResources intres = InternalResources.getInstance();

	private static final SecureRandom seeder = new SecureRandom();

	private static Admin internalAdmin = new Admin(Admin.TYPE_INTERNALUSER);

	private IProtectedLogSessionLocal protectedLogSession = null;
	
	/**
	 * A handle to the unique Singleton instance.
	 */
	private static ILogDevice instance;
	private ReentrantLock fairLock;
	private boolean isDestructorInvoked;
	private boolean systemShutdownNotice;
	private Properties properties;
	private int nodeGUID;
	private long counter;
	private long protectedCounter;
	private HashMap lastProtectedLogRowHashTime;	// <Long, HashTime> 
	long lastProtectedLogRowCount;
	private long lastTime;
	private long protectionIntensity;
	private String nodeIP;
	private ProtectedLogToken protectedLogToken;
	private String protectionHashAlgorithm;
	private boolean isFirstLogEvent;
	private ProtectedLogActions protectedLogActions;
	private boolean allowConfigurableEvents;
	private String deviceName;
	private long lastTimeOfSearchForLogEvents;
	private long lastTimeOfSearchForOwnLogEvent;
	private long intensityOfSearchForLogEvents;
	private long intensityOfSearchForOwnLogEvent;
	private long searchWindow;
	
	protected ProtectedLogDevice(Properties properties) throws Exception {
		fairLock = new ReentrantLock(true);		// Create a fair lock.
		resetDevice(properties);
	}
	
	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public void resetDevice(Properties properties) {
		if (log.isDebugEnabled()) {
			log.debug("ProtectedLog properties: "+properties);
		}
		// Init of local variables
		isDestructorInvoked = false;
		systemShutdownNotice = false;
		lastProtectedLogRowHashTime = new HashMap();	// <Long, HashTime>
		lastProtectedLogRowCount = 0;
		lastTime = 0;
		protectedLogToken = null;
		isFirstLogEvent = true;
		allowConfigurableEvents = false;
		lastTimeOfSearchForLogEvents = 0;
		lastTimeOfSearchForOwnLogEvent = 0;
		// Init depending on properties
		this.properties = properties;
		deviceName = properties.getProperty(ILogDevice.PROPERTY_DEVICENAME, DEFAULT_DEVICE_NAME);
		nodeIP = getNodeIP(properties); // the instance is not set yet so getInxtenceProperties will not work.
		nodeGUID = seeder.nextInt();
		if (log.isDebugEnabled()) {
			log.debug("This node with ip "+nodeIP+" uses node GUID: "+nodeGUID);
		}
		counter = 0;
		protectedCounter = 0;
		protectionIntensity = Long.parseLong(properties.getProperty(CONFIG_PROTECTION_INTENSITY, "0")) * 1000; 
		allowConfigurableEvents = properties.getProperty(CONFIG_ALLOW_EVENTSCONFIG, "false").equalsIgnoreCase("true"); 
		protectionHashAlgorithm = properties.getProperty(CONFIG_HASHALGO, "SHA-256");
		protectedLogActions = new ProtectedLogActions(properties);
		if (protectionIntensity != 0 && properties.getProperty(ProtectedLogExporter.CONF_DELETE_AFTER_EXPORT, "false").equalsIgnoreCase("true")) {
	    	log.warn(intres.getLocalizedMessage("protectedlog.warn.usingunsafeconfig", ProtectedLogExporter.CONF_DELETE_AFTER_EXPORT, CONFIG_PROTECTION_INTENSITY));
		}
		if (properties.getProperty(CONFIG_TOKENREFTYPE, CONFIG_TOKENREFTYPE_NONE).equalsIgnoreCase(CONFIG_TOKENREFTYPE_NONE)) {
			// Disable link-in searches since no real token is used anyway..
			intensityOfSearchForLogEvents = -1000;
			intensityOfSearchForOwnLogEvent = -1000;
		} else  {
			intensityOfSearchForLogEvents = Long.parseLong(properties.getProperty(CONFIG_LINKIN_INTENSITY, "1")) * 1000; 
			intensityOfSearchForOwnLogEvent = Long.parseLong(properties.getProperty(CONFIG_VERIFYOWN_INTENSITY, "1")) * 1000; 
		}
		searchWindow = Long.parseLong(properties.getProperty(CONFIG_SEARCHWINDOW, "300")) * 1000;
	}

	/**
	 * Creates (if needed) the log device and returns the object.
	 *
	 * @param prop Arguments needed for the eventual creation of the object
	 * @return An instance of the log device.
	 */
	public static synchronized ILogDevice instance(Properties prop) throws Exception {
		if (instance == null) {
			instance = new ProtectedLogDevice(prop);
		}
		return instance;
	}

	/**
	 * @return the existing device or null if none exist.
	 */
	public static synchronized ILogDevice instance() {
		return instance;
	}
	
	/**
	 * Should only be called by the StartServicesServlet.
	 */
	public void setSystemShutdownNotice() {
		log(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_LOG, new Date(), null, null, LogConstants.EVENT_SYSTEM_STOPPED_LOGGING , "Terminating log session for this node.",null);
		systemShutdownNotice = true;
	}
	
	/**
	 * @return true if the application server is about to go down.
	 */
	public boolean getSystemShutdownNotice() {
		return systemShutdownNotice;
	}
	
	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	synchronized public void destructor() {
		// This could be called from several LogSession beans, but we only keep one instance..
		if (!isDestructorInvoked) {
			isDestructorInvoked = true;
		}
	}
	
	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public String getDeviceName() {
		return deviceName;
	}
	
	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public Properties getProperties() {
		return properties;
	}

	private IProtectedLogSessionLocal getProtectedLogSession() {
		try {
			if (protectedLogSession == null) {
				protectedLogSession = ((IProtectedLogSessionLocalHome) ServiceLocator.getInstance().getLocalHome(IProtectedLogSessionLocalHome.COMP_NAME)).create();
			}
			return protectedLogSession;
		} catch (Exception e) {
			throw new EJBException(e);
		}
	}

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public boolean getAllowConfigurableEvents() {
		return allowConfigurableEvents;
	}

	
	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	private ProtectedLogToken getProtectedLogToken() {
		if (protectedLogToken == null) {
			protectedLogToken = getProtectedLogSession().getProtectedLogToken();
		}
		return protectedLogToken;
	}
	
	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public void log(Admin admininfo, int caid, int module, Date time, String username, Certificate certificate, int event, String comment, Exception exception) {
		fairLock.lock();
		try {
			// Is first LogEvent? Write Initiating Log Event
			if (isFirstLogEvent) {
				isFirstLogEvent = false;
				logInternal(internalAdmin, internalAdmin.getCaId(), LogConstants.MODULE_LOG, new Date(time.getTime()-1), null, null, LogConstants.EVENT_SYSTEM_INITILIZED_LOGGING, "Initiating log for this node.",null);
				//protectedLogActions.takeActions(IProtectedLogAction.CAUSE_TESTING);
			}
			if (!systemShutdownNotice || event == LogConstants.EVENT_SYSTEM_STOPPED_LOGGING) {
				logInternal(admininfo, caid, module, time, username, certificate, event, comment, exception);
			} else {
				logInternalOnShutDown(admininfo, caid, module, time, username, certificate, event, comment, exception);
			}
		} finally {
			fairLock.unlock();
		}
	}

	/**
	 * Implementation of the log-protection algorithm.
	 * 
	 * Chains each new log-event together with last processed and any new event found in database.
	 */
	private void logInternal(Admin admininfo, int caid, int module, Date time, String username, Certificate certificate, int event, String comment, Exception exception) {
		// Convert exception to something loggable
		if (exception != null) {
			comment += ", Exception: " + exception.getMessage(); 
		}
		try {
			// Find nodes to link in
			ProtectedLogEventIdentifier[] linkedInEventIdentifiers = null;
			ArrayList linkedInEventIdentifiersCollection = new ArrayList(0);	// <ProtectedLogEventIdentifier>
			if (counter == 0) {
				// Find newest protected event in database to link in
				ProtectedLogEventIdentifier protectedLogEventIdentifier = null;
				protectedLogEventIdentifier = getProtectedLogSession().findNewestProtectedLogEventRow();
				if (protectedLogEventIdentifier != null) {
					linkedInEventIdentifiersCollection.add(protectedLogEventIdentifier);
				} else {
					// Database log is empty!
			    	log.error(intres.getLocalizedMessage("protectedlog.error.emptyorunprotected"));
					protectedLogActions.takeActions(IProtectedLogAction.CAUSE_EMPTY_LOG);
				}
				lastTimeOfSearchForLogEvents = System.currentTimeMillis();
			} else {
				// Find all new events from other nodes in database to link in, if the right amount of time has passed since last time
				long now = System.currentTimeMillis();
				if (intensityOfSearchForLogEvents != -1000 && lastTimeOfSearchForLogEvents + intensityOfSearchForLogEvents < now) {
					long searchLimit = Math.min(now - searchWindow, lastTimeOfSearchForLogEvents - 50);
					ProtectedLogEventIdentifier[] protectedLogEventIdentifiers = getProtectedLogSession().findNewestProtectedLogEventsForAllOtherNodes(
							nodeGUID, searchLimit);
					if (protectedLogEventIdentifiers != null) {
						for (int i=0; i<protectedLogEventIdentifiers.length; i++) {
							linkedInEventIdentifiersCollection.add(protectedLogEventIdentifiers[i]);
						}
					}
					lastTimeOfSearchForLogEvents = now;
				}
			}
			// Verify all events about to be linked in (except this nodes last event that is verified seperately
			ArrayList protectedLogEventIdentifiersToRemove = new ArrayList(0);	// <ProtectedLogEventIdentifier>
			if (!linkedInEventIdentifiersCollection.isEmpty()) {
				Iterator i = linkedInEventIdentifiersCollection.iterator();
				while (i.hasNext()) {
					ProtectedLogEventIdentifier protectedLogEventIdentifier = (ProtectedLogEventIdentifier) i.next();
					ProtectedLogEventRow protectedLogEventRow = getProtectedLogSession().getProtectedLogEventRow(protectedLogEventIdentifier);
					if (protectedLogEventRow == null ) {
				    	log.error(intres.getLocalizedMessage("protectedlog.error.logrowmissing", protectedLogEventIdentifier.getNodeGUID(),
				    			protectedLogEventIdentifier.getCounter()));
				    	if (log.isDebugEnabled()) {
				    		log.debug("Logrow was missing verifying nodes about to be linked in.");
				    	}
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
						protectedLogEventIdentifiersToRemove.add(protectedLogEventIdentifier);
						continue;
					}
					ProtectedLogToken protectedLogToken = getProtectedLogSession().getToken(protectedLogEventRow.getProtectionKeyIdentifier());
					if (protectedLogToken == null ) {
				    	log.error(intres.getLocalizedMessage("protectedlog.error.tokenmissing", protectedLogEventRow.getProtectionKeyIdentifier()));
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_TOKEN);
						// Add for removal if the event fails verification
						protectedLogEventIdentifiersToRemove.add(protectedLogEventIdentifier);
						continue;
					}
					if ( !protectedLogToken.verify(protectedLogEventRow.getAsByteArray(false), protectedLogEventRow.getProtection())) {
				    	log.error(intres.getLocalizedMessage("protectedlog.error.logrowchanged", protectedLogEventIdentifier.getNodeGUID(),
				    			protectedLogEventIdentifier.getCounter()));
						protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MODIFIED_LOGROW);
						// Add for removal if the event fails verification
						protectedLogEventIdentifiersToRemove.add(protectedLogEventIdentifier);
						continue;
					}
				}
			}
			Iterator iterator = protectedLogEventIdentifiersToRemove.iterator();
			while (iterator.hasNext()) {
				linkedInEventIdentifiersCollection.remove(iterator.next());
			}
			// Add previous ProtectedLogEventRow this node has produced, if any
			// Start by verifying that the last event in the database is correct, but only do this if sufficient time has passed since this was last verified
			if (counter != 0) {
				// Even though we might not be able to read the last event yet from the database, we use this for continuous linking (it will be verified in the future)
				linkedInEventIdentifiersCollection.add(new ProtectedLogEventIdentifier(nodeGUID, counter-1));
				long now = System.currentTimeMillis();
				if (intensityOfSearchForOwnLogEvent != -1000 && lastTimeOfSearchForOwnLogEvent + intensityOfSearchForOwnLogEvent < now) {
					ProtectedLogEventIdentifier lastProtectedLogEventIdentifier = getProtectedLogSession().findNewestProtectedLogEventRow(nodeGUID);
					if (lastProtectedLogEventIdentifier == null) {
						// If the first event in local cache is older that the threshold we should warn
						Set counters = lastProtectedLogRowHashTime.keySet();
						Iterator i = counters.iterator();
						long lowest = Integer.MAX_VALUE;
						while (i.hasNext()) {
							long current = (Long) i.next();
							lowest = Math.min(current, lowest);
						}
						if (log.isDebugEnabled()) {
							log.debug("The earliest present log-event in HashTime-cache is " + lowest + " (counter is " + counter + ").");							
						}
						HashTime hashTime = (HashTime) lastProtectedLogRowHashTime.get(lowest);
						if (lowest != 0 || hashTime.getTime() < (now - searchWindow) ) {
					    	log.error(intres.getLocalizedMessage("protectedlog.error.logrowmissing", nodeGUID, lowest));
							protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
						}
					} else {
						// Verify the event we found
						ProtectedLogEventRow protectedLogEventRow = getProtectedLogSession().getProtectedLogEventRow(lastProtectedLogEventIdentifier);
						HashTime hashTime = (HashTime) lastProtectedLogRowHashTime.get(lastProtectedLogEventIdentifier.getCounter());
						if (protectedLogEventRow == null) {
					    	log.error(intres.getLocalizedMessage("protectedlog.error.logrowmissing", nodeGUID, lastProtectedLogEventIdentifier.getCounter()));
							protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
						} else if (hashTime == null) {
					    	log.error("Missing hashTime for counter=" + lastProtectedLogEventIdentifier.getCounter() + ". lastProtectedLogRowHashTime.size=" + lastProtectedLogRowHashTime.size());
							protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
						} else if (!Arrays.equals(protectedLogEventRow.calculateHash(), hashTime.getHash() )) {
					    	log.error(intres.getLocalizedMessage("protectedlog.error.logrowchanged", nodeGUID, lastProtectedLogEventIdentifier.getCounter()));
							protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MODIFIED_LOGROW);
						} else {
							if ( ((now - protectedLogEventRow.getEventTime()) > searchWindow) ) {
								// we come here if we only found log records older than the search window in the DB. This will occur of the node have been idling for a while.
								if (protectionIntensity == 0) {
									// We can only compare with the counter-1 if we sign every row. If we don't sign every row (because protectionIntensity is 10 seconds or something)
									// lastProtectedLogEventIdentifier.getCounter() can be counter-2 or something even earlier
									if (lastProtectedLogEventIdentifier.getCounter() != (counter-1))  {
										// Too old and the last one that should have been written = some are missing
								    	log.error(intres.getLocalizedMessage("protectedlog.error.logrowmissing", nodeGUID, lastProtectedLogEventIdentifier.getCounter()));
								    	if (log.isDebugEnabled()) {
								    		log.debug("The last found event was more than " + (now - protectedLogEventRow.getEventTime()) + " milliseconds old. Looking for counter-1 "+(counter-1)+". lastProtectedLogEventIdentifier.getCounter() is "+lastProtectedLogEventIdentifier.getCounter());
								    	}
								    	protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
									}
								} else {
									if (lastProtectedLogEventIdentifier.getCounter() != protectedCounter)  {
										// the last protected log row is not the one we have internally cached as the last protected = some are missing
								    	log.error(intres.getLocalizedMessage("protectedlog.error.logrowmissing", nodeGUID, lastProtectedLogEventIdentifier.getCounter()));
								    	if (log.isDebugEnabled()) {
								    		log.debug("The last found event was more than " + (now - protectedLogEventRow.getEventTime()) + " milliseconds old. Looking for protectedCounter "+(protectedCounter)+". lastProtectedLogEventIdentifier.getCounter() is "+lastProtectedLogEventIdentifier.getCounter());
								    	}
								    	protectedLogActions.takeActions(IProtectedLogAction.CAUSE_MISSING_LOGROW);
									}									
								}
							}
							// Remove all saved events before (now-searchWindow) unless it's the last one
							while ( lastProtectedLogRowHashTime.size() > 1 && ((HashTime)lastProtectedLogRowHashTime.get(lastProtectedLogRowCount)).getTime() < (now - searchWindow) ) {
								lastProtectedLogRowHashTime.remove(lastProtectedLogRowCount);
								lastProtectedLogRowCount++;
							}
						}
					}
					lastTimeOfSearchForOwnLogEvent = now;
				}
			}
			linkedInEventIdentifiers = (ProtectedLogEventIdentifier[]) linkedInEventIdentifiersCollection.toArray(new ProtectedLogEventIdentifier[0]);
			// Create a hash of the linked in nodes
			MessageDigest messageDigest = MessageDigest.getInstance(protectionHashAlgorithm, "BC");
			// Chain nodes with hash
			byte[] linkedInEventsHash = null;
			if (linkedInEventIdentifiers != null && linkedInEventIdentifiers.length != 0) {
				for (int i=0; i<linkedInEventIdentifiers.length; i++) {
					if (linkedInEventIdentifiers[i].equals(new ProtectedLogEventIdentifier(nodeGUID, counter-1))) {
						// Don't trust the database for this, use saved value
						messageDigest.update(((HashTime) lastProtectedLogRowHashTime.get(counter-1)).getHash());
					} else {
						messageDigest.update(getProtectedLogSession().getProtectedLogEventRow(linkedInEventIdentifiers[i]).calculateHash());
					}
				}
				linkedInEventsHash = messageDigest.digest();
			}
			String certificateSerialNumber = null;
			String certificateIssuerDN = null; 
			if (certificate != null) {
				certificateSerialNumber = CertTools.getSerialNumberAsString(certificate);
				certificateIssuerDN = CertTools.getIssuerDN(certificate); 
			}
			ProtectedLogEventRow protectedLogEventRow = new ProtectedLogEventRow(
					admininfo.getAdminType(), admininfo.getAdminData(), caid, module, time.getTime(), username, certificateSerialNumber, 
					certificateIssuerDN, event, comment, new ProtectedLogEventIdentifier(nodeGUID, counter), nodeIP, linkedInEventIdentifiers,
					linkedInEventsHash, protectionHashAlgorithm, getProtectedLogToken().getIdentifier(),
					getProtectedLogToken().getProtectionAlgorithm(), null);
			// Add protection to row depending on choosen intensity
			byte[] currentRowData = protectedLogEventRow.getAsByteArray(false);
			// Since the might be somewhat unsynched from different nodes 0 mean all rows.. 
			if (protectionIntensity == 0 || (lastTime + protectionIntensity) <= time.getTime() ) {
				protectedLogEventRow.setProtection(getProtectedLogToken().protect(currentRowData));
				lastTime = time.getTime();
			}
			getProtectedLogSession().addProtectedLogEventRow(protectedLogEventRow);
			lastProtectedLogRowHashTime.put(counter, new HashTime(protectedLogEventRow.calculateHash(), protectedLogEventRow.getEventTime()));
			//log.debug("counter:"+counter+", protectedLogEventRow.getProtection(): "+protectedLogEventRow.getProtection());
			if (protectedLogEventRow.getProtection() != null) {
				// If we have protectionIntensity set to > 0 we don't sign every log row
				protectedCounter = counter;
				if (log.isTraceEnabled()) {
					log.trace("Updated protected counter: "+protectedCounter);
				}
			}
			counter++;				
		} catch (Exception e) {
        	log.error(intres.getLocalizedMessage("protectedlog.error.internallogerror"), e);
			protectedLogActions.takeActions(IProtectedLogAction.CAUSE_INTERNAL_ERROR);
			throw new EJBException(e);
		}
	} // logInternal

	/**
	 * At this point we can no longer rely on beans to exist. We do our best to log as much as possible, unprotected.
	 * The admin has to manually "accept" these log-events on another node or when the server is back up using the CLI.
	 */
	private void logInternalOnShutDown(Admin admininfo, int caid, int module, Date time, String username, Certificate certificate, int event, String comment, Exception exception) {
		ProtectedLogToken protectedLogToken = new ProtectedLogToken();
		String certificateSerialNumber = null;
		String certificateIssuerDN = null; 
		if (certificate != null) {
			certificateSerialNumber = CertTools.getSerialNumberAsString(certificate);
			certificateIssuerDN = CertTools.getIssuerDN(certificate); 
		}
		ProtectedLogEventIdentifier[] linkedInEventIdentifiers = new ProtectedLogEventIdentifier[1];
		linkedInEventIdentifiers[0] = new ProtectedLogEventIdentifier(nodeGUID, counter-1);
		byte[] linkedInEventsHash = null;
		try {
			MessageDigest messageDigest = MessageDigest.getInstance(protectionHashAlgorithm, "BC");
			linkedInEventsHash = messageDigest.digest(((HashTime) lastProtectedLogRowHashTime.get(counter-1)).getHash());
		} catch (NoSuchAlgorithmException e1) {
			// Nothing to do about it.. the has will be null
		} catch (NoSuchProviderException e1) {
			// Nothing to do about it.. the has will be null
		}
		ProtectedLogEventRow protectedLogEventRow = new ProtectedLogEventRow(
				admininfo.getAdminType(), admininfo.getAdminData(), caid, module, time.getTime(), username, certificateSerialNumber, 
				certificateIssuerDN, event, comment, new ProtectedLogEventIdentifier(nodeGUID, counter), nodeIP, linkedInEventIdentifiers,
				linkedInEventsHash, protectionHashAlgorithm, protectedLogToken.getIdentifier(),
				protectedLogToken.getProtectionAlgorithm(), null);
		try {
			getProtectedLogSession().addProtectedLogEventRow(protectedLogEventRow);
		} catch (Exception e) {
        	log.error(intres.getLocalizedMessage("protectedlog.error.logdropped",admininfo.getAdminType()+" "+admininfo.getAdminData()+" "
        			+caid+" "+" "+module+" "+" "+time+" "+username+" "+(certificate==null?"null":CertTools.getSerialNumberAsString(certificate)+" "
        			+CertTools.getIssuerDN(certificate))+" "+event+" "+comment+" "+exception));
			return;
		}
    	log.error(intres.getLocalizedMessage("protectedlog.error.logunprotected",admininfo.getAdminType()+" "+admininfo.getAdminData()+" "
    			+caid+" "+" "+module+" "+" "+time+" "+username+" "+(certificate==null?"null":CertTools.getSerialNumberAsString(certificate)+" "
       			+CertTools.getIssuerDN(certificate))+" "+event+" "+comment+" "+exception));
		lastProtectedLogRowHashTime.put(counter, new HashTime(protectedLogEventRow.calculateHash(), protectedLogEventRow.getEventTime()));
		counter++;
	} // logInternalOnShutDown

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public byte[] export(Admin admin, Query query, String viewlogprivileges, String capriviledges, ILogExporter logexporter) throws IllegalQueryException, CADoesntExistsException, ExtendedCAServiceRequestException, IllegalExtendedCAServiceRequestException, ExtendedCAServiceNotActiveException {
		return null;
	}

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public Collection query(Query query, String viewlogprivileges, String capriviledges) throws IllegalQueryException {
		log.trace(">query()");
		Collection ret = null;
		if (capriviledges == null || capriviledges.length() == 0 || !query.isLegalQuery()) {
			throw new IllegalQueryException();
		}
		String queryString = query.getQueryString();
	    //private final String LOGENTRYDATA_COL = "id, adminType, adminData, caid, module, time, username, certificateSNR, event"; "comment"; or "comment_"
		queryString = queryString.replaceAll("caid", "caId").replaceAll("event", "eventId").replaceAll("certificateSNR", "certificateSerialNumber");
		queryString = queryString.replaceAll("comment", "eventComment").replaceAll("time", "eventTime");
		String sql = "SELECT pk, adminType, adminData, caId, module, eventTime, username, certificateSerialNumber, eventId, eventComment from ProtectedLogData where ( "
		+ queryString + ") and (" + capriviledges + ")";
		if (StringUtils.isNotEmpty(viewlogprivileges)) {
			sql += " and (" + viewlogprivileges + ")";
		}
		// Finally order the return values
		sql += " order by eventTime desc";
		if (log.isDebugEnabled()) {
			log.debug("Query: "+sql);			
		}
		ret = getProtectedLogSession().performQuery(sql);
		log.trace("<query()");
		return ret;
	} // query
	
	public static Properties getPropertiesFromInstance() {
		if (instance == null) {
			return new Properties();
		}
		return instance.getProperties();
	}
	
	public static int getMaxVerificationsSteps() {
		return Integer.parseInt(getPropertiesFromInstance().getProperty(CONFIG_MAX_VERIFICATION_STEPS, DEFAULT_MAX_VERIFICATION_STEPS));
	}

	public static long getFreezeTreshold() {
		return Long.parseLong(getPropertiesFromInstance().getProperty(ProtectedLogVerifier.CONF_FREEZE_THRESHOLD, ProtectedLogVerifier.DEFAULT_FREEZE_THRESHOLD)) * 60 * 1000;
	}

	private String getNodeIP(Properties prop) {
		String nodeIP = getNodeIP();
		nodeIP = prop.getProperty(CONFIG_NODEIP, nodeIP);
		return nodeIP;
	}

	public static String getNodeIP() {
		String nodeIP = DEFAULT_NODEIP;
        try {
        	nodeIP = InetAddress.getLocalHost().getHostAddress();
        }
        catch (java.net.UnknownHostException uhe) {
        }
		nodeIP = getPropertiesFromInstance().getProperty(CONFIG_NODEIP, nodeIP);
		return nodeIP;
	}
	
	private class HashTime {
		private byte[] hash = null;
		private long time = 0;

		public HashTime(byte[] hash, long time) {
			this.hash = hash;
			this.time = time;
		}
		
		public byte[] getHash() { return hash; }
		public long getTime() { return time; }
	}
}
