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
 
/*
 * HardTokenDoesntExistsException.java
 *
 * Created on 20 januari 2003, 21:29
 */

package org.ejbca.core.model.hardtoken;

/**
 * An exception thrown when someone tries to remove or change a hard token that doesn't exits
 *
 * @author  Philip Vendil
 * @version $Id: HardTokenDoesntExistsException.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class HardTokenDoesntExistsException extends java.lang.Exception {
    
    /**
     * Creates a new instance of <code>HardTokenDoesntExistsException</code> without detail message.
     */
    public HardTokenDoesntExistsException() {
        super();
    }
    
    
    /**
     * Constructs an instance of <code>HardTokenDoesntExistsException</code> with the specified detail message.
     * @param msg the detail message.
     */
    public HardTokenDoesntExistsException(String msg) {
        super(msg);
    }
}
