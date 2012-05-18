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
package org.ejbca.core.protocol.ws.logger;

import java.util.regex.Pattern;

import org.apache.log4j.Logger;
import org.ejbca.config.ConfigurationHolder;
import org.ejbca.util.DummyPatternLogger;
import org.ejbca.util.GUIDGenerator;
import org.ejbca.util.IPatternLogger;
import org.ejbca.util.PatternLogger;

public class TransactionLogger {

    
    private static final TransactionLogger instance = new TransactionLogger();

    /** regexp pattern to match ${identifier} patterns */// ${DN};${IP}
    // private final static Pattern PATTERN = Pattern.compile("\\$\\{(.+?)\\}"); // TODO this should be configurable from file
    final private Pattern PATTERN;// = Pattern.compile("\\$\\{(.+?)\\}");// TODO this should be configurable from file

    //  = "${LOG_ID};${STATUS};\"${CLIENT_IP}\";\"${SIGN_ISSUER_NAME_DN}\";\"${SIGN_SUBJECT_NAME}\";${SIGN_SERIAL_NO};" +
    //      "\"${LOG_TIME}\";${NUM_CERT_ID};0;0;0;0;0;0;0;" +
    //      "\"${ISSUER_NAME_DN}\";${ISSUER_NAME_HASH};${ISSUER_KEY};${DIGEST_ALGOR};${SERIAL_NOHEX};${CERT_STATUS}";
    final private String orderString;
    final private String logDateFormat; 
    final private String timeZone;
    final private boolean doLog;
    final private String sessionID = GUIDGenerator.generateGUID(this);
    final private Logger log = Logger.getLogger(TransactionLogger.class.getName());
    private int transaktionID = 0;

    private IPatternLogger getNewPatternLogger() {
        if ( !this.doLog ) {
            return new DummyPatternLogger();
        }
        IPatternLogger pl = new PatternLogger(this.PATTERN.matcher(this.orderString), this.orderString, this.log, this.logDateFormat, this.timeZone);
        pl.paramPut(TransactionTags.ERROR_MESSAGE.toString(), "NO_ERROR");
        pl.paramPut(TransactionTags.METHOD.toString(), new Throwable().getStackTrace()[2].getMethodName());
        pl.paramPut(IPatternLogger.LOG_ID, Integer.toString(this.transaktionID++));
        pl.paramPut(IPatternLogger.SESSION_ID, this.sessionID);
        return pl;
    }

    /**
     * 
     */
    private TransactionLogger() {
        this.PATTERN = Pattern.compile(ConfigurationHolder.getString("ejbcaws.trx-log-pattern", "\\$\\{(.+?)\\}"));
        this.orderString = ConfigurationHolder.getString("ejbcaws.trx-log-order", "${LOG_TIME};${SESSION_ID};${LOG_ID};${REPLY_TIME};"+
                                                         TransactionTags.METHOD.getTag()+";"+TransactionTags.ERROR_MESSAGE.getTag())+";"+
                                                         TransactionTags.ADMIN_DN.getTag()+";"+TransactionTags.ADMIN_ISSUER_DN.getTag();
        this.logDateFormat = ConfigurationHolder.getString("ejbcaws.log-date", "yyyy/MM/dd HH:mm:ss.SSS");
        this.timeZone = ConfigurationHolder.getString("ejbcaws.log-timezone", "GMT");
        final String value = ConfigurationHolder.getString("ejbcaws.trx-log", null);
        this.doLog = value!=null && value.toLowerCase().indexOf("false")<0 && value.toLowerCase().indexOf("no")<0;
    }
    /**
     * @return allways same
     */
    static public IPatternLogger getPatternLogger() {
        return instance.getNewPatternLogger();
    }
}
