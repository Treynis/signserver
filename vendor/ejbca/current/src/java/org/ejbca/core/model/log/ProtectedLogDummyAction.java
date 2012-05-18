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

import java.io.Serializable;
import java.util.Properties;

/**
 * Dummy implementation. This is the simplest possible (and most useless) implementation.
 * @version $Id: ProtectedLogDummyAction.java 6976 2009-02-19 09:04:09Z anatom $
 */
public class ProtectedLogDummyAction implements IProtectedLogAction, Serializable {

	private static final long serialVersionUID = -7056505975194222537L;

	public ProtectedLogDummyAction(Properties properties) {
		// This constructor is used by ProtectedLogActions
	}

	/**
	 * @see org.ejbca.core.model.log.IProtectedLogAction
	 */
	public void action(String causeIdentifier) {
		// Does nothing
	}

}
