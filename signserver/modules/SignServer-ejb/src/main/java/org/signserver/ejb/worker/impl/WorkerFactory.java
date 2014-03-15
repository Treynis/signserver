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

import java.util.*;
import javax.persistence.EntityManager;
import org.apache.log4j.Logger;
import org.signserver.common.*;
import org.signserver.server.*;
import org.signserver.server.archive.Archiver;
import org.signserver.server.archive.ArchiverInitException;
import org.signserver.server.archive.olddbarchiver.OldDatabaseArchiver;
import org.signserver.server.config.entities.IWorkerConfigDataService;
import org.signserver.server.log.AllFieldsWorkerLogger;
import org.signserver.server.log.IWorkerLogger;

/**
 * Handles creation of workers and their components.
 *
 * @version $Id$
 */
public class WorkerFactory {

    /** Logger for this class. */
    public static final Logger LOG = Logger.getLogger(WorkerFactory.class);
    
    private static final String WORKERLOGGER = "WORKERLOGGER";
    
    private static final String ACCOUNTER = "ACCOUNTER";
    
    
    /**
     * Method returning a worker given it's id. The signer should be defined in 
     * the global configuration along with it's id.
     * 
     * The worker will only be created upon first call, then it's stored in memory until
     * the flush method is called.
     * 
     * @param workerId the Id that should match the one in the config file.
     * @param workerConfigHome The service interface of the signer config entity bean
     * @param gc
     * @param workerContext
     * @return A ISigner as defined in the configuration file, or null if no configuration
     * for the specified signerId could be found.
     */
    public IWorker getWorker(int workerId, 
            IWorkerConfigDataService workerConfigHome, 
            GlobalConfiguration gc, 
            WorkerContext workerContext) {
        IWorker result = null;

        if (LOG.isDebugEnabled()) {
            LOG.debug("Loading workers into WorkerFactory.");
        }

        final String className = gc.getWorkerClassPath(workerId);
        try {

            if (LOG.isDebugEnabled()) {
                LOG.debug("Loading worker with classpath: " + className);
            }
            if (className == null) {
                LOG.error("Missing worker class name for worker " + workerId);
            } else {
                WorkerConfig config = workerConfigHome.getWorkerProperties(workerId);

                ClassLoader cl = this.getClass().getClassLoader();
                Class<?> implClass = cl.loadClass(className);

                Object obj = implClass.newInstance();

                ((IWorker) obj).init(workerId, config, workerContext, null);
                result = (IWorker) obj;
            }
        } catch (ClassNotFoundException e) {
            LOG.error("Worker class not found (is the module included in the build?): " + className);
        } catch (IllegalAccessException e) {
            LOG.error("Could not access worker class: " + className);
        } catch (InstantiationException e) {
            LOG.error("Could not instantiate worker class: " + className);
        }

        if (result == null) {
            LOG.info("Trying to get worker with Id that does not exist: " + workerId);
        }
        return result;
    }

    /**
     * Returns the configured authorizer for the given worker.
     * 
     * @param workerId id of worker 
     * @param authType one of ISigner.AUTHTYPE_ constants or class path to custom implementation
     * @param config
     * @param em
     * @return initialized authorizer.
     * @throws org.signserver.common.IllegalRequestException
     */
    public IAuthorizer getAuthenticator(int workerId, String authType, WorkerConfig config, EntityManager em) throws IllegalRequestException {
        IAuthorizer result = null;
        if (authType.equalsIgnoreCase(IProcessable.AUTHTYPE_NOAUTH)) {
            result = new NoAuthorizer();
        } else if (authType.equalsIgnoreCase(IProcessable.AUTHTYPE_CLIENTCERT)) {
            result = new ClientCertAuthorizer();
        } else {
            try {
                Class<?> c = this.getClass().getClassLoader().loadClass(authType);
                result = (IAuthorizer) c.newInstance();
            } catch (ClassNotFoundException e) {
                LOG.error("Error worker with id " + workerId + " missconfiguration, AUTHTYPE setting : " + authType + " is not a correct class path.", e);
                throw new IllegalRequestException("Error worker with id " + workerId + " missconfiguration, AUTHTYPE setting : " + authType + " is not a correct class path.");
            } catch (InstantiationException e) {
                LOG.error("Error worker with id " + workerId + " missconfiguration, AUTHTYPE setting : " + authType + " is not a correct class path.", e);
                throw new IllegalRequestException("Error worker with id " + workerId + " missconfiguration, AUTHTYPE setting : " + authType + " is not a correct class path.");
            } catch (IllegalAccessException e) {
                LOG.error("Error worker with id " + workerId + " missconfiguration, AUTHTYPE setting : " + authType + " is not a correct class path.", e);
                throw new IllegalRequestException("Error worker with id " + workerId + " missconfiguration, AUTHTYPE setting : " + authType + " is not a correct class path.");
            }
        }

        try {
            result.init(workerId, config, em);
        } catch (SignServerException e) {
            LOG.error("Error initializing authorizer for worker " + workerId + " with authtype " + authType + ", message : " + e.getMessage(), e);
        }
        return result;
    }

