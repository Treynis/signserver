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

package se.anatom.ejbca.ca.store;

import java.math.BigInteger;
import java.security.cert.Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Vector;

import javax.naming.Context;
import javax.naming.NamingException;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ca.store.CertificateDataBean;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;

/**
 * @version $Id: TestCertificateRetrival.java 7463 2009-05-11 03:43:28Z jeklund $
 */
public class TestCertificateRetrival extends TestCase {

    static byte[] testrootcert = Base64.decode(("MIICnTCCAgagAwIBAgIBADANBgkqhkiG9w0BAQQFADBEMQswCQYDVQQGEwJTRTET"
            + "MBEGA1UECBMKU29tZS1TdGF0ZTEPMA0GA1UEChMGQW5hdG9tMQ8wDQYDVQQDEwZU"
            + "ZXN0Q0EwHhcNMDMwODIxMTcyMzAyWhcNMTMwNTIwMTcyMzAyWjBEMQswCQYDVQQG"
            + "EwJTRTETMBEGA1UECBMKU29tZS1TdGF0ZTEPMA0GA1UEChMGQW5hdG9tMQ8wDQYD"
            + "VQQDEwZUZXN0Q0EwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBAMoSn6W9BU6G"
            + "BLoasmAZ56uuOVV0pspyuPrPVtuNjEiJqwNr6S7Xa3+MoMq/bhogfml8YuU320o3"
            + "CWKB4n6kcRMiRZkhWtSL6HlO9MtE5Gq1NT1WrjkMefOYA501//U0LxLerPa8YLlD"
            + "CvT6GCY+B1KA8fo2GMditEfVL2uEJZpDAgMBAAGjgZ4wgZswHQYDVR0OBBYEFGU3"
            + "qE54h3lFUuQI+TGLRT798DhlMGwGA1UdIwRlMGOAFGU3qE54h3lFUuQI+TGLRT79"
            + "8DhloUikRjBEMQswCQYDVQQGEwJTRTETMBEGA1UECBMKU29tZS1TdGF0ZTEPMA0G"
            + "A1UEChMGQW5hdG9tMQ8wDQYDVQQDEwZUZXN0Q0GCAQAwDAYDVR0TBAUwAwEB/zAN"
            + "BgkqhkiG9w0BAQQFAAOBgQCn9g0SR06RTLFXN0zABYIVHe1+N1n3DcrOIrySg2h1"
            + "fIUV9fB9KsPp9zbLkoL2+UmnXsK8kCH0Tc7WaV0xXKrjtMxN6XIc431WS51QGW+B"
            + "X4XyXWbKwiJEadp6QZWCHhuXhYZnUNry3uVRWHj465P2OYlYH0rOtA2TVAl8ox5R"
            + "iQ==").getBytes());

    static byte[] testcacert = Base64.decode(("MIIB/zCCAWgCAQMwDQYJKoZIhvcNAQEEBQAwRDELMAkGA1UEBhMCU0UxEzARBgNV"
            + "BAgTClNvbWUtU3RhdGUxDzANBgNVBAoTBkFuYXRvbTEPMA0GA1UEAxMGVGVzdENB"
            + "MB4XDTAzMDkyMjA5MTExNVoXDTEzMDQyMjA5MTExNVowTDELMAkGA1UEBhMCU0Ux"
            + "EzARBgNVBAgTClNvbWUtU3RhdGUxDzANBgNVBAoTBkFuYXRvbTEXMBUGA1UEAxMO"
            + "U3Vib3JkaW5hdGUgQ0EwgZ8wDQYJKoZIhvcNAQEBBQADgY0AMIGJAoGBALATItEt"
            + "JrFmMswJRBxwhc8T8MXGrTGmovLCRIYmgX/0cklcK0pM7pDl63cX9Ps+3OsX90Ys"
            + "d3v0YWVEULi3YThRnH3HJgB4W4QoALuBhcewzgpLePPhzyhn/YOqRIT/yY0tspCN"
            + "AMLdu+Iqn/j20sFwva1NyLoA6sH28o/Jmf5zAgMBAAEwDQYJKoZIhvcNAQEEBQAD"
            + "gYEAMBTTmQl6axoNsMflQOzCkZPqk30Z9yltdMMT7Q1tCQDjbOiBs6tS/3au5DSZ"
            + "Xf9SBoWysdxNVHdYOIT5dkqJtCjC6nGiqnj5NZDXDUZ/4++NPlTEULy6ECszv2i7"
            + "NQ3q4x7h0mgUMaCA7sayQmLe/eOcwYxpGk2x0y5hrHJmcao=").getBytes());

