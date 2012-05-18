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
package org.ejbca.core.model.ra.raadmin;

import org.ejbca.core.model.ra.UserDataVO;

/**
 * Interface for plug-in to retrieve notification recipient addresses. 
 * Implement this interface and put "CUSTOM:you.plugin.full.classname" as the recipient for 
 * user notifications.
 * 
 * @author Tomas Gustavsson
 * @version $Id: ICustomNotificationRecipient.java 5585 2008-05-01 20:55:00Z anatom $
 */
public interface ICustomNotificationRecipient {

	/** Returns a comma separated list of recipient email addresses.
	 * 
	 * @param user UserDataVO of the user that will be notified
	 * @return a comma separated list of email addresses
	 */
	public String getRecipientEmails(UserDataVO user);
}
