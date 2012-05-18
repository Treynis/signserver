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

package se.anatom.ejbca.ca.caadmin;

import java.math.BigInteger;
import java.security.PublicKey;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.bouncycastle.jce.provider.JCEECPublicKey;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.caadmin.CAExistsException;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.caadmin.CVCCAInfo;
import org.ejbca.core.model.ca.caadmin.X509CAInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.CmsCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.OCSPCAServiceInfo;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.XKMSCAServiceInfo;
import org.ejbca.core.model.ca.catoken.CATokenConstants;
import org.ejbca.core.model.ca.catoken.CATokenInfo;
import org.ejbca.core.model.ca.catoken.SoftCATokenInfo;
import org.ejbca.core.model.ca.certificateprofiles.CACertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.CertificatePolicy;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.PKCS10RequestMessage;
import org.ejbca.cvc.CVCAuthenticatedRequest;
import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CardVerifiableCertificate;
import org.ejbca.cvc.CertificateParser;
import org.ejbca.util.CertTools;
import org.ejbca.util.TestTools;

/**
 * Tests the ca data entity bean.
 *
 * @version $Id: TestCAs.java 7324 2009-04-24 12:36:11Z netmackan $
 */
public class TestCAs extends TestCase {
    private static final Logger log = Logger.getLogger(TestCAs.class);
    private static final Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
    
    private static Collection rootcacertchain = null;

    /**
     * Creates a new TestCAs object.
     *
     * @param name name
     */
    public TestCAs(String name) {
        super(name);
        CertTools.installBCProvider();
    }

    protected void setUp() throws Exception {
    }

    protected void tearDown() throws Exception {
    }

    /**
     * adds a CA using RSA keys to the database.
     *
     * It also checks that the CA is stored correctly.
     *
     * @throws Exception error
     */
    public void test01AddRSACA() throws Exception {
        log.trace(">test01AddRSACA()");
        boolean ret = false;
        try {
        	TestTools.removeTestCA();	// We cant be sure this CA was not left over from some other failed test
            TestTools.getAuthorizationSession().initialize(admin, TestTools.getTestCAId());
            SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
            catokeninfo.setSignKeySpec("1024");
            catokeninfo.setEncKeySpec("1024");
            catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            // Create and active OSCP CA Service.
            ArrayList extendedcaservices = new ArrayList();
            extendedcaservices.add(new OCSPCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE,
                    "CN=OCSPSignerCertificate, " + "CN=TEST",
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_RSA));
            extendedcaservices.add(new XKMSCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
                    "CN=XKMSCertificate, " + "CN=TEST",
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_RSA));


            X509CAInfo cainfo = new X509CAInfo("CN=TEST",
                    "TEST", SecConst.CA_ACTIVE, new Date(),
                    "", SecConst.CERTPROFILE_FIXED_ROOTCA,
                    3650,
                    null, // Expiretime
                    CAInfo.CATYPE_X509,
                    CAInfo.SELFSIGNED,
                    (Collection) null,
                    catokeninfo,
                    "JUnit RSA CA",
                    -1, null,
                    null, // PolicyId
                    24, // CRLPeriod
                    0, // CRLIssueInterval
                    10, // CRLOverlapTime
                    10, // Delta CRL period
                    new ArrayList(),
                    true, // Authority Key Identifier
                    false, // Authority Key Identifier Critical
                    true, // CRL Number
                    false, // CRL Number Critical
                    null, // defaultcrldistpoint 
                    null, // defaultcrlissuer 
                    null, // defaultocsplocator
                    null, // defaultfreshestcrl
                    true, // Finish User
                    extendedcaservices,
                    false, // use default utf8 settings
                    new ArrayList(), // Approvals Settings
                    1, // Number of Req approvals
                    false, // Use UTF8 subject DN by default
            		true, // Use LDAP DN order by default
            		false, // Use CRL Distribution Point on CRL
            		false,  // CRL Distribution Point on CRL critical
            		true);

            TestTools.getCAAdminSession().createCA(admin, cainfo);

            CAInfo info = TestTools.getCAAdminSession().getCAInfo(admin, "TEST");

