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

package org.ejbca.core.model.services;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Properties;
import java.util.Random;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.store.CertificateDataBean;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.store.CertificateInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.services.actions.NoAction;
import org.ejbca.core.model.services.intervals.PeriodicalInterval;
import org.ejbca.core.model.services.workers.CertificateExpirationNotifierWorker;
import org.ejbca.core.model.services.workers.EmailSendingWorker;
import org.ejbca.util.CertTools;
import org.ejbca.util.TestTools;
import org.ejbca.util.keystore.KeyTools;

/** Tests the certificate expiration notifications.
 *
 * @version $Id: TestCertificateExpire.java 8380 2009-11-30 15:28:15Z anatom $
 */
public class TestCertificateExpire extends TestCase {

    private static final Logger log = Logger.getLogger(TestCertificateExpire.class);
    private static final Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
    private static final String CA_NAME = "CertExpNotifCA";
    private static final int caid = TestTools.getTestCAId(CA_NAME);

    private static String username;
    private static String pwd;
    
    private static final String CERTIFICATE_EXPIRATION_SERVICE = "CertificateExpirationService";

    /**
     * Creates a new TestUserPasswordExpire object.
     *
     * @param name DOCUMENT ME!
     */
    public TestCertificateExpire(String name) {
        super(name);
        CertTools.installBCProvider();	// Install BouncyCastle provider
        assertTrue("Could not create TestCA.", TestTools.createTestCA(CA_NAME));
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    private String genRandomUserName() throws Exception {
        // Gen random user
        Random rand = new Random(new Date().getTime() + 4711);
        String username = "";
        for (int i = 0; i < 6; i++) {
            int randint = rand.nextInt(9);
            username += (new Integer(randint)).toString();
        }
        log.debug("Generated random username: username =" + username);
        return username;
    } // genRandomUserName

    private String genRandomPwd() throws Exception {
        // Gen random pwd
        Random rand = new Random(new Date().getTime() + 4812);
        String password = "";

        for (int i = 0; i < 8; i++) {
            int randint = rand.nextInt(9);
            password += (new Integer(randint)).toString();
        }
        log.debug("Generated random pwd: password=" + password);
        return password;
    } // genRandomPwd


    /** Add a new user and an expire service. Test that the service expires the users password
     *
     */
    public void test01ExpireCertificate() throws Exception {
        log.trace(">test01CreateNewUser()");
        
        // Create a new user
        username = genRandomUserName();
        pwd = genRandomPwd();
        TestTools.getUserAdminSession().addUser(admin,username,pwd,"C=SE,O=AnaTom,CN="+username,null,null,false,SecConst.EMPTY_ENDENTITYPROFILE,SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.USER_ENDUSER,SecConst.TOKEN_SOFT_PEM,0,caid);
        log.debug("created user: "+username);
        
        KeyPair keys = KeyTools.genKeys("1024", "RSA");
	    X509Certificate cert = (X509Certificate) TestTools.getSignSession().createCertificate(admin, username, pwd, keys.getPublic());
	    assertNotNull("Failed to create certificate", cert);
	    ICertificateStoreSessionRemote certificateStoreSession = TestTools.getCertificateStoreSession();
        String fp = CertTools.getFingerprintAsString(cert);
        X509Certificate ce = (X509Certificate) certificateStoreSession.findCertificateByFingerprint(admin,fp);
        assertNotNull("Cannot find certificate with fp="+fp,ce);
        CertificateInfo info = certificateStoreSession.getCertificateInfo(admin, fp);
        //log.info("Got certificate info for cert with fp="+fp);
        assertEquals("fingerprint does not match.",fp,info.getFingerprint());
        assertEquals("serialnumber does not match.",cert.getSerialNumber(),info.getSerialNumber());
        assertEquals("issuerdn does not match.",CertTools.getIssuerDN(cert),info.getIssuerDN());
        assertEquals("subjectdn does not match.",CertTools.getSubjectDN(cert),info.getSubjectDN());
        // The cert was just stored above with status INACTIVE
        assertEquals("status does not match.",CertificateDataBean.CERT_ACTIVE,info.getStatus());   
        long seconds = (cert.getNotAfter().getTime() - new Date ().getTime())/1000l;
        log.debug("ceritificate OK in store, expires in " + seconds + " seconds");
	    
        // Create a new UserPasswordExpireService
		ServiceConfiguration config = new ServiceConfiguration();
		config.setActive(true);
		config.setDescription("This is a description");
		// No mailsending for this Junit test service
		config.setActionClassPath(NoAction.class.getName());
		config.setActionProperties(null); 
		config.setIntervalClassPath(PeriodicalInterval.class.getName());
		Properties intervalprop = new Properties();
		// Run the service every 3:rd second
		intervalprop.setProperty(PeriodicalInterval.PROP_VALUE, "3");
		intervalprop.setProperty(PeriodicalInterval.PROP_UNIT, PeriodicalInterval.UNIT_SECONDS);
		config.setIntervalProperties(intervalprop);
		config.setWorkerClassPath(CertificateExpirationNotifierWorker.class.getName());
		Properties workerprop = new Properties();
		workerprop.setProperty(EmailSendingWorker.PROP_SENDTOADMINS, "FALSE");
		workerprop.setProperty(EmailSendingWorker.PROP_SENDTOENDUSERS, "FALSE");
		workerprop.setProperty(BaseWorker.PROP_CAIDSTOCHECK, String.valueOf(caid));
		workerprop.setProperty(BaseWorker.PROP_TIMEBEFOREEXPIRING, String.valueOf(seconds - 10));
		workerprop.setProperty(BaseWorker.PROP_TIMEUNIT, BaseWorker.UNIT_SECONDS);
		config.setWorkerProperties(workerprop);
		
		TestTools.getServiceSession().addService(admin, CERTIFICATE_EXPIRATION_SERVICE, config);
        TestTools.getServiceSession().activateServiceTimer(admin, CERTIFICATE_EXPIRATION_SERVICE);
        
        // The service will run...
        Thread.sleep(5000);
        info = certificateStoreSession.getCertificateInfo(admin, fp);
        assertEquals("status does not match.",CertificateDataBean.CERT_ACTIVE,info.getStatus());   
       
        // The service will run...since there is a random delay of 30 seconds we have to wait a long time
        Thread.sleep(35000);
        info = certificateStoreSession.getCertificateInfo(admin, fp);
        assertEquals("status does not match.",CertificateDataBean.CERT_NOTIFIEDABOUTEXPIRATION,info.getStatus());   
        
     
        log.trace("<test01CreateNewUser()");
    }


    /**
     * Remove all data stored by JUnit tests
     *
     */
    public void test99CleanUp() throws Exception {
        log.trace(">test99CleanUp()");
        TestTools.getUserAdminSession().deleteUser(admin,username);
        log.debug("Removed user: "+username);
        TestTools.getServiceSession().removeService(admin, CERTIFICATE_EXPIRATION_SERVICE);
        log.debug("Removed service:" + CERTIFICATE_EXPIRATION_SERVICE);
        TestTools.removeTestCA(CA_NAME);
        log.debug("Removed test CA");
        log.trace("<test99CleanUp()");
    }
}
