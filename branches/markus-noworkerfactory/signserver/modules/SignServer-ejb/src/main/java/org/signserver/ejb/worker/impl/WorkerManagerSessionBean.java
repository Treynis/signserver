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
package org.signserver.ejb.worker.impl;

import java.util.ArrayList;
import java.util.Enumeration;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import javax.annotation.PostConstruct;
import javax.ejb.EJB;
import javax.ejb.Stateless;
import javax.persistence.EntityManager;

import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;
import org.signserver.common.GlobalConfiguration;
import org.signserver.common.IllegalRequestException;
import org.signserver.common.WorkerConfig;
import org.signserver.ejb.interfaces.IGlobalConfigurationSession;
import org.signserver.ejb.interfaces.IGlobalConfigurationSession.ILocal;
import org.signserver.server.*;
import org.signserver.server.archive.Archiver;
import org.signserver.server.config.entities.FileBasedWorkerConfigDataService;
import org.signserver.server.config.entities.IWorkerConfigDataService;
import org.signserver.server.config.entities.WorkerConfigDataService;
import org.signserver.server.entities.FileBasedKeyUsageCounterDataService;
import org.signserver.server.entities.IKeyUsageCounterDataService;
import org.signserver.server.entities.KeyUsageCounterDataService;
import org.signserver.server.log.IWorkerLogger;
import org.signserver.server.nodb.FileBasedDatabaseManager;
import org.signserver.server.timedservices.ITimedService;

/**
 * Session bean managing the worker life-cycle.
 *
 * @see WorkerFactory
 * @author Markus Kilås
 * @version $Id$
 */
@Stateless
public class WorkerManagerSessionBean implements IWorkerManagerSessionLocal {
    
    /** Logger for this class. */
    private static final org.apache.log4j.Logger LOG = org.apache.log4j.Logger.getLogger(WorkerManagerSessionBean.class);
    
    EntityManager em;
    
    private IWorkerConfigDataService workerConfigService;
    private IKeyUsageCounterDataService keyUsageCounterDataService;
    
    private final WorkerFactory workerFactory = new WorkerFactory();
    
    private SignServerContext workerContext;
    
    @EJB
    private SecurityEventsLoggerSessionLocal logSession;
    
