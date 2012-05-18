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

package org.ejbca.ui.web.pub.cluster;

import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.sql.Connection;
import java.sql.Statement;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ejbca.core.ejb.JNDINames;
import org.ejbca.util.JDBCUtil;

/** Base class for health checkers with functionality that is common to at least
 * both EJBCA and External OCSP.
 * 
 * @version $Id: CommonHealthCheck.java 7091 2009-03-10 19:59:42Z anatom $
 */
public abstract class CommonHealthCheck implements IHealthCheck {
	private static final Logger log = Logger.getLogger(CommonHealthCheck.class);

	private int minfreememory = 0;
	private String checkDBString = null;
	private String maintenanceFile = null;
	private String maintenancePropertyName = null;

	public CommonHealthCheck() {
		super();
	}
	
	public abstract String checkHealth(HttpServletRequest request);

	public void init(ServletConfig config) {
		minfreememory = Integer.parseInt(config.getInitParameter("MinimumFreeMemory")) * 1024 * 1024;
		checkDBString = config.getInitParameter("checkDBString");
		maintenanceFile = config.getInitParameter("MaintenanceFile");
		maintenancePropertyName = config.getInitParameter("MaintenancePropertyName");
		initMaintenanceFile();
	}

	protected String checkMemory(){
		log.debug("Checking JVM memory.");
		String retval = "";
        if(minfreememory >= Runtime.getRuntime().freeMemory()){
          retval = "\nMEM: Error Virtual Memory is about to run out, currently free memory :" + Runtime.getRuntime().freeMemory();	
        }		
		
		return retval;
	}

	protected String checkDB(){
		log.debug("Checking database connection.");
		String retval = "";
		Connection con = null;
		try{	
		  con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
		  Statement statement = con.createStatement();
		  statement.execute(checkDBString);		  
		}catch(Exception e){
			retval = "\nDB: Error creating connection to EJBCA Database: "+e.getMessage();
			log.error("Error creating connection to EJBCA Database.",e);
		} finally {
			JDBCUtil.close(con);
		}
		return retval;
	}

	protected String checkMaintenance() {
		Properties maintenanceProperties = new Properties();
		if (StringUtils.isEmpty(maintenanceFile)) {
			log.debug("Maintenance file not specified, node will be monitored");
			return "";
		} 
		InputStream in = null;
		try {
			in = new FileInputStream(maintenanceFile);
			maintenanceProperties.load(in);
		} catch (IOException e) {
			log.debug("Could not read Maintenance File. Expected to find file at: "+ maintenanceFile);
			return "";
		} finally {
			if (in != null) {
				try {
					in.close();					
				} catch (IOException e) {
					log.error("Error closing file: ", e);
				}
			}
		}
		try {
			String temp = maintenanceProperties.getProperty(maintenancePropertyName).toString();
			if (temp.equalsIgnoreCase("true")) {
				return "MAINT: "+maintenancePropertyName;
			} else {
				return "";
			}
		} catch (NullPointerException e) {
			log.info("Could not find property " + maintenancePropertyName+ " in " + maintenanceFile+ ", will continue to monitor this node");
			return "";
		}			
	}
	
	private void initMaintenanceFile() {
		if (StringUtils.isEmpty(maintenanceFile)) {
			log.debug("Maintenance file not specified, node will be monitored");
		} else {
			Properties maintenanceProperties = new Properties();
			InputStream in = null;
			try {
				in = new FileInputStream(maintenanceFile);
				maintenanceProperties.load(in);
			} catch (IOException e) {
				log.debug("Could not read Maintenance File. Expected to find file at: "+ maintenanceFile);
				OutputStream out = null;
				try {
					out = new FileOutputStream("filename.properties");
					maintenanceProperties.store(out, null);
				} catch (IOException e2) {
					log.error("Could not create Maintenance File at: "+ maintenanceFile);
				} finally {
					if (out != null) {
						try {
							out.close();					
						} catch (IOException oe) {
							log.error("Error closing file: ", e);
						}
					}
				}
			} finally {
				if (in != null) {
					try {
						in.close();					
					} catch (IOException e) {
						log.error("Error closing file: ", e);
					}
				}
			}
		}
	}

}