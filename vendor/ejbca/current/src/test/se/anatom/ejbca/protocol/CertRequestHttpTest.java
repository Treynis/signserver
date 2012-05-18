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

package se.anatom.ejbca.protocol;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.KeyStore;
import java.security.PublicKey;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.Collection;
import java.util.Iterator;

import javax.ejb.DuplicateKeyException;
import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;
import junit.framework.TestSuite;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionHome;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionRemote;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.util.CertTools;

/** Tests http servlet for certificate request
 * 
 * @version $Id: CertRequestHttpTest.java 6709 2008-12-08 15:00:18Z anatom $
 */
public class CertRequestHttpTest extends TestCase {
    private static Logger log = Logger.getLogger(CertRequestHttpTest.class);

    protected final String httpReqPath;
    protected final String resourceReq;

    private static Context ctx;
    protected ICertificateStoreSessionHome storehome;
    private static IUserAdminSessionRemote usersession;
    protected static int caid = 0;
    protected static Admin admin;
    protected static X509Certificate cacert = null;

    public static void main(String args[]) {
        junit.textui.TestRunner.run(suite());
    }


    public static TestSuite suite() {
        return new TestSuite(CertRequestHttpTest.class);
    }


    public CertRequestHttpTest(String name) throws Exception {
        this(name,"http://127.0.0.1:8080/ejbca", "certreq");
    }

    protected  CertRequestHttpTest(String name, String reqP, String res) throws Exception {
        super(name);
        httpReqPath = reqP;
        resourceReq = res;
        admin = new Admin(Admin.TYPE_BATCHCOMMANDLINE_USER);

        // Install BouncyCastle provider
        CertTools.installBCProvider();

        ctx = getInitialContext();
        Object obj = ctx.lookup(ICAAdminSessionHome.JNDI_NAME);
        ICAAdminSessionHome cahome = (ICAAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj, ICAAdminSessionHome.class);
        ICAAdminSessionRemote casession = cahome.create();
        setCAID(casession);
        CAInfo cainfo = casession.getCAInfo(admin, caid);
        Collection certs = cainfo.getCertificateChain();
        if (certs.size() > 0) {
            Iterator certiter = certs.iterator();
            cacert = (X509Certificate) certiter.next();
        } else {
            log.error("NO CACERT for caid " + caid);
        }
        obj = ctx.lookup(IUserAdminSessionHome.JNDI_NAME);
        IUserAdminSessionHome userhome = (IUserAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj, IUserAdminSessionHome.class);
        usersession = userhome.create();

    }

    protected void setCAID(ICAAdminSessionRemote casession) throws RemoteException {
        Collection caids = casession.getAvailableCAs(admin);
        Iterator iter = caids.iterator();        
        if (!iter.hasNext()) {
        	assertTrue("No active CA! Must have at least one active CA to run tests!", false);
        }
        while (iter.hasNext()) {
            caid = ((Integer) iter.next()).intValue();
            CAInfo cainfo = casession.getCAInfo(admin, caid);
            if (cainfo.getCAType() == CAInfo.CATYPE_X509) {
                if (cainfo.getStatus() == SecConst.CA_ACTIVE) {
                	break;
                }            	
            }
        } 
    }
    protected void setUp() throws Exception {
        log.trace(">setUp()");
        log.trace("<setUp()");
    }

    protected void tearDown() throws Exception {
    }

    private Context getInitialContext() throws NamingException {
        log.trace(">getInitialContext");
        Context ctx = new javax.naming.InitialContext();
        log.trace("<getInitialContext");
        return ctx;
    }