    @PostConstruct
    public void create() {
        if (em == null) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("No EntityManager injected. Running without database.");
            }
            workerConfigService = new FileBasedWorkerConfigDataService(FileBasedDatabaseManager.getInstance());
            keyUsageCounterDataService = new FileBasedKeyUsageCounterDataService(FileBasedDatabaseManager.getInstance());
        } else {
            if (LOG.isDebugEnabled()) {
                LOG.debug("EntityManager injected. Running with database.");
            }
            workerConfigService = new WorkerConfigDataService(em);
            keyUsageCounterDataService = new KeyUsageCounterDataService(em);
        }
        workerContext = new SignServerContext(em, keyUsageCounterDataService);
    }

    @Override
    public IWorker getWorker(final int workerId, final IGlobalConfigurationSession globalSession) {
        return getWorker(workerId, globalSession.getGlobalConfiguration());
    }
    
    private IWorker getWorker(final int workerId, final GlobalConfiguration gc) {
        return workerFactory.getWorker(workerId,
                workerConfigService, gc, workerContext);
    }

    /**
     * XXX: Expensive
     * @param workerName
     * @param globalSession
     * @return 
     */
    @Override
    public int getIdFromName(final String workerName, final IGlobalConfigurationSession globalSession) {
        LOG.warn("Expensive call to getIdFromName");
        int result = 0;
        
        GlobalConfiguration gc = globalSession.getGlobalConfiguration();
        List<Integer> workers = getWorkers(GlobalConfiguration.WORKERTYPE_ALL, gc);
        for (Integer workerId : workers) {
            WorkerConfig conf = workerConfigService.getWorkerProperties(workerId);
            if (conf != null && workerName.equalsIgnoreCase(conf.getProperty("NAME"))) {
                result = workerId;
                break;
            }
        }
        
        if (result == 0) {
            LOG.warn("No such workerName: " + workerName);
        }
        
        return result;
    }

    @Override
    public void reloadWorker(int workerId, ILocal globalConfigurationSession) {
        LOG.warn("reloadWorker is a no-op");
    }

    @Override
    public IWorkerLogger getWorkerLogger(int workerId, WorkerConfig awc) throws IllegalRequestException {
        final IWorkerLogger logger = workerFactory.getWorkerLogger(workerId, awc, em);
        logger.setEjbs(getEjbs());
        
        return logger;
    }

    @Override
    public IAuthorizer getAuthenticator(int workerId, String authenticationType, WorkerConfig awc) throws IllegalRequestException {
        return workerFactory.getAuthenticator(workerId,
                            authenticationType,
                            awc,
                            em);
    }

    @Override
    public IAccounter getAccounter(int workerId, WorkerConfig awc) throws IllegalRequestException {
        return workerFactory.getAccounter(workerId,
                                    awc,
                                    em);
    }

    @Override
    public List<Archiver> getArchivers(int workerId, WorkerConfig awc) throws IllegalRequestException {
        return workerFactory.getArchivers(workerId, awc, workerContext);
    }

    @Override
    public void flush() {
        LOG.warn("flush is a no-op");
    }
    
    /**
     * @see org.signserver.ejb.interfaces.IWorkerSession#getWorkers(int)
     */
    @Override
    public List<Integer> getWorkers(int workerType, IGlobalConfigurationSession globalConfigurationSession) {
        return getWorkers(workerType, globalConfigurationSession.getGlobalConfiguration());
    }
    
    private List<Integer> getWorkers(int workerType, GlobalConfiguration gc) {
        ArrayList<Integer> result = new ArrayList<Integer>();
        Enumeration<String> en = gc.getKeyEnumeration();
        while (en.hasMoreElements()) {
            String key = en.nextElement();
            if (LOG.isTraceEnabled()) {
                LOG.trace("getWorkers, processing key : " + key);
            }
            if (key.startsWith("GLOB.WORKER") || key.startsWith("GLOB.SIGNER")) {
                getWorkerHelper(result, key, workerType, false, gc);
            }
        }
        return result;
    }

    private void getWorkerHelper(List<Integer> retval, String key, int workerType, boolean signersOnly, GlobalConfiguration gc) {
        String unScopedKey = key.substring("GLOB.".length());
        if (LOG.isTraceEnabled()) {
            LOG.trace("unScopedKey : " + unScopedKey);
        }
        String strippedKey = key.substring("GLOB.WORKER".length());
        if (LOG.isTraceEnabled()) {
            LOG.trace("strippedKey : " + strippedKey);
        }
        String[] splittedKey = strippedKey.split("\\.");
        if (LOG.isTraceEnabled()) {
            LOG.trace("splittedKey : " + splittedKey.length + ", " + splittedKey[0]);
        }
        if (splittedKey.length > 1) {
            if (splittedKey[1].equals("CLASSPATH")) {
                int id = Integer.parseInt(splittedKey[0]);
                if (workerType == GlobalConfiguration.WORKERTYPE_ALL) {
                    retval.add(new Integer(id));
                } else {
                    IWorker obj = getWorker(id, gc);
                    if (workerType == GlobalConfiguration.WORKERTYPE_PROCESSABLE) {
                        if (obj instanceof IProcessable) {
                            if (LOG.isDebugEnabled()) {
                                LOG.debug("Adding Signer " + id);
                            }
                            retval.add(new Integer(id));
                        }
                    } else {
                        if (workerType == GlobalConfiguration.WORKERTYPE_SERVICES && !signersOnly) {
                            if (obj instanceof ITimedService) {
                                if (LOG.isDebugEnabled()) {
                                    LOG.debug("Adding Service " + id);
                                }
                                retval.add(new Integer(id));
                            }
                        }
                    }
                }
            }
        }
    }
    
    private Map<Class<?>, Object> getEjbs() {
        final Map<Class<?>, Object> ejbs = new HashMap<Class<? extends Object>, Object>();
        ejbs.put(SecurityEventsLoggerSessionLocal.class, logSession);
        
        return ejbs;
    }
    
}
