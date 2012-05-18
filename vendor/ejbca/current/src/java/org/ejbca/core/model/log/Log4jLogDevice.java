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
import java.security.cert.Certificate;
import java.text.DateFormat;
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import org.apache.log4j.Level;
import org.apache.log4j.Logger;
import org.apache.log4j.Priority;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.ca.caadmin.CADoesntExistsException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceNotActiveException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequestException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.IllegalExtendedCAServiceRequestException;
import org.ejbca.util.CertTools;
import org.ejbca.util.query.IllegalQueryException;
import org.ejbca.util.query.Query;


/**
 * Implements a log device using Log4j, implementes the Singleton pattern.
 *
 * @version $Id: Log4jLogDevice.java 7351 2009-04-30 09:26:58Z anatom $
 */
public class Log4jLogDevice implements ILogDevice, Serializable {

	public final static String DEFAULT_DEVICE_NAME	= "Log4jLogDevice";

    /** Log4j instance  */
    private static final Logger log = Logger.getLogger(Log4jLogDevice.class);

    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    /**
     * A handle to the unique Singleton instance.
     */
    private static Log4jLogDevice instance;

	private String deviceName = null;

    /**
     * Initializes all internal data
     *
     * @param prop Arguments needed for the eventual creation of the object
     */

    protected Log4jLogDevice(Properties properties) throws Exception {
    	resetDevice(properties);
    }
    
	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public void resetDevice(Properties properties) {
		deviceName = properties.getProperty(ILogDevice.PROPERTY_DEVICENAME, DEFAULT_DEVICE_NAME);
	}


    /**
     * Creates (if needed) the log device and returns the object.
     *
     * @param prop Arguments needed for the eventual creation of the object
     * @return An instance of the log device.
     */
    public static synchronized ILogDevice instance(Properties prop) throws Exception {
        if (instance == null) {
            instance = new Log4jLogDevice(prop);
        }
        return instance;
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
		return null;
	}

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
    public void log(Admin admininfo, int caid, int module, Date time, String username, Certificate certificate, int event, String comment, Exception exception) {

    	String user = intres.getLocalizedMessage("log.nouserinvolved");
    	String cert = intres.getLocalizedMessage("log.nocertinvolved");
    	String admin = intres.getLocalizedMessage("log.adminnotknown");

        if (username != null) {
            user = username;
        }

        if (certificate != null) {
        	cert = CertTools.getSerialNumberAsString(certificate) + ", issuer: " + CertTools.getIssuerDN(certificate);        		
        }

        if (admininfo.getAdminType() == Admin.TYPE_CLIENTCERT_USER) {
            admin = Admin.ADMINTYPETEXTS[Admin.TYPE_CLIENTCERT_USER] + ", Certificate SNR : " + admininfo.getAdminData();
        } else if (admininfo.getAdminType() == Admin.TYPE_PUBLIC_WEB_USER) {
            if (admininfo.getAdminData() != null) {
                if (!admininfo.getAdminData().equals("")) {
                    admin = Admin.ADMINTYPETEXTS[Admin.TYPE_PUBLIC_WEB_USER] + ", IP Address : " + admininfo.getAdminData();
                }
            } else {
                admin = Admin.ADMINTYPETEXTS[Admin.TYPE_PUBLIC_WEB_USER];
            }
        } else {
            admin = Admin.ADMINTYPETEXTS[admininfo.getAdminType()];
        }

        Priority priority = Level.INFO;
        String eventText = "";
        if (event >= LogConstants.EVENT_SYSTEM_BOUNDRARY) {
            event -= LogConstants.EVENT_SYSTEM_BOUNDRARY;
            eventText = LogConstants.EVENTNAMES_SYSTEM[event];
        } else if (event >= LogConstants.EVENT_ERROR_BOUNDRARY) {
            priority = Level.ERROR;
            event -= LogConstants.EVENT_ERROR_BOUNDRARY;
            eventText = LogConstants.EVENTNAMES_ERROR[event];
        }else{
        	eventText = LogConstants.EVENTNAMES_INFO[event];	
        }

        String logline = DateFormat.getDateTimeInstance(DateFormat.LONG, DateFormat.LONG).format(time) + ", CAId : " + caid + ", " + LogConstants.MODULETEXTS[module] + ", " + eventText + ", Administrator : " +
                admin + ", User : " + user + ", Certificate : " + cert + ", Comment : " + comment;
        log.log(priority, logline, null);

        if (exception != null) {
            log.log(priority, "Exception : ", exception);
        }
    }

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public byte[] export(Admin admin, Query query, String viewlogprivileges, String capriviledges, ILogExporter logexporter) throws IllegalQueryException, CADoesntExistsException, ExtendedCAServiceRequestException, IllegalExtendedCAServiceRequestException, ExtendedCAServiceNotActiveException {
		// Does not make sense to implement.. just return null
		return null;
	}

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public Collection query(Query query, String viewlogprivileges, String capriviledges) throws IllegalQueryException {
		// Does not make sense to implement.. just return null
		return null;
	}

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public void destructor() {
		// No action needed
	}

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public boolean getAllowConfigurableEvents() {
		return true;
	}
}
