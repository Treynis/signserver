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
package org.signserver.ejb;

import java.util.*;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.EJBException;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.log.AuditRecordStorageException;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.signserver.common.*;
import org.signserver.ejb.interfaces.IGlobalConfigurationSession;
import org.signserver.ejb.worker.impl.IWorkerManagerSessionLocal;
import org.signserver.server.config.entities.FileBasedGlobalConfigurationDataService;
import org.signserver.server.config.entities.GlobalConfigurationDataBean;
import org.signserver.server.config.entities.GlobalConfigurationDataService;
import org.signserver.server.config.entities.IGlobalConfigurationDataService;
import org.signserver.server.log.AdminInfo;
import org.signserver.server.log.SignServerEventTypes;
import org.signserver.server.log.SignServerModuleTypes;
import org.signserver.server.log.SignServerServiceTypes;
import org.signserver.server.nodb.FileBasedDatabaseManager;

/**
 * The implementation of the GlobalConfiguration Session Bean.
 * 
 * @see org.signserver.ejb.interfaces.IGlobalConfigurationSession           
 * @version $Id$
 */
@Stateless
public class GlobalConfigurationSessionBean implements IGlobalConfigurationSession.ILocal, IGlobalConfigurationSession.IRemote {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(GlobalConfigurationSessionBean.class);
    
    @EJB
    private IWorkerManagerSessionLocal workerManagerSession;
    
    @EJB
    private SecurityEventsLoggerSessionLocal logSession;
    
    EntityManager em;

    private static final long serialVersionUID = 1L;

    static {
        SignServerUtil.installBCProvider();
    }

    private IGlobalConfigurationDataService globalConfigurationDataService;
    
