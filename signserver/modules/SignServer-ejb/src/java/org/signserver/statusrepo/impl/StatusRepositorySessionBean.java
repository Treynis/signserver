/*************************************************************************
 *                                                                       *
 *  SignServer: The OpenSource Automated Signing Server                  *
 *                                                                       *
 *  This software is free software; you can redistribute it and/or       *
 *  modify it under the terms of the GNU Lesser General Public           *
 *  License as published by the Free Software Foundation; either         *
 *  version 2.1 of the License, or any later version.                    *
 *                                                                       *
 *  See terms of license at gnu.org.                                     *
 *                                                                       *
 *************************************************************************/
package org.signserver.statusrepo.impl;

import java.util.HashMap;
import java.util.Map;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import org.apache.log4j.Logger;
import org.signserver.server.log.ISystemLogger;
import org.signserver.server.log.SystemLoggerException;
import org.signserver.server.log.SystemLoggerFactory;
import org.signserver.statusrepo.IStatusRepositorySession;
import org.signserver.statusrepo.common.NoSuchPropertyException;
import org.signserver.statusrepo.common.StatusEntry;
import org.signserver.statusrepo.common.StatusName;

/**
 * Session bean offering an interface towards the status repository.
 *
 * @author Markus Kilås
 * @version $Id: StatusRepositorySessionBean.java 1823 2011-08-10 07:53:56Z netmackan $
 */
@Stateless
public class StatusRepositorySessionBean implements
        IStatusRepositorySession.ILocal, IStatusRepositorySession.IRemote {

    /** Logger for this class. */
    private static final Logger LOG =
            Logger.getLogger(StatusRepositorySessionBean.class);

    /** Audit logger. */
    private static final ISystemLogger AUDITLOG = SystemLoggerFactory
            .getInstance().getLogger(StatusRepository.class);

    /** The repository instance. */
    private final StatusRepository repository;

    
    /**
     * Constructs this class.
     */
    public StatusRepositorySessionBean() {
        repository = StatusRepository.getInstance();
    }

    /**
     * Get a property.
     *
     * @param key Key to get the value for
     * @return The value if existing and not expired, otherwise null
     */
    @Override
    public StatusEntry getValidEntry(String key) throws NoSuchPropertyException {
        try {
            final StatusEntry result;
            final StatusEntry data = repository.get(StatusName.valueOf(key));

            final long time = System.currentTimeMillis();

            if (data != null && LOG.isDebugEnabled()) {
                LOG.debug("data.expire=" + data.getExpirationTime() + ", " + time);
            }

            // First check the expiration and then read the value
            if (data != null && (data.getExpirationTime() == 0  || data.getExpirationTime() > time)) {
                result = data;
            } else {
                result = null;
            }
            return result;
        } catch (IllegalArgumentException ex) {
            throw new NoSuchPropertyException(key);
        }
    }

    /**
     * Set a property without expiration, the value will live until the
     * application is restarted.
     *
     * @param key The key to set the value for
     * @param value The value to set
     */
    @Override
    public void update(final String key, final String value) throws NoSuchPropertyException {
        update(key, value, 0L);
    }

     /**
     * Set a property with a given expiration timestamp.
     *
     * After the expiration the get method will return null.
     *
     * @param key The key to set the value for
     * @param value The value to set
     */
    @Override
    public void update(final String key, final String value,
            final long expiration) throws NoSuchPropertyException {
        try {
            final long currentTime = System.currentTimeMillis();
            repository.set(StatusName.valueOf(key), new StatusEntry(currentTime, value, expiration));
            auditLog("setProperty", key, value, expiration);
        } catch (IllegalArgumentException ex) {
            throw new NoSuchPropertyException(key);
        }
    }

    /**
     * @return An unmodifiable map of all properties
     */
    @Override
    public Map<String, StatusEntry> getAllEntries() {
        return repository.getEntries();
    }
    
    private static void auditLog(String operation, String property, 
            String value,
            Long expiration) {
        try {

            final Map<String, String> logMap = new HashMap<String, String>();

            logMap.put(ISystemLogger.LOG_CLASS_NAME,
                    StatusRepositorySessionBean.class.getSimpleName());
            logMap.put(IStatusRepositorySession.LOG_OPERATION,
                    operation);
            logMap.put(IStatusRepositorySession.LOG_PROPERTY,
                    property);
            if (value != null) {
                logMap.put(IStatusRepositorySession.LOG_VALUE,
                        value);
            }
            if (expiration != null) {
                logMap.put(IStatusRepositorySession.LOG_EXPIRATION,
                    value);
            }

            AUDITLOG.log(logMap);
        } catch (SystemLoggerException ex) {
            LOG.error("Audit log failure", ex);
            throw new EJBException("Audit log failure", ex);
        }
    }
}