    static byte[] testcert = Base64.decode(("MIICBDCCAW0CAQMwDQYJKoZIhvcNAQEEBQAwTDELMAkGA1UEBhMCU0UxEzARBgNV"
            + "BAgTClNvbWUtU3RhdGUxDzANBgNVBAoTBkFuYXRvbTEXMBUGA1UEAxMOU3Vib3Jk"
            + "aW5hdGUgQ0EwHhcNMDMwOTIyMDkxNTEzWhcNMTMwNDIyMDkxNTEzWjBJMQswCQYD"
            + "VQQGEwJTRTETMBEGA1UECBMKU29tZS1TdGF0ZTEPMA0GA1UEChMGQW5hdG9tMRQw"
            + "EgYDVQQDEwtGb29CYXIgVXNlcjCBnzANBgkqhkiG9w0BAQEFAAOBjQAwgYkCgYEA"
            + "xPpmVYVBzlGJxUfZa6IsHsk+HrMTbHWr/EUkiZIam95t+0SIFZHUers2PIv+GWVp"
            + "TmH/FTXNWVWw+W6bFlb17rfbatAkVfAYuBGRh+nUS/CPTPNw1jDeCuZRweD+DCNr"
            + "icx/svv0Hi/9scUqrADwtO2O7oBy7Lb/Vfa6BOnBdiECAwEAATANBgkqhkiG9w0B"
            + "AQQFAAOBgQAo5RzuUkLdHdAyJIG2IRptIJDOa0xq8eH2Duw9Xa3ieI9+ogCNaqWy"
            + "V5Oqx2lLsdn9CXxAwT/AsqwZ0ZFOJY1V2BgLTPH+vxnPOm0Xu61fl2XLtRBAycva"
            + "9iknwKZ3PCILvA5qjL9VedxiFhcG/p83SnPOrIOdsHykMTvO8/j8mA==").getBytes());

    private static Logger m_log = Logger.getLogger(TestCertificateRetrival.class);

    private Context m_ctx;
    private ICertificateStoreSessionHome m_storehome;
    private HashSet m_certs;
    private HashSet m_certfps;
    private String rootCaFp = null;
    private String subCaFp = null;
    private String endEntityFp = null;
    private Admin admin;

    private static void dumpCertificates(Collection certs) {
        m_log.trace(">dumpCertificates()");
        if (null != certs && !certs.isEmpty()) {
            Iterator iter = certs.iterator();

            while (iter.hasNext()) {
                Certificate obj = (Certificate)iter.next();
                m_log.debug("***** Certificate");
                m_log.debug("   SubjectDN : "
                		+ CertTools.getSubjectDN(obj));
                m_log.debug("   IssuerDN  : "
                		+ CertTools.getIssuerDN(obj));
            }
        } else {
            m_log.warn("Certificate collection is empty or NULL.");
        }
        m_log.trace("<dumpCertificates()");
    }

    public TestCertificateRetrival(String name) {
        super(name);
    }

    private Context getInitialContext() throws NamingException {
        m_log.trace(">getInitialContext");

        Context ctx = new javax.naming.InitialContext();
        m_log.trace("<getInitialContext");

        return ctx;
    }

