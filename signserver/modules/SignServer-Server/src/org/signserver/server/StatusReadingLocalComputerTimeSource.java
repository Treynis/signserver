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
package org.signserver.server;

import java.util.Calendar;
import java.util.Date;
import java.util.Properties;
import javax.ejb.EJB;
import org.apache.log4j.Logger;
import org.signserver.common.ServiceLocator;
import org.signserver.server.ITimeSource;
import org.signserver.statusrepo.common.StatusEntry;
import org.signserver.statusrepo.common.NoSuchPropertyException;
import org.signserver.statusrepo.IStatusRepositorySession;
import org.signserver.statusrepo.common.StatusName;

/**
 * ITimeSource taking the current time from the computer clock as long as the 
 * status property TIMESOURCE0_INSYNC is true and has not expired.
 *
 * It reads a status property TIMESOURCE0_INSYNC from the status repository.
 * It has no defined worker properties.
 *
 * $Id$
 */
public class StatusReadingLocalComputerTimeSource implements ITimeSource {

    /** Logger for this class. */
    private static final Logger LOG = Logger.getLogger(
            StatusReadingLocalComputerTimeSource.class);

    /** Status repository session. */
    @EJB
    private IStatusRepositorySession.IRemote statusSession;

    private StatusName insyncPropertyName = StatusName.TIMESOURCE0_INSYNC;
    private StatusName leapsecondPropertyName = StatusName.LEAPSECOND;

    // property constants
    private static final String HANDLE_LEAPSECOND_CHANGE = "HANDLE_LEAPSECOND_CHANGE";
    
    private boolean handleLeapsecondChange;
    
    /**
     * @param props Properties for this TimeSource (not used)
     * @see org.signserver.server.ITimeSource#init(java.util.Properties)
     */
    @Override
    public void init(final Properties props) {
        try {
            statusSession = ServiceLocator.getInstance().lookupRemote(
                        IStatusRepositorySession.IRemote.class);
            handleLeapsecondChange =
                    Boolean.parseBoolean(props.getProperty(HANDLE_LEAPSECOND_CHANGE, Boolean.FALSE.toString()));
        } catch (Exception ex) {
            LOG.error("Looking up status repository session", ex);
        }
    }

    /**
     * Main method that should retrieve the current time from the device.
     * @return an accurate current time or null if it is not available.
     */
    @Override
    public Date getGenTime() {
        try {
            final Date date;
            final StatusEntry entry = statusSession.getValidEntry(insyncPropertyName.name());
            if (entry != null && Boolean.valueOf(entry.getValue())) {
                date = new Date();
                
                // check if a leapsecond is near
                if (handleLeapsecondChange && isPotentialLeapsecond(date)) {
                    final StatusEntry leapsecond = statusSession.getValidEntry(leapsecondPropertyName.name());
                    
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("Check for leapsecond");
                    }
                    
                    if (leapsecond == null) {
                        // leapsecond property is expired
                        return null;
                    }
                }
                
            } else {
                date = null;
            }
            return date;
        } catch (NoSuchPropertyException ex) {
            throw new RuntimeException(ex);
        }
    }
    
    /**
     * Returns true if passed in date is near a potential leapsecond
     * @param date
     * @return true if possible leapsecond
     */
    protected static boolean isPotentialLeapsecond(final Date date) {
        final Calendar cal = Calendar.getInstance();
        
        cal.setTime(date);
        
        final int month = cal.get(Calendar.MONTH);
        final int day = cal.get(Calendar.DAY_OF_MONTH);
        final int hour = cal.get(Calendar.HOUR_OF_DAY);
        final int min = cal.get(Calendar.MINUTE);
        final int sec = cal.get(Calendar.SECOND);
        
        // check for the last two seconds of a potential
        // leapsecond month-shift
        if ((month == Calendar.JUNE && day == 30) ||
            (month == Calendar.DECEMBER && day == 31)) {
            
            
            if (hour == 23 && min == 59 && sec >= 58) {
                return true;
            }
        }
        
        // check for the first two seconds following
        // a potential leapsecond month-shift
        if ((month == Calendar.JANUARY || month == Calendar.JULY) &&
            day == 1) {
            if (hour == 0 && min == 0 && sec <= 1) {
                return true;
            }
        }
        
        return false;
    }

}