    /**
     * 
     * @param workerId
     * @param config
     * @param em
     * @return
     * @throws IllegalRequestException 
     */
    public IWorkerLogger getWorkerLogger(final int workerId,
            final WorkerConfig config, final EntityManager em)
            throws IllegalRequestException {
        IWorkerLogger result = null;
        final String fullClassName = config.getProperty(WORKERLOGGER);

        if (fullClassName == null || "".equals(fullClassName)) {
            result = new AllFieldsWorkerLogger();
        } else {
            try {
                final Class<?> c = this.getClass().getClassLoader().loadClass(fullClassName);
                result = (IWorkerLogger) c.newInstance();
            } catch (ClassNotFoundException e) {
                final String error =
                        "Error worker with id " + workerId
                        + " missconfiguration, "
                        + WORKERLOGGER + " setting : "
                        + fullClassName
                        + " is not a correct "
                        + "fully qualified class name "
                        + "of an IWorkerLogger.";
                LOG.error(error, e);
                throw new IllegalRequestException(error);
            } catch (InstantiationException e) {
                final String error =
                        "Error worker with id " + workerId
                        + " missconfiguration, "
                        + WORKERLOGGER + " setting : "
                        + fullClassName
                        + " is not a correct "
                        + "fully qualified class name "
                        + "of an IWorkerLogger.";
                LOG.error(error, e);
                throw new IllegalRequestException(error);

            } catch (IllegalAccessException e) {
                final String error =
                        "Error worker with id " + workerId
                        + " missconfiguration, "
                        + WORKERLOGGER + " setting : "
                        + fullClassName
                        + " is not a correct "
                        + "fully qualified class name "
                        + "of an IWorkerLogger.";
                LOG.error(error, e);
                throw new IllegalRequestException(error);
            }
        }
        result.init(config.getProperties());
        return result;
    }

    /**
     * 
     * @param workerId
     * @param config
     * @param em
     * @return
     * @throws IllegalRequestException 
     */
    public IAccounter getAccounter(final int workerId,
            final WorkerConfig config, final EntityManager em)
            throws IllegalRequestException {
        IAccounter result = null;
        final String fullClassName = config.getProperty(ACCOUNTER);

        if (fullClassName == null || "".equals(fullClassName)) {
            result = new NoAccounter();
        } else {
            try {
                final Class<?> c = this.getClass().getClassLoader().loadClass(fullClassName);
                result = (IAccounter) c.newInstance();
            } catch (ClassNotFoundException e) {
                final String error =
                        "Error worker with id " + workerId
                        + " missconfiguration, "
                        + ACCOUNTER + " setting : "
                        + fullClassName
                        + " is not a correct "
                        + "fully qualified class name "
                        + "of an IAccounter.";
                LOG.error(error, e);
                throw new IllegalRequestException(error);
            } catch (InstantiationException e) {
                final String error =
                        "Error worker with id " + workerId
                        + " missconfiguration, "
                        + ACCOUNTER + " setting : "
                        + fullClassName
                        + " is not a correct "
                        + "fully qualified class name "
                        + "of an IAccounter.";
                LOG.error(error, e);
                throw new IllegalRequestException(error);

            } catch (IllegalAccessException e) {
                final String error =
                        "Error worker with id " + workerId
                        + " missconfiguration, "
                        + ACCOUNTER + " setting : "
                        + fullClassName
                        + " is not a correct "
                        + "fully qualified class name "
                        + "of an IAccounter.";
                LOG.error(error, e);
                throw new IllegalRequestException(error);
            }
        }
        result.init(config.getProperties());
        return result;
    }

    /**
     * 
     * @param workerId
     * @param config
     * @param context
     * @return
     * @throws IllegalRequestException 
     */
    public List<Archiver> getArchivers(final int workerId,
            final WorkerConfig config, final SignServerContext context)
            throws IllegalRequestException {
        List<Archiver> result = new LinkedList<Archiver>();
        final String list;

        // Support for old way of setting archiving and the new one
        if (config.getProperty(SignServerConstants.ARCHIVE,
                Boolean.FALSE.toString()).equalsIgnoreCase(Boolean.TRUE.toString())) {
            list = OldDatabaseArchiver.class.getName();
        } else {
            list = config.getProperty(SignServerConstants.ARCHIVERS);
        }

        if (list != null) {
            int index = 0;
            for (String className : list.split(",")) {
                className = className.trim();

                if (!className.isEmpty()) {
                    try {
                        final Class<?> c = this.getClass().getClassLoader().loadClass(className);
                        final Archiver archiver = (Archiver) c.newInstance();
                        result.add(archiver);
                        try {
                            archiver.init(index, config, context);
                        } catch (ArchiverInitException e) {
                            final String error =
                                    "Error worker with id " + workerId
                                    + " missconfiguration, "
                                    + "failed to initialize archiver "
                                    + index + ".";
                            LOG.error(error, e);
                            throw new IllegalRequestException(error);
                        }
                        index++;
                    } catch (ClassNotFoundException e) {
                        final String error =
                                "Error worker with id " + workerId
                                + " missconfiguration, "
                                + SignServerConstants.ARCHIVERS
                                + " setting : "
                                + className
                                + " is not a correct "
                                + "fully qualified class name "
                                + "of an Archiver.";
                        LOG.error(error, e);
                        throw new IllegalRequestException(error);
                    } catch (InstantiationException e) {
                        final String error =
                                "Error worker with id " + workerId
                                + " missconfiguration, "
                                + SignServerConstants.ARCHIVERS
                                + " setting : "
                                + className
                                + " is not a correct "
                                + "fully qualified class name "
                                + "of an Archiver.";
                        LOG.error(error, e);
                        throw new IllegalRequestException(error);

                    } catch (IllegalAccessException e) {
                        final String error =
                                "Error worker with id " + workerId
                                + " missconfiguration, "
                                + SignServerConstants.ARCHIVERS
                                + " setting : "
                                + className
                                + " is not a correct "
                                + "fully qualified class name "
                                + "of an Archiver.";
                        LOG.error(error, e);
                        throw new IllegalRequestException(error);
                    }
                }
            }
        }
        return result;
    }

}