            rootcacertchain = info.getCertificateChain();
            X509Certificate cert = (X509Certificate) rootcacertchain.iterator().next();
            String sigAlg = CertTools.getSignatureAlgorithm(cert);
            assertEquals(CATokenInfo.SIGALG_SHA1_WITH_RSA, sigAlg);
            assertTrue("Error in created ca certificate", cert.getSubjectDN().toString().equals("CN=TEST"));
            assertTrue("Creating CA failed", info.getSubjectDN().equals("CN=TEST"));
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof RSAPublicKey) {
            	RSAPublicKey rsapk = (RSAPublicKey) pk;
				assertEquals(rsapk.getAlgorithm(), "RSA");
			} else {
				assertTrue("Public key is not EC", false);
			}
            assertTrue("CA is not valid for the specified duration.",cert.getNotAfter().after(new Date(new Date().getTime()+10*364*24*60*60*1000L)) && cert.getNotAfter().before(new Date(new Date().getTime()+10*366*24*60*60*1000L)));
            ret = true;
            
            // Test to generate a certificate request from the CA
            Collection cachain = info.getCertificateChain();
            byte[] request = TestTools.getCAAdminSession().makeRequest(admin, info.getCAId(), cachain, false, null, false);
            PKCS10RequestMessage msg = new PKCS10RequestMessage(request);
            assertEquals("CN=TEST", msg.getRequestDN());
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }

        assertTrue("Creating RSA CA failed", ret);
        log.trace("<test01AddRSACA()");
    }

    /**
     * renames CA in database.
     *
     * @throws Exception error
     */
    public void test02RenameCA() throws Exception {
        log.trace(">test02RenameCA()");

        boolean ret = false;
        try {
            TestTools.getCAAdminSession().renameCA(admin, "TEST", "TEST2");
            TestTools.getCAAdminSession().renameCA(admin, "TEST2", "TEST");
            ret = true;
        } catch (CAExistsException cee) {
        }
        assertTrue("Renaming CA failed", ret);

        log.trace("<test02RenameCA()");
    }


    /**
     * edits ca and checks that it's stored correctly.
     *
     * @throws Exception error
     */
    public void test03EditCA() throws Exception {
        log.trace(">test03EditCA()");

        X509CAInfo info = (X509CAInfo) TestTools.getCAAdminSession().getCAInfo(admin, "TEST");
        info.setCRLPeriod(33);
        TestTools.getCAAdminSession().editCA(admin, info);
        X509CAInfo info2 = (X509CAInfo) TestTools.getCAAdminSession().getCAInfo(admin, "TEST");
        assertTrue("Editing CA failed", info2.getCRLPeriod() == 33);

        log.trace("<test03EditCA()");
    }

    /**
     * adds a CA Using ECDSA keys to the database.
     *
     * It also checks that the CA is stored correctly.
     *
     * @throws Exception error
     */
    public void test04AddECDSACA() throws Exception {
        log.trace(">test04AddECDSACA()");
        boolean ret = false;
        try {
        	TestTools.getAuthorizationSession().initialize(admin, "CN=TESTECDSA".hashCode());

            SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
            catokeninfo.setSignKeySpec("prime192v1");
            catokeninfo.setEncKeySpec("1024");
            catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_ECDSA);
            catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA256_WITH_ECDSA);
            catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            // Create and active OSCP CA Service.
            ArrayList extendedcaservices = new ArrayList();
            extendedcaservices.add(new OCSPCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE,
                    "CN=OCSPSignerCertificate, " + "CN=TESTECDSA",
                    "",
                    "prime192v1",
                    CATokenConstants.KEYALGORITHM_ECDSA));
            extendedcaservices.add(new XKMSCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
                    "CN=XKMSSignerCertificate, " + "CN=TESTECDSA",
                    "",
                    "prime192v1",
                    CATokenConstants.KEYALGORITHM_ECDSA));

            ArrayList policies = new ArrayList(1);
            policies.add(new CertificatePolicy("2.5.29.32.0", "", ""));
            
            X509CAInfo cainfo = new X509CAInfo("CN=TESTECDSA",
                    "TESTECDSA", SecConst.CA_ACTIVE, new Date(),
                    "", SecConst.CERTPROFILE_FIXED_ROOTCA,
                    365,
                    null, // Expiretime
                    CAInfo.CATYPE_X509,
                    CAInfo.SELFSIGNED,
                    (Collection) null,
                    catokeninfo,
                    "JUnit ECDSA CA",
                    -1, null,
                    policies, // PolicyId
                    24, // CRLPeriod
                    0, // CRLIssueInterval
                    10, // CRLOverlapTime
                    0, // Delta CRL period
                    new ArrayList(),
                    true, // Authority Key Identifier
                    false, // Authority Key Identifier Critical
                    true, // CRL Number
                    false, // CRL Number Critical
                    null, // defaultcrldistpoint 
                    null, // defaultcrlissuer 
                    null, // defaultocsplocator
                    null, // defaultfreshestcrl
                    true, // Finish User
                    extendedcaservices,
                    false, // use default utf8 settings
                    new ArrayList(), // Approvals Settings
                    1, // Number of Req approvals
                    false, // Use UTF8 subject DN by default
                    true, // Use LDAP DN order by default
                    false, // Use CRL Distribution Point on CRL
                    false,  // CRL Distribution Point on CRL critical
                    true // include in Health Check
                    );


            TestTools.getCAAdminSession().createCA(admin, cainfo);


            CAInfo info = TestTools.getCAAdminSession().getCAInfo(admin, "TESTECDSA");

            X509Certificate cert = (X509Certificate) info.getCertificateChain().iterator().next();
            String sigAlg = CertTools.getSignatureAlgorithm(cert);
            assertEquals(CATokenInfo.SIGALG_SHA256_WITH_ECDSA, sigAlg);
            assertTrue("Error in created ca certificate", cert.getSubjectDN().toString().equals("CN=TESTECDSA"));
            assertTrue("Creating CA failed", info.getSubjectDN().equals("CN=TESTECDSA"));
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof JCEECPublicKey) {
				JCEECPublicKey ecpk = (JCEECPublicKey) pk;
				assertEquals(ecpk.getAlgorithm(), "EC");
				org.bouncycastle.jce.spec.ECParameterSpec spec = ecpk.getParameters();
				assertNotNull("ImplicitlyCA must have null spec", spec);
			} else {
				assertTrue("Public key is not EC", false);
			}

            ret = true;
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }

        assertTrue("Creating ECDSA CA failed", ret);
        log.trace("<test04AddECDSACA()");
    }

    /**
     * adds a CA Using ECDSA 'implicitlyCA' keys to the database.
     *
     * It also checks that the CA is stored correctly.
     *
     * @throws Exception error
     */
    public void test05AddECDSAImplicitlyCACA() throws Exception {
        log.trace(">test05AddECDSAImplicitlyCACA()");
        boolean ret = false;
        try {
        	TestTools.getAuthorizationSession().initialize(admin, "CN=TESTECDSAImplicitlyCA".hashCode());

            SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
            catokeninfo.setSignKeySpec("implicitlyCA");
            catokeninfo.setEncKeySpec("1024");
            catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_ECDSA);
            catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA256_WITH_ECDSA);
            catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            // Create and active OSCP CA Service.
            ArrayList extendedcaservices = new ArrayList();
            extendedcaservices.add(new OCSPCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE,
                    "CN=OCSPSignerCertificate, " + "CN=TESTECDSAImplicitlyCA",
                    "",
                    "prime192v1",
                    CATokenConstants.KEYALGORITHM_ECDSA));
            
            extendedcaservices.add(new XKMSCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
                    "CN=XKMSCertificate, " + "CN=TESTECDSAImplicitlyCA",
                    "",
                    "prime192v1",
                    CATokenConstants.KEYALGORITHM_ECDSA));

            ArrayList policies = new ArrayList(1);
            policies.add(new CertificatePolicy("2.5.29.32.0", "", ""));
            
            X509CAInfo cainfo = new X509CAInfo("CN=TESTECDSAImplicitlyCA",
                    "TESTECDSAImplicitlyCA", SecConst.CA_ACTIVE, new Date(),
                    "", SecConst.CERTPROFILE_FIXED_ROOTCA,
                    365,
                    null, // Expiretime
                    CAInfo.CATYPE_X509,
                    CAInfo.SELFSIGNED,
                    (Collection) null,
                    catokeninfo,
                    "JUnit ECDSA ImplicitlyCA CA",
                    -1, null,
                    policies, // PolicyId
                    24, // CRLPeriod
                    0, // CRLIssueInterval
                    10, // CRLOverlapTime
                    0, // Delta CRL period
                    new ArrayList(),
                    true, // Authority Key Identifier
                    false, // Authority Key Identifier Critical
                    true, // CRL Number
                    false, // CRL Number Critical
                    null, // defaultcrldistpoint 
                    null, // defaultcrlissuer 
                    null, // defaultocsplocator
                    null, // defaultfreshestcrl
                    true, // Finish User
                    extendedcaservices,
                    false, // use default utf8 settings
                    new ArrayList(), // Approvals Settings
                    1, // Number of Req approvals
                    false, // Use UTF8 subject DN by default 
                    true, // Use LDAP DN order by default
                    false, // Use CRL Distribution Point on CRL
                    false,  // CRL Distribution Point on CRL critical
                    true // Include in healthCheck
            );

            TestTools.getCAAdminSession().createCA(admin, cainfo);


            CAInfo info = TestTools.getCAAdminSession().getCAInfo(admin, "TESTECDSAImplicitlyCA");

            X509Certificate cert = (X509Certificate) info.getCertificateChain().iterator().next();
            assertTrue("Error in created ca certificate", cert.getSubjectDN().toString().equals("CN=TESTECDSAImplicitlyCA"));
            assertTrue("Creating CA failed", info.getSubjectDN().equals("CN=TESTECDSAImplicitlyCA"));
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof JCEECPublicKey) {
				JCEECPublicKey ecpk = (JCEECPublicKey) pk;
				assertEquals(ecpk.getAlgorithm(), "EC");
				org.bouncycastle.jce.spec.ECParameterSpec spec = ecpk.getParameters();
				assertNull("ImplicitlyCA must have null spec", spec);
				
			} else {
				assertTrue("Public key is not EC", false);
			}

            ret = true;
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }

        assertTrue("Creating ECDSA ImplicitlyCA CA failed", ret);
        log.trace("<test05AddECDSAImplicitlyCACA()");
    }

    /**
     * adds a CA using RSA keys to the database.
     *
     * It also checks that the CA is stored correctly.
     *
     * @throws Exception error
     */
    public void test06AddRSASha256WithMGF1CA() throws Exception {
        log.trace(">test06AddRSASha256WithMGF1CA()");
        boolean ret = false;
        try {
        	String cadn = "CN=TESTSha256WithMGF1";

        	TestTools.getAuthorizationSession().initialize(admin, cadn.hashCode());

            SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
            catokeninfo.setSignKeySpec("1024");
            catokeninfo.setEncKeySpec("1024");
            catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA256_WITH_RSA_AND_MGF1);
            catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA256_WITH_RSA_AND_MGF1);
            // Create and active OSCP CA Service.
            ArrayList extendedcaservices = new ArrayList();
            extendedcaservices.add(new OCSPCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE,
                    "CN=OCSPSignerCertificate, " + cadn,
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_RSA));
            extendedcaservices.add(new XKMSCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
                    "CN=XKMSCertificate, " + cadn,
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_RSA));


            X509CAInfo cainfo = new X509CAInfo(cadn,
                    "TESTSha256WithMGF1", SecConst.CA_ACTIVE, new Date(),
                    "", SecConst.CERTPROFILE_FIXED_ROOTCA,
                    365,
                    null, // Expiretime
                    CAInfo.CATYPE_X509,
                    CAInfo.SELFSIGNED,
                    (Collection) null,
                    catokeninfo,
                    "JUnit RSA CA",
                    -1, null,
                    null, // PolicyId
                    24, // CRLPeriod
                    0, // CRLIssueInterval
                    10, // CRLOverlapTime
                    0, // Delta CRL period
                    new ArrayList(),
                    true, // Authority Key Identifier
                    false, // Authority Key Identifier Critical
                    true, // CRL Number
                    false, // CRL Number Critical
                    null, // defaultcrldistpoint 
                    null, // defaultcrlissuer 
                    null, // defaultocsplocator
                    null, // defaultfreshestcrl
                    true, // Finish User
                    extendedcaservices,
                    false, // use default utf8 settings
                    new ArrayList(), // Approvals Settings
                    1, // Number of Req approvals
                    false, // Use UTF8 subject DN by default
                    true, // Use LDAP DN order by default
                    false, // Use CRL Distribution Point on CRL
                    false,  // CRL Distribution Point on CRL critical
                    true // Include in healthCheck
                    );
            TestTools.getCAAdminSession().createCA(admin, cainfo);

            CAInfo info = TestTools.getCAAdminSession().getCAInfo(admin, "TESTSha256WithMGF1");

            X509Certificate cert = (X509Certificate) info.getCertificateChain().iterator().next();
            String sigAlg = CertTools.getSignatureAlgorithm(cert);
            assertEquals(CATokenInfo.SIGALG_SHA256_WITH_RSA_AND_MGF1, sigAlg);
            assertTrue("Error in created ca certificate", cert.getSubjectDN().toString().equals(cadn));
            assertTrue("Creating CA failed", info.getSubjectDN().equals(cadn));
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof RSAPublicKey) {
            	RSAPublicKey rsapk = (RSAPublicKey) pk;
				assertEquals(rsapk.getAlgorithm(), "RSA");
			} else {
				assertTrue("Public key is not RSA", false);
			}

            ret = true;
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }

        assertTrue("Creating RSA CA failed", ret);
        log.trace("<test06AddRSASha256WithMGF1CA()");
    }

    public void test07AddRSACA4096() throws Exception {
        log.trace(">test07AddRSACA4096()");
        boolean ret = false;
        try {
        	String dn = CertTools.stringToBCDNString("CN=TESTRSA4096,OU=FooBaaaaaar veeeeeeeery long ou,OU=Another very long very very long ou,O=FoorBar Very looong O,L=Lets ad a loooooooooooooooooong Locality as well,C=SE");
        	TestTools.getAuthorizationSession().initialize(admin, dn.hashCode());

            SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
            catokeninfo.setSignKeySpec("4096");
            catokeninfo.setEncKeySpec("2048");
            catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA256_WITH_RSA);
            catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            // Create and active OSCP CA Service.
            ArrayList extendedcaservices = new ArrayList();
            extendedcaservices.add(new OCSPCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE,
                    "CN=OCSPSignerCertificate, " + dn,
                    "",
                    "2048",
                    CATokenConstants.KEYALGORITHM_RSA));
            extendedcaservices.add(new XKMSCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
                    "CN=XKMSCertificate, " + dn,
                    "",
                    "2048",
                    CATokenConstants.KEYALGORITHM_RSA));


            X509CAInfo cainfo = new X509CAInfo(dn,
            		"TESTRSA4096", SecConst.CA_ACTIVE, new Date(),
            		"", SecConst.CERTPROFILE_FIXED_ROOTCA,
            		365,
            		null, // Expiretime
            		CAInfo.CATYPE_X509,
            		CAInfo.SELFSIGNED,
            		(Collection) null,
            		catokeninfo,
            		"JUnit RSA CA, we ned also a very long CA description for this CA, because we want to create a CA Data string that is more than 36000 characters or something like that. All this is because Oracle can not set very long strings with the JDBC provider and we must test that we can handle long CAs",
            		-1, null,
            		null, // PolicyId
            		24, // CRLPeriod
            		0, // CRLIssueInterval
            		10, // CRLOverlapTime
            		0, // Delta CRL period
            		new ArrayList(),
            		true, // Authority Key Identifier
            		false, // Authority Key Identifier Critical
            		true, // CRL Number
            		false, // CRL Number Critical
            		null, // defaultcrldistpoint 
            		null, // defaultcrlissuer 
            		null, // defaultocsplocator
            		null, // defaultfreshestcrl
            		true, // Finish User
            		extendedcaservices,
            		false, // use default utf8 settings
            		new ArrayList(), // Approvals Settings
            		1, // Number of Req approvals
            		false, // Use UTF8 subject DN by default
                    true, // Use LDAP DN order by default
                    false, // Use CRL Distribution Point on CRL
                    false,  // CRL Distribution Point on CRL critical
                    true // Include in HealthCheck
                    );

            TestTools.getCAAdminSession().createCA(admin, cainfo);


            CAInfo info = TestTools.getCAAdminSession().getCAInfo(admin, "TESTRSA4096");

            X509Certificate cert = (X509Certificate) info.getCertificateChain().iterator().next();
            String sigAlg = CertTools.getSignatureAlgorithm(cert);
            assertEquals(CATokenInfo.SIGALG_SHA256_WITH_RSA, sigAlg);
            assertTrue("Error in created ca certificate", CertTools.stringToBCDNString(cert.getSubjectDN().toString()).equals(dn));
            assertTrue("Creating CA failed", info.getSubjectDN().equals(dn));
            // Normal order
            assertEquals(cert.getSubjectX500Principal().getName(), "C=SE,L=Lets ad a loooooooooooooooooong Locality as well,O=FoorBar Very looong O,OU=Another very long very very long ou,OU=FooBaaaaaar veeeeeeeery long ou,CN=TESTRSA4096");
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof RSAPublicKey) {
            	RSAPublicKey rsapk = (RSAPublicKey) pk;
				assertEquals(rsapk.getAlgorithm(), "RSA");
			} else {
				assertTrue("Public key is not EC", false);
			}

            ret = true;
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }

        assertTrue("Creating RSA CA 4096 failed", ret);
        log.trace("<test07AddRSACA4096()");
    }

    public void test08AddRSACAReverseDN() throws Exception {
        log.trace(">test08AddRSACAReverseDN()");
        boolean ret = false;
        try {
        	String dn = CertTools.stringToBCDNString("CN=TESTRSAReverse,O=FooBar,OU=BarFoo,C=SE");
        	String name = "TESTRSAREVERSE";
        	TestTools.getAuthorizationSession().initialize(admin, dn.hashCode());

            SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
            catokeninfo.setSignKeySpec("1024");
            catokeninfo.setEncKeySpec("1024");
            catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            // Create and active OSCP CA Service.
            ArrayList extendedcaservices = new ArrayList();
            extendedcaservices.add(new OCSPCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE,
                    "CN=OCSPSignerCertificate, " + dn,
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_RSA));
            extendedcaservices.add(new XKMSCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
                    "CN=XKMSCertificate, " + dn,
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_RSA));


            X509CAInfo cainfo = new X509CAInfo(dn,
            		name, SecConst.CA_ACTIVE, new Date(),
            		"", SecConst.CERTPROFILE_FIXED_ROOTCA,
            		365,
            		null, // Expiretime
            		CAInfo.CATYPE_X509,
            		CAInfo.SELFSIGNED,
            		(Collection) null,
            		catokeninfo,
            		"JUnit RSA CA, we ned also a very long CA description for this CA, because we want to create a CA Data string that is more than 36000 characters or something like that. All this is because Oracle can not set very long strings with the JDBC provider and we must test that we can handle long CAs",
            		-1, null,
            		null, // PolicyId
            		24, // CRLPeriod
            		0, // CRLIssueInterval
            		10, // CRLOverlapTime
            		0, // Delta CRL period
            		new ArrayList(),
            		true, // Authority Key Identifier
            		false, // Authority Key Identifier Critical
            		true, // CRL Number
            		false, // CRL Number Critical
            		null, // defaultcrldistpoint 
            		null, // defaultcrlissuer 
            		null, // defaultocsplocator
            		null, // defaultfreshestcrl
            		true, // Finish User
            		extendedcaservices,
            		false, // use default utf8 settings
            		new ArrayList(), // Approvals Settings
            		1, // Number of Req approvals
            		false, // Use UTF8 subject DN by default
                    false, // Use X500 DN order
                    false, // Use CRL Distribution Point on CRL
                    false,  // CRL Distribution Point on CRL critical
                    true // Include in health check
                    );

            TestTools.getCAAdminSession().createCA(admin, cainfo);


            CAInfo info = TestTools.getCAAdminSession().getCAInfo(admin, name);

            X509Certificate cert = (X509Certificate) info.getCertificateChain().iterator().next();
            String sigAlg = CertTools.getSignatureAlgorithm(cert);
            assertEquals(CATokenInfo.SIGALG_SHA1_WITH_RSA, sigAlg);
            assertEquals("Error in created ca certificate", CertTools.stringToBCDNString(cert.getSubjectDN().toString()),dn);
            assertTrue("Creating CA failed", info.getSubjectDN().equals(dn));
            // reverse order
            assertEquals(cert.getSubjectX500Principal().getName(), "CN=TESTRSAReverse,OU=BarFoo,O=FooBar,C=SE");
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof RSAPublicKey) {
            	RSAPublicKey rsapk = (RSAPublicKey) pk;
				assertEquals(rsapk.getAlgorithm(), "RSA");
			} else {
				assertTrue("Public key is not EC", false);
			}

            ret = true;
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }

        assertTrue("Creating RSA CA reverse failed", ret);
        log.trace("<test08AddRSACAReverseDN()");
    }
    
    public void test09AddCVCCA() throws Exception {
        log.trace(">test09AddCVCCA()");
        boolean ret = false;
        SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
        catokeninfo.setSignKeySpec("1024");
        catokeninfo.setEncKeySpec("1024");
        catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
        catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
        catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA256_WITH_RSA_AND_MGF1);
        catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA256_WITH_RSA_AND_MGF1);
        // No CA Services.
        ArrayList extendedcaservices = new ArrayList();

        String rootcadn = "CN=TESTCVCA,C=SE";
    	String rootcaname = "TESTCVCA";
        String dvddn = "CN=TESTDV-D,C=SE";
    	String dvdcaname = "TESTDV-D";
        String dvfdn = "CN=TESTDV-F,C=FI";
    	String dvfcaname = "TESTDV-F";

    	CAInfo dvdcainfo = null; // to be used for renewal
    	CAInfo cvcainfo = null; // to be used for making request
    	
    	// Create a root CVCA
        try {
        	TestTools.getAuthorizationSession().initialize(admin, rootcadn.hashCode());

            CVCCAInfo cvccainfo = new CVCCAInfo(rootcadn, rootcaname, SecConst.CA_ACTIVE, new Date(),
            		SecConst.CERTPROFILE_FIXED_ROOTCA, 3650, 
                    null, // Expiretime 
                    CAInfo.CATYPE_CVC, CAInfo.SELFSIGNED,
                    null, catokeninfo, "JUnit CVC CA", 
                    -1, null,
                    24, // CRLPeriod
                    0, // CRLIssueInterval
                    10, // CRLOverlapTime
                    10, // Delta CRL period
                    new ArrayList(), // CRL publishers
                    true, // Finish User
                    extendedcaservices,
                    new ArrayList(), // Approvals Settings
                    1, // Number of Req approvals
                    true // Include in health check
                    );
            
            TestTools.getCAAdminSession().createCA(admin, cvccainfo);

            cvcainfo = TestTools.getCAAdminSession().getCAInfo(admin, rootcaname);
            assertEquals(CAInfo.CATYPE_CVC, cvcainfo.getCAType());

            Certificate cert = (Certificate)cvcainfo.getCertificateChain().iterator().next();
            String sigAlg = CertTools.getSignatureAlgorithm(cert);
            assertEquals(CATokenInfo.SIGALG_SHA256_WITH_RSA_AND_MGF1, sigAlg);
            assertEquals("CVC", cert.getType());
            assertEquals(rootcadn, CertTools.getSubjectDN(cert));
            assertEquals(rootcadn, CertTools.getIssuerDN(cert));
            assertEquals(rootcadn, cvcainfo.getSubjectDN());
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof RSAPublicKey) {
            	RSAPublicKey rsapk = (RSAPublicKey) pk;
				assertEquals(rsapk.getAlgorithm(), "RSA");
				BigInteger modulus = rsapk.getModulus(); 
				int len = modulus.bitLength();
				assertEquals(1024, len);
			} else {
				assertTrue("Public key is not RSA", false);
			}
            assertTrue("CA is not valid for the specified duration.",CertTools.getNotAfter(cert).after(new Date(new Date().getTime()+10*364*24*60*60*1000L)) && CertTools.getNotAfter(cert).before(new Date(new Date().getTime()+10*366*24*60*60*1000L)));
            // Check role
            CardVerifiableCertificate cvcert = (CardVerifiableCertificate)cert;
            String role = cvcert.getCVCertificate().getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole().name();
            assertEquals("SETESTCVCA00001", cvcert.getCVCertificate().getCertificateBody().getHolderReference().getConcatenated());
            assertEquals("CVCA", role);
            ret = true;
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }

        // Create a Sub DV domestic
        try {
        	TestTools.getAuthorizationSession().initialize(admin, dvddn.hashCode());
            // Create a Certificate profile
            CertificateProfile profile = new CACertificateProfile();
            profile.setType(CertificateProfile.TYPE_SUBCA);
            TestTools.getCertificateStoreSession().addCertificateProfile(admin, "TESTCVCDV", profile);
            int profileid = TestTools.getCertificateStoreSession().getCertificateProfileId(admin, "TESTCVCDV");

            CVCCAInfo cvccainfo = new CVCCAInfo(dvddn, dvdcaname, SecConst.CA_ACTIVE, new Date(),
            		profileid, 3650, 
                    null, // Expiretime 
                    CAInfo.CATYPE_CVC, rootcadn.hashCode(),
                    null, catokeninfo, "JUnit CVC CA", 
                    -1, null,
                    24, // CRLPeriod
                    0, // CRLIssueInterval
                    10, // CRLOverlapTime
                    10, // Delta CRL period
                    new ArrayList(), // CRL publishers
                    true, // Finish User
                    extendedcaservices,
                    new ArrayList(), // Approvals Settings
                    1, // Number of Req approvals
                    true // Include in health check
                    );
            
            TestTools.getCAAdminSession().createCA(admin, cvccainfo);

            dvdcainfo = TestTools.getCAAdminSession().getCAInfo(admin, dvdcaname);
            assertEquals(CAInfo.CATYPE_CVC, dvdcainfo.getCAType());

            Certificate cert = (Certificate)dvdcainfo.getCertificateChain().iterator().next();
            assertEquals("CVC", cert.getType());
            assertEquals(CertTools.getSubjectDN(cert), dvddn);
            assertEquals(CertTools.getIssuerDN(cert), rootcadn);
            assertEquals(dvdcainfo.getSubjectDN(), dvddn);
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof RSAPublicKey) {
            	RSAPublicKey rsapk = (RSAPublicKey) pk;
				assertEquals(rsapk.getAlgorithm(), "RSA");
				BigInteger modulus = rsapk.getModulus(); 
				int len = modulus.bitLength();
				assertEquals(1024, len);
			} else {
				assertTrue("Public key is not RSA", false);
			}
            assertTrue("CA is not valid for the specified duration.",CertTools.getNotAfter(cert).after(new Date(new Date().getTime()+10*364*24*60*60*1000L)) && CertTools.getNotAfter(cert).before(new Date(new Date().getTime()+10*366*24*60*60*1000L)));
            // Check role
            CardVerifiableCertificate cvcert = (CardVerifiableCertificate)cert;
            assertEquals("SETESTDV-D00001", cvcert.getCVCertificate().getCertificateBody().getHolderReference().getConcatenated());
            String role = cvcert.getCVCertificate().getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole().name();
            assertEquals("DV_D", role);
            String accessRights = cvcert.getCVCertificate().getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getAccessRight().name();
            assertEquals("READ_ACCESS_DG3_AND_DG4", accessRights);
            ret = true;
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }

        // Create a Sub DV foreign
        try {
            TestTools.getAuthorizationSession().initialize(admin, dvfdn.hashCode());

            CVCCAInfo cvccainfo = new CVCCAInfo(dvfdn, dvfcaname, SecConst.CA_ACTIVE, new Date(),
            		SecConst.CERTPROFILE_FIXED_SUBCA, 3650, 
                    null, // Expiretime 
                    CAInfo.CATYPE_CVC, rootcadn.hashCode(),
                    null, catokeninfo, "JUnit CVC CA", 
                    -1, null,
                    24, // CRLPeriod
                    0, // CRLIssueInterval
                    10, // CRLOverlapTime
                    10, // Delta CRL period
                    new ArrayList(), // CRL publishers
                    true, // Finish User
                    extendedcaservices,
                    new ArrayList(), // Approvals Settings
                    1, // Number of Req approvals
                    true // Include in health check
                    );
            
            TestTools.getCAAdminSession().createCA(admin, cvccainfo);

            CAInfo info = TestTools.getCAAdminSession().getCAInfo(admin, dvfcaname);
            assertEquals(CAInfo.CATYPE_CVC, info.getCAType());

            Certificate cert = (Certificate)info.getCertificateChain().iterator().next();
            assertEquals("CVC", cert.getType());
            assertEquals(CertTools.getSubjectDN(cert), dvfdn);
            assertEquals(CertTools.getIssuerDN(cert), rootcadn);
            assertEquals(info.getSubjectDN(), dvfdn);
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof RSAPublicKey) {
            	RSAPublicKey rsapk = (RSAPublicKey) pk;
				assertEquals(rsapk.getAlgorithm(), "RSA");
				BigInteger modulus = rsapk.getModulus(); 
				int len = modulus.bitLength();
				assertEquals(1024, len);
			} else {
				assertTrue("Public key is not RSA", false);
			}
            assertTrue("CA is not valid for the specified duration.",CertTools.getNotAfter(cert).after(new Date(new Date().getTime()+10*364*24*60*60*1000L)) && CertTools.getNotAfter(cert).before(new Date(new Date().getTime()+10*366*24*60*60*1000L)));
            // Check role
            CardVerifiableCertificate cvcert = (CardVerifiableCertificate)cert;
            assertEquals("FITESTDV-F00001", cvcert.getCVCertificate().getCertificateBody().getHolderReference().getConcatenated());
            String role = cvcert.getCVCertificate().getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole().name();
            assertEquals("DV_F", role);
            ret = true;
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }
        assertTrue("Creating CVC CAs failed", ret);

        // Test to renew a CVC CA using a different access right
        CertificateProfile profile = TestTools.getCertificateStoreSession().getCertificateProfile(admin, "TESTCVCDV");
        profile.setCVCAccessRights(CertificateProfile.CVC_ACCESS_DG3);
        TestTools.getCertificateStoreSession().changeCertificateProfile(admin, "TESTCVCDV", profile);

        int caid = dvdcainfo.getCAId();
        TestTools.getCAAdminSession().renewCA(admin, caid, null, false);
        dvdcainfo = TestTools.getCAAdminSession().getCAInfo(admin, dvdcaname);
        assertEquals(CAInfo.CATYPE_CVC, dvdcainfo.getCAType());
        Certificate cert = (Certificate)dvdcainfo.getCertificateChain().iterator().next();
        assertEquals("CVC", cert.getType());
        assertEquals(CertTools.getSubjectDN(cert), dvddn);
        assertEquals(CertTools.getIssuerDN(cert), rootcadn);
        assertEquals(dvdcainfo.getSubjectDN(), dvddn);
        // It's not possible to check the time for renewal of a CVC CA since the resolution of validity is only days.
        // The only way is to generate a certificate with different access rights in it
        CardVerifiableCertificate cvcert = (CardVerifiableCertificate)cert;
        String role = cvcert.getCVCertificate().getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole().name();
        assertEquals("DV_D", role);
        String accessRights = cvcert.getCVCertificate().getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getAccessRight().name();
        assertEquals("READ_ACCESS_DG3", accessRights);


        // Make a certificate request from a CVCA
        Collection cachain = cvcainfo.getCertificateChain();
        assertEquals(1, cachain.size());
        Certificate cert1 = (Certificate)cachain.iterator().next();
        CardVerifiableCertificate cvcert1 = (CardVerifiableCertificate)cert1;
        assertEquals("SETESTCVCA00001", cvcert1.getCVCertificate().getCertificateBody().getHolderReference().getConcatenated());
        byte[] request = TestTools.getCAAdminSession().makeRequest(admin, cvcainfo.getCAId(), cachain, false, null, false);
		Certificate req = CertTools.getCertfromByteArray(request);
		CardVerifiableCertificate cardcert = (CardVerifiableCertificate)req;
		CVCertificate reqcert = cardcert.getCVCertificate();
        assertEquals("SETESTCVCA00001", reqcert.getCertificateBody().getHolderReference().getConcatenated());
        assertEquals("SETESTCVCA00001", reqcert.getCertificateBody().getAuthorityReference().getConcatenated());

        // Make a certificate request from a DV, regenerating keys
        cachain = dvdcainfo.getCertificateChain();
        request = TestTools.getCAAdminSession().makeRequest(admin, dvdcainfo.getCAId(), cachain, false, "foo123", true);
		req = CertTools.getCertfromByteArray(request);
		cardcert = (CardVerifiableCertificate)req;
		reqcert = cardcert.getCVCertificate();
        assertEquals("SETESTDV-D00002", reqcert.getCertificateBody().getHolderReference().getConcatenated());
        // This request is made from the DV targeted for the DV, so the old DV certificate will be the holder ref.
        // Normally you would target an external CA, and thus send in it's cachain. The caRef would be the external CAs holderRef.
        assertEquals("SETESTDV-D00001", reqcert.getCertificateBody().getAuthorityReference().getConcatenated());
        
        // Get the DVs certificate request signed by the CVCA
        byte[] authrequest = TestTools.getCAAdminSession().signRequest(admin, cvcainfo.getCAId(), request, false, false);
		CVCObject parsedObject = CertificateParser.parseCVCObject(authrequest);
        CVCAuthenticatedRequest authreq = (CVCAuthenticatedRequest)parsedObject;
        assertEquals("SETESTDV-D00002", authreq.getRequest().getCertificateBody().getHolderReference().getConcatenated());
        assertEquals("SETESTDV-D00001", authreq.getRequest().getCertificateBody().getAuthorityReference().getConcatenated());
        assertEquals("SETESTCVCA00001", authreq.getAuthorityReference().getConcatenated());

        // Get the DVs certificate request signed by the CVCA creating a link certificate.
        // Passing in a request without authrole should return a regular authenticated request though.
        authrequest = TestTools.getCAAdminSession().signRequest(admin, cvcainfo.getCAId(), request, false, true);
		parsedObject = CertificateParser.parseCVCObject(authrequest);
		authreq = (CVCAuthenticatedRequest)parsedObject;
		// Pass in a certificate instead
		CardVerifiableCertificate dvdcert = (CardVerifiableCertificate)cachain.iterator().next();
        authrequest = TestTools.getCAAdminSession().signRequest(admin, cvcainfo.getCAId(), dvdcert.getEncoded(), false, true);
		parsedObject = CertificateParser.parseCVCObject(authrequest);
		CVCertificate linkcert = (CVCertificate)parsedObject;
        assertEquals("SETESTCVCA00001", linkcert.getCertificateBody().getAuthorityReference().getConcatenated());
        assertEquals("SETESTDV-D00001", linkcert.getCertificateBody().getHolderReference().getConcatenated());

        // Renew again but regenerate keys this time to make sure sequence is updated
        caid = dvdcainfo.getCAId();
        TestTools.getCAAdminSession().renewCA(admin, caid, "foo123", true);
        dvdcainfo = TestTools.getCAAdminSession().getCAInfo(admin, dvdcaname);
        assertEquals(CAInfo.CATYPE_CVC, dvdcainfo.getCAType());
        cert = (Certificate)dvdcainfo.getCertificateChain().iterator().next();
        assertEquals("CVC", cert.getType());
        assertEquals(CertTools.getSubjectDN(cert), dvddn);
        assertEquals(CertTools.getIssuerDN(cert), rootcadn);
        assertEquals(dvdcainfo.getSubjectDN(), dvddn);
        cvcert = (CardVerifiableCertificate)cert;
        role = cvcert.getCVCertificate().getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole().name();
        assertEquals("DV_D", role);
        String holderRef = cvcert.getCVCertificate().getCertificateBody().getHolderReference().getConcatenated();
        // Sequence must have been updated with 1
        assertEquals("SETESTDV-D00003", holderRef);

        log.trace("<test09AddCVCCA()");
    }

    /** Test that we can create a SubCA signed by an external RootCA.
     * The SubCA create a certificate request sent to the RootCA that creates a certificate which is then received on the SubCA again.
     * @throws Exception
     */
    public void test10RSASignedByExternal() throws Exception {
        log.trace(">test10RSASignedByExternal()");
        boolean ret = false;
        CAInfo info =null;
        try {
            TestTools.getAuthorizationSession().initialize(admin, "CN=TESTSIGNEDBYEXTERNAL".hashCode());

            SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
            catokeninfo.setSignKeySpec("1024");
            catokeninfo.setEncKeySpec("1024");
            catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            // Create and active OSCP CA Service.
            ArrayList extendedcaservices = new ArrayList();
            extendedcaservices.add(new OCSPCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE,
                    "CN=OCSPSignerCertificate, " + "CN=TESTSIGNEDBYEXTERNAL",
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_RSA));
            extendedcaservices.add(new XKMSCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
                    "CN=XKMSCertificate, " + "CN=TESTSIGNEDBYEXTERNAL",
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_RSA));
			 extendedcaservices.add(new CmsCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
						  "CN=CMSCertificate, " + "CN=TESTSIGNEDBYEXTERNAL",
			     		  "",
			     		  "1024",
			     		 CATokenConstants.KEYALGORITHM_RSA));

            X509CAInfo cainfo = new X509CAInfo("CN=TESTSIGNEDBYEXTERNAL",
                    "TESTSIGNEDBYEXTERNAL", SecConst.CA_ACTIVE, new Date(),
                    "", SecConst.CERTPROFILE_FIXED_SUBCA,
                    1000,
                    null, // Expiretime
                    CAInfo.CATYPE_X509,
                    CAInfo.SIGNEDBYEXTERNALCA, // Signed by the first TEST CA we created
                    (Collection) null,
                    catokeninfo,
                    "JUnit RSA CA Signed by external",
                    -1, null,
                    null, // PolicyId
                    24, // CRLPeriod
                    0, // CRLIssueInterval
                    10, // CRLOverlapTime
                    10, // Delta CRL period
                    new ArrayList(),
                    true, // Authority Key Identifier
                    false, // Authority Key Identifier Critical
                    true, // CRL Number
                    false, // CRL Number Critical
                    null, // defaultcrldistpoint 
                    null, // defaultcrlissuer 
                    null, // defaultocsplocator
                    null, // defaultfreshestcrl
                    true, // Finish User
                    extendedcaservices,
                    false, // use default utf8 settings
                    new ArrayList(), // Approvals Settings
                    1, // Number of Req approvals
                    false, // Use UTF8 subject DN by default
            		true, // Use LDAP DN order by default
            		false, // Use CRL Distribution Point on CRL
            		false,  // CRL Distribution Point on CRL critical
            		true);

            TestTools.getCAAdminSession().createCA(admin, cainfo);

            info = TestTools.getCAAdminSession().getCAInfo(admin, "TESTSIGNEDBYEXTERNAL");

            // Generate a certificate request from the CA and send to the TEST CA
            byte[] request = TestTools.getCAAdminSession().makeRequest(admin, info.getCAId(), rootcacertchain, false, null, false);
            PKCS10RequestMessage msg = new PKCS10RequestMessage(request);
            assertEquals("CN=TESTSIGNEDBYEXTERNAL", msg.getRequestDN());

            // Receive the certificate request on the TEST CA
            info.setSignedBy("CN=TEST".hashCode());
            IResponseMessage resp = TestTools.getCAAdminSession().processRequest(admin, info, msg);
            
            // Receive the signed certificate back on our SubCA
            TestTools.getCAAdminSession().receiveResponse(admin, info.getCAId(), resp, null);
            
            // Check that the CA has the correct certificate chain now
            info = TestTools.getCAAdminSession().getCAInfo(admin, "TESTSIGNEDBYEXTERNAL");
            Iterator iter = info.getCertificateChain().iterator();
            X509Certificate cert = (X509Certificate) iter.next();
            String sigAlg = CertTools.getSignatureAlgorithm(cert);
            assertEquals(CATokenInfo.SIGALG_SHA1_WITH_RSA, sigAlg);
            assertTrue("Error in created ca certificate", CertTools.getSubjectDN(cert).equals("CN=TESTSIGNEDBYEXTERNAL"));
            assertTrue("Error in created ca certificate", CertTools.getIssuerDN(cert).equals("CN=TEST"));
            assertTrue("Creating CA failed", info.getSubjectDN().equals("CN=TESTSIGNEDBYEXTERNAL"));
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof RSAPublicKey) {
            	RSAPublicKey rsapk = (RSAPublicKey) pk;
				assertEquals(rsapk.getAlgorithm(), "RSA");
			} else {
				assertTrue("Public key is not EC", false);
			}
            cert = (X509Certificate) iter.next();
            assertTrue("Error in root ca certificate", CertTools.getSubjectDN(cert).equals("CN=TEST"));
            assertTrue("Error in root ca certificate", CertTools.getIssuerDN(cert).equals("CN=TEST"));
            
            ret = true;

        } catch (CAExistsException pee) {
            log.info("CA exists: ", pee);
        }

        // Make a certificate request from the CA
        Collection cachain = info.getCertificateChain();
        byte[] request = TestTools.getCAAdminSession().makeRequest(admin, info.getCAId(), cachain, false, null, false);
        PKCS10RequestMessage msg = new PKCS10RequestMessage(request);
        assertEquals("CN=TESTSIGNEDBYEXTERNAL", msg.getRequestDN());

        assertTrue("Creating RSA CA (signed by external) failed", ret);
        log.trace("<test10RSASignedByExternal");
    }
    
    /**
     * adds a CA using DSA keys to the database.
     *
     * It also checks that the CA is stored correctly.
     *
     * @throws Exception error
     */
    public void test11AddDSACA() throws Exception {
        log.trace(">test11AddDSACA()");
        boolean ret = false;
        try {
        	TestTools.removeTestCA("TESTDSA");	// We cant be sure this CA was not left over from some other failed test
            TestTools.getAuthorizationSession().initialize(admin, TestTools.getTestCAId());
            SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
            catokeninfo.setSignKeySpec("1024");
            catokeninfo.setEncKeySpec("1024");
            catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_DSA);
            catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
            catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_DSA);
            catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA1_WITH_RSA);
            // Create and active OSCP CA Service.
            ArrayList extendedcaservices = new ArrayList();
            extendedcaservices.add(new OCSPCAServiceInfo(ExtendedCAServiceInfo.STATUS_ACTIVE,
                    "CN=OCSPSignerCertificate, " + "CN=TESTDSA",
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_DSA));
            extendedcaservices.add(new XKMSCAServiceInfo(ExtendedCAServiceInfo.STATUS_INACTIVE,
                    "CN=XKMSCertificate, " + "CN=TESTDSA",
                    "",
                    "1024",
                    CATokenConstants.KEYALGORITHM_DSA));


            X509CAInfo cainfo = new X509CAInfo("CN=TESTDSA",
                    "TESTDSA", SecConst.CA_ACTIVE, new Date(),
                    "", SecConst.CERTPROFILE_FIXED_ROOTCA,
                    3650,
                    null, // Expiretime
                    CAInfo.CATYPE_X509,
                    CAInfo.SELFSIGNED,
                    (Collection) null,
                    catokeninfo,
                    "JUnit DSA CA",
                    -1, null,
                    null, // PolicyId
                    24, // CRLPeriod
                    0, // CRLIssueInterval
                    10, // CRLOverlapTime
                    10, // Delta CRL period
                    new ArrayList(),
                    true, // Authority Key Identifier
                    false, // Authority Key Identifier Critical
                    true, // CRL Number
                    false, // CRL Number Critical
                    null, // defaultcrldistpoint 
                    null, // defaultcrlissuer 
                    null, // defaultocsplocator
                    null, // defaultfreshestcrl
                    true, // Finish User
                    extendedcaservices,
                    false, // use default utf8 settings
                    new ArrayList(), // Approvals Settings
                    1, // Number of Req approvals
                    false, // Use UTF8 subject DN by default
            		true, // Use LDAP DN order by default
            		false, // Use CRL Distribution Point on CRL
            		false,  // CRL Distribution Point on CRL critical
            		true);

            TestTools.getCAAdminSession().createCA(admin, cainfo);

            CAInfo info = TestTools.getCAAdminSession().getCAInfo(admin, "TESTDSA");

            rootcacertchain = info.getCertificateChain();
            X509Certificate cert = (X509Certificate) rootcacertchain.iterator().next();
            String sigAlg = CertTools.getSignatureAlgorithm(cert);
            assertEquals(CATokenInfo.SIGALG_SHA1_WITH_DSA, sigAlg);
            assertTrue("Error in created ca certificate", cert.getSubjectDN().toString().equals("CN=TESTDSA"));
            assertTrue("Creating CA failed", info.getSubjectDN().equals("CN=TESTDSA"));
            PublicKey pk = cert.getPublicKey();
            if (pk instanceof DSAPublicKey) {
            	DSAPublicKey rsapk = (DSAPublicKey) pk;
				assertEquals(rsapk.getAlgorithm(), "DSA");
			} else {
				assertTrue("Public key is not DSA", false);
			}
            assertTrue("CA is not valid for the specified duration.",cert.getNotAfter().after(new Date(new Date().getTime()+10*364*24*60*60*1000L)) && cert.getNotAfter().before(new Date(new Date().getTime()+10*366*24*60*60*1000L)));
            ret = true;
            
            // Test to generate a certificate request from the CA
            Collection cachain = info.getCertificateChain();
            byte[] request = TestTools.getCAAdminSession().makeRequest(admin, info.getCAId(), cachain, false, null, false);
            PKCS10RequestMessage msg = new PKCS10RequestMessage(request);
            assertEquals("CN=TESTDSA", msg.getRequestDN());
        } catch (CAExistsException pee) {
            log.info("CA exists.");
        }

        assertTrue("Creating DSA CA failed", ret);
        log.trace("<test11AddDSACA()");
    }

}