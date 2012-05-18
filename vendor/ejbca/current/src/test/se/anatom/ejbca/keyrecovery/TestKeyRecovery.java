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

package se.anatom.ejbca.keyrecovery;

import java.security.KeyPair;
import java.security.cert.X509Certificate;
import java.util.Arrays;
import java.util.Date;
import java.util.Random;

import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.sign.ISignSessionHome;
import org.ejbca.core.ejb.ca.sign.ISignSessionRemote;
import org.ejbca.core.ejb.ra.IUserAdminSessionHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.catoken.CATokenConstants;
import org.ejbca.core.model.keyrecovery.KeyRecoveryData;
import org.ejbca.core.model.log.Admin;
import org.ejbca.util.CertTools;
import org.ejbca.util.TestTools;
import org.ejbca.util.keystore.KeyTools;

/**
 * Tests the key recovery modules.
 *
 * @version $Id: TestKeyRecovery.java 6668 2008-11-28 16:28:44Z jeklund $
 */
public class TestKeyRecovery extends TestCase {
    private final static Logger log = Logger.getLogger(TestKeyRecovery.class);
    private final static Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
    private static final String user = genRandomUserName();

    private static KeyPair keypair = null;
    private static X509Certificate cert = null;

    /**
     * Creates a new TestLog object.
     *
     * @param name name
     */
    public TestKeyRecovery(String name) {
        super(name);
    }

    protected void setUp() throws Exception {
        log.trace(">setUp()");
        CertTools.installBCProvider();
        assertTrue("Could not create TestCA.", TestTools.createTestCA());
        log.trace("<setUp()");
    }

    protected void tearDown() throws Exception {
    }

    private Context getInitialContext() throws NamingException {
        //log.trace(">getInitialContext");
        Context ctx = new javax.naming.InitialContext();
        //log.trace("<getInitialContext");
        return ctx;
    }


    /**
     * tests adding a keypair and checks if it can be read again.
     *
     * @throws Exception error
     */
    public void test01AddKeyPair() throws Exception {
        log.trace(">test01AddKeyPair()");
        // Generate test keypair and certificate.
        try {

            ISignSessionHome home = (ISignSessionHome) javax.rmi.PortableRemoteObject.narrow(getInitialContext().lookup("RSASignSession"), ISignSessionHome.class);
            ISignSessionRemote ss = home.create();

            Object obj = getInitialContext().lookup("UserAdminSession");
            IUserAdminSessionHome userhome = (IUserAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj, IUserAdminSessionHome.class);
            IUserAdminSessionRemote usersession = userhome.create();

            String email = "test@test.se";
            if (!usersession.existsUser(admin, user)) {
                keypair = KeyTools.genKeys("512", CATokenConstants.KEYALGORITHM_RSA);
                usersession.addUser(admin, user, "foo123", "CN=TESTKEYREC", "rfc822name=" + email, email, false, SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER, SecConst.USER_ENDUSER, SecConst.TOKEN_SOFT_P12, 0, TestTools.getTestCAId());
                cert = (X509Certificate) ss.createCertificate(admin, user, "foo123", keypair.getPublic());
            }
        } catch (Exception e) {
            log.error("Exception generating keys/cert: ", e);
            assertTrue("Exception generating keys/cert", false);            
        }
        TestTools.getKeyRecoverySession().addKeyRecoveryData(admin, cert, user, keypair);

        assertTrue("Couldn't save key's in database", TestTools.getKeyRecoverySession().existsKeys(admin, cert));

        log.trace("<test01AddKeyPair()");
    }

    /**
     * tests marks the keypair in database and recovers it.
     *
     * @throws Exception error
     */
    public void test02MarkAndRecoverKeyPair() throws Exception {
        log.trace(">test02MarkAndRecoverKeyPair()");
        CertTools.installBCProvider();
        assertTrue("Couldn't mark user for recovery in database", !TestTools.getKeyRecoverySession().isUserMarked(admin, user));
        TestTools.getKeyRecoverySession().markAsRecoverable(admin, cert,SecConst.EMPTY_ENDENTITYPROFILE);
        assertTrue("Couldn't mark user for recovery in database", TestTools.getKeyRecoverySession().isUserMarked(admin, user));
        KeyRecoveryData data = TestTools.getKeyRecoverySession().keyRecovery(admin, user, SecConst.EMPTY_ENDENTITYPROFILE);

        assertTrue("Couldn't recover keys from database", Arrays.equals(data.getKeyPair().getPrivate().getEncoded(), keypair.getPrivate().getEncoded()));

        log.trace("<test02MarkAndRecoverKeyPair()");
    }

    /**
     * tests removes all keydata.
     *
     * @throws Exception error
     */
    public void test03RemoveKeyPair() throws Exception {
        log.trace(">test03RemoveKeyPair()");
        CertTools.installBCProvider();
        TestTools.getKeyRecoverySession().removeKeyRecoveryData(admin, cert);
        assertTrue("Couldn't remove keys from database", !TestTools.getKeyRecoverySession().existsKeys(admin, cert));

        log.trace("<test03RemoveKeyPair()");
    }

    private static String genRandomUserName() {
        // Gen random user
        Random rand = new Random(new Date().getTime() + 4711);
        String username = "";
        for (int i = 0; i < 6; i++) {
            int randint = rand.nextInt(9);
            username += (new Integer(randint)).toString();
        }
        //log.debug("Generated random username: username =" + username);
        return username;
    } // genRandomUserName

	public void test99RemoveTestCA() throws Exception {
		TestTools.removeTestCA();
	}
}
