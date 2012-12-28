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
package org.signserver.test.performance.impl;

import java.io.*;
import java.util.Date;
import java.util.Random;
import org.apache.log4j.Logger;
import org.signserver.test.performance.FailedException;
import org.signserver.test.performance.FailureCallback;
import org.signserver.test.performance.WorkerThread;

/**
 * 
 * @author Marcus Lundblad
 * @version $Id$
 */
public class TimeStampThread extends WorkerThread {
    /** Logger for this class */
    Logger LOG = Logger.getLogger(TimeStampThread.class);

    private int maxWaitTime;
    private TimeStamp tsa;
    private Random random;
    private long startTime;
    private long warmupTime;
    private final File statFile;
    
    public TimeStampThread(final String name, final FailureCallback failureCallback, final String url, int maxWaitTime,
    		int seed, long warmupTime, final File statFile) {
        super(name, failureCallback);
        this.maxWaitTime = maxWaitTime;
        this.random = new Random(seed);
        this.tsa = new TimeStamp(url, random);
        this.warmupTime = warmupTime;
        this.statFile = statFile;
    }

    @Override
    public void run() {
        startTime = (new Date()).getTime();
        
        LOG.info("   Thread " + getName() + ": Started");
        
        BufferedWriter out = null;
        try {
            if (statFile != null) {
                out = new BufferedWriter(new FileWriter(statFile));
            }
            while (!isStop()) {
            	long currentTime = (new Date().getTime());
                long estimatedTime;
                
                try {
                    estimatedTime = tsa.run();
                } catch (FailedException ex) {
                    fireFailure("THREAD " + getName() + " failed after " + getOperationsPerformed() + " signings: " + ex.getMessage());
                    break;
                }
              
                if (currentTime > startTime + warmupTime) {
                    addResponseTime(estimatedTime);
                    if (out != null) {
                        out.write((System.currentTimeMillis() /*- startTime*/) + ";" + estimatedTime);
                        out.newLine();
                    }
                    increaseOperationsPerformed();
                }
                
                // Sleep
                Thread.sleep((int) (random.nextDouble() * maxWaitTime));
            }
        } catch (IOException ex) {
            if (LOG.isDebugEnabled()) {
                LOG.debug("File could not be written", ex);
            }
            LOG.error("File could not be written: " + ex.getMessage());
        } catch (InterruptedException ex) {
            LOG.error("Interrupted: " + ex.getMessage());
        } finally {
            if (out != null) {
                try {
                    out.close();
                } catch (IOException ex) {
                    if (LOG.isDebugEnabled()) {
                        LOG.debug("File could not be closed", ex);
                    }
                    LOG.error("File could not be closed: " + ex.getMessage());
                }
            }
        }
    }
    
}
