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
package org.signserver.server.archive;

import org.apache.log4j.Logger;
import org.signserver.common.RequestContext;

/**
 * Utility methods for archiving
 * 
 * @author Marcus Lundblad
 * @version $Id$
 *
 */
public class ArchiverUtils {

    private static Logger LOG = Logger.getLogger(ArchiverUtils.class);
    
    public static String getXForwardedFor(final RequestContext requestContext) {
        final String xForwardedFor = (String) requestContext.get(RequestContext.X_FORWARDED_FOR);
        
        if (LOG.isDebugEnabled()) {
            LOG.debug("Using X-Forwarded-For: " + xForwardedFor);
        }
        
        if (xForwardedFor != null) {
            // the X-FORWARDED-FOR contains a comma-separated list of IP addresses, take the the last one
            final String[] ips = xForwardedFor.split(",");
            final String ip = ips[ips.length - 1].trim();
            
            if (LOG.isDebugEnabled()) {
                LOG.debug("Got IP address from X-Forwarded-For: " + ip);
            }
            
            return ip;
        }
        
        return null;
    }
    
}
