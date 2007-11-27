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
package org.signserver.groupkeyservice.common;

import java.util.Date;

/**
 * Class containing info about the remove group keys request
 * with time based specification.
 * 
 * 
 * @author Philip Vendil 13 nov 2007
 *
 * @version $Id: TimeRemoveGroupKeyRequest.java,v 1.1 2007-11-27 06:05:06 herrvendil Exp $
 */
public class TimeRemoveGroupKeyRequest implements IRemoveGroupKeyRequest {

	private static final long serialVersionUID = 1L;
	
	public static final int TYPE_CREATIONDATE = 0;
	public static final int TYPE_FIRSTUSEDDATE = 1;
	public static final int TYPE_LASTFETCHEDDATE = 2;
	
	private int type;
	private Date beginDate;
	private Date endDate;
	
	/**
	 * 
	 * @param type one of the TYPE_constants 
	 * @param beginDate the start date in the interval to remove.
	 * @param endDate the end date in the interval to remove.
	 */
	public TimeRemoveGroupKeyRequest(int type, Date beginDate, Date endDate) {
		super();
		this.type = type;
		this.beginDate = beginDate;
		this.endDate = endDate;
	}

	/**
	 * 
	 * @return one of the TYPE_constants 
	 */
	public int getType() {
		return type;
	}

	/**
	 * @return the start date in the interval to remove.
	 */
	public Date getBeginDate() {
		return beginDate;
	}

	/**
	 * @return the end date in the interval to remove.
	 */
	public Date getEndDate() {
		return endDate;
	}

}
