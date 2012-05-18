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
import java.util.Collection;
import java.util.Date;
import java.util.Properties;

import org.ejbca.core.model.ca.caadmin.CADoesntExistsException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceNotActiveException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequestException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.IllegalExtendedCAServiceRequestException;
import org.ejbca.util.query.IllegalQueryException;
import org.ejbca.util.query.Query;

/**
 * The dummy logging device. This does absolutely nothing and is just here for other developers to steal.. =)
 * @version $Id: DummyLogDevice.java 5992 2008-08-11 09:34:41Z anatom $
 */
public class DummyLogDevice implements ILogDevice, Serializable {

	public final static String DEFAULT_DEVICE_NAME = "DummyLogDevice";

	private static DummyLogDevice instance;
	private Properties properties;
	
	private String deviceName = null;

	protected DummyLogDevice(Properties properties) throws Exception {
		resetDevice(properties);
	}
	
	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public void resetDevice(Properties properties) {
		this.properties = properties;
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
			instance = new DummyLogDevice(prop);
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
		return properties;
	}
	
	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public byte[] export(Admin admin, Query query, String viewlogprivileges, String capriviledges, ILogExporter logexporter) throws IllegalQueryException, CADoesntExistsException, ExtendedCAServiceRequestException, IllegalExtendedCAServiceRequestException, ExtendedCAServiceNotActiveException {
		return null;
	}

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public void log(Admin admininfo, int caid, int module, Date time, String username, Certificate certificate, int event, String comment, Exception exception) {
	}

	/**
	 * @see org.ejbca.core.model.log.ILogDevice
	 */
	public Collection query(Query query, String viewlogprivileges, String capriviledges) throws IllegalQueryException {
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
