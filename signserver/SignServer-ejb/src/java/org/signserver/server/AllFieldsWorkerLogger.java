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
package org.signserver.server;

import java.util.Map;
import java.util.Properties;
import org.apache.log4j.Logger;

/**
 *
 * @author Markus Kil�s
 * @version $Id$
 */
public class AllFieldsWorkerLogger implements IWorkerLogger {

    private static final Logger ACCOUNTLOG =
            Logger.getLogger(IWorkerLogger.class);

    public void init(Properties props) {
        
    }

    public void log(Map<String, String> entries) throws WorkerLoggerException {
        final StringBuilder str = new StringBuilder();
        str.append("AllVariablesLogger; ");
        for (Map.Entry<String, String> entry : entries.entrySet()) {
            str.append(entry.getKey());
            str.append(": ");
            str.append(entry.getValue());
            str.append("; ");
        }
        
        // Last thing: add time for logging
        str.append(IWorkerLogger.LOG_REPLY_TIME);
        str.append(":");
        str.append(String.valueOf(System.currentTimeMillis()));

        // Do log
        ACCOUNTLOG.info(str.toString());
    }
}
