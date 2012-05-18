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

package org.ejbca.core.protocol.cmp;

import org.ejbca.core.protocol.IResponseMessage;

/**
 * Interface for message handler handling a specific CMP message
 * @author tomas
 * @version $Id: ICmpMessageHandler.java 5585 2008-05-01 20:55:00Z anatom $
 */
public interface ICmpMessageHandler {
	
	/**
	 * 
	 * @param msg input message
	 * @return response message
	 */
	public IResponseMessage handleMessage(BaseCmpMessage msg);

}
