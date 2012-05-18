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
package org.ejbca.ui.cli.batch;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.util.Properties;

import org.apache.log4j.Logger;

/**
 * Class used to manage the batch tool property file.
 * 
 * @author Philip Vendil 2006 sep 19
 *
 * @version $Id: BatchToolProperties.java 5585 2008-05-01 20:55:00Z anatom $
 */
public class BatchToolProperties {
	
	private static final String PROPERTY_KEYSPEC          = "keys.spec";
	private static final String PROPERTY_KEYALG           = "keys.alg";
	

	Properties batchToolProperties = new Properties();
	private static final Logger log = Logger.getLogger(BatchToolProperties.class);
	
	BatchToolProperties(){
		load();
	}
	
	/**
	 * Returns the configured keysize
	 * Default is 1024
	 */
	public String getKeySpec(){
		return batchToolProperties.getProperty(PROPERTY_KEYSPEC,"1024");
	}
	
	/**
	 * Returns the configured key algorithm
	 * Default is RSA, can be ECDSA
	 */
	public String getKeyAlg(){
		return batchToolProperties.getProperty(PROPERTY_KEYALG,"RSA");
	}

	
	
	
	/**
	 * Method that tries to read the property file 'batchtool.properties'
	 * in the home directory then in the current directory and finally 
	 * in the bin\batchtool.properties 
	 *
	 */
	private void load(){
        File file = new File( System.getProperty("user.home"),
                "batchtool.properties");
        try {
        	try{
			FileInputStream fis = new FileInputStream(file);
			batchToolProperties.load(fis);
		    } catch (FileNotFoundException e) {
		    	try{
		    		FileInputStream fis = new FileInputStream("batchtool.properties");
		    		batchToolProperties.load(fis);
		    	}catch (FileNotFoundException e1) {
		    		try{
		    			FileInputStream fis = new FileInputStream("bin/batchtool.properties");
		    			batchToolProperties.load(fis);
		    		}catch (FileNotFoundException e2) {
		    			log.info("Could not find any batchtool property file, default values will be used.");
		    			log.debug(e);
		    		}
		    	}
		    }
		} catch (IOException e) {
			log.error("Error reading batchtool property file ");
			log.debug(e);
		}
	}
	
	
	
}
