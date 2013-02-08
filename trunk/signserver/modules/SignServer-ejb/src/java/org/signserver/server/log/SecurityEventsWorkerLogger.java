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
package org.signserver.server.log;

import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Properties;
import java.util.Set;

import org.cesecore.audit.enums.EventStatus;
import org.cesecore.audit.log.SecurityEventsLoggerSessionLocal;

/**
 * Worker logger implementation using CESeCore's SecurityEventsLogger
 * 
 * @author Marcus Lundblad
 * @version $Id$
 *
 */

public class SecurityEventsWorkerLogger implements IWorkerLogger {
    private SecurityEventsLoggerSessionLocal logger;
    
    /** configuration keys for selecting included/excluded fields. */
    private static final String INCLUDE_FIELDS = "INCLUDEFIELDS";
    private static final String EXCLUDE_FIELDS = "EXCLUDEFIELDS";
    
    private Set<String> includedFields;
    private Set<String> excludedFields;
    
    @Override
    public void init(Properties props) {
        final String include = props.getProperty(INCLUDE_FIELDS);
        final String exclude = props.getProperty(EXCLUDE_FIELDS);
        
        if (include != null) {
            final String[] includes = include.split(",");
            includedFields = new HashSet<String>();
            
            for (final String field : includes) {
                includedFields.add(field.trim());
            }
        }
        
        if (exclude != null) {
            final String[] excludes = exclude.split(",");
            excludedFields = new HashSet<String>();
            
            for (final String field : excludes) {
                excludedFields.add(field.trim());
            }
        }
    }

    @Override
    public void log(final AdminInfo adminInfo, Map<String, String> fields) throws WorkerLoggerException {
        final Map<String, Object> details = new LinkedHashMap<String, Object>();
        
        // strip out the worker ID from the additionalDetails field (it's put customID)
        for (String key : fields.keySet()) {
            if (!IWorkerLogger.LOG_WORKER_ID.equals(key) &&
                (includedFields == null || includedFields.contains(key)) &&
                (excludedFields == null || !excludedFields.contains(key))) {
                details.put(key, fields.get(key));
            }
        }
        final String serNo = adminInfo.getCertSerialNumber() != null ? adminInfo.getCertSerialNumber().toString(16) : null;
        logger.log(SignServerEventTypes.PROCESS, EventStatus.SUCCESS, SignServerModuleTypes.WORKER, SignServerServiceTypes.SIGNSERVER,
                adminInfo.getSubjectDN(), adminInfo.getIssuerDN(), serNo, fields.get(IWorkerLogger.LOG_WORKER_ID), details);
    }

    @Override
    public void setEjbs(Map<Class<?>, ?> ejbs) {
        logger = (SecurityEventsLoggerSessionLocal) ejbs.get(SecurityEventsLoggerSessionLocal.class);
    }

}