    /** Tests request for a pkcs12
     * @throws Exception error
     */
    public void test01RequestPKCS12() throws Exception {
        log.trace(">test01RequestPKCS12()");

        // find a CA (TestCA?) create a user
        // Send certificate request for a server generated PKCS12
        setupUser();
        
        // POST the OCSP request
        URL url = new URL(httpReqPath + '/' + resourceReq);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        // we are going to do a POST
        con.setDoOutput(true);
        con.setRequestMethod("POST");

        // POST it
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = con.getOutputStream();
        os.write("user=reqtest&password=foo123&keylength=2048".getBytes("UTF-8"));
        os.close();
        assertEquals("Response code", 200, con.getResponseCode());
        // Some appserver (Weblogic) responds with "application/x-pkcs12; charset=UTF-8"
        assertTrue(con.getContentType().startsWith("application/x-pkcs12"));
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This works for small requests, and PKCS12 requests are small
        InputStream in = con.getInputStream();
        int b = in.read();
        while (b != -1) {
            baos.write(b);
            b = in.read();
        }
        baos.flush();
        in.close();
        byte[] respBytes = baos.toByteArray();
        assertTrue(respBytes.length > 0);
        
        KeyStore store = KeyStore.getInstance("PKCS12", "BC");
        ByteArrayInputStream is = new ByteArrayInputStream(respBytes); 
        store.load(is, "foo123".toCharArray());
        assertTrue(store.containsAlias("ReqTest"));
        X509Certificate cert = (X509Certificate)store.getCertificate("ReqTest");
        PublicKey pk = cert.getPublicKey();
        if (pk instanceof RSAPublicKey) {
        	RSAPublicKey rsapk = (RSAPublicKey) pk;
			assertEquals(rsapk.getAlgorithm(), "RSA");
			assertEquals(2048, rsapk.getModulus().bitLength());
		} else {
			assertTrue("Public key is not RSA", false);
		}
        
        log.trace("<test01RequestPKCS12()");
    }

    /** Tests request for a unknown user
     * @throws Exception error
     */
    public void test02RequestUnknownUser() throws Exception {
        log.trace(">test02RequestUnknownUser()");

        // POST the OCSP request
        URL url = new URL(httpReqPath + '/' + resourceReq);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        // we are going to do a POST
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setInstanceFollowRedirects(false);
        con.setAllowUserInteraction(false);

        // POST it
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = con.getOutputStream();
        os.write("user=reqtestunknown&password=foo123&keylength=2048".getBytes("UTF-8"));
        os.close();
        assertEquals("Response code", 200, con.getResponseCode());
        System.out.println("Content-Type: "+con.getContentType());
        boolean ok = false;
        // Some containers return the content type with a space and some without...
        if ("text/html;charset=UTF-8".equals(con.getContentType())) ok = true;
        if ("text/html; charset=UTF-8".equals(con.getContentType())) ok = true;
        assertTrue(con.getContentType(), ok);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This works for small requests, and PKCS12 requests are small
        InputStream in = con.getInputStream();
        int b = in.read();
        while (b != -1) {
            baos.write(b);
            b = in.read();
        }
        baos.flush();
        in.close();
        byte[] respBytes = baos.toByteArray();
        String error = new String(respBytes);
        int index = error.indexOf("<pre>");
        int index2 = error.indexOf("</pre>");
        String errormsg = error.substring(index+5, index2);
        System.out.println(errormsg);
        assertEquals("Username: reqtestunknown\nNon existent username. To generate a certificate a valid username and password must be supplied.\n", errormsg);
        log.trace("<test02RequestUnknownUser()");
    }

    /** Tests request for a wrong password
     * @throws Exception error
     */
    public void test03RequestWrongPwd() throws Exception {
        log.trace(">test03RequestWrongPwd()");

        setupUser();
        
        // POST the OCSP request
        URL url = new URL(httpReqPath + '/' + resourceReq);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        // we are going to do a POST
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setInstanceFollowRedirects(false);
        con.setAllowUserInteraction(false);

        // POST it
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = con.getOutputStream();
        os.write("user=reqtest&password=foo456&keylength=2048".getBytes("UTF-8"));
        os.close();
        assertEquals("Response code", 200, con.getResponseCode());
        boolean ok = false;
        // Some containers return the content type with a space and some without...
        if ("text/html;charset=UTF-8".equals(con.getContentType())) ok = true;
        if ("text/html; charset=UTF-8".equals(con.getContentType())) ok = true;
        assertTrue(con.getContentType(), ok);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This works for small requests, and PKCS12 requests are small
        InputStream in = con.getInputStream();
        int b = in.read();
        while (b != -1) {
            baos.write(b);
            b = in.read();
        }
        baos.flush();
        in.close();
        byte[] respBytes = baos.toByteArray();
        String error = new String(respBytes);
        int index = error.indexOf("<pre>");
        int index2 = error.indexOf("</pre>");
        String errormsg = error.substring(index+5, index2);
        assertEquals("Username: reqtest\nWrong username or password! To generate a certificate a valid username and password must be supplied.\n", errormsg);
        System.out.println(errormsg);
        log.trace("<test03RequestWrongPwd()");
    }

    /** Tests request with wrong status
     * @throws Exception error
     */
    public void test04RequestWrongStatus() throws Exception {
        log.trace(">test04RequestWrongStatus()");

        setupUser();
        setupUserStatus(UserDataConstants.STATUS_GENERATED);
        
        // POST the OCSP request
        URL url = new URL(httpReqPath + '/' + resourceReq);
        HttpURLConnection con = (HttpURLConnection)url.openConnection();
        // we are going to do a POST
        con.setDoOutput(true);
        con.setRequestMethod("POST");
        con.setInstanceFollowRedirects(false);
        con.setAllowUserInteraction(false);

        // POST it
        con.setRequestProperty("Content-Type", "application/x-www-form-urlencoded");
        OutputStream os = con.getOutputStream();
        os.write("user=reqtest&password=foo456&keylength=2048".getBytes("UTF-8"));
        os.close();
        assertEquals("Response code", 200, con.getResponseCode());
        boolean ok = false;
        // Some containers return the content type with a space and some without...
        if ("text/html;charset=UTF-8".equals(con.getContentType())) ok = true;
        if ("text/html; charset=UTF-8".equals(con.getContentType())) ok = true;
        assertTrue(con.getContentType(), ok);
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        // This works for small requests, and PKCS12 requests are small
        InputStream in = con.getInputStream();
        int b = in.read();
        while (b != -1) {
            baos.write(b);
            b = in.read();
        }
        baos.flush();
        in.close();
        byte[] respBytes = baos.toByteArray();
        String error = new String(respBytes);
        int index = error.indexOf("<pre>");
        int index2 = error.indexOf("</pre>");
        String errormsg = error.substring(index+5, index2);
        assertEquals("Username: reqtest\nWrong user status! To generate a certificate for a user the user must have status New, Failed or In process.\n", errormsg);
        System.out.println(errormsg);
        log.trace("<test04RequestWrongStatus()");
    }

    /**
     * removes test user
     *
     * @throws Exception error
     */
    public void test99Cleanup() throws Exception {
        log.trace(">test99Cleanup()");
        usersession.deleteUser(admin, "reqtest");
        log.trace("<test99Cleanup()");
    }

    //
    // Private helper methods
    //

    private void setupUser() throws Exception {
        // Make user that we know...
        boolean userExists = false;
        try {
            usersession.addUser(admin,"reqtest","foo123","C=SE,O=PrimeKey,CN=ReqTest",null,"reqtest@primekey.se",false,SecConst.EMPTY_ENDENTITYPROFILE,SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.USER_ENDUSER,SecConst.TOKEN_SOFT_P12,0,caid);
            log.debug("created user: reqtest, foo123, C=SE, O=PrimeKey, CN=ReqTest");
        } catch (RemoteException re) {
        	userExists = true;
        } catch (DuplicateKeyException dke) {
            userExists = true;
        }

        if (userExists) {
            log.debug("User reqtest already exists.");
            usersession.changeUser(admin, "reqtest", "foo123", "C=SE,O=PrimeKey,CN=ReqTest",null,"reqtest@anatom.se",false, SecConst.EMPTY_ENDENTITYPROFILE,SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.USER_ENDUSER,SecConst.TOKEN_SOFT_P12,0,UserDataConstants.STATUS_NEW, caid);
            log.debug("Reset status to NEW");
        }
    }

    private void setupUserStatus(int status) throws Exception {
            usersession.changeUser(admin, "reqtest", "foo123", "C=SE,O=PrimeKey,CN=ReqTest",null,"reqtest@anatom.se",false, SecConst.EMPTY_ENDENTITYPROFILE,SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.USER_ENDUSER,SecConst.TOKEN_SOFT_P12,0,status, caid);
            log.debug("Set status to: "+status);
    }

}
