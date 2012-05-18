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

import java.util.Collection;

/** This interface is used for exporting a number of log entries to 
 * any format defined by the implementing class.
 * 
 * @author tomas
 * @version $Id: ILogExporter.java 5585 2008-05-01 20:55:00Z anatom $
 */
public interface ILogExporter {

	/** Sets the entries to be exported. Entries can also be set in the contructor if it is more suitable for 
	 * the implementing class.  
	 * 
	 * @param logentries a Collection of LogEntry
	 */
	public void setEntries(Collection logentries);
	
	/** Returns the number of log intries that are about to be exported
	 * 
	 * @return positive integer or 0
	 */
	public int getNoOfEntries();

	/** Gets a CA used to create a signed CMS message of the log export, can be null for plain export
	 * 
	 * @return signCA CA (caid in string format, 12345) used to create a signed CMS message of the log export, or null for plain export
	 */
	public String getSigningCA();
	
	/** Returns the exported data, determined by the exporting class. Can be binary or text data.
	 * 
	 * @return byte data or null if no of exported entries are 0.
	 */
	public byte[] export();

}

