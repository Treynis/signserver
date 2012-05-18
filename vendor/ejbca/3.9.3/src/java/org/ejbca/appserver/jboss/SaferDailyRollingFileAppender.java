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

package org.ejbca.appserver.jboss;

import java.io.File;

import org.ejbca.core.protocol.ocsp.ISaferAppenderListener;

/**
* @author Tham Wickenberg
* @version  $Id: SaferDailyRollingFileAppender.java 6516 2008-10-28 16:00:01Z anatom $
*/
public class SaferDailyRollingFileAppender extends org.jboss.logging.appender.DailyRollingFileAppender {
	private static ISaferAppenderListener subscriber;
	public void append(org.apache.log4j.spi.LoggingEvent evt){
		super.append(evt);
		File logfile;
		try {
			logfile = new File(super.getFile());
			if ((subscriber != null) && (logfile != null) ){
				if (logfile.canWrite()) {
					subscriber.setCanlog(true);
				} 
				else {
					subscriber.setCanlog(false);
				}
			}
		} catch(Exception e) {
			if (subscriber != null) {
				subscriber.setCanlog(false);
			}
		}

	}
	

	public static void addSubscriber ( ISaferAppenderListener pSubscriber) {
		subscriber = pSubscriber;
	}
}