    protected void setUp() throws Exception {
        m_log.trace(">setUp()");
        CertTools.installBCProvider();

        m_ctx = getInitialContext();

        Object obj2 = m_ctx.lookup("CertificateStoreSession");
        m_storehome = (ICertificateStoreSessionHome) javax.rmi.PortableRemoteObject.narrow(obj2,
                ICertificateStoreSessionHome.class);
        ICertificateStoreSessionRemote store = m_storehome.create();
        Certificate cert;
        Admin adm = new Admin(Admin.TYPE_INTERNALUSER);
        m_certs = new HashSet();
        m_certfps = new HashSet();
        cert = CertTools.getCertfromByteArray(testrootcert);
        m_certs.add(cert);
        m_certfps.add(CertTools.getFingerprintAsString(cert));
        //System.out.println(cert.getIssuerDN().getName()+";"+cert.getSerialNumber().toString(16)+";"+CertTools.getFingerprintAsString(cert));
        rootCaFp = CertTools.getFingerprintAsString(cert);
        try {
            if (store.findCertificateByFingerprint(adm, rootCaFp) == null) {
                store.storeCertificate(adm
                        , cert
                        , "o=AnaTom,c=SE"
                        , rootCaFp
                        , CertificateDataBean.CERT_ACTIVE
                        , CertificateDataBean.CERTTYPE_ROOTCA, SecConst.CERTPROFILE_FIXED_ROOTCA, null, new Date().getTime());
            }
            cert = CertTools.getCertfromByteArray(testcacert);
            m_certs.add(cert);
            m_certfps.add(CertTools.getFingerprintAsString(cert));
            //System.out.println(cert.getIssuerDN().getName()+";"+cert.getSerialNumber().toString(16)+";"+CertTools.getFingerprintAsString(cert));
            subCaFp = CertTools.getFingerprintAsString(cert);
            if (store.findCertificateByFingerprint(adm, subCaFp) == null) {
                store.storeCertificate(adm
                        , cert
                        , "o=AnaTom,c=SE"
                        , subCaFp
                        , CertificateDataBean.CERT_ACTIVE
                        , CertificateDataBean.CERTTYPE_SUBCA, SecConst.CERTPROFILE_FIXED_SUBCA, null, new Date().getTime());
            }
            cert = CertTools.getCertfromByteArray(testcert);
            m_certs.add(cert);
            m_certfps.add(CertTools.getFingerprintAsString(cert));
            //System.out.println(cert.getIssuerDN().getName()+";"+cert.getSerialNumber().toString(16)+";"+CertTools.getFingerprintAsString(cert));
            endEntityFp = CertTools.getFingerprintAsString(cert);
            if (store.findCertificateByFingerprint(adm, endEntityFp) == null) {
                store.storeCertificate(adm
                        , cert
                        , "o=AnaTom,c=SE"
                        , endEntityFp
                        , CertificateDataBean.CERT_ACTIVE
                        , CertificateDataBean.CERTTYPE_ENDENTITY, SecConst.CERTPROFILE_FIXED_ENDUSER, null, new Date().getTime());
            }
        } catch (Exception e) {
            m_log.error("Error: ", e);
            assertTrue("Error seting up tests: " + e.getMessage(), false);
        }
        admin = new Admin(Admin.TYPE_INTERNALUSER);
        m_log.trace("<setUp()");
    }

    protected void tearDown() throws Exception {
    }

    public void test01AddCertificates() throws Exception {
        m_log.trace(">test01AddCertificates()");
        m_log.trace("<test01AddCertificates()");
    }

