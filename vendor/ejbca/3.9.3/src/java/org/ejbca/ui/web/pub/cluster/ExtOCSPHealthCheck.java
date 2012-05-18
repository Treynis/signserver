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

import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.log4j.Logger;
import org.ejbca.ui.web.protocol.IHealtChecker;



/**
 * External OCSP Health Checker. 
 * 
 * Does the following system checks.
 * 
 * * Not about to run out if memory (configurable through web.xml with param "MinimumFreeMemory")
 * * Database connection can be established.
 * * All OCSPSignTokens are active if not set as offline.
 * 
 * @author Philip Vendil
 * @version $Id: ExtOCSPHealthCheck.java 7847 2009-08-07 15:47:07Z primelars $
 */

public class ExtOCSPHealthCheck extends CommonHealthCheck {
	
	private static final Logger log = Logger.getLogger(ExtOCSPHealthCheck.class);
	private static IHealtChecker healthChecker;

	private boolean doSignTest = false;

	static public void setHealtChecker(IHealtChecker hc) {
		healthChecker = hc;
	}
	
	public void init(ServletConfig config) {
		super.init(config);
		final String sDoSign = config.getInitParameter("OCSPSignTest");
		this.doSignTest = sDoSign!=null && sDoSign.toLowerCase().indexOf("true")>=0;
        log.debug("OCSPSignTest: "+this.doSignTest);
	}
	
	public String checkHealth(HttpServletRequest request) {
		log.debug("Starting HealthCheck requested by : " + request.getRemoteAddr());
		String errormessage = "";
		
		errormessage += checkMaintenance();
		if( !errormessage.equals("") ) { 
			// if Down for maintenance do not perform more checks
			return errormessage; 
		} 
		errormessage += checkDB();
		if(errormessage.equals("")){
		  errormessage += checkMemory();								
		  errormessage += checkOCSPSignTokens();	
		}
		
		if(errormessage.equals("")){
			// everything seems ok.
			errormessage = null;
		}
		
		return errormessage;
	}
	
	
	private String checkOCSPSignTokens(){
		if ( healthChecker!=null ) {
		    if ( this.doSignTest ) {
		        return healthChecker.healthCheck();
		    }
		} else {
			return "No OCSP token health checker set";
		}
        return "";
	}
}
