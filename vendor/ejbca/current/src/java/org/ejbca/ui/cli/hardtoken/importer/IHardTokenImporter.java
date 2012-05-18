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

package org.ejbca.ui.cli.hardtoken.importer;


import java.io.IOException;
import java.util.Properties;

import org.ejbca.core.model.hardtoken.HardTokenData;



/**
 * Interface used to customize the hard token data import, from an existing
 * datasource or file
 * 
 * 
 * @author Philip Vendil 2007 apr 23
 *
 * @version $Id: IHardTokenImporter.java 5585 2008-05-01 20:55:00Z anatom $
 */

public interface IHardTokenImporter {

	/**
	 * Method reponsible for the initialization of the importer.
	 * Is called once before the import of hard token datas is done.
	 * 
	 * @param props properties defiened for the importer
	 * @throws IOException
	 */
	void startImport(Properties props) throws IOException;
	
	/**
	 * Method reading one hard token from a source of data , and i supposed to return
	 * A HardTokenData from it. That will be added to the database.
	 * 
	 * @param source can be any ty
	 * @param null, if this is the last entry in the database.
	 *
	 */
	HardTokenData readHardTokenData() throws IOException;
	
	/**
	 * Method reponsible for finalizing the importer.
	 * 
	 */
	void endImport() throws IOException;	
	
}