    /**
     *
     * @throws Exception error
     */
    public void test02FindCACertificates() throws Exception {
        m_log.trace(">test02FindCACertificates()");
        ICertificateStoreSessionRemote store = m_storehome.create();

        // List all certificates to see
        Collection certfps = store.findCertificatesByType(admin
                , CertificateDataBean.CERTTYPE_SUBCA
                , null);
        assertNotNull("failed to list certs", certfps);
        assertTrue("failed to list certs", certfps.size() != 0);

        Iterator iter = certfps.iterator();
        boolean found = false;
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (!(obj instanceof Certificate)) {
                assertTrue("method 'findCertificatesByType' does not return Certificate objects.\n"
                        + "Class of returned object '" + obj.getClass().getName() + "'"
                        , false);
            }
            Certificate cert = (Certificate)obj;
            String fp = CertTools.getFingerprintAsString(cert);
            if (fp.equals(subCaFp)) {
                found = true;
            }
        }
        assertTrue(found);
        m_log.trace("<test02FindCACertificates()");
    }

    /**
     *
     * @throws Exception error
     */
    public void test03FindEndEntityCertificates() throws Exception {
        m_log.trace(">test03FindEndEntityCertificates()");

        ICertificateStoreSessionRemote store = m_storehome.create();

        // List all certificates to see, but only from our test certificates issuer, or we might get OutOfMemmory if there are plenty of certs
        Collection certfps = store.findCertificatesByType(admin
                , CertificateDataBean.CERTTYPE_ENDENTITY
                , "CN=Subordinate CA,O=Anatom,ST=Some-State,C=SE");
        assertNotNull("failed to list certs", certfps);
        assertTrue("failed to list certs", certfps.size() != 0);

        Iterator iter = certfps.iterator();
        boolean found = false;
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (!(obj instanceof Certificate)) {
                assertTrue("method 'findCertificatesByType' does not return Certificate objects.\n"
                        + "Class of returned object '" + obj.getClass().getName() + "'"
                        , false);
            }
            Certificate cert = (Certificate)obj;
            String fp = CertTools.getFingerprintAsString(cert);
            if (fp.equals(endEntityFp)) {
                found = true;
            }
        }
        assertTrue(found);

        m_log.trace("<test03FindEndEntityCertificates()");
    }

    /**
     *
     * @throws Exception error
     */
    public void test04FindRootCertificates() throws Exception {
        m_log.trace(">test04FindRootCertificates()");

        ICertificateStoreSessionRemote store = m_storehome.create();

        // List all certificates to see
        Collection certfps = store.findCertificatesByType(admin
                , CertificateDataBean.CERTTYPE_ROOTCA
                , null);
        assertNotNull("failed to list certs", certfps);
        assertTrue("failed to list certs", certfps.size() != 0);

        Iterator iter = certfps.iterator();
        boolean found = false;
        while (iter.hasNext()) {
            Object obj = iter.next();
            if (!(obj instanceof Certificate)) {
                assertTrue("method 'findCertificatesByType' does not return Certificate objects.\n"
                        + "Class of returned object '" + obj.getClass().getName() + "'"
                        , false);
            }
            Certificate cert = (Certificate)obj;
            String fp = CertTools.getFingerprintAsString(cert);
            if (fp.equals(rootCaFp)) {
                found = true;
            }
        }
        assertTrue(found);

        m_log.trace("<test04FindRootCertificates()");
    }

    /**
     *
     * @throws Exception error
     */
    public void test05CertificatesByIssuerAndSernos() throws Exception {
        m_log.trace(">test05CertificatesByIssuerAndSernos()");
        ICertificateStoreSessionRemote store = m_storehome.create();
        Certificate rootcacert;
        Certificate subcacert;
        Certificate cert;
        Vector sernos;
        Collection certfps;

        rootcacert = CertTools.getCertfromByteArray(testrootcert);
        subcacert = CertTools.getCertfromByteArray(testcacert);
        cert = CertTools.getCertfromByteArray(testcert);

        sernos = new Vector();
        sernos.add(CertTools.getSerialNumber(subcacert));
        sernos.add(CertTools.getSerialNumber(rootcacert));
        certfps = store.findCertificatesByIssuerAndSernos(admin
                , CertTools.getSubjectDN(rootcacert)
                , sernos);
        assertNotNull("failed to list certs", certfps);
        // we expect two certificates cause the rootca certificate is
        // self signed and so the issuer is identical with the subject
        // to which the certificate belongs
        dumpCertificates(certfps);
        assertTrue("failed to list certs", certfps.size() == 2);

        sernos = new Vector();
        sernos.add(CertTools.getSerialNumber(cert));
        certfps = store.findCertificatesByIssuerAndSernos(admin
                , CertTools.getSubjectDN(subcacert)
                , sernos);
        assertNotNull("failed to list certs", certfps);
        dumpCertificates(certfps);
        assertTrue("failed to list certs", certfps.size() == 1);
        assertTrue("Unable to find test certificate."
                , m_certfps.contains(CertTools.getFingerprintAsString((Certificate)certfps.iterator().next())));
        m_log.trace("<test05CertificatesByIssuerAndSernos()");
    }

    /**
     *
     * @throws Exception error
     */
    /* Don't run this test since it can lookup a looot of certs and you will get an OutOfMemoryException
    public void test06RetriveAllCertificates() throws Exception {
        m_log.trace(">test06CertificatesByIssuer()");
        ICertificateStoreSessionRemote store = m_storehome.create();

        // List all certificates to see
        Collection certfps = store.findCertificatesByType(admin
                , CertificateDataBean.CERTTYPE_ROOTCA + CertificateDataBean.CERTTYPE_SUBCA + CertificateDataBean.CERTTYPE_ENDENTITY
                , null);
        assertNotNull("failed to list certs", certfps);
        assertTrue("failed to list certs", certfps.size() >= 2);
        // Iterate over m_certs to see that we found all our certs (we probably found alot more...)
        Iterator iter = m_certs.iterator();
        while (iter.hasNext()) {
            assertTrue("Unable to find all test certificates.", certfps.contains(iter.next()));
        }
        m_log.trace("<test06CertificatesByIssuer()");
    } */

    /**
     *
     * @throws Exception error
     */
    public void test07FindCACertificatesWithIssuer() throws Exception {
        m_log.trace(">test07FindCACertificatesWithIssuer()");

        ICertificateStoreSessionRemote store = m_storehome.create();
        Certificate rootcacert = CertTools.getCertfromByteArray(testrootcert);

        // List all certificates to see
        Collection certfps = store.findCertificatesByType(admin
                , CertificateDataBean.CERTTYPE_SUBCA
                , CertTools.getSubjectDN(rootcacert));
        assertNotNull("failed to list certs", certfps);
        assertTrue("failed to list certs", certfps.size() >= 1);
        Iterator iter = certfps.iterator();
        boolean found = false;
        while (iter.hasNext()) {
            Certificate cert = (Certificate) iter.next();
            if (subCaFp.equals(CertTools.getFingerprintAsString(cert))) {
                found = true;
            }
        }
        assertTrue("Unable to find all test certificates.", found);
        m_log.trace("<test07FindCACertificatesWithIssuer()");
    }

    /**
     *
     * @throws Exception error
     */
    public void test08LoadRevocationInfo() throws Exception {
        m_log.trace(">test08LoadRevocationInfo()");

        ArrayList revstats = new ArrayList();
        Certificate rootcacert;
        Certificate subcacert;
        ICertificateStoreSessionRemote store = m_storehome.create();

        ArrayList sernos = new ArrayList();
        rootcacert = CertTools.getCertfromByteArray(testrootcert);
        subcacert = CertTools.getCertfromByteArray(testcacert);
        sernos.add(CertTools.getSerialNumber(rootcacert));
        sernos.add(CertTools.getSerialNumber(subcacert));

        Iterator iter = sernos.iterator();
        while (iter.hasNext()) {
        	BigInteger bi = (BigInteger)iter.next();
            RevokedCertInfo rev = store.isRevoked(admin
                    , CertTools.getSubjectDN(rootcacert)
                    , bi);
            revstats.add(rev);
        }

        assertNotNull("Unable to retrive certificate revocation status.", revstats);
        assertTrue("Method 'isRevoked' does not return status for ALL certificates.", revstats.size() >= 2);

        iter = revstats.iterator();
        while (iter.hasNext()) {
            RevokedCertInfo rci = (RevokedCertInfo) iter.next();
            m_log.debug("Certificate revocation information:\n"
                    + "   Serialnumber      : " + rci.getUserCertificate().toString() + "\n"
                    + "   Revocation date   : " + rci.getRevocationDate().toString() + "\n"
                    + "   Revocation reason : " + rci.getReason() + "\n");
        }
        m_log.trace("<test08LoadRevocationInfo()");
    }
}