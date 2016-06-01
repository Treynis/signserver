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
package org.signserver.module.tsa;

import java.util.Map;
import org.apache.log4j.Logger;
import org.signserver.common.RequestContext;
import org.signserver.common.WorkerConfig;
import org.signserver.server.SignServerContext;
import org.signserver.server.log.AdminInfo;
import org.signserver.server.log.BaseWorkerLogger;
import org.signserver.server.log.IWorkerLogger;
import org.signserver.server.log.Loggable;
import org.signserver.server.log.WorkerLoggerException;

/**
 * A custom made time stamp logger.
 *
 * @author Markus Kilås
 * @version $Id$
 */
public class CustomTimeStampLogger1 extends BaseWorkerLogger implements ITimeStampLogger {

    private static final Logger ACCOUNTLOG =
            Logger.getLogger(IWorkerLogger.class);

    @Override
    public void init(final int workerId, final WorkerConfig config, final SignServerContext context) {
        // No properties
    }

    @Override
    public void log(final AdminInfo adminInfo, final Map<String, Loggable> fields, final RequestContext context)
            throws WorkerLoggerException {
        final StringBuilder str = new StringBuilder();

        str.append("CustomLogger1; ");

        str.append("LOG_ID");
        str.append(": ");
        str.append(fields.get(IWorkerLogger.LOG_ID).logValue());
        str.append("; ");

        str.append("CLIENT_IP");
        str.append(": ");
        str.append(fields.get(IWorkerLogger.LOG_CLIENT_IP).logValue());
        str.append("; ");

        str.append("REQUEST_FULLURL");
        str.append(": ");
        str.append(fields.get(IWorkerLogger.LOG_REQUEST_FULLURL).logValue());
        str.append("; ");

        str.append("RequestTime");
        str.append(": ");
        str.append(fields.get(IWorkerLogger.LOG_TIME).logValue());
        str.append("; ");

        str.append("ResponseTime");
        str.append(": ");
        str.append(String.valueOf(System.currentTimeMillis()));
        str.append("; ");

        str.append("TimeStamp");
        str.append(": ");
        str.append(fields.get(ITimeStampLogger.LOG_TSA_TIME).logValue());
        str.append("; ");

        str.append("PKIStatus");
        str.append(": ");
        str.append(fields.get(ITimeStampLogger.LOG_TSA_PKISTATUS).logValue());
        str.append("; ");

        str.append("PKIFailureInfo");
        str.append(": ");
        str.append(fields.get(ITimeStampLogger.LOG_TSA_PKIFAILUREINFO).logValue());
        str.append("; ");

        str.append("TSA_POLICYID");
        str.append(": ");
        str.append(fields.get(ITimeStampLogger.LOG_TSA_POLICYID).logValue());
        str.append("; ");

        str.append("SIGNER_CERT_SERIALNUMBER");
        str.append(": ");
        str.append(fields.get(ITimeStampLogger.LOG_SIGNER_CERT_SERIALNUMBER).logValue());
        str.append("; ");

        str.append("SIGNER_CERT_ISSUERDN");
        str.append(": ");
        str.append(fields.get(ITimeStampLogger.LOG_SIGNER_CERT_ISSUERDN).logValue());
        str.append("; ");

        str.append("TSA_TIMESTAMPREQUEST_ENCODED");
        str.append(": ");
        str.append(fields.get(
                ITimeStampLogger.LOG_TSA_TIMESTAMPREQUEST_ENCODED).logValue());
        str.append("; ");

        str.append("TSA_TIMESTAMPRESPONSE_ENCODED");
        str.append(": ");
        str.append(fields.get(
                ITimeStampLogger.LOG_TSA_TIMESTAMPRESPONSE_ENCODED).logValue());
        str.append("; ");

        str.append("TSA_EXCEPTION");
        str.append(": ");
        str.append(fields.get(ITimeStampLogger.LOG_TSA_EXCEPTION).logValue());
        str.append("; ");

        str.append("EXCEPTION");
        str.append(": ");
        str.append(fields.get(IWorkerLogger.LOG_EXCEPTION).logValue());
        str.append("; ");

        ACCOUNTLOG.info(str.toString());
    }

}
