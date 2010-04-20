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
package org.signserver.server.timedservices;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintWriter;
import java.security.cert.Certificate;
import java.util.Collections;
import java.util.Hashtable;
import java.util.LinkedList;
import java.util.List;
import javax.ejb.EJB;
import javax.naming.Context;
import javax.naming.InitialContext;
import javax.naming.NamingException;

import javax.persistence.EntityManager;

import org.apache.log4j.Logger;
import org.signserver.common.CryptoTokenOfflineException;
import org.signserver.common.InvalidWorkerIdException;
import org.signserver.common.SignServerConstants;
import org.signserver.common.WorkerConfig;
import org.signserver.common.WorkerStatus;
import org.signserver.ejb.interfaces.IWorkerSession;
import org.signserver.server.KeyUsageCounter;
import org.signserver.server.ServiceExecutionFailedException;
import org.signserver.server.WorkerContext;

/**
 * TimedService that outputs a status report for a configured set of signers.
 *
 * @author Markus Kilas
 * @version $Id$
 */
public class SignerStatusReportTimedService extends BaseTimedService {

    /** Logger for this class. */
    private static final Logger LOG
            = Logger.getLogger(SignerStatusReportTimedService.class);

    /** Property OUTPUTFILE. **/
    private static final String PROPERTY_OUTPUTFILE = "OUTPUTFILE";

    /** Property WORKERS. **/
    private static final String PROPERTY_WORKERS = "WORKERS";

    private static final String STATUS_ACTIVE = "ACTIVE";
    private static final String STATUS_OFFLINE = "OFFLINE";

    private static final String SEPARATOR = ", ";

    /** Output file. */
    private File outputFile;

    /** List of worker names. */
    private List<String> workers = Collections.emptyList();

    /** Workersession. */
    @EJB
    private IWorkerSession.ILocal workerSession;

    /**
     * Initializes the worker.
     * @param workerId Id of worker
     * @param config the configuration
     * @param workerContext the context
     * @param workerEntityManager entity manager
     */
    @Override
    public void init(final int workerId, final WorkerConfig config,
            final WorkerContext workerContext,
            final EntityManager workerEntityManager) {
        super.init(workerId, config, workerContext, workerEntityManager);

        final String outputfileValue = config.getProperties()
                .getProperty(PROPERTY_OUTPUTFILE);
        if (outputfileValue != null) {
            outputFile = new File(outputfileValue);
        
            LOG.info("Output file: " + outputFile.getAbsolutePath());
        } else {
            LOG.error("Property OUTPUTFILE missing!");
        }

        workers = new LinkedList<String>();
        final String workersValue = config.getProperty(PROPERTY_WORKERS);
        if (workersValue == null) {
            LOG.error("Property WORKERS missing!");
        } else {
            for (String worker : workersValue.split(",")) {
                workers.add(worker);
            }
        }
        LOG.info("Worker[" + workerId +"]: " + "Workers: " + workers.size());

        getWorkerSession();
    }

    /**
     * Called to execute this timed service.
     * @see org.signserver.server.timedservices.ITimedService#work()
     * @throws ServiceExecutionFailedException in case of exception
     */
    public final void work() throws ServiceExecutionFailedException {
        LOG.trace(">work");
        LOG.info("Worker[" + workerId + "]: Service called");

        PrintWriter out = null;
        try {
            out = new PrintWriter(new FileOutputStream(outputFile));

            for (String worker : workers) {
                int workerId = workerSession.getWorkerId(worker);
                if (workerId == 0) {
                    LOG.warn("No such worker: " + worker);
                } else {
                    LOG.debug("Worker: " + worker);
                    String statusString = STATUS_ACTIVE;
                    KeyUsageCounter signings = null;
                    final String pk = getKeyHash(workerId);
                    if (pk == null) {
                        statusString = STATUS_OFFLINE;
                    } else {

                        WorkerStatus status = null;
                        try {
                            status = workerSession.getStatus(workerId);
                        } catch (InvalidWorkerIdException ex) {
                            LOG.error("Invalid worker id: " + workerId, ex);
                        }
                        if (status == null || status.isOK() != null) {
                            statusString = STATUS_OFFLINE;
                        }

                        try {
                        signings = em.find(
                            KeyUsageCounter.class, pk);
                        } catch (IllegalArgumentException ex) {
                            LOG.warn(ex, ex);
                        }
                    }
                    
                    final StringBuilder sb = new StringBuilder();
                    sb.append("workerName=");
                    sb.append(worker);
                    sb.append(SEPARATOR);
                    sb.append("status=");
                    sb.append(statusString);
                    sb.append(SEPARATOR);

                    if (signings != null) {
                        final long keyUsageLimit = Long.valueOf(
                                workerSession.getCurrentWorkerConfig(workerId)
                                .getProperty(
                                    SignServerConstants.KEYUSAGELIMIT, "-1"));
                        sb.append("signings=");
                        sb.append(signings.getCounter());
                        sb.append(SEPARATOR);
                        sb.append("signLimit=");
                        sb.append(keyUsageLimit);
                        sb.append(SEPARATOR);
                    }

                    out.println(sb.toString());
                }
            }

            if (out.checkError()) {
                LOG.error("Error occured trying to write output file");
            }
        } catch (IOException ex) {
            throw new ServiceExecutionFailedException(
                    "IO exception executing service " + workerId + " "
                    + ex.getMessage(), ex);
        } finally {
            if (out != null) {
                out.close();
            }
        }

        LOG.trace("<work");
    }

    private String getKeyHash(final int worker) {
        String ret = null;
        try {
            final Certificate cert = workerSession
                    .getSignerCertificate(worker);
            if (cert != null) {
                ret = KeyUsageCounter.createKeyHash(cert.getPublicKey());
            }
        } catch (CryptoTokenOfflineException ignored) {}
        return ret;
    }

    private IWorkerSession.ILocal getWorkerSession() {
        if (workerSession == null) {
            try {
                final Context context = getInitialContext();
                workerSession = (IWorkerSession.ILocal)
                        context.lookup(IWorkerSession.ILocal.JNDI_NAME);
            } catch (NamingException ex) {
                throw new RuntimeException("Unable to lookup worker session",
                        ex);
            }
        }
        return workerSession;
    }

    private Context getInitialContext() throws NamingException {
        final Hashtable<String, String> props =
                new Hashtable<String, String>();
        props.put(Context.INITIAL_CONTEXT_FACTORY,
                "org.jnp.interfaces.NamingContextFactory");
        props.put(Context.URL_PKG_PREFIXES,
                "org.jboss.naming:org.jnp.interfaces");
        props.put(Context.PROVIDER_URL, "jnp://localhost:1099");
        return new InitialContext(props);
    }
}