    @PostConstruct
    public void create() {
        if (em == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No EntityManager injected. Running without database.");
            }
            globalConfigurationDataService = new FileBasedGlobalConfigurationDataService(FileBasedDatabaseManager.getInstance());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("EntityManager injected. Running with database.");
            }
            globalConfigurationDataService = new GlobalConfigurationDataService(em);
        }
    }
    
    private IGlobalConfigurationDataService getGlobalConfigurationDataService() {
        return globalConfigurationDataService;
    }
    
    /**
     * @see org.signserver.ejb.interfaces.IGlobalConfigurationSession#setProperty(String, String, String)
     */
    @Override
    public void setProperty(String scope, String key, String value) {
        setProperty(new AdminInfo("CLI user", null, null), scope, key, value);
    }

    /**
     * @see org.signserver.ejb.interfaces.IGlobalConfigurationSession.ILocal#setProperty(AdminInfo, String, String, String)
     */    
    @Override
    public void setProperty(AdminInfo adminInfo, String scope, String key,
            String value) {
        auditLog(adminInfo, SignServerEventTypes.SET_GLOBAL_PROPERTY, scope + key, value);

        setPropertyHelper(propertyKeyHelper(scope, key), value);
    }

    private String propertyKeyHelper(String scope, String key) {
        String retval = null;
        String tempKey = key.toUpperCase();

        if (scope.equals(GlobalConfiguration.SCOPE_NODE)) {
            retval = GlobalConfiguration.SCOPE_NODE + WorkerConfig.getNodeId() + "." + tempKey;
        } else {
            if (scope.equals(GlobalConfiguration.SCOPE_GLOBAL)) {
                retval = GlobalConfiguration.SCOPE_GLOBAL + tempKey;
            } else {
                LOG.error("Error : Invalid scope " + scope);
            }
        }

        return retval;
    }

    /**
     * @see org.signserver.ejb.interfaces.IGlobalConfigurationSession.ILocal#removeProperty(AdminInfo, String, String)
     */
    @Override
    public boolean removeProperty(final AdminInfo adminInfo, String scope, String key) {
        boolean retval = false;

        auditLog(adminInfo, SignServerEventTypes.REMOVE_GLOBAL_PROPERTY, scope + key, null);

        try {
            retval = getGlobalConfigurationDataService().removeGlobalProperty(propertyKeyHelper(scope, key));
        } catch (Throwable e) {
            LOG.error("Error connecting to database, configuration is un-syncronized", e);
        }
        return retval;
    }
    
    /**
     * @see org.signserver.ejb.interfaces.IGlobalConfigurationSession#removeProperty(String, String)
     */    
    @Override
    public boolean removeProperty(String scope, String key) {
        return removeProperty(new AdminInfo("CLI user", null, null), scope, key);
    }
    

    /**
     * @see org.signserver.ejb.interfaces.IGlobalConfigurationSession#getGlobalConfiguration()
     */
    @Override
    public GlobalConfiguration getGlobalConfiguration() {
        GlobalConfiguration retval;

        Properties properties = new Properties();

        Iterator<GlobalConfigurationDataBean> iter = getGlobalConfigurationDataService().findAll().iterator();
        while (iter.hasNext()) {
            GlobalConfigurationDataBean data = iter.next();
            String rawkey = data.getPropertyKey();
            String propertyValue = data.getPropertyValue();

            if (rawkey.startsWith(GlobalConfiguration.SCOPE_NODE)) {
                String key = rawkey.replaceFirst(WorkerConfig.getNodeId() + ".", "");
                properties.setProperty(key, propertyValue == null ? "" : propertyValue);
            } else {
                if (rawkey.startsWith(GlobalConfiguration.SCOPE_GLOBAL)) {
                    properties.setProperty(rawkey,
                                    propertyValue == null ? "" : propertyValue);
                } else {
                    LOG.error("Illegal property in Global Configuration " + rawkey);
                }
            }
        }

        retval = new GlobalConfiguration(properties, 
                GlobalConfiguration.STATE_INSYNC, 
                CompileTimeSettings.getInstance().getProperty(CompileTimeSettings.SIGNSERVER_VERSION));

        return retval;
    }

    /**
     * @throws org.signserver.common.ResyncException
     * @see org.signserver.ejb.interfaces.IGlobalConfigurationSession.ILocal#resync()
     */
    @Override
    public void resync(final AdminInfo adminInfo) throws ResyncException {
        throw new UnsupportedOperationException("No longer supported operation");
    }

    @Override
    public void resync() throws ResyncException {
        resync(new AdminInfo("CLI user", null, null));
    }
    
    /**
     * @see org.signserver.ejb.interfaces.IGlobalConfigurationSession.ILocal#reload()
     */
    @Override
    public void reload(final AdminInfo adminInfo) {
        auditLog(adminInfo, SignServerEventTypes.GLOBAL_CONFIG_RELOAD, null, null);

        workerManagerSession.flush();
        getGlobalConfiguration();
    }
    
    @Override
    public void reload() {
        reload(new AdminInfo("CLI user", null, null));
    }
    
    /**
     * Helper method used to set properties in a table.
     * @param tempKey
     * @param value
     */
    private void setPropertyHelper(String key, String value) {
        try {
            getGlobalConfigurationDataService().setGlobalProperty(key, value);
        } catch (Throwable e) {
            String message = "Error connecting to database, configuration is un-syncronized :";
            LOG.error(message, e);
        }

    }

    private void auditLog(final AdminInfo adminInfo, final SignServerEventTypes eventType, final String property,
            final String value) {
        try {
            Map<String, Object> details = new LinkedHashMap<String, Object>();

            if (property != null) {
                details.put(IGlobalConfigurationSession.LOG_PROPERTY, property);
            }
            if (value != null) {
                details.put(IGlobalConfigurationSession.LOG_VALUE, value);

            }
            
            final String serialNo =
                    adminInfo.getCertSerialNumber() == null ? null : adminInfo.getCertSerialNumber().toString(16);
            logSession.log(eventType, EventStatus.SUCCESS, SignServerModuleTypes.GLOBAL_CONFIG, SignServerServiceTypes.SIGNSERVER, 
                    adminInfo.getSubjectDN(), adminInfo.getIssuerDN(), serialNo, null, details);
        } catch (AuditRecordStorageException ex) {
            LOG.error("Audit log failure", ex);
            throw new EJBException("Audit log failure", ex);
        }
    }
}
