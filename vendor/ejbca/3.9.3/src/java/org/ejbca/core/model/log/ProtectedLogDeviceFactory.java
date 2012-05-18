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

import java.util.Properties;

/**
 * 
 * @version $Id: ProtectedLogDeviceFactory.java 5992 2008-08-11 09:34:41Z anatom $
 *
 */
public class ProtectedLogDeviceFactory {

    /**
     * Creates a new ProtectedLogDeviceFactory object.
     */
    public ProtectedLogDeviceFactory() {
    }
    
    /**
     * Creates (if needed) the log device and returns the object.
     *
     * @param prop Arguments needed for the eventual creation of the object
     *
     * @return An instance of the log device.
     */
    public synchronized ILogDevice makeInstance(Properties prop) throws Exception {
        return ProtectedLogDevice.instance(prop);
    }
}

