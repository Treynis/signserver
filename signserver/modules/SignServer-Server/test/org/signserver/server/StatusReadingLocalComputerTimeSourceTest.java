package org.signserver.server;

import java.util.Calendar;
import java.util.Date;
import java.util.Map;

import org.signserver.statusrepo.IStatusRepositorySession;
import org.signserver.statusrepo.common.NoSuchPropertyException;
import org.signserver.statusrepo.common.StatusEntry;
import org.signserver.statusrepo.common.StatusName;

import junit.framework.TestCase;

/**
 * Tests the leapsecond support in status-reading local timesource.
 * 
 * @author Marcus Lundblad
 * @version $Id$
 *
 */

public class StatusReadingLocalComputerTimeSourceTest extends TestCase {

    
    private void assertPotentialLeapsecond(int year, int month, int day, int hour, int min, int sec) {
        Date date;
        Calendar cal = Calendar.getInstance();
        
        cal.set(year, month - 1, day, hour, min, sec);
        date = cal.getTime();
        
        assertTrue("Should detect possible leapsecond: " + date.toString(), StatusReadingLocalComputerTimeSource.isPotentialLeapsecond(date));
    }
    
    private void assertNotPotentialLeapsecond(int year, int month, int day, int hour, int min, int sec) {
        Date date;
        Calendar cal = Calendar.getInstance();
        
        cal.set(year, month - 1, day, hour, min, sec);
        date = cal.getTime();
        
        assertFalse("Should not detect possible leapsecond: " + date.toString(), StatusReadingLocalComputerTimeSource.isPotentialLeapsecond(date));
    }
    
    /**
     * Test that the last second of December is detected as a potential leapsecond event.
     * 
     * @throws Exception
     */
    public void test01PotentialLeapSecondDecember() throws Exception {
        assertPotentialLeapsecond(2012, 12, 31, 23, 59, 59);
    }
    
    /**
     * Test that the last second of June is detected as a potential leapsecond event.
     * 
     * @throws Exception
     */
    public void test02PotentialLeapSecondJune() throws Exception {
        assertPotentialLeapsecond(2013, 6, 30, 23, 59, 59);
    }
    
    /**
     * Test that the first second of January is detected as a potential leapsecond event.
     * (an interval around the actual second should be detected).
     * 
     * @throws Exception
     */
    public void test03PotentialLeapSecondJanuary() throws Exception {
        assertPotentialLeapsecond(2013, 1, 1, 0, 0, 0);
    }
    
    /**
     * Test that another arbitrary date is not detected as a potential leapsecond event.
     * 
     * @throws Exception
     */
    public void test04NotPotentialLeapSecondOther() throws Exception {
        assertNotPotentialLeapsecond(2013, 4, 7, 12, 47, 11);
    }
    
    
    /** Test that requesting time when a leapsecond is near,
     * the time is returned right after.
     */
    public void test05RequestTimeBeforeLeapsecond() throws Exception {
        final StatusReadingLocalComputerTimeSource timeSource =
                new StatusReadingLocalComputerTimeSource() {
            @Override
            protected Date getCurrentDate() {
                // return fake date triggering a leap second
                Calendar cal = Calendar.getInstance();
                cal.set(2012, 11, 31, 23, 59, 59);
                
                return cal.getTime();
            }
        };
        
        timeSource.setHandleLeapsecondChange(true);
        timeSource.setStatusSession(new PositiveLeapsecondStatusRepositorySession());
        
        long startTime = new Date().getTime();
        final Date date = timeSource.getGenTime();
        long finishTime = new Date().getTime();
        
        // the call should take at least 4 s
        long elapsed = finishTime - startTime;
        assertTrue("Timesource did not wait long enough: " + elapsed, elapsed > 4000);
    }
    
    
    /**
     * Base class for status repository mockups
     */
    private abstract class LeapsecondStatusRepositorySession implements IStatusRepositorySession {
        @Override
        public void update(String key, String value)
                throws NoSuchPropertyException {
            throw new UnsupportedOperationException("Not implemented");
            
        }

        @Override
        public void update(String key, String value, long expiration)
                throws NoSuchPropertyException {
            throw new UnsupportedOperationException("Not implemented");
            
        }

        @Override
        public Map<String, StatusEntry> getAllEntries() {
            throw new UnsupportedOperationException("Not implemented");
        }

    }
    
    /**
     * Mock status session returning a positive leapsecond value
     * when queried for the leapsecond status property.
     */
    private class PositiveLeapsecondStatusRepositorySession extends LeapsecondStatusRepositorySession {

        @Override
        public StatusEntry getValidEntry(String key)
                throws NoSuchPropertyException {
            long time = new Date().getTime();
            if (StatusName.LEAPSECOND.name().equals(key)) {
                // return a status entry valid for an hour, we won't actually expire it, but for good measure...
                return new StatusEntry(time, StatusReadingLocalComputerTimeSource.LEAPSECOND_POSITIVE, time + 3600 * 1000);
            } else if (StatusName.TIMESOURCE0_INSYNC.name().equals(key)) {
                return new StatusEntry(time, Boolean.TRUE.toString(), time + 3600 * 1000);
            }
            return null;
        }
    }
    
    /**
     * Mock status session returning a negative leapsecond value
     * when queried for the leapsecond status property.
     */
    private class NegativeLeapsecondStatusRepositorySession extends LeapsecondStatusRepositorySession {

        @Override
        public StatusEntry getValidEntry(String key)
                throws NoSuchPropertyException {
            long time = new Date().getTime();

            if (StatusName.LEAPSECOND.name().equals(key)) {
                // return a status entry valid for an hour, we won't actually expire it, but for good measure...
                return new StatusEntry(time, StatusReadingLocalComputerTimeSource.LEAPSECOND_NEGATIVE, time + 3600 * 1000);
            } else if (StatusName.TIMESOURCE0_INSYNC.name().equals(key)) {
                return new StatusEntry(time, Boolean.TRUE.toString(), time + 3600 * 1000);
            }
            return null;
        }
    }
}
