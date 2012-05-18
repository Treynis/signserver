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
package org.ejbca.core.model.ca.caadmin;

import java.security.cert.Certificate;
import java.text.DateFormat;
import java.text.ParseException;
import java.util.Date;
import java.util.Locale;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ra.ExtendedInformation;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.util.CertTools;
import org.ejbca.util.ValidityDate;

public class CertificateValidity {

    private static final Logger log = Logger.getLogger(CertificateValidity.class);
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    private Date lastDate;
    private Date firstDate;
    
    private static Date tooLateExpireDate;
    static {
        final String sDate = "@ca.toolateexpiredate@";
        if ( sDate==null || sDate.length()<1 ) {
        	log.debug("Using default value for ca.toolateexpiredate.");
            tooLateExpireDate = new Date(Long.MAX_VALUE);
        } else if (StringUtils.startsWith(sDate, "@ca.toolateexpiredate")) {
        	// If we are running in eclipse or something so the value has not been replaced, go for the default value.
        	log.warn("Using default value for ca.toolateexpiredate because it has not been replaced in CertificateValidity.java.");
            tooLateExpireDate = new Date(Long.MAX_VALUE);        	
        } else {
            tooLateExpireDate = ValidityDate.getDateFromString(sDate);
        	log.debug("tooLateExpireData is set to: "+tooLateExpireDate);
        }
    }
    /** Protected method so we can JUnit test this
     */
    protected static void setTooLateExpireDate(Date date) {
    	tooLateExpireDate = date;
    }
    
	public CertificateValidity(UserDataVO subject, CertificateProfile certProfile, 
			Date notBefore, Date notAfter, 
			Certificate cacert, boolean isRootCA) throws IllegalValidityException {
		if (log.isDebugEnabled()) {
			log.debug("Requested notBefore: "+notBefore);
			log.debug("Requested notAfter: "+notAfter);
		}
		if ( tooLateExpireDate==null ) {
		    throw new IllegalValidityException("ca.toolateexpiredate in ejbca.properties is not a valid date.");
		}
        // Set back start date ten minutes to avoid some problems with unsynchronized clocks.
        Date now = new Date((new Date()).getTime() - 10 * 60 * 1000);
		Date startTimeDate = null; 
		Date endTimeDate = null; 
        // Extract requested start and endtime from end endtity profile / user data
        ExtendedInformation ei = subject.getExtendedinformation();
        if ( ei != null ) {
            String eiStartTime = ei.getCustomData(ExtendedInformation.CUSTOM_STARTTIME);
	        String eiEndTime = ei.getCustomData(ExtendedInformation.CUSTOM_ENDTIME);
        	if ( eiStartTime != null ) {
        		if ( eiStartTime.matches("^\\d+:\\d?\\d:\\d?\\d$") ) {
        			String[] startTimeArray = eiStartTime.split(":");
        			long relative = (Long.parseLong(startTimeArray[0])*24*60 + Long.parseLong(startTimeArray[1])*60 +
        					Long.parseLong(startTimeArray[2])) * 60 * 1000;
        			startTimeDate = new Date(now.getTime() + relative);
        		} else {
        			try {
        				startTimeDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).parse(eiStartTime);
        			} catch (ParseException e) {
        				log.error(intres.getLocalizedMessage("signsession.errorinvalidstarttime",eiStartTime));
        			}
        		}
    			if ( startTimeDate != null && startTimeDate.before(now)) {
                	startTimeDate = now;
    			}
	        }
	        if ( eiEndTime != null ) {
        		if ( eiEndTime.matches("^\\d+:\\d?\\d:\\d?\\d$") ) {
        			String[] endTimeArray = eiEndTime.split(":");
        			long relative = (Long.parseLong(endTimeArray[0])*24*60 + Long.parseLong(endTimeArray[1])*60 +
        					Long.parseLong(endTimeArray[2])) * 60 * 1000;
        			endTimeDate = new Date(now.getTime() + relative);
        		} else {
        			try {
        				endTimeDate = DateFormat.getDateTimeInstance(DateFormat.MEDIUM, DateFormat.SHORT, Locale.US).parse(eiEndTime);
        			} catch (ParseException e) {
        				log.error(intres.getLocalizedMessage("signsession.errorinvalidstarttime",eiEndTime));
        			}
        		}
	        }
        }
        // Find out what start and end time to actually use..
        if (certProfile.getAllowValidityOverride()) {
            // Prio 1 is infomation supplied in Extended information object. This allows RA-users to set the time-span.
            firstDate = startTimeDate;
            lastDate = endTimeDate;
            // Prio 2 is the information supplied in the arguments
            if (firstDate == null) {
            	firstDate = notBefore;
            }
            if (lastDate == null) {
            	lastDate = notAfter;
            }    	
        }
        // Prio 3 is default values
        if (firstDate == null) {
        	firstDate = now;
        }
        final long val = certProfile.getValidity();        
        Date certProfileLastDate = ValidityDate.getDate(val,firstDate);
        if (lastDate == null) {
        	lastDate = certProfileLastDate;
        }
        // Limit validity: Do not allow last date to be before first date
        if (lastDate.before(firstDate)) {
			log.info(intres.getLocalizedMessage("signsession.errorinvalidcausality",firstDate,lastDate));
        	Date tmp = lastDate;
        	lastDate = firstDate;
        	firstDate = tmp;
        }
		// Limit validity: We do not allow a certificate to be valid before the current date, i.e. not back dated start dates
        // Unless allowValidityOverride is set, then we allow everything
        // So this check is probably completely unneeded and can never be true
    	if (firstDate.before(now) && !certProfile.getAllowValidityOverride()) {
			log.error(intres.getLocalizedMessage("signsession.errorbeforecurrentdate",firstDate,subject.getUsername()));
    		firstDate = now;
    		// Update valid length from the profile since the starting point has changed
			certProfileLastDate = ValidityDate.getDate(val,firstDate);
    		// Update lastDate if we use maximum validity
    		if (lastDate.equals(certProfileLastDate)) {
    			lastDate = certProfileLastDate;
    		}
    	}
		// Limit validity: We do not allow a certificate to be valid after the the validity of the certificate profile
    	if (lastDate.after(certProfileLastDate)) {
    		log.error(intres.getLocalizedMessage("signsession.errorbeyondmaxvalidity",lastDate,subject.getUsername(),certProfileLastDate));
    		lastDate = certProfileLastDate;
    	}
		// Limit validity: We do not allow a certificate to be valid after the the validity of the CA (unless it's RootCA during renewal)
        if (cacert != null && lastDate.after(CertTools.getNotAfter(cacert)) && !isRootCA) {
        	log.info(intres.getLocalizedMessage("signsession.limitingvalidity", lastDate.toString(), CertTools.getNotAfter(cacert)));
            lastDate = CertTools.getNotAfter(cacert);
        }            
        if ( !lastDate.before(CertificateValidity.tooLateExpireDate) ) {
        	String msg = intres.getLocalizedMessage("signsession.errorbeyondtoolateexpiredate", lastDate.toString(), CertificateValidity.tooLateExpireDate.toString()); 
        	log.info(msg);
            throw new IllegalValidityException(msg);
        }
	}

	public Date getNotAfter() {
		return lastDate;
	}

	public Date getNotBefore() {
		return firstDate;
	}
}
