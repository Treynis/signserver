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

package org.ejbca.core.ejb.ca.caadmin;

import java.util.ArrayList;
import java.util.Date;

import javax.naming.Context;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.caadmin.X509CAInfo;
import org.ejbca.core.model.ca.catoken.CATokenConstants;
import org.ejbca.core.model.ca.catoken.CATokenInfo;
import org.ejbca.core.model.ca.catoken.CATokenOfflineException;
import org.ejbca.core.model.ca.catoken.ICAToken;
import org.ejbca.core.model.ca.catoken.SoftCATokenInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.util.CertTools;

/**
 * Tests for the Soft catoken removal functionality.
 * 
 * A CA keystore can be exported using CAAdminSessionBean.exportCAKeyStore().
 * A CA keystore can be removed using CAAdminSessionBean.removeCAKeyStore().
 * A CA keystore can be restored using CAAdminSessionBean.restoreCAKeyStore().
 * 
 * @author Markus Kilås
 * @version $Id: TestCAKeystoreExportRemoveRestore.java 8252 2009-11-05 09:36:08Z anatom $
 */
public class TestCAKeystoreExportRemoveRestore extends TestCase  {
    
	private static Logger log = Logger.getLogger(TestCAKeystoreExportRemoveRestore.class);
    private static Context ctx;
    private static ICAAdminSessionRemote caadminsession;

    
    public TestCAKeystoreExportRemoveRestore(String name) {
        super(name);
        CertTools.installBCProviderIfNotAvailable();
    }
    
    /**
     * Setup test environment.
     *
     * @throws Exception
     */
    protected void setUp() throws Exception {
        log.trace(">setUp()");
		ctx = org.ejbca.core.ejb.InitialContextBuilder.getInstance().getInitialContext();
		ICAAdminSessionHome home = (org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(ctx.lookup("CAAdminSession"), org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionHome.class );            
		caadminsession = home.create();          
		log.trace("<setUp()");
    }
    
    /**
     * Tear down test environment. Does nothing.
     *
     * @throws Exception
     */
    protected void tearDown() throws Exception {
    }
    
    /**
     * Tries to export, remove and restore with a CA that is using SHA1withRSA as signature algorithm.
     * 
     * @throws Exception
     */
	public void test01ExportRemoveRestoreSHA1WithRSA() throws Exception {
	    log.trace("<test01ExportRemoveRestoreSHA1WithRSA()");
        
	    SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
        catokeninfo.setSignatureAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo.setSignKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo.setSignKeySpec("2048");
        catokeninfo.setEncryptionAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo.setEncKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo.setEncKeySpec("2048");
        exportRemoveRestore(catokeninfo);
        
	    log.trace("<test01ExportRemoveRestoreSHA1WithRSA()");
	}
	
	/**
     * Tries to export, remove and restore with a CA that is using SHA256withRSA and 1024 bit RSA key for 
     * signing but SHA1WithRSA and a RSA 2048 bit key for encryption.
     * 
     * @throws Exception
     */
	public void test02ExportRemoveRestoreSHA256WithRSAForSigning() throws Exception {
	    log.trace(">test02ExportRemoveRestoreSHA256WithRSAForSigning()");
        
	    SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
        catokeninfo.setSignatureAlgorithm(CATokenConstants.SIGALG_SHA256_WITH_RSA);
        catokeninfo.setSignKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo.setSignKeySpec("1024");
        catokeninfo.setEncryptionAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo.setEncKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo.setEncKeySpec("2048");
        exportRemoveRestore(catokeninfo);
        
	    log.trace("<test02ExportRemoveRestoreSHA256WithRSAForSigning()");
	}
	
	/**
     * Tries to export, remove and restore with a CA that is using SHA1withECDSA and a key using the prime192v1 curve for 
     * signing but SHA256WithRSA and a 1024 bit RSA key for encryption.
     * 
     * @throws Exception
     */
	public void test03ExportRemoveRestoreSHA1WithECDSAForSigning() throws Exception {
	    log.trace(">test03ExportRemoveRestoreSHA1WithECDSAForSigning()");
        
	    SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
        catokeninfo.setSignatureAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_ECDSA);
        catokeninfo.setSignKeyAlgorithm(CATokenConstants.KEYALGORITHM_ECDSA);
        catokeninfo.setSignKeySpec("prime192v1");
        catokeninfo.setEncryptionAlgorithm(CATokenConstants.SIGALG_SHA256_WITH_RSA);
        catokeninfo.setEncKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo.setEncKeySpec("1024");
        exportRemoveRestore(catokeninfo);
        
	    log.trace("<test03ExportRemoveRestoreSHA1WithECDSAForSigning()");
	}
	
	/**
     * Tries to export, remove and restore with a CA that is using SHA1withDSA and a 1024 bit DSA key for 
     * signing but SHA256WithRSA and a 1024 bit RSA key for encryption.
     * 
     * @throws Exception
     */
	public void test04ExportRemoveRestoreSHA1WithDSAForSigning() throws Exception {
	    log.trace(">test04ExportRemoveRestoreSHA1WithDSAForSigning()");
        
	    SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
        catokeninfo.setSignatureAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_DSA);
        catokeninfo.setSignKeyAlgorithm(CATokenConstants.KEYALGORITHM_DSA);
        catokeninfo.setSignKeySpec("1024");
        catokeninfo.setEncryptionAlgorithm(CATokenConstants.SIGALG_SHA256_WITH_RSA);
        catokeninfo.setEncKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo.setEncKeySpec("1024");
        exportRemoveRestore(catokeninfo);
        
	    log.trace("<test04ExportRemoveRestoreSHA1WithDSAForSigning()");
	}
	
	/**
	 * Tests that it is not possible to accidentally restore the wrong keystore.
	 * @throws Exception
	 */
	public void test05RestoreWrong() throws Exception {
		log.trace(">test05RestoreWrong()");

		Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
		String capassword = "foo123";
		byte[] keystorebytes1 = null;
		byte[] keystorebytes2 = null;
		byte[] keystorebytes3 = null;
		
		// CA using SHA1withRSA and 2048 bit RSA KEY
		String caname1 = "TestExportRemoveRestoreCA1";
		SoftCATokenInfo catokeninfo1 = new SoftCATokenInfo();
        catokeninfo1.setSignatureAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo1.setSignKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo1.setSignKeySpec("2048");
        catokeninfo1.setEncryptionAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo1.setEncKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo1.setEncKeySpec("2048");
        X509CAInfo cainfo1 = getNewCAInfo(caname1, catokeninfo1);
        
        // This CA uses DSA instead
        String caname2 = "TestExportRemoveRestoreCA2";
		SoftCATokenInfo catokeninfo2 = new SoftCATokenInfo();
        catokeninfo2.setSignatureAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_DSA);
        catokeninfo2.setSignKeyAlgorithm(CATokenConstants.KEYALGORITHM_DSA);
        catokeninfo2.setSignKeySpec("1024");
        catokeninfo2.setEncryptionAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo2.setEncKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo2.setEncKeySpec("1024");
        X509CAInfo cainfo2 = getNewCAInfo(caname2, catokeninfo2);
        
        // This CA uses RSA but with 1024 bits
        String caname3 = "TestExportRemoveRestoreCA3";
		SoftCATokenInfo catokeninfo3 = new SoftCATokenInfo();
        catokeninfo3.setSignatureAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo3.setSignKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo3.setSignKeySpec("1024");
        catokeninfo3.setEncryptionAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo3.setEncKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo3.setEncKeySpec("1024");
        X509CAInfo cainfo3 = getNewCAInfo(caname3, catokeninfo3);
        
    	// Remove CAs if they already exists
		try {
			caadminsession.removeCA(admin, cainfo1.getCAId());
		} catch (Exception ignored) { }
		try {
			caadminsession.removeCA(admin, cainfo2.getCAId());
		} catch (Exception ignored) { }
		try {
			caadminsession.removeCA(admin, cainfo3.getCAId());
		} catch (Exception ignored) { }
		
		// Create CAs
		try {
			caadminsession.createCA(admin, cainfo1);
		} catch (Exception e) {
			log.error("createCA", e);
			fail("createCA: " + e.getMessage());
		}
		try {
			caadminsession.createCA(admin, cainfo2);
		} catch (Exception e) {
			log.error("createCA", e);
			fail("createCA: " + e.getMessage());
		}
		try {
			caadminsession.createCA(admin, cainfo3);
		} catch (Exception e) {
			log.error("createCA", e);
			fail("createCA: " + e.getMessage());
		}
		
		// Export keystores
		try {
			keystorebytes1 = caadminsession.exportCAKeyStore(admin, caname1, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
		} catch (Exception e) {
			log.error("exportCAKeyStore", e);
			fail("exportCAKeyStore: " + e.getMessage());
		}
		try {
			keystorebytes2 = caadminsession.exportCAKeyStore(admin, caname2, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
		} catch (Exception e) {
			log.error("exportCAKeyStore", e);
			fail("exportCAKeyStore: " + e.getMessage());
		}
		try {
			keystorebytes3 = caadminsession.exportCAKeyStore(admin, caname3, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
		} catch (Exception e) {
			log.error("exportCAKeyStore", e);
			fail("exportCAKeyStore: " + e.getMessage());
		}
		
		// Remove keystore from CA1
		try {
			caadminsession.removeCAKeyStore(admin, caname1);
		} catch (Exception e) {
			log.error("removeKeyStores", e);
			fail("removeKeyStores: " + e.getMessage());
		}
		
		// Try to restore with wrong keystore
		try {
			caadminsession.restoreCAKeyStore(admin, caname1, keystorebytes2, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
			fail("Should not be possible to restore with a keystore with different parameters");
		} catch (Exception e) {
			// OK
		}
		try {
			caadminsession.restoreCAKeyStore(admin, caname1, keystorebytes3, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
			fail("Should not be possible to restore with a keystore with different parameters");
		} catch (Exception e) {
			// OK
		}
		
		// Finally try with the right keystore to see that it works
		try {
			caadminsession.restoreCAKeyStore(admin, caname1, keystorebytes1, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
		} catch (Exception e) {
			log.error("restoreCAKeyStore", e);
			fail("restoreCAKeyStore: " + e.getMessage());
		}
		
		// Clean up
		try {
			caadminsession.removeCA(admin, cainfo1.getCAId());
		} catch (Exception e) {
			log.error("removeCA", e);
			fail("removeCA: " + e.getMessage());
		}
		try {
			caadminsession.removeCA(admin, cainfo2.getCAId());
		} catch (Exception e) {
			log.error("removeCA", e);
			fail("removeCA: " + e.getMessage());
		}
		try {
			caadminsession.removeCA(admin, cainfo3.getCAId());
		} catch (Exception e) {
			log.error("removeCA", e);
			fail("removeCA: " + e.getMessage());
		}

		log.trace("<test05RestoreWrong()");
	}
	
	/**
	 * Tests that it is not possible to restore a CA that has not been removed
	 *  (or at least not one with an active CA token).
	 * @throws Exception
	 */
	public void test06RestoreNotRemoved() throws Exception {
		log.trace(">test06RestoreNotRemoved()");

		Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
		String capassword = "foo123";
		byte[] keystorebytes1 = null;
		
		// CA1
		String caname1 = "TestExportRemoveRestoreCA1";
		SoftCATokenInfo catokeninfo1 = new SoftCATokenInfo();
        catokeninfo1.setSignatureAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo1.setSignKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo1.setSignKeySpec("2048");
        catokeninfo1.setEncryptionAlgorithm(CATokenConstants.SIGALG_SHA1_WITH_RSA);
        catokeninfo1.setEncKeyAlgorithm(CATokenConstants.KEYALGORITHM_RSA);
        catokeninfo1.setEncKeySpec("2048");
        X509CAInfo cainfo1 = getNewCAInfo(caname1, catokeninfo1);
        
        // Remove if they already exists
		try {
			caadminsession.removeCA(admin, cainfo1.getCAId());
		} catch (Exception ignored) { }
		
		// Create CAs
		try {
			caadminsession.createCA(admin, cainfo1);
		} catch (Exception e) {
			log.error("createCA", e);
			fail("createCA: " + e.getMessage());
		}
        
		// Export keystore
		try {
			keystorebytes1 = caadminsession.exportCAKeyStore(admin, caname1, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
		} catch (Exception e) {
			log.error("exportCAKeyStore", e);
			fail("exportCAKeyStore: " + e.getMessage());
		}
		
		// Just created CA should be active
		CAInfo info = caadminsession.getCAInfo(admin, caname1);
		assertEquals("active token", ICAToken.STATUS_ACTIVE, info.getCATokenInfo().getCATokenStatus());
		
		// Try to restore the first CA even do it has not been removed
        try {
        	caadminsession.restoreCAKeyStore(admin, caname1, keystorebytes1, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
        	fail("Should fail when trying to restore an online CA");
        } catch(Exception e) {
			// OK
        }
		
		// Clean up
		try {
			caadminsession.removeCA(admin, cainfo1.getCAId());
		} catch (Exception e) {
			log.error("removeCA", e);
			fail("removeCA: " + e.getMessage());
		}
		
		log.trace("<test06RestoreNotRemoved()");
	}

	/**
	 * Does export, remove and restore and performs tests.
	 * @param catokeninfo Information with algorithm and key selections
	 * @throws Exception
	 */
	private void exportRemoveRestore(CATokenInfo catokeninfo) throws Exception {
		byte[] keystorebytes = null;
        String caname = "TestExportRemoveRestoreCA1";
        String capassword = "foo123";
        String keyFingerPrint = null;
        
        X509CAInfo cainfo = getNewCAInfo(caname, catokeninfo);
    	Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
    	
    	
    	// Remove if it already exists
		try {
			caadminsession.removeCA(admin, cainfo.getCAId());
		} catch (Exception ignored) { }
		
		// Create CA
		try {
			caadminsession.createCA(admin, cainfo);
		} catch (Exception e) {
			log.error("createCA", e);
			fail("createCA: " + e.getMessage());
		}
		
		try {
			keyFingerPrint = caadminsession.getKeyFingerPrint(admin, caname);
		} catch (Exception e) {
			log.error("getKeyFingerPrint", e);
			fail("getKeyFingerPrint: " + e.getMessage());
		}
		
		try {
			keystorebytes = caadminsession.exportCAKeyStore(admin, caname, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
			
		} catch (Exception e) {
			log.error("exportCAKeyStore", e);
			fail("exportCAKeyStore: " + e.getMessage());
		}
		
		// Remove the ca token soft keystore
		try {
			caadminsession.removeCAKeyStore(admin, caname);
		} catch (Exception e) {
			log.error("removeKeyStores", e);
			fail("removeKeyStores: " + e.getMessage());
		}
		
		// The token should now be offline
		CAInfo info = caadminsession.getCAInfo(admin, caname);
		assertEquals("offline token", ICAToken.STATUS_OFFLINE, info.getCATokenInfo().getCATokenStatus());
		
		// Should not be possible to activate
		caadminsession.activateCAToken(admin, cainfo.getCAId(), capassword);	
		info = caadminsession.getCAInfo(admin, caname);
		assertEquals("offline token", ICAToken.STATUS_OFFLINE, info.getCATokenInfo().getCATokenStatus());
		
		// Should not be possible to export
		try {
			byte[] emptyBytes = caadminsession.exportCAKeyStore(admin, caname, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
			assertEquals("empty keystore", 0, emptyBytes.length);
		} catch (Exception ignored) {
			// OK
		}
		
		try {
			String emptyFingerprint = caadminsession.getKeyFingerPrint(admin, caname);
			log.error("Got fingerprint: " + emptyFingerprint);
			fail("Should not have got a fingerprint");
		} catch(Exception e) {
			if(e.getCause() instanceof CATokenOfflineException) {
				// OK
			} else {
				log.error("getKeyFingerPrint", e);
				fail("getKeyFingerPrint: " + e.getMessage());
			}
		}
		
		// Restore keystore
		try {
			caadminsession.restoreCAKeyStore(admin, caname, keystorebytes, capassword, capassword, "SignatureKeyAlias", "EncryptionKeyAlias");
		} catch (Exception e) {
			log.error("restoreKeyStores", e);
			fail("restoreKeyStores: " + e.getMessage());
		}
		
		// Compare fingerprints
		try {
			String restoredFingerprint = caadminsession.getKeyFingerPrint(admin, caname);
			assertEquals("restored fingerprint", keyFingerPrint, restoredFingerprint);
		} catch(Exception e) {
			log.error("getKeyFingerPrint2", e);
			fail("getKeyFingerPrint2: " + e.getMessage());
		}
		
		// Clean up
		try {
			caadminsession.removeCA(admin, cainfo.getCAId());
		} catch (Exception e) {
			log.error("removeCA", e);
			fail("removeCA: " + e.getMessage());
		}
	}

    /**
     * Creates a CAinfo for testing.
     *  
     * @param caname The name this CA-info will be assigned
     * @param catokeninfo The tokeninfo for this CA-info
     * @return The new X509CAInfo for testing.
     */
	private X509CAInfo getNewCAInfo(String caname, CATokenInfo catokeninfo) {
		X509CAInfo cainfo = new X509CAInfo("CN="+caname,
        		caname, SecConst.CA_ACTIVE, new Date(), 
                "", SecConst.CERTPROFILE_FIXED_ROOTCA,
                365,
                new Date(System.currentTimeMillis()+364*24*3600*1000), // Expiretime
                CAInfo.CATYPE_X509,
                CAInfo.SELFSIGNED,
                null, // certificatechain
                catokeninfo,
                "Used for testing CA keystore export, remove and restore",
                -1, null, // revokationreason, revokationdate
                new ArrayList(), // PolicyId
                24, // CRLPeriod
                0, // CRLIssuePeriod
                10, // CRLOverlapTime
                0, //DeltaCRLOverlapTime                
                new ArrayList(),
                true, // Authority Key Identifier
                false, // Authority Key Identifier Critical
                true, // CRL Number
                false, // CRL Number Critical
                "", // Default CRL Dist Point
                "", // Default CRL Issuer
                "", // Default OCSP Service Locator  
                null, // defaultfreshestcrl
                true, // Finish User
                new ArrayList(), //extendedcaservices
                false, // use default utf8 settings
                new ArrayList(), // Approvals Settings
                1, // Number of Req approvals
                false, // Use UTF8 subject DN by default
                true, // Use LDAP DN order by default
                false, // Use CRL Distribution Point on CRL
                false,  // CRL Distribution Point on CRL critical
                true // include in health check
        );
		return cainfo;
	}
}
