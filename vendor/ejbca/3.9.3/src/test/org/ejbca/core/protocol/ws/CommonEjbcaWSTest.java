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
package org.ejbca.core.protocol.ws; 

import java.io.File;
import java.net.URL;
import java.rmi.RemoteException;
import java.security.KeyPair;
import java.security.KeyPairGenerator;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.SecureRandom;
import java.security.cert.CertStore;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;
import java.util.List;
import java.util.Random;

import javax.ejb.CreateException;
import javax.naming.Context;
import javax.naming.NamingException;
import javax.xml.namespace.QName;

import junit.framework.TestCase;

import org.bouncycastle.asn1.DERSet;
import org.bouncycastle.cms.CMSSignedData;
import org.bouncycastle.jce.PKCS10CertificationRequest;
import org.bouncycastle.util.encoders.Hex;
import org.ejbca.core.ejb.approval.IApprovalSessionHome;
import org.ejbca.core.ejb.approval.IApprovalSessionRemote;
import org.ejbca.core.ejb.authorization.IAuthorizationSessionHome;
import org.ejbca.core.ejb.authorization.IAuthorizationSessionRemote;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionHome;
import org.ejbca.core.ejb.ca.caadmin.ICAAdminSessionRemote;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.ejb.hardtoken.IHardTokenSessionHome;
import org.ejbca.core.ejb.hardtoken.IHardTokenSessionRemote;
import org.ejbca.core.ejb.ra.IUserAdminSessionHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionHome;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionRemote;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.approval.ApprovalDataVO;
import org.ejbca.core.model.approval.approvalrequests.TestRevocationApproval;
import org.ejbca.core.model.authorization.AdminEntity;
import org.ejbca.core.model.authorization.AdminGroup;
import org.ejbca.core.model.ca.caadmin.CAExistsException;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.caadmin.CVCCAInfo;
import org.ejbca.core.model.ca.catoken.CATokenConstants;
import org.ejbca.core.model.ca.catoken.CATokenInfo;
import org.ejbca.core.model.ca.catoken.SoftCATokenInfo;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.ca.certificateprofiles.EndUserCertificateProfile;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.ca.publisher.CustomPublisherContainer;
import org.ejbca.core.model.ca.publisher.DummyCustomPublisher;
import org.ejbca.core.model.ca.publisher.PublisherQueueData;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.model.ra.raadmin.GlobalConfiguration;
import org.ejbca.core.protocol.ws.client.gen.AlreadyRevokedException_Exception;
import org.ejbca.core.protocol.ws.client.gen.ApprovalException_Exception;
import org.ejbca.core.protocol.ws.client.gen.AuthorizationDeniedException_Exception;
import org.ejbca.core.protocol.ws.client.gen.CADoesntExistsException_Exception;
import org.ejbca.core.protocol.ws.client.gen.Certificate;
import org.ejbca.core.protocol.ws.client.gen.CertificateResponse;
import org.ejbca.core.protocol.ws.client.gen.EjbcaException_Exception;
import org.ejbca.core.protocol.ws.client.gen.EjbcaWS;
import org.ejbca.core.protocol.ws.client.gen.EjbcaWSService;
import org.ejbca.core.protocol.ws.client.gen.ErrorCode;
import org.ejbca.core.protocol.ws.client.gen.HardTokenDataWS;
import org.ejbca.core.protocol.ws.client.gen.HardTokenDoesntExistsException_Exception;
import org.ejbca.core.protocol.ws.client.gen.HardTokenExistsException_Exception;
import org.ejbca.core.protocol.ws.client.gen.KeyStore;
import org.ejbca.core.protocol.ws.client.gen.NameAndId;
import org.ejbca.core.protocol.ws.client.gen.NotFoundException_Exception;
import org.ejbca.core.protocol.ws.client.gen.PinDataWS;
import org.ejbca.core.protocol.ws.client.gen.RevokeStatus;
import org.ejbca.core.protocol.ws.client.gen.TokenCertificateRequestWS;
import org.ejbca.core.protocol.ws.client.gen.TokenCertificateResponseWS;
import org.ejbca.core.protocol.ws.client.gen.UserDataVOWS;
import org.ejbca.core.protocol.ws.client.gen.UserMatch;
import org.ejbca.core.protocol.ws.client.gen.WaitingForApprovalException_Exception;
import org.ejbca.core.protocol.ws.common.CertificateHelper;
import org.ejbca.core.protocol.ws.common.HardTokenConstants;
import org.ejbca.core.protocol.ws.common.IEjbcaWS;
import org.ejbca.core.protocol.ws.common.KeyStoreHelper;
import org.ejbca.cvc.AuthorizationRoleEnum;
import org.ejbca.cvc.CAReferenceField;
import org.ejbca.cvc.CVCAuthenticatedRequest;
import org.ejbca.cvc.CVCObject;
import org.ejbca.cvc.CVCertificate;
import org.ejbca.cvc.CardVerifiableCertificate;
import org.ejbca.cvc.CertificateGenerator;
import org.ejbca.cvc.CertificateParser;
import org.ejbca.cvc.HolderReferenceField;
import org.ejbca.ui.cli.batch.BatchMakeP12;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.TestTools;
import org.ejbca.util.dn.DnComponents;
import org.ejbca.util.keystore.KeyTools;

public class CommonEjbcaWSTest extends TestCase {
	
	protected static EjbcaWS ejbcaraws;

	protected IHardTokenSessionRemote hardTokenSession;
    protected static IHardTokenSessionHome hardTokenSessionHome;
	protected ICertificateStoreSessionRemote certStoreSession;
    protected static ICertificateStoreSessionHome certStoreSessionHome;
	protected IRaAdminSessionRemote raAdminSession;
    protected static IRaAdminSessionHome raAdminSessionHome;
    protected IUserAdminSessionRemote userAdminSession;
    protected static IUserAdminSessionHome userAdminSessionHome;
    protected ICAAdminSessionRemote caAdminSession;
    protected static ICAAdminSessionHome caAdminSessionHome;
    protected IAuthorizationSessionRemote authSession;
    protected static IAuthorizationSessionHome authSessionHome;
    protected IApprovalSessionRemote approvalSession;
    protected static IApprovalSessionHome approvalSessionHome;
    
    protected final static String wsTestAdminUsername = "wstest";
	protected final static String wsTestNonAdminUsername = "wsnonadmintest";
    protected static Admin intAdmin = new Admin(Admin.TYPE_INTERNALUSER);
	protected final static String HOSTNAME = "localhost";
	private static final String BADCANAME = "BadCaName";
    
    protected String getAdminCAName() {
    	return "AdminCA1";
    }
    protected String getCVCCAName() {
    	return "WSTESTDVCA";
    }
    
	protected void setUpAdmin() throws Exception {
		super.setUp();
		CertTools.installBCProvider();
		
        if(new File("p12/wstest.jks").exists()){
        	

        	String urlstr = "https://" + HOSTNAME + ":8443/ejbca/ejbcaws/ejbcaws?wsdl";

        	System.out.println("Contacting webservice at " + urlstr);                       

        	System.setProperty("javax.net.ssl.trustStore","p12/wstest.jks");
        	System.setProperty("javax.net.ssl.trustStorePassword","foo123");  

        	System.setProperty("javax.net.ssl.keyStore","p12/wstest.jks");
        	System.setProperty("javax.net.ssl.keyStorePassword","foo123");      



        	QName qname = new QName("http://ws.protocol.core.ejbca.org/", "EjbcaWSService");
        	EjbcaWSService service = new EjbcaWSService(new URL(urlstr),qname);
        	ejbcaraws = service.getEjbcaWSPort();
        }
	}
	
	protected void setUpNonAdmin() throws Exception {
		super.setUp();
		CertTools.installBCProvider();
		
        if(new File("p12/wsnonadmintest.jks").exists()){

        	String urlstr = "https://" + HOSTNAME + ":8443/ejbca/ejbcaws/ejbcaws?wsdl";

        	System.out.println("Contacting webservice at " + urlstr);                       

        	System.setProperty("javax.net.ssl.trustStore","p12/wsnonadmintest.jks");
        	System.setProperty("javax.net.ssl.trustStorePassword","foo123");  

        	System.setProperty("javax.net.ssl.keyStore","p12/wsnonadmintest.jks");
        	System.setProperty("javax.net.ssl.keyStorePassword","foo123");      



        	QName qname = new QName("http://ws.protocol.core.ejbca.org/", "EjbcaWSService");
        	EjbcaWSService service = new EjbcaWSService(new URL(urlstr),qname);
        	ejbcaraws = service.getEjbcaWSPort();
        }
	}




	protected void tearDown() throws Exception {
		super.tearDown();
		
		
	}


	protected Context getInitialContext() throws NamingException {
        Context ctx = new javax.naming.InitialContext();
        return ctx;
    }

	protected IHardTokenSessionRemote getHardTokenSession() throws RemoteException, CreateException, NamingException {
	    if (hardTokenSession == null) {
	        if (hardTokenSessionHome == null) {
	            Context jndiContext = getInitialContext();
	            Object obj1 = jndiContext.lookup(IHardTokenSessionHome.JNDI_NAME);
	            hardTokenSessionHome = (IHardTokenSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, IHardTokenSessionHome.class);
	        }
	        hardTokenSession = hardTokenSessionHome.create();
	    }
	    return hardTokenSession;
	}

	
	protected ICertificateStoreSessionRemote getCertStore() throws RemoteException, CreateException, NamingException{
        if (certStoreSession == null) {
            if (certStoreSessionHome == null) {
                Context jndiContext = getInitialContext();
                Object obj1 = jndiContext.lookup(ICertificateStoreSessionHome.JNDI_NAME);
                certStoreSessionHome = (ICertificateStoreSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, ICertificateStoreSessionHome.class);

            }

            certStoreSession = certStoreSessionHome.create();            
        }
        return certStoreSession;
	}
	
	protected IRaAdminSessionRemote getRAAdmin() throws RemoteException, CreateException, NamingException{
        if (raAdminSession == null) {
            if (raAdminSessionHome == null) {
                Context jndiContext = getInitialContext();
                Object obj1 = jndiContext.lookup(IRaAdminSessionHome.JNDI_NAME);
                raAdminSessionHome = (IRaAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, IRaAdminSessionHome.class);

            }

            raAdminSession = raAdminSessionHome.create();            
        }
        return raAdminSession;
	}
	
	protected IUserAdminSessionRemote getUserAdminSession() throws RemoteException, CreateException, NamingException{
        if (userAdminSession == null) {
            if (userAdminSessionHome == null) {
                Context jndiContext = getInitialContext();
                Object obj1 = jndiContext.lookup(IUserAdminSessionHome.JNDI_NAME);
                userAdminSessionHome = (IUserAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, IUserAdminSessionHome.class);
            }

            userAdminSession = userAdminSessionHome.create();            
        }
        return userAdminSession;
	}
	
	protected ICAAdminSessionRemote getCAAdminSession() throws RemoteException, CreateException, NamingException{
        if (caAdminSession == null) {
            if (caAdminSessionHome == null) {
                Context jndiContext = getInitialContext();
                Object obj1 = jndiContext.lookup(ICAAdminSessionHome.JNDI_NAME);
                caAdminSessionHome = (ICAAdminSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, ICAAdminSessionHome.class);
            }

            caAdminSession = caAdminSessionHome.create();            
        }
        return caAdminSession;
	}
	
	protected IAuthorizationSessionRemote getAuthSession() throws RemoteException, CreateException, NamingException{
        if (authSession == null) {
            if (authSessionHome == null) {
                Context jndiContext = getInitialContext();
                Object obj1 = jndiContext.lookup(IAuthorizationSessionHome.JNDI_NAME);
                authSessionHome = (IAuthorizationSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, IAuthorizationSessionHome.class);
            }

            authSession = authSessionHome.create();            
        }
        return authSession;
	}
	
	protected IApprovalSessionRemote getApprovalSession() throws RemoteException, CreateException, NamingException {
		if (approvalSession == null) {
			if (approvalSessionHome == null) {
                Context jndiContext = getInitialContext();
                Object obj1 = jndiContext.lookup(IApprovalSessionHome.JNDI_NAME);
                approvalSessionHome = (IApprovalSessionHome) javax.rmi.PortableRemoteObject.narrow(obj1, IApprovalSessionHome.class);
			}
			approvalSession = approvalSessionHome.create();
		}
		return approvalSession;
	}
	
	protected void test00SetupAccessRights() throws Exception{
		boolean userAdded = false;
		
		if(!getUserAdminSession().existsUser(intAdmin, wsTestAdminUsername)){
			UserDataVO user1 = new UserDataVO();
			user1.setUsername(wsTestAdminUsername);
			user1.setPassword("foo123");			
			user1.setDN("CN=wstest");			
			CAInfo cainfo = getCAAdminSession().getCAInfo(intAdmin, getAdminCAName());
			user1.setCAId(cainfo.getCAId());
			user1.setEmail(null);
			user1.setSubjectAltName(null);
			user1.setStatus(UserDataConstants.STATUS_NEW);
			user1.setTokenType(SecConst.TOKEN_SOFT_JKS);
			user1.setEndEntityProfileId(SecConst.EMPTY_ENDENTITYPROFILE);
			user1.setCertificateProfileId(SecConst.CERTPROFILE_FIXED_ENDUSER);
			user1.setType(65);
			
			getUserAdminSession().addUser(intAdmin, user1, true);
			userAdded = true;

			boolean adminExists = false;
			AdminGroup admingroup = getAuthSession().getAdminGroup(intAdmin, AdminGroup.TEMPSUPERADMINGROUP);
			Iterator iter = admingroup.getAdminEntities().iterator();
			while(iter.hasNext()){
				AdminEntity adminEntity = (AdminEntity) iter.next();
				if(adminEntity.getMatchValue().equals(wsTestAdminUsername)){
					adminExists = true;
				}
			}
			
			if(!adminExists){
				ArrayList list = new ArrayList();
				list.add(new AdminEntity(AdminEntity.WITH_COMMONNAME,AdminEntity.TYPE_EQUALCASE,wsTestAdminUsername,cainfo.getCAId()));
				getAuthSession().addAdminEntities(intAdmin, AdminGroup.TEMPSUPERADMINGROUP, list);
				getAuthSession().forceRuleUpdate(intAdmin);
			}
			
		}
		
		if(!getUserAdminSession().existsUser(intAdmin, wsTestNonAdminUsername)){
			UserDataVO user1 = new UserDataVO();
			user1.setUsername(wsTestNonAdminUsername);
			user1.setPassword("foo123");			
			user1.setDN("CN=wsnonadmintest");			
			CAInfo cainfo = getCAAdminSession().getCAInfo(intAdmin, getAdminCAName());
			user1.setCAId(cainfo.getCAId());
			user1.setEmail(null);
			user1.setSubjectAltName(null);
			user1.setStatus(UserDataConstants.STATUS_NEW);
			user1.setTokenType(SecConst.TOKEN_SOFT_JKS);
			user1.setEndEntityProfileId(SecConst.EMPTY_ENDENTITYPROFILE);
			user1.setCertificateProfileId(SecConst.CERTPROFILE_FIXED_ENDUSER);
			user1.setType(1);
			
			getUserAdminSession().addUser(intAdmin, user1, true);
			userAdded = true;	
		}
		
		if(userAdded){
			BatchMakeP12 batch = new BatchMakeP12();
			batch.setMainStoreDir("p12");
			batch.createAllNew();
		}

	}
	

	protected void test01EditUser(boolean performSetup) throws Exception{
		if(performSetup){
		  setUpAdmin();
		}
		// Test to add a user.
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setUsername("WSTESTUSER1");
		user1.setPassword("foo123");
		user1.setClearPwd(true);
		user1.setSubjectDN("CN=WSTESTUSER1");
		user1.setCaName(getAdminCAName());
		user1.setEmail(null);
		user1.setSubjectAltName(null);
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("USERGENERATED");
		user1.setEndEntityProfileName("EMPTY");
		user1.setCertificateProfileName("ENDUSER");

            ejbcaraws.editUser(user1);

        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER1");
		
	 	List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 1);
		UserDataVOWS userdata = userdatas.get(0);
		assertTrue(userdata.getUsername().equals("WSTESTUSER1"));
		assertTrue(userdata.getPassword() == null);
		assertTrue(!userdata.isClearPwd());
        assertTrue(userdata.getSubjectDN().equals("CN=WSTESTUSER1"));
        assertTrue(userdata.getCaName().equals(getAdminCAName()));
        assertTrue(userdata.getSubjectAltName() == null);
        assertTrue(userdata.getEmail() == null);
        assertTrue(userdata.getCertificateProfileName().equals("ENDUSER"));
        assertTrue(userdata.getEndEntityProfileName().equals("EMPTY"));
        assertTrue(userdata.getTokenType().equals("USERGENERATED"));        
        assertTrue(userdata.getStatus() == 10);
        
        // Edit the user
        userdata.setSubjectDN("CN=WSTESTUSER1,O=Test");
        ejbcaraws.editUser(userdata);
        List<UserDataVOWS> userdatas2 = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas2 != null);
		assertTrue(userdatas2.size() == 1);  
		UserDataVOWS userdata2 = userdatas.get(0);
        assertTrue(userdata2.getSubjectDN().equals("CN=WSTESTUSER1,O=Test"));
		
	}
	

	protected void test02findUser(boolean performSetup) throws Exception{
		if(performSetup){
		  setUpAdmin();
		}
		
		//Nonexisting users should return null		
		UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER2");		
		List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 0);
		
		// Find an exising user
		usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER1");	
		
        List<UserDataVOWS> userdatas2 = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas2 != null);
		assertTrue(userdatas2.size() == 1);
		
		// Find by O
		usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_ORGANIZATION);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_BEGINSWITH);
        usermatch.setMatchvalue("Te");			
        List<UserDataVOWS> userdatas3 = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas3 != null);
		assertTrue(userdatas3.size() == 1);
		assertTrue(userdatas3.get(0).getSubjectDN().equals("CN=WSTESTUSER1,O=Test"));
		
		// Find by subjectDN pattern
		usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_DN);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_CONTAINS);
        usermatch.setMatchvalue("WSTESTUSER1");				
		List<UserDataVOWS> userdatas4 = ejbcaraws.findUser(usermatch);
		assertNotNull(userdatas4 != null);
		assertEquals(1, userdatas4.size());
		assertEquals("CN=WSTESTUSER1,O=Test", userdatas4.get(0).getSubjectDN());
		
		usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_ENDENTITYPROFILE);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("EMPTY");			
        List<UserDataVOWS> userdatas5 = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas5 != null);
		assertTrue(userdatas5.size() > 0);
		
		usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_CERTIFICATEPROFILE);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("ENDUSER");		
		List<UserDataVOWS> userdatas6 = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas6 != null);
		assertTrue(userdatas6.size() > 0);

		usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_CA);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue(getAdminCAName());			
        List<UserDataVOWS> userdatas7 = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas7 != null);
		assertTrue(userdatas7.size() > 0);

		usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_TOKEN);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("USERGENERATED");			
		List<UserDataVOWS> userdatas8 = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas8 != null);
		assertTrue(userdatas8.size() > 0);
	}
	

	protected void test03GeneratePkcs10(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		
		KeyPair keys = KeyTools.genKeys("1024", CATokenConstants.KEYALGORITHM_RSA);
		PKCS10CertificationRequest  pkcs10 = new PKCS10CertificationRequest("SHA1WithRSA",
                CertTools.stringToBcX509Name("CN=NOUSED"), keys.getPublic(), new DERSet(), keys.getPrivate());
		
		CertificateResponse certenv =  ejbcaraws.pkcs10Request("WSTESTUSER1","foo123",new String(Base64.encode(pkcs10.getEncoded())),null, CertificateHelper.RESPONSETYPE_CERTIFICATE);
		
		assertNotNull(certenv);
		
		X509Certificate cert = (X509Certificate) CertificateHelper.getCertificate(certenv.getData()); 
		
		assertNotNull(cert);
		
		assertEquals("CN=WSTESTUSER1,O=Test", cert.getSubjectDN().toString());
		
	}
	
	private static final String crmf = "MIIBdjCCAXIwgdkCBQCghr4dMIHPgAECpRYwFDESMBAGA1UEAxMJdW5kZWZpbmVk"+
	"poGfMA0GCSqGSIb3DQEBAQUAA4GNADCBiQKBgQCi6+Bmo+0I/ye8k6B6BkhXgv03"+
	"1jEeD3mEuvjIEZUmmdt2RBvW2qfJzqXV8dsI1HZT4fZqo8SBsrYls4AC7HooWI6g"+
	"DjSyd3kFcb5HP+qnNlz6De/Ab+qAF1rLJhfb2cXib4C7+bap2lwA56jTjY0qWRYb"+
	"v3IIfxEEKozVlbg0LQIDAQABqRAwDgYDVR0PAQH/BAQDAgXgoYGTMA0GCSqGSIb3"+
	"DQEBBQUAA4GBAJEhlvfoWNIAOSvFnLpg59vOj5jG0Urfv4w+hQmtCdK7MD0nyGKU"+
	"cP5CWCau0vK9/gikPoA49n0PK81SPQt9w2i/A81OJ3eSLIxTqi8MJS1+/VuEmvRf"+
	"XvedU84iIqnjDq92dTs6v01oRyPCdcjX8fpHuLk1VA96hgYai3l/D8lg";

	protected void test03GenerateCrmf(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		// Edit our favorite test user
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setUsername("WSTESTUSER1");
		user1.setPassword("foo123");
		user1.setClearPwd(true);
        user1.setSubjectDN("CN=WSTESTUSER1,O=Test");
		user1.setCaName(getAdminCAName());
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("USERGENERATED");
		user1.setEndEntityProfileName("EMPTY");
		user1.setCertificateProfileName("ENDUSER");
		ejbcaraws.editUser(user1);

		CertificateResponse certenv =  ejbcaraws.crmfRequest("WSTESTUSER1","foo123",crmf,null, CertificateHelper.RESPONSETYPE_CERTIFICATE);
		
		assertNotNull(certenv);
		
		X509Certificate cert = (X509Certificate) CertificateHelper.getCertificate(certenv.getData()); 
		
		assertNotNull(cert);
		System.out.println(cert.getSubjectDN().toString());
		assertEquals("CN=WSTESTUSER1,O=Test", cert.getSubjectDN().toString());
	}

	private static final String spkac = "MIICSjCCATIwggEiMA0GCSqGSIb3DQEBAQUAA4IBDwAwggEKAoIBAQDbiUJ4Q7a9"+
		"oaSaHjv4GxYWFTJ3qv1dUmpnEXvIwdWps9W2HHWNki9VzsbT2dBck3kISU7MBCI/"+
		"J4xgL5I766r4rdvXjy6w9K3pvXcyi+odTngxw8zU1PaKWONcAm7ulDEAiAzM3boM"+
		"/TGnF+0EzPU6mUv/cWfOICDdhFkGuAscKdewdWvJn6zJpizbgVimewM0p8QDHsoS"+
		"elap2stD9TPP+KKf3dZGN0NcmndTbtoPxyBgXCQZJfavFP7FLpAgC3EKVWLqtRij"+
		"5PBmYEMzd306/hSEECp4kJZi704p5pCMgzC9/3086AuAo+VEMDalsd0GwUan4YFi"+
		"G+I/CTHq8AszAgMBAAEWCjExMjU5ODMwMjEwDQYJKoZIhvcNAQEEBQADggEBAK/D"+
		"JcXBf2SESg/gguctpDn/z1uueuzxWwaHeD25WBUeqrdNOsGEqGarKP/Xtw2zPO9f"+
		"NSJ/AtxaNXRLUL0qpGgbhuclX4qJk4+rYAdlse9S2uJFIZEn41qLO1uoygvdoKZh"+
		"QJN3EABQ5QJP3R3Mhiu2tEtUuZ5zPq3vd/RBoOx5JbzZ1WZdk+dPbqdhyjsCy5ne"+
		"EkXFB6zflvR1fRrIxhDD0EnylHP1fz2p2kj2nOaQI6vQBH9CgTwkrAGEhy/Iq8aU"+
		"slAJUoE1+eCkUN/RHm/Z5XaZ2Le4BnjaDRTWJIglAUvFhuCEm7qCi1/bMof8V9Md"+
		"IP7NsueJRV9KvzdA7y0=";

	protected void test03GenerateSpkac(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		// Edit our favorite test user
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setUsername("WSTESTUSER1");
		user1.setPassword("foo123");
		user1.setClearPwd(true);
        user1.setSubjectDN("CN=WSTESTUSER1,O=Test");
		user1.setCaName(getAdminCAName());
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("USERGENERATED");
		user1.setEndEntityProfileName("EMPTY");
		user1.setCertificateProfileName("ENDUSER");
		ejbcaraws.editUser(user1);

		CertificateResponse certenv =  ejbcaraws.spkacRequest("WSTESTUSER1","foo123",spkac,null, CertificateHelper.RESPONSETYPE_CERTIFICATE);
		
		assertNotNull(certenv);
		
		X509Certificate cert = (X509Certificate) CertificateHelper.getCertificate(certenv.getData()); 
		
		assertNotNull(cert);
		
		assertEquals("CN=WSTESTUSER1,O=Test", cert.getSubjectDN().toString());
	}

	
	protected void test04GeneratePkcs12(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}

		boolean exceptionThrown = false;
		try{
           ejbcaraws.pkcs12Req("WSTESTUSER1","foo123",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
		}catch(EjbcaException_Exception e){
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown);// Should fail
		
		// Change token to P12
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER1");       		
	 	List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 1);        
        userdatas.get(0).setTokenType("P12");
        ejbcaraws.editUser(userdatas.get(0));
        
        exceptionThrown = false;
		try{
          ejbcaraws.pkcs12Req("WSTESTUSER1","foo123",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
		}catch(EjbcaException_Exception e){
			exceptionThrown = true;
		}
		assertTrue(exceptionThrown); // Should fail
		
		// Change password to foo456 and status to NEW     		   
        userdatas.get(0).setStatus(UserDataConstants.STATUS_NEW);
        userdatas.get(0).setPassword("foo456");
        userdatas.get(0).setClearPwd(true);
        ejbcaraws.editUser(userdatas.get(0));
        
        KeyStore ksenv = null;
        try{
          ksenv = ejbcaraws.pkcs12Req("WSTESTUSER1","foo456",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
        }catch(EjbcaException_Exception e){        	
        	assertTrue(e.getMessage(),false);
        }
        
        assertNotNull(ksenv);
                
        java.security.KeyStore ks = KeyStoreHelper.getKeyStore(ksenv.getKeystoreData(),"PKCS12","foo456");
        
        assertNotNull(ks);
        Enumeration en = ks.aliases();
        String alias = (String) en.nextElement();
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        assertEquals(cert.getSubjectDN().toString(), "CN=WSTESTUSER1,O=Test");
        PrivateKey privK1 = (PrivateKey)ks.getKey(alias, "foo456".toCharArray());
        System.out.println("test04GeneratePkcs12() Certificate " +cert.getSubjectDN().toString() + " equals CN=WSTESTUSER1,O=Test");
        
        // Generate a new one and make sure it is a new one and that key recovery does not kick in by misstake
		// Set status to new
		usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER1");       		
	 	userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 1);        
        userdatas.get(0).setStatus(UserDataConstants.STATUS_NEW);
        userdatas.get(0).setPassword("foo456");
        userdatas.get(0).setClearPwd(true);
        ejbcaraws.editUser(userdatas.get(0));
		// A new PK12 request now should return the same key and certificate
        KeyStore ksenv2 = ejbcaraws.pkcs12Req("WSTESTUSER1","foo456",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
        java.security.KeyStore ks2 = KeyStoreHelper.getKeyStore(ksenv2.getKeystoreData(),"PKCS12","foo456");
        assertNotNull(ks2);
        en = ks2.aliases();
        alias = (String) en.nextElement();
        X509Certificate cert2 = (X509Certificate) ks2.getCertificate(alias);
        assertEquals(cert2.getSubjectDN().toString(), "CN=WSTESTUSER1,O=Test");
        PrivateKey privK2 = (PrivateKey)ks2.getKey(alias, "foo456".toCharArray());
        
        // Compare certificates, must not be the same
        assertFalse(cert.getSerialNumber().toString(16).equals(cert2.getSerialNumber().toString(16)));
        // Compare keys, must not be the same
        String key1 = new String(Hex.encode(privK1.getEncoded()));
        String key2 = new String(Hex.encode(privK2.getEncoded()));
        assertFalse(key1.equals(key2));

	}

	protected void test05findCerts(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		
		// First find all certs
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER1");		
	 	List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 1);        
        userdatas.get(0).setTokenType("P12");       		   
        userdatas.get(0).setStatus(UserDataConstants.STATUS_NEW);
        userdatas.get(0).setPassword("foo123");
        userdatas.get(0).setClearPwd(true);
        ejbcaraws.editUser(userdatas.get(0));        
        KeyStore ksenv = null;
        try{
            ksenv = ejbcaraws.pkcs12Req("WSTESTUSER1","foo123",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
        }catch(EjbcaException_Exception e){        	
          	assertTrue(e.getMessage(),false);
        }
        java.security.KeyStore ks = KeyStoreHelper.getKeyStore(ksenv.getKeystoreData(),"PKCS12","foo123");
        
        assertNotNull(ks);
        Enumeration<String> en = ks.aliases();
        String alias = en.nextElement();
        java.security.cert.Certificate gencert = (java.security.cert.Certificate) ks.getCertificate(alias);
        
        List<Certificate> foundcerts = ejbcaraws.findCerts("WSTESTUSER1",false);
        assertTrue(foundcerts != null);
        assertTrue(foundcerts.size() > 0);
        
        boolean certFound = false;
        for(int i=0;i<foundcerts.size();i++){
        	java.security.cert.Certificate cert = (java.security.cert.Certificate) CertificateHelper.getCertificate(foundcerts.get(i).getCertificateData());
        	if(CertTools.getSerialNumber(gencert).equals(CertTools.getSerialNumber(cert))){
        		certFound = true;
        	}
        }
        assertTrue(certFound);
		
        String issuerdn = CertTools.getIssuerDN(gencert);
        String serno = CertTools.getSerialNumberAsString(gencert);
        
        ejbcaraws.revokeCert(issuerdn,serno, RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE);
        
        foundcerts = ejbcaraws.findCerts("WSTESTUSER1",true);
        assertTrue(foundcerts != null);
        assertTrue(foundcerts.size() > 0);
        
        certFound = false;
        for(int i=0;i<foundcerts.size();i++){
        	java.security.cert.Certificate cert = (java.security.cert.Certificate) CertificateHelper.getCertificate(foundcerts.get(i).getCertificateData());
        	if(CertTools.getSerialNumber(gencert).equals(CertTools.getSerialNumber(cert))){
        		certFound = true;
        	}
        }
        assertFalse(certFound);       
        
        
	}
	

	
	protected void test06revokeCert(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER1");			
		List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 1);        
		userdatas.get(0).setTokenType("P12");
		userdatas.get(0).setStatus(UserDataConstants.STATUS_NEW);
		userdatas.get(0).setPassword("foo456");
		userdatas.get(0).setClearPwd(true);
		ejbcaraws.editUser(userdatas.get(0));
		
        KeyStore ksenv = null;
        try{
          ksenv = ejbcaraws.pkcs12Req("WSTESTUSER1","foo456",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
        }catch(EjbcaException_Exception e){        	
        	assertTrue(e.getMessage(),false);
        }
        
        java.security.KeyStore ks = KeyStoreHelper.getKeyStore(ksenv.getKeystoreData(),"PKCS12","foo456");        
        assertNotNull(ks);
        Enumeration en = ks.aliases();
        String alias = (String) en.nextElement();
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        assertTrue(cert.getSubjectDN().toString().equals("CN=WSTESTUSER1,O=Test"));       
		
        String issuerdn = cert.getIssuerDN().toString();
        String serno = cert.getSerialNumber().toString(16);
        
        ejbcaraws.revokeCert(issuerdn,serno, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD);
        
        RevokeStatus revokestatus = ejbcaraws.checkRevokationStatus(issuerdn,serno);
        assertNotNull(revokestatus);
        assertTrue(revokestatus.getReason() == RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD);
        
        assertTrue(revokestatus.getCertificateSN().equals(serno));
        assertTrue(revokestatus.getIssuerDN().equals(issuerdn));
        assertNotNull(revokestatus.getRevocationDate());
        
        ejbcaraws.revokeCert(issuerdn,serno, RevokedCertInfo.NOT_REVOKED);
        
        revokestatus = ejbcaraws.checkRevokationStatus(issuerdn,serno);
        assertNotNull(revokestatus);
        assertTrue(revokestatus.getReason() == RevokedCertInfo.NOT_REVOKED);
        
        ejbcaraws.revokeCert(issuerdn,serno, RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE);
        
        revokestatus = ejbcaraws.checkRevokationStatus(issuerdn,serno);
        assertNotNull(revokestatus);
        assertTrue(revokestatus.getReason() == RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE);
        
        try{
          ejbcaraws.revokeCert(issuerdn,serno, RevokedCertInfo.NOT_REVOKED);
          assertTrue(false);
        }catch(EjbcaException_Exception e){}
        
	}
	

	
	protected void test07revokeToken(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER1");			
		List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);    
		userdatas.get(0).setTokenType("P12");       		   
		userdatas.get(0).setStatus(UserDataConstants.STATUS_NEW);
		userdatas.get(0).setPassword("foo123");
		userdatas.get(0).setClearPwd(true);
		ejbcaraws.editUser(userdatas.get(0));        
		KeyStore ksenv = null;
		try{
			ksenv = ejbcaraws.pkcs12Req("WSTESTUSER1","foo123","12345678","1024", CATokenConstants.KEYALGORITHM_RSA);
		}catch(EjbcaException_Exception e){        	
			assertTrue(e.getMessage(),false);
		}
		java.security.KeyStore ks = KeyStoreHelper.getKeyStore(ksenv.getKeystoreData(),"PKCS12","foo123");
		
		assertNotNull(ks);
		Enumeration en = ks.aliases();
		String alias = (String) en.nextElement();
		X509Certificate cert1 = (X509Certificate) ks.getCertificate(alias);
		
		userdatas.get(0).setStatus(UserDataConstants.STATUS_NEW);
		userdatas.get(0).setPassword("foo123");
		userdatas.get(0).setClearPwd(true);
		ejbcaraws.editUser(userdatas.get(0));  
		
		try{
			ksenv = ejbcaraws.pkcs12Req("WSTESTUSER1","foo123","12345678","1024", CATokenConstants.KEYALGORITHM_RSA);
		}catch(EjbcaException_Exception e){        	
			assertTrue(e.getMessage(),false);
		}
		ks = KeyStoreHelper.getKeyStore(ksenv.getKeystoreData(),"PKCS12","foo123");
		
		assertNotNull(ks);
		en = ks.aliases();
		alias = (String) en.nextElement();
		X509Certificate cert2 = (X509Certificate) ks.getCertificate(alias);
		
		ejbcaraws.revokeToken("12345678",RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE);
		
		String issuerdn1 = cert1.getIssuerDN().toString();
		String serno1 = cert1.getSerialNumber().toString(16);
		
		RevokeStatus revokestatus = ejbcaraws.checkRevokationStatus(issuerdn1,serno1);
		assertNotNull(revokestatus);
		assertTrue(revokestatus.getReason() == RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE);
		
		String issuerdn2 = cert2.getIssuerDN().toString();
		String serno2 = cert2.getSerialNumber().toString(16);
		
		revokestatus = ejbcaraws.checkRevokationStatus(issuerdn2,serno2);
		assertNotNull(revokestatus);
		assertTrue(revokestatus.getReason() == RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE);
		
	}


	
	protected void test08checkRevokeStatus(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		UserMatch usermatch = new UserMatch();
		usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
		usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
		usermatch.setMatchvalue("WSTESTUSER1");				
		List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);    
		userdatas.get(0).setTokenType("P12");       		   
		userdatas.get(0).setStatus(UserDataConstants.STATUS_NEW);
		userdatas.get(0).setPassword("foo123");
		userdatas.get(0).setClearPwd(true);
		ejbcaraws.editUser(userdatas.get(0));        
		KeyStore ksenv = null;
		try{
			ksenv = ejbcaraws.pkcs12Req("WSTESTUSER1","foo123","12345678","1024", CATokenConstants.KEYALGORITHM_RSA);
		}catch(EjbcaException_Exception e){        	
			assertTrue(e.getMessage(),false);
		}
		java.security.KeyStore ks = KeyStoreHelper.getKeyStore(ksenv.getKeystoreData(),"PKCS12","foo123");
		
		assertNotNull(ks);
		Enumeration en = ks.aliases();
		String alias = (String) en.nextElement();
		X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
		
        String issuerdn = cert.getIssuerDN().toString();
        String serno = cert.getSerialNumber().toString(16);
		
		RevokeStatus revokestatus = ejbcaraws.checkRevokationStatus(issuerdn,serno);
		assertNotNull(revokestatus);
		assertTrue(revokestatus.getReason() == RevokedCertInfo.NOT_REVOKED);		
        
        ejbcaraws.revokeCert(issuerdn,serno, RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE);
		
		revokestatus = ejbcaraws.checkRevokationStatus(issuerdn,serno);
		assertNotNull(revokestatus);
		assertTrue(revokestatus.getReason() == RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE);
        assertTrue(revokestatus.getCertificateSN().equals(serno));
        assertTrue(revokestatus.getIssuerDN().equals(issuerdn));
        assertNotNull(revokestatus.getRevocationDate());
	}
	

	
	protected void test09UTF8(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		
		// Test to add a user.
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setUsername("WSTESTUSER1");
		user1.setPassword("foo123");
		user1.setClearPwd(true);
		user1.setSubjectDN("CN=WS������");
		user1.setCaName(getAdminCAName());
		user1.setEmail(null);
		user1.setSubjectAltName(null);
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("USERGENERATED");
		user1.setEndEntityProfileName("EMPTY");
		user1.setCertificateProfileName("ENDUSER");
			
		ejbcaraws.editUser(user1);
		
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER1");
		
	 	List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 1);
		UserDataVOWS userdata = userdatas.get(0);
		assertTrue(userdata.getUsername().equals("WSTESTUSER1"));
        assertTrue(userdata.getSubjectDN().equals("CN=WS������"));
		
	}
	
	

	protected void test10revokeUser(boolean performSetup) throws Exception{
		if(performSetup){
		 setUpAdmin();
		}
		
		// Revoke and delete
		ejbcaraws.revokeUser("WSTESTUSER1",RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE,true);
		
		UserMatch usermatch = new UserMatch();
		usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
		usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
		usermatch.setMatchvalue("WSTESTUSER1");	
		List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 0);

	}

    
    protected void test12genTokenCertificates(boolean performSetup, boolean onlyOnce) throws Exception{
    	if(performSetup){
    		setUpAdmin();
    	}
    	
    	GlobalConfiguration gc = getRAAdmin().loadGlobalConfiguration(intAdmin);
    	boolean originalProfileSetting = gc.getEnableEndEntityProfileLimitations();
    	gc.setEnableEndEntityProfileLimitations(false);
    	getRAAdmin().saveGlobalConfiguration(intAdmin, gc);
    	if(getCertStore().getCertificateProfileId(intAdmin, "WSTESTPROFILE") != 0){
        	getCertStore().removeCertificateProfile(intAdmin, "WSTESTPROFILE");
        }
        
        CertificateProfile profile = new EndUserCertificateProfile();
        profile.setAllowValidityOverride(true);
        getCertStore().addCertificateProfile(intAdmin, "WSTESTPROFILE", profile);
    	
    	// first a simple test
		UserDataVOWS tokenUser1 = new UserDataVOWS();
		tokenUser1.setUsername("WSTESTTOKENUSER1");
		tokenUser1.setPassword("foo123");
		tokenUser1.setClearPwd(true);
		tokenUser1.setSubjectDN("CN=WSTESTTOKENUSER1");
		tokenUser1.setCaName(getAdminCAName());
		tokenUser1.setEmail(null);
		tokenUser1.setSubjectAltName(null);
		tokenUser1.setStatus(UserDataConstants.STATUS_NEW);
		tokenUser1.setTokenType("USERGENERATED");
		tokenUser1.setEndEntityProfileName("EMPTY");
		tokenUser1.setCertificateProfileName("ENDUSER"); 
		
		KeyPair basickeys = KeyTools.genKeys("1024", CATokenConstants.KEYALGORITHM_RSA);		
		PKCS10CertificationRequest  basicpkcs10 = new PKCS10CertificationRequest("SHA1WithRSA",
                CertTools.stringToBcX509Name("CN=NOUSED"), basickeys.getPublic(), new DERSet(), basickeys.getPrivate());

		ArrayList<TokenCertificateRequestWS> requests = new ArrayList<TokenCertificateRequestWS>();
		TokenCertificateRequestWS tokenCertReqWS = new TokenCertificateRequestWS();
		tokenCertReqWS.setCAName(getAdminCAName());
		tokenCertReqWS.setCertificateProfileName("WSTESTPROFILE");
		tokenCertReqWS.setValidityIdDays("1");
		tokenCertReqWS.setPkcs10Data(basicpkcs10.getDEREncoded());
		tokenCertReqWS.setType(HardTokenConstants.REQUESTTYPE_PKCS10_REQUEST);
		requests.add(tokenCertReqWS);
		tokenCertReqWS = new TokenCertificateRequestWS();
		tokenCertReqWS.setCAName(getAdminCAName());
		tokenCertReqWS.setCertificateProfileName("ENDUSER");
		tokenCertReqWS.setKeyalg("RSA");
		tokenCertReqWS.setKeyspec("1024");
		tokenCertReqWS.setType(HardTokenConstants.REQUESTTYPE_KEYSTORE_REQUEST);
		requests.add(tokenCertReqWS);
		
		HardTokenDataWS hardTokenDataWS = new HardTokenDataWS();
		hardTokenDataWS.setLabel(HardTokenConstants.LABEL_PROJECTCARD);
		hardTokenDataWS.setTokenType(HardTokenConstants.TOKENTYPE_SWEDISHEID);
		hardTokenDataWS.setHardTokenSN("12345678");		
		PinDataWS basicPinDataWS = new PinDataWS();
		basicPinDataWS.setType(HardTokenConstants.PINTYPE_BASIC);
		basicPinDataWS.setInitialPIN("1234");
		basicPinDataWS.setPUK("12345678");
		PinDataWS signaturePinDataWS = new PinDataWS();
		signaturePinDataWS.setType(HardTokenConstants.PINTYPE_SIGNATURE);
		signaturePinDataWS.setInitialPIN("5678");
		signaturePinDataWS.setPUK("23456789");
		
		hardTokenDataWS.getPinDatas().add(basicPinDataWS);
		hardTokenDataWS.getPinDatas().add(signaturePinDataWS);
				
		List<TokenCertificateResponseWS> responses = ejbcaraws.genTokenCertificates(tokenUser1, requests, hardTokenDataWS, true, false);
		assertTrue(responses.size() == 2);
		
		Iterator<TokenCertificateResponseWS> iter= responses.iterator();		
		TokenCertificateResponseWS next = iter.next();
		assertTrue(next.getType() == HardTokenConstants.RESPONSETYPE_CERTIFICATE_RESPONSE);
		Certificate cert = next.getCertificate();
		X509Certificate realcert = (X509Certificate) CertificateHelper.getCertificate(cert.getCertificateData());
		assertNotNull(realcert);
		assertTrue(realcert.getNotAfter().toString(),realcert.getNotAfter().before(new Date(System.currentTimeMillis() + 2 *24* 3600 *1000)));
		next = iter.next();
		assertTrue(next.getType() == HardTokenConstants.RESPONSETYPE_KEYSTORE_RESPONSE);
		KeyStore keyStore = next.getKeyStore();
		java.security.KeyStore realKeyStore = KeyStoreHelper.getKeyStore(keyStore.getKeystoreData(), HardTokenConstants.TOKENTYPE_PKCS12, "foo123");
		assertTrue(realKeyStore.containsAlias("WSTESTTOKENUSER1"));
		assertTrue(((X509Certificate) realKeyStore.getCertificate("WSTESTTOKENUSER1")).getNotAfter().after(new Date(System.currentTimeMillis() + 48 * 24 * 3600 *1000)));
		
		if(!onlyOnce){
			try{
				responses = ejbcaraws.genTokenCertificates(tokenUser1, requests, hardTokenDataWS, false, false);
				assertTrue(false);
			}catch(HardTokenExistsException_Exception e){

			}
		}
		
		getCertStore().removeCertificateProfile(intAdmin, "WSTESTPROFILE");
		gc.setEnableEndEntityProfileLimitations(originalProfileSetting);
    	getRAAdmin().saveGlobalConfiguration(intAdmin, gc);
		//hardTokenAdmin.removeHardToken(intAdmin, "12345678");
		
		
		
	} 
    
   
    
    protected void test13getExistsHardToken(boolean performSetup) throws Exception{
    	if(performSetup){
    		setUpAdmin();
    	}
    	assertTrue(ejbcaraws.existsHardToken("12345678"));
    	assertFalse(ejbcaraws.existsHardToken("23456789"));    
    }

    
    
    protected void test14getHardTokenData(boolean performSetup, boolean onlyOnce) throws Exception{
    	if(performSetup){
    		setUpAdmin();
    	}
    	HardTokenDataWS hardTokenDataWS = ejbcaraws.getHardTokenData("12345678", true, true);
    	assertNotNull(hardTokenDataWS);
    	assertTrue(""+hardTokenDataWS.getTokenType(), hardTokenDataWS.getTokenType() == HardTokenConstants.TOKENTYPE_SWEDISHEID);
    	assertTrue(hardTokenDataWS.getHardTokenSN().equals("12345678"));
    	assertTrue(hardTokenDataWS.getCopyOfSN(), hardTokenDataWS.getCopyOfSN() == null);
    	assertTrue(hardTokenDataWS.getCopies().size()==0);
    	//assertTrue(hardTokenDataWS.getCertificates().size() == 2);
    	assertTrue(hardTokenDataWS.getPinDatas().size() == 2);
    	
    	Iterator<PinDataWS> iter = hardTokenDataWS.getPinDatas().iterator();
    	while(iter.hasNext()){
    		PinDataWS next = iter.next();
    		if(next.getType() == HardTokenConstants.PINTYPE_BASIC){
    			assertTrue(next.getPUK().equals("12345678"));
    			assertTrue(next.getInitialPIN().equals("1234"));
    		}
    		if(next.getType() == HardTokenConstants.PINTYPE_SIGNATURE){
    			assertTrue(next.getPUK(),next.getPUK().equals("23456789"));
    			assertTrue(next.getInitialPIN().equals("5678"));    			
    		}
    	}
    	if(!onlyOnce){
    		hardTokenDataWS = ejbcaraws.getHardTokenData("12345678", false, false);
    		assertNotNull(hardTokenDataWS);
    		//assertTrue(""+ hardTokenDataWS.getCertificates().size(), hardTokenDataWS.getCertificates().size() == 2);
    		assertTrue(""+ hardTokenDataWS.getPinDatas().size(), hardTokenDataWS.getPinDatas().size() == 0);

    		try{
    			ejbcaraws.getHardTokenData("12345679", false, false);
    			assertTrue(false);
    		}catch(HardTokenDoesntExistsException_Exception e){

    		}
    	}

            
    }
    
  
    protected void test15getHardTokenDatas(boolean performSetup) throws Exception{
    	if(performSetup){
    		setUpAdmin();
    	}
    	
    	Collection<HardTokenDataWS> hardTokenDatas = ejbcaraws.getHardTokenDatas("WSTESTTOKENUSER1", true, true);
    	assertTrue(hardTokenDatas.size() == 1);
    	HardTokenDataWS hardTokenDataWS = hardTokenDatas.iterator().next();
    	assertNotNull(hardTokenDataWS);
    	assertTrue(""+hardTokenDataWS.getTokenType(), hardTokenDataWS.getTokenType() == HardTokenConstants.TOKENTYPE_SWEDISHEID);
    	assertTrue(hardTokenDataWS.getHardTokenSN().equals("12345678"));
    	assertTrue(hardTokenDataWS.getCopyOfSN(), hardTokenDataWS.getCopyOfSN() == null);
    	assertTrue(hardTokenDataWS.getCopies().size()==0);
    	assertTrue(hardTokenDataWS.getCertificates().size() == 2);
    	assertTrue(hardTokenDataWS.getPinDatas().size() == 2);
    	
    	Iterator<PinDataWS> iter = hardTokenDataWS.getPinDatas().iterator();
    	while(iter.hasNext()){
    		PinDataWS next = iter.next();
    		if(next.getType() == HardTokenConstants.PINTYPE_BASIC){
    			assertTrue(next.getPUK().equals("12345678"));
    			assertTrue(next.getInitialPIN().equals("1234"));
    		}
    		if(next.getType() == HardTokenConstants.PINTYPE_SIGNATURE){
    			assertTrue(next.getPUK(),next.getPUK().equals("23456789"));
    			assertTrue(next.getInitialPIN().equals("5678"));    			
    		}
    	}

    	try{
    	  hardTokenDatas = ejbcaraws.getHardTokenDatas("WSTESTTOKENUSER2", true, true);    	
    	  assertTrue(hardTokenDatas.size() == 0);
    	}catch(EjbcaException_Exception e){
    		
    	}
    }


    protected void test16CustomLog(boolean performSetup) throws Exception{
    	if(performSetup){
    		setUpAdmin();
    	}
        // The logging have to be checked manually	     
        ejbcaraws.customLog(IEjbcaWS.CUSTOMLOG_LEVEL_INFO, "Test", getAdminCAName(), "WSTESTTOKENUSER1", null, "Message 1 generated from WS test Script");
        ejbcaraws.customLog(IEjbcaWS.CUSTOMLOG_LEVEL_ERROR, "Test", getAdminCAName(), "WSTESTTOKENUSER1", null, "Message 1 generated from WS test Script");
    }

   
    
  
    
    protected void test17GetCertificate(boolean performSetup) throws Exception{
    	if(performSetup){
    		setUpAdmin();
    	}
    	
    	List<Certificate> certs = ejbcaraws.findCerts("WSTESTTOKENUSER1", true);
    	Certificate cert = certs.get(0);
    	X509Certificate realcert = (X509Certificate) CertificateHelper.getCertificate(cert.getCertificateData());
    	
    	cert = ejbcaraws.getCertificate(realcert.getSerialNumber().toString(16), CertTools.getIssuerDN(realcert));
    	assertNotNull(cert);
    	X509Certificate realcert2 = (X509Certificate) CertificateHelper.getCertificate(cert.getCertificateData());
    	
    	assertTrue(realcert.getSerialNumber().equals(realcert2.getSerialNumber()));
    	
    	cert = ejbcaraws.getCertificate("1234567", CertTools.getIssuerDN(realcert));
    	assertNull(cert);
    }
    
	protected void test18RevocationApprovals(boolean performSetup) throws Exception {
		final String APPROVINGADMINNAME = "superadmin";
        final String TOKENSERIALNUMBER = "42424242";
        final String TOKENUSERNAME = "WSTESTTOKENUSER3";
		final String ERRORNOTSENTFORAPPROVAL = "The request was never sent for approval."; 
    	final String ERRORNOTSUPPORTEDSUCCEEDED = "Reactivation of users is not supported, but succeeded anyway.";
		if(performSetup){
			  setUpAdmin();
		}
	    // Generate random username and CA name
		String randomPostfix = Integer.toString((new Random(new Date().getTime() + 4711)).nextInt(999999));
		String caname = "wsRevocationCA" + randomPostfix;
		String username = "wsRevocationUser" + randomPostfix;
		int caID = -1;
	    try {
	    	caID = TestRevocationApproval.createApprovalCA(intAdmin, caname, CAInfo.REQ_APPROVAL_REVOCATION, getCAAdminSession());
			X509Certificate adminCert = (X509Certificate) getCertStore().findCertificatesByUsername(intAdmin, APPROVINGADMINNAME).iterator().next();
	    	Admin approvingAdmin = new Admin(adminCert);
	    	try {
	    		X509Certificate cert = createUserAndCert(username,caID);
		        String issuerdn = cert.getIssuerDN().toString();
		        String serno = cert.getSerialNumber().toString(16);
			    // revoke via WS and verify response
	        	try {
					ejbcaraws.revokeCert(issuerdn, serno, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD);
					assertTrue(ERRORNOTSENTFORAPPROVAL, false);
				} catch (WaitingForApprovalException_Exception e1) {
				}
	        	try {
					ejbcaraws.revokeCert(issuerdn, serno, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD);
					assertTrue(ERRORNOTSENTFORAPPROVAL, false);
				} catch (ApprovalException_Exception e1) {
				}
				RevokeStatus revokestatus = ejbcaraws.checkRevokationStatus(issuerdn,serno);
		        assertNotNull(revokestatus);
		        assertTrue(revokestatus.getReason() == RevokedCertInfo.NOT_REVOKED);
				// Approve revocation and verify success
		        TestRevocationApproval.approveRevocation(intAdmin, approvingAdmin, username, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD,
		        		ApprovalDataVO.APPROVALTYPE_REVOKECERTIFICATE, getCertStore(), getApprovalSession());
		        // Try to unrevoke certificate
		        try {
		        	ejbcaraws.revokeCert(issuerdn,serno, RevokedCertInfo.NOT_REVOKED);
		        	assertTrue(ERRORNOTSENTFORAPPROVAL, false);
				} catch (WaitingForApprovalException_Exception e) {
				}
		        try {
		        	ejbcaraws.revokeCert(issuerdn,serno, RevokedCertInfo.NOT_REVOKED);
		        	assertTrue(ERRORNOTSENTFORAPPROVAL, false);
		        } catch (ApprovalException_Exception e) {
		        }
				// Approve revocation and verify success
		        TestRevocationApproval.approveRevocation(intAdmin, approvingAdmin, username, RevokedCertInfo.NOT_REVOKED,
		        		ApprovalDataVO.APPROVALTYPE_REVOKECERTIFICATE, getCertStore(), getApprovalSession());
		        // Revoke user
		        try {
		        	ejbcaraws.revokeUser(username, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD, false);
		        	assertTrue(ERRORNOTSENTFORAPPROVAL, false);
				} catch (WaitingForApprovalException_Exception e) {
				}
		        try {
		        	ejbcaraws.revokeUser(username, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD, false);
		        	assertTrue(ERRORNOTSENTFORAPPROVAL, false);
		        } catch (ApprovalException_Exception e) {
		        }
				// Approve revocation and verify success
		        TestRevocationApproval.approveRevocation(intAdmin, approvingAdmin, username, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD,
		        		ApprovalDataVO.APPROVALTYPE_REVOKEENDENTITY, getCertStore(), getApprovalSession());
		        // Try to reactivate user
		        try {
		        	ejbcaraws.revokeUser(username, RevokedCertInfo.NOT_REVOKED, false);
		        	assertTrue(ERRORNOTSUPPORTEDSUCCEEDED, false);
		        } catch (AlreadyRevokedException_Exception e) {
		        }
	    	} finally {
		    	getUserAdminSession().deleteUser(intAdmin, username);
	    	}
	        try {
		        // Create a hard token issued by this CA
		        createHardToken(TOKENUSERNAME, caname, TOKENSERIALNUMBER);
		    	assertTrue(ejbcaraws.existsHardToken(TOKENSERIALNUMBER));
		        // Revoke token
		        try {
			    	ejbcaraws.revokeToken(TOKENSERIALNUMBER, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD);
		        	assertTrue(ERRORNOTSENTFORAPPROVAL, false);
				} catch (WaitingForApprovalException_Exception e) {
				}
		        try {
			    	ejbcaraws.revokeToken(TOKENSERIALNUMBER, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD);
		        	assertTrue(ERRORNOTSENTFORAPPROVAL, false);
		        } catch (ApprovalException_Exception e) {
		        }
		        // Approve actions and verify success
		        TestRevocationApproval.approveRevocation(intAdmin, approvingAdmin, TOKENUSERNAME, RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD,
		        		ApprovalDataVO.APPROVALTYPE_REVOKECERTIFICATE, getCertStore(), getApprovalSession());
	        } finally {
		        getHardTokenSession().removeHardToken(intAdmin, TOKENSERIALNUMBER);
	        }
	    } finally {
			// Nuke CA
	        try {
	        	getCAAdminSession().revokeCA(intAdmin, caID, RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED);
	        } finally {
	        	getCAAdminSession().removeCA(intAdmin, caID);
	        }
	    }
	} // testRevocationApprovals
	

	protected void test19GeneratePkcs10Request(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		
		// Change token to P12
        UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSER1");       		
	 	List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 1);        
        userdatas.get(0).setTokenType("USERGENERATED");  		   
        userdatas.get(0).setStatus(UserDataConstants.STATUS_NEW);
        userdatas.get(0).setPassword("foo123");
        userdatas.get(0).setClearPwd(true);
        ejbcaraws.editUser(userdatas.get(0));
		
		KeyPair keys = KeyTools.genKeys("1024", CATokenConstants.KEYALGORITHM_RSA);
		PKCS10CertificationRequest  pkcs10 = new PKCS10CertificationRequest("SHA1WithRSA",
                CertTools.stringToBcX509Name("CN=NOUSED"), keys.getPublic(), new DERSet(), keys.getPrivate());
		
		CertificateResponse certenv =  ejbcaraws.pkcs10Request("WSTESTUSER1","foo123",new String(Base64.encode(pkcs10.getEncoded())),null,CertificateHelper.RESPONSETYPE_CERTIFICATE);
		
		assertNotNull(certenv);		
		assertTrue(certenv.getResponseType().equals(CertificateHelper.RESPONSETYPE_CERTIFICATE));
		X509Certificate cert = (X509Certificate) CertificateHelper.getCertificate(certenv.getData()); 
		
		assertNotNull(cert);		
		assertTrue(cert.getSubjectDN().toString().equals("CN=WSTESTUSER1,O=Test"));
		
        ejbcaraws.editUser(userdatas.get(0));
        certenv =  ejbcaraws.pkcs10Request("WSTESTUSER1","foo123",new String(Base64.encode(pkcs10.getEncoded())),null,CertificateHelper.RESPONSETYPE_PKCS7);
        assertTrue(certenv.getResponseType().equals(CertificateHelper.RESPONSETYPE_PKCS7));
		CMSSignedData cmsSignedData = new CMSSignedData(CertificateHelper.getPKCS7(certenv.getData()));
		assertTrue(cmsSignedData != null);

		CertStore certStore = cmsSignedData.getCertificatesAndCRLs("Collection","BC");
		assertTrue(certStore.getCertificates(null).size() ==1);
		        
	}
	
	protected void test20KeyRecover(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		GlobalConfiguration gc = getRAAdmin().loadGlobalConfiguration(intAdmin);
		boolean krenabled = gc.getEnableKeyRecovery();
		if (krenabled == true) {
			gc.setEnableKeyRecovery(false);
			getRAAdmin().saveGlobalConfiguration(intAdmin, gc);
		}

		boolean trows = false;
		try{
			// This should throw an exception that key recovery is not enabled
			ejbcaraws.keyRecoverNewest("WSTESTUSER1");
		}catch(EjbcaException_Exception e){
			trows = true;
			//e.printStackTrace();
			assertEquals(e.getMessage(),"Keyrecovery have to be enabled in the system configuration in order to use this command.");
		}
		assertTrue(trows);

		// Set key recovery enabled
		gc.setEnableKeyRecovery(true);
		getRAAdmin().saveGlobalConfiguration(intAdmin, gc);

		trows = false;
		try{
			// This should throw an exception that the user does not exist
			ejbcaraws.keyRecoverNewest("sdfjhdiuwerw43768754###");
		}catch(NotFoundException_Exception e){
			trows = true;
			//e.printStackTrace();
			assertEquals(e.getMessage(),"Error: User sdfjhdiuwerw43768754### doesn't exist.");
		}
		assertTrue(trows);

		// Add a new End entity profile, KEYRECOVERY
        EndEntityProfile profile = new EndEntityProfile();
        profile.addField(DnComponents.COMMONNAME);
    	profile.setUse(EndEntityProfile.KEYRECOVERABLE, 0, true);
        profile.setValue(EndEntityProfile.KEYRECOVERABLE, 0, EndEntityProfile.TRUE);
        profile.setUse(EndEntityProfile.KEYRECOVERABLE, 0, true);
        profile.setUse(EndEntityProfile.CLEARTEXTPASSWORD, 0,true);
        profile.setReUseKeyRevoceredCertificate(true);
        profile.setValue(EndEntityProfile.AVAILCAS,0, Integer.toString(SecConst.ALLCAS));
    	getRAAdmin().addEndEntityProfile(intAdmin, "KEYRECOVERY", profile);

		// Add a new user, set token to P12, status to new and end entity profile to key recovery
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setKeyRecoverable(true);
		user1.setUsername("WSTESTUSERKEYREC1");
		user1.setPassword("foo456");
		user1.setClearPwd(true);
		user1.setSubjectDN("CN=WSTESTUSERKEYREC1");
		user1.setCaName(getAdminCAName());
		user1.setEmail(null);
		user1.setSubjectAltName(null);
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("P12");
		user1.setEndEntityProfileName("KEYRECOVERY");
		user1.setCertificateProfileName("ENDUSER");
        ejbcaraws.editUser(user1);

        KeyStore ksenv = ejbcaraws.pkcs12Req("WSTESTUSERKEYREC1","foo456",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
        java.security.KeyStore ks = KeyStoreHelper.getKeyStore(ksenv.getKeystoreData(),"PKCS12","foo456");
        assertNotNull(ks);
        Enumeration en = ks.aliases();
        String alias = (String) en.nextElement();
        X509Certificate cert = (X509Certificate) ks.getCertificate(alias);
        assertEquals(cert.getSubjectDN().toString(), "CN=WSTESTUSERKEYREC1");
        PrivateKey privK = (PrivateKey)ks.getKey(alias, "foo456".toCharArray());
        
		// This should work now
		ejbcaraws.keyRecoverNewest("WSTESTUSERKEYREC1");
		
		// Set status to new
		UserMatch usermatch = new UserMatch();
        usermatch.setMatchwith(org.ejbca.util.query.UserMatch.MATCH_WITH_USERNAME);
        usermatch.setMatchtype(org.ejbca.util.query.UserMatch.MATCH_TYPE_EQUALS);
        usermatch.setMatchvalue("WSTESTUSERKEYREC1");       		
	 	List<UserDataVOWS> userdatas = ejbcaraws.findUser(usermatch);
		assertTrue(userdatas != null);
		assertTrue(userdatas.size() == 1);        
        userdatas.get(0).setStatus(UserDataConstants.STATUS_KEYRECOVERY);
        ejbcaraws.editUser(userdatas.get(0));
		// A new PK12 request now should return the same key and certificate
        KeyStore ksenv2 = ejbcaraws.pkcs12Req("WSTESTUSERKEYREC1","foo456",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
        java.security.KeyStore ks2 = KeyStoreHelper.getKeyStore(ksenv2.getKeystoreData(),"PKCS12","foo456");
        assertNotNull(ks2);
        en = ks2.aliases();
        alias = (String) en.nextElement();
        X509Certificate cert2 = (X509Certificate) ks2.getCertificate(alias);
        assertEquals(cert2.getSubjectDN().toString(), "CN=WSTESTUSERKEYREC1");
        PrivateKey privK2 = (PrivateKey)ks2.getKey(alias, "foo456".toCharArray());
        
        // Compare certificates
        assertEquals(cert.getSerialNumber().toString(16), cert2.getSerialNumber().toString(16));
        // Compare keys
        String key1 = new String(Hex.encode(privK.getEncoded()));
        String key2 = new String(Hex.encode(privK2.getEncoded()));
        assertEquals(key1, key2);

	} // test20KeyRecover

	protected void test21GetAvailableCAs(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		ICAAdminSessionRemote casession = getCAAdminSession();
		Collection ids = casession.getAvailableCAs(intAdmin);
		List<NameAndId> cas = ejbcaraws.getAvailableCAs();
		assertNotNull(cas);
		assertEquals(cas.size(), ids.size());
		boolean found = false;
		for (NameAndId n : cas) {
			if (n.getName().equals(getAdminCAName())) {
				found = true;
			}
		}
		assertTrue(found);
	} // test21GetAvailableCAs

	protected void test22GetAuthorizedEndEntityProfiles(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		Collection<Integer> ids = getRAAdmin().getAuthorizedEndEntityProfileIds(intAdmin);
		List<NameAndId> profs = ejbcaraws.getAuthorizedEndEntityProfiles();
		assertNotNull(profs);
		assertEquals(profs.size(), ids.size());
		boolean foundkeyrec = false;
		for (NameAndId n : profs) {
			System.out.println("name: "+n.getName());
			if (n.getName().equals("KEYRECOVERY")) {
				foundkeyrec= true;
			}
			boolean found = false;
			for (Integer i : ids) {
				// All ids must be in profs
				if (n.getId() ==  i) {
					found = true;
				}
			}
			assertTrue(found);
		}
		assertTrue(foundkeyrec);
	} // test22GetAuthorizedEndEntityProfiles

	protected void test23GetAvailableCertificateProfiles(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		int id = getRAAdmin().getEndEntityProfileId(intAdmin, "KEYRECOVERY");
		List<NameAndId> profs = ejbcaraws.getAvailableCertificateProfiles(id);
		assertNotNull(profs);
		for (NameAndId n : profs) {
			System.out.println("name: "+n.getName());			
		}
		assertTrue(profs.size() > 1);
		NameAndId n = profs.get(0);
		// This profile only has the enduser certificate profile available
		assertEquals(1, n.getId());
		assertEquals("ENDUSER", n.getName());
	} // test23GetAvailableCertificateProfiles

	protected void test24GetAvailableCAsInProfile(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		int id = getRAAdmin().getEndEntityProfileId(intAdmin, "KEYRECOVERY");
		System.out.println("id: "+id);
		List<NameAndId> cas = ejbcaraws.getAvailableCAsInProfile(id);
		assertNotNull(cas);
		// This profile only has ALLCAS available, so this list will be empty
		assertTrue(cas.size() == 0);
//		boolean found = false;
//		for (NameAndId n : cas) {
//			System.out.println("Available CA: "+n.getName());
//			if (getAdminCAName().equals(n.getName())) found = true;
//		}
//		assertTrue(found);
	} // test24GetAvailableCAsInProfile
	
	protected void test25CreateCRL(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		String caname = getAdminCAName();
		// This will throw exception if it fails
		ejbcaraws.createCRL(caname);
	} // test25CreateCRL
	
	
	protected void test26CVCRequest(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		createCVCCA();

		// 
		// create a set of requests for WS test
		//
        // Create new keyparis
        KeyPairGenerator keyGen = KeyPairGenerator.getInstance("RSA", "BC");
        keyGen.initialize(1024, new SecureRandom());
        KeyPair keyPair = keyGen.generateKeyPair();
        KeyPair keyPair1 = keyGen.generateKeyPair();
        KeyPair keyPair2 = keyGen.generateKeyPair();

        CAReferenceField caRef = new CAReferenceField("SE","WSTEST","00111");
        HolderReferenceField holderRef = new HolderReferenceField(caRef.getCountry(), caRef.getMnemonic(), caRef.getSequence());
        String algorithmName = "SHA256withRSAAndMGF1";

        // Simple self signed request
        CVCertificate request = CertificateGenerator.createRequest(keyPair, algorithmName, caRef, holderRef);

        // A renew request with an outer signature created with the same keys as the old one
        CVCAuthenticatedRequest authRequestSameKeys = CertificateGenerator.createAuthenticatedRequest(request, keyPair, algorithmName, caRef);

        // An renew request with an inner request with new keys and an outer request with the same keys as in the last request
        CVCertificate request1 = CertificateGenerator.createRequest(keyPair1, algorithmName, caRef, holderRef);
        CVCAuthenticatedRequest authRequestRenew = CertificateGenerator.createAuthenticatedRequest(request1, keyPair, algorithmName, caRef);

        // A false renew request with new keys all over, both for inner ant outer signatures
        CVCertificate request2 = CertificateGenerator.createRequest(keyPair2, algorithmName, caRef, holderRef);
        CVCAuthenticatedRequest authRequestRenewFalse = CertificateGenerator.createAuthenticatedRequest(request2, keyPair2, algorithmName, caRef);

		//
		// First test that we register a new user (like in admin GUI) and gets a certificate for that. This should work fine.
		// 
				
		// Edit our favorite test user
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setUsername("WSTESTUSER1");
		user1.setPassword("foo123");
		user1.setClearPwd(true);
        user1.setSubjectDN("CN=Test,C=SE");
		user1.setCaName(getCVCCAName());
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("USERGENERATED");
		user1.setEndEntityProfileName("EMPTY");
		user1.setCertificateProfileName("ENDUSER");
		// editUser and set status to new
		ejbcaraws.editUser(user1);

		List<Certificate> certenv =  ejbcaraws.cvcRequest(user1.getUsername(), user1.getPassword(), new String(Base64.encode(request.getDEREncoded())));
	
		assertNotNull(certenv);
		
		Certificate wscert = certenv.get(0);
		byte[] b64cert = wscert.getCertificateData();
		CVCObject parsedObject = CertificateParser.parseCertificate(Base64.decode(b64cert));
		CVCertificate cert = (CVCertificate)parsedObject;
		CardVerifiableCertificate cvcert = new CardVerifiableCertificate(cert);
		
		assertNotNull(cert);
		assertEquals("CN=Test,C=SE", CertTools.getSubjectDN(cvcert));
		assertEquals("00111", CertTools.getSerialNumberAsString(cvcert));
		PublicKey pk = cvcert.getPublicKey();
		assertEquals("CVC", pk.getFormat());
		// Verify that we have the complete chain
		assertEquals(3, certenv.size());
		Certificate wsdvcert = certenv.get(1);
		Certificate wscvcacert = certenv.get(2);
		b64cert = wsdvcert.getCertificateData();
		parsedObject = CertificateParser.parseCertificate(Base64.decode(b64cert));
		CVCertificate dvcert = (CVCertificate)parsedObject;
		b64cert = wscvcacert.getCertificateData();
		parsedObject = CertificateParser.parseCertificate(Base64.decode(b64cert));
		CVCertificate cvcacert = (CVCertificate)parsedObject;
		assertEquals(AuthorizationRoleEnum.DV_D, dvcert.getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole());
		assertEquals(AuthorizationRoleEnum.CVCA, cvcacert.getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole());
		cvcert.verify(dvcert.getCertificateBody().getPublicKey());
		CardVerifiableCertificate dvjavacert = new CardVerifiableCertificate(dvcert);
		dvjavacert.verify(cvcacert.getCertificateBody().getPublicKey());
		
		//
		// Second test that we try to get a new certificate for this user without outer (renewal) signature. This should fail.
		// 
        boolean thrown = false;
        try {
    		certenv =  ejbcaraws.cvcRequest(user1.getUsername(), user1.getPassword(), new String(Base64.encode(request.getDEREncoded())));        	
        } catch (EjbcaException_Exception e) {
        	thrown = true;
        	String msg = e.getMessage();
        	assertTrue(msg.contains("NEW, FAILED or INPROCESS required"));
        }
        assertTrue(thrown);

		//
		// Third test that we can not renew a certificate with the same keys as the old request. This should fail.
		// 
        thrown = false;
        try {
        	certenv =  ejbcaraws.cvcRequest(user1.getUsername(), user1.getPassword(), new String(Base64.encode(authRequestSameKeys.getDEREncoded())));        	
        } catch (AuthorizationDeniedException_Exception e) {
        	thrown = true;
        	String msg = e.getMessage();
        	assertTrue(msg.contains("Trying to renew a certificate using the same key"));
        }
        assertTrue(thrown);

		//
		// Fourth test that we can renew a certificate using an outer signature made with the old keys. This should succeed.
		// 
        certenv =  ejbcaraws.cvcRequest(user1.getUsername(), user1.getPassword(), new String(Base64.encode(authRequestRenew.getDEREncoded())));        	
		assertNotNull(certenv);
		wscert = certenv.get(0);
		b64cert = wscert.getCertificateData();
		parsedObject = CertificateParser.parseCertificate(Base64.decode(b64cert));
		cert = (CVCertificate)parsedObject;
		cvcert = new CardVerifiableCertificate(cert);		
		assertNotNull(cert);
		assertEquals("CN=Test,C=SE", CertTools.getSubjectDN(cvcert));
		assertEquals("00111", CertTools.getSerialNumberAsString(cvcert));

		//
		// Fifth test try to renew with an outer signature which is not by the last issued cert (false renew request). This should fail.
		//
        thrown = false;
        try {
    		certenv =  ejbcaraws.cvcRequest(user1.getUsername(), user1.getPassword(), new String(Base64.encode(authRequestRenewFalse.getDEREncoded())));        	
        } catch (AuthorizationDeniedException_Exception e) {
        	thrown = true;
        	String msg = e.getMessage();
        	assertTrue(msg.contains("No certificate found that could authenticate request"));
        }
        assertTrue(thrown);
        
        // Finally remove the CAs
		deleteCVCCA();

	}

	protected void test27EjbcaVersion(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
		String version = ejbcaraws.getEjbcaVersion();
		assertTrue(version.contains("EJBCA 3")); // We don't know which specific version we are testing
	}
	
	protected void test28getLastCertChain(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}
        List<Certificate> foundcerts = ejbcaraws.getLastCertChain("WSTESTUSER1");
        assertTrue(foundcerts != null);
        assertTrue(foundcerts.size() > 1);
        
    	java.security.cert.Certificate cacert = (java.security.cert.Certificate) CertificateHelper.getCertificate(foundcerts.get(foundcerts.size()-1).getCertificateData());
    	assertTrue(CertTools.isSelfSigned(cacert));
    	java.security.cert.Certificate cert = (java.security.cert.Certificate) CertificateHelper.getCertificate(foundcerts.get(0).getCertificateData());
    	assertEquals("CN=WSTESTUSER1,O=Test", CertTools.getSubjectDN(cert));
    	for (int i = 1; i < foundcerts.size(); i++) {
        	java.security.cert.Certificate cert2 = (java.security.cert.Certificate) CertificateHelper.getCertificate(foundcerts.get(i).getCertificateData());
    		cert.verify(cert2.getPublicKey()); // will throw if verification fails
    		cert = cert2;
    	}
	}
	
	protected void test29ErrorOnEditUser(boolean performSetup) throws Exception{
		if(performSetup){
		  setUpAdmin();
		}
		// Test to add a user.
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setUsername("WSTESTUSER29");
		user1.setPassword("foo123");
		user1.setClearPwd(true);
		user1.setSubjectDN("CN=WSTESTUSER29");
		user1.setEmail(null);
		user1.setSubjectAltName(null);
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("USERGENERATED");
		user1.setEndEntityProfileName("EMPTY");
		user1.setCertificateProfileName("ENDUSER");

        ErrorCode errorCode = null;

        ///// Check ErrorCode.CA_NOT_EXISTS /////
		user1.setCaName(BADCANAME);
        try {
            ejbcaraws.editUser(user1);
        } catch (CADoesntExistsException_Exception e) {
            errorCode = e.getFaultInfo().getErrorCode();
        }
        assertNotNull("error code should not be null", errorCode);
        assertEquals(errorCode.getInternalErrorCode(), 
                org.ejbca.core.ErrorCode.CA_NOT_EXISTS.getInternalErrorCode());

        // restore CA name
		user1.setCaName(getAdminCAName());
        errorCode = null;

        ///// Check ErrorCode.EE_PROFILE_NOT_EXISTS /////
		user1.setEndEntityProfileName("Bad EE profile");
        try {
            ejbcaraws.editUser(user1);
        } catch (EjbcaException_Exception e) {
            errorCode = e.getFaultInfo().getErrorCode();
        }

        assertNotNull("error code should not be null", errorCode);
        assertEquals(errorCode.getInternalErrorCode(), 
                org.ejbca.core.ErrorCode.EE_PROFILE_NOT_EXISTS.getInternalErrorCode());

        // restore EE profile
		user1.setEndEntityProfileName("EMPTY");
        errorCode = null;

        ///// Check ErrorCode.CERT_PROFILE_NOT_EXISTS /////
		user1.setCertificateProfileName("Bad cert profile");
        try {
            ejbcaraws.editUser(user1);
        } catch (EjbcaException_Exception e) {
            errorCode = e.getFaultInfo().getErrorCode();
        }

        assertNotNull("error code should not be null", errorCode);
        assertEquals(errorCode.getInternalErrorCode(), 
                org.ejbca.core.ErrorCode.CERT_PROFILE_NOT_EXISTS.getInternalErrorCode());

        // restore Certificate profile
		user1.setCertificateProfileName("ENDUSER");
        errorCode = null;

        ///// Check ErrorCode.UNKOWN_TOKEN_TYPE /////
		user1.setTokenType("Bad token type");
        try {
            ejbcaraws.editUser(user1);
        } catch (EjbcaException_Exception e) {
            errorCode = e.getFaultInfo().getErrorCode();
        }

        assertNotNull("error code should not be null", errorCode);
        assertEquals(errorCode.getInternalErrorCode(), 
                org.ejbca.core.ErrorCode.UNKOWN_TOKEN_TYPE.getInternalErrorCode());
    }

	protected void test30ErrorOnGeneratePkcs10(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}

		// Add a user for this test purpose.
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setUsername("WSTESTUSER30");
		user1.setPassword("foo1234");
		user1.setClearPwd(true);
		user1.setSubjectDN("CN=WSTESTUSER30");
		user1.setEmail(null);
		user1.setSubjectAltName(null);
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("USERGENERATED");
		user1.setEndEntityProfileName("EMPTY");
		user1.setCertificateProfileName("ENDUSER");
		user1.setCaName(getAdminCAName());
        ejbcaraws.editUser(user1);


        KeyPair keys = null;
        PKCS10CertificationRequest  pkcs10 = null;
        ErrorCode errorCode = null;

        /////// Check Error.LOGIN_ERROR ///////
		keys = KeyTools.genKeys("1024", CATokenConstants.KEYALGORITHM_RSA);
		pkcs10 = new PKCS10CertificationRequest("SHA1WithRSA",
                CertTools.stringToBcX509Name("CN=WSTESTUSER30"), 
                keys.getPublic(), new DERSet(), keys.getPrivate());

        try {
            ejbcaraws.pkcs10Request("WSTESTUSER30","foo123",new String(Base64.encode(pkcs10.getEncoded())),
                null, CertificateHelper.RESPONSETYPE_CERTIFICATE);
        } catch (EjbcaException_Exception e) {
            errorCode = e.getFaultInfo().getErrorCode();
        }

        assertNotNull("error code should not be null", errorCode);
        assertEquals(errorCode.getInternalErrorCode(), 
                org.ejbca.core.ErrorCode.LOGIN_ERROR.getInternalErrorCode());

        errorCode = null;

        /////// Check Error.USER_WRONG_STATUS ///////
		user1.setStatus(UserDataConstants.STATUS_REVOKED);
        ejbcaraws.editUser(user1);

		keys = KeyTools.genKeys("1024", CATokenConstants.KEYALGORITHM_RSA);
		pkcs10 = new PKCS10CertificationRequest("SHA1WithRSA",
                CertTools.stringToBcX509Name("CN=WSTESTUSER30"), keys.getPublic(), new DERSet(), keys.getPrivate());

        try {
            ejbcaraws.pkcs10Request("WSTESTUSER30","foo1234",new String(Base64.encode(pkcs10.getEncoded())),
                null, CertificateHelper.RESPONSETYPE_CERTIFICATE);
        } catch (EjbcaException_Exception e) {
            errorCode = e.getFaultInfo().getErrorCode();
        }

        assertNotNull("error code should not be null", errorCode);
        assertEquals(errorCode.getInternalErrorCode(), 
                org.ejbca.core.ErrorCode.USER_WRONG_STATUS.getInternalErrorCode());

		
	}

	protected void test31ErrorOnGeneratePkcs12(boolean performSetup) throws Exception{
		if(performSetup){
			setUpAdmin();
		}

		// Add a user for this test purpose.
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setUsername("WSTESTUSER31");
		user1.setPassword("foo1234");
		user1.setClearPwd(true);
		user1.setSubjectDN("CN=WSTESTUSER31");
		user1.setEmail(null);
		user1.setSubjectAltName(null);
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("USERGENERATED");
		user1.setEndEntityProfileName("EMPTY");
		user1.setCertificateProfileName("ENDUSER");
		user1.setCaName(getAdminCAName());
        ejbcaraws.editUser(user1);

        ErrorCode errorCode = null;

        // Should failed because of the bad token type (USERGENERATED instead of P12)
        try {
            ejbcaraws.pkcs12Req("WSTESTUSER31","foo1234",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
        } catch (EjbcaException_Exception ex) {
            errorCode = ex.getFaultInfo().getErrorCode();
            assertEquals(org.ejbca.core.ErrorCode.BAD_USER_TOKEN_TYPE.getInternalErrorCode(), 
                errorCode.getInternalErrorCode());
        }
        assertNotNull(errorCode);
        errorCode = null;
        // restore correct token type
		user1.setTokenType("P12");
        ejbcaraws.editUser(user1);

        // Should failed because of the bad password
        try {
            ejbcaraws.pkcs12Req("WSTESTUSER31","foo123",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
        } catch (EjbcaException_Exception ex) {
            errorCode = ex.getFaultInfo().getErrorCode();
            assertEquals(org.ejbca.core.ErrorCode.LOGIN_ERROR.getInternalErrorCode(), 
                errorCode.getInternalErrorCode());
        }
        assertNotNull(errorCode);
        errorCode = null;


        // insert wrong status
        user1.setStatus(UserDataConstants.STATUS_REVOKED);
        ejbcaraws.editUser(user1);

        // Should failed because certificate already exists.
        try {
            ejbcaraws.pkcs12Req("WSTESTUSER31","foo1234",null,"1024", CATokenConstants.KEYALGORITHM_RSA);
        } catch (EjbcaException_Exception ex) {
            errorCode = ex.getFaultInfo().getErrorCode();
            assertEquals(org.ejbca.core.ErrorCode.USER_WRONG_STATUS.getInternalErrorCode(), 
                errorCode.getInternalErrorCode());
        }
        assertNotNull(errorCode);
	}

	protected void test32OperationOnNonexistingCA(boolean performSetup) throws Exception{
		final String MOCKSERIAL = "AABBCCDDAABBCCDD";
		if(performSetup){
			setUpAdmin();
		}
		// Add a user for this test purpose.
		UserDataVOWS user1 = new UserDataVOWS();
		user1.setUsername("WSTESTUSER32");
		user1.setPassword("foo1234");
		user1.setClearPwd(true);
		user1.setSubjectDN("CN=WSTESTUSER32");
		user1.setEmail(null);
		user1.setSubjectAltName(null);
		user1.setStatus(UserDataConstants.STATUS_NEW);
		user1.setTokenType("P12");
		user1.setEndEntityProfileName("EMPTY");
		user1.setCertificateProfileName("ENDUSER");
		user1.setCaName(BADCANAME);
		try {
	        ejbcaraws.editUser(user1);
	        assertTrue("WS did not throw CADoesntExistsException as expected", false);
		} catch (CADoesntExistsException_Exception e) {	}	// Expected
        // Untested: ejbcaraws.pkcs10Request
        // Untested: ejbcaraws.pkcs12Req
		try {
	        ejbcaraws.revokeCert("CN="+BADCANAME, MOCKSERIAL, RevokedCertInfo.NOT_REVOKED);
	        assertTrue("WS did not throw CADoesntExistsException as expected", false);
		} catch (CADoesntExistsException_Exception e) {	}	// Expected
        // Untested: ejbcaraws.revokeUser
		// Untested: ejbcaraws.keyRecoverNewest
		// Untested: ejbcaraws.revokeToken
		try {
			ejbcaraws.checkRevokationStatus("CN="+BADCANAME, MOCKSERIAL);
	        assertTrue("WS did not throw CADoesntExistsException as expected", false);
		} catch (CADoesntExistsException_Exception e) {	}	// Expected
		// Untested: ejbcaraws.genTokenCertificates
		try {
			UserDataVOWS badUserDataWS = new UserDataVOWS();
			badUserDataWS.setCaName(BADCANAME);
			ejbcaraws.genTokenCertificates(badUserDataWS, new ArrayList<TokenCertificateRequestWS>(), null, false, false);
	        assertTrue("WS did not throw CADoesntExistsException as expected", false);
		} catch (CADoesntExistsException_Exception e) {	}	// Expected
		// Untested: ejbcaraws.getHardTokenData
		// Untested: ejbcaraws.getHardTokenDatas
		try {
			ejbcaraws.republishCertificate(MOCKSERIAL, "CN=" + BADCANAME);
	        assertTrue("WS did not throw CADoesntExistsException as expected", false);
		} catch (CADoesntExistsException_Exception e) {	}	// Expected
		try {
			ejbcaraws.customLog(IEjbcaWS.CUSTOMLOG_LEVEL_ERROR, "prefix", BADCANAME, null, null, "This should not have been logged");
	        assertTrue("WS did not throw CADoesntExistsException as expected", false);
		} catch (CADoesntExistsException_Exception e) {	}	// Expected
		try {
			ejbcaraws.getCertificate(MOCKSERIAL, "CN=" + BADCANAME);
	        assertTrue("WS did not throw CADoesntExistsException as expected", false);
		} catch (CADoesntExistsException_Exception e) {	}	// Expected
		try {
			ejbcaraws.createCRL(BADCANAME);
	        assertTrue("WS did not throw CADoesntExistsException as expected", false);
		} catch (CADoesntExistsException_Exception e) {	}	// Expected
	}

	protected void test33checkQueueLength(boolean performSetup) throws Exception {
		if(performSetup){
			setUpAdmin();
		}
		final String PUBLISHER_NAME = "myPublisher";
		final Admin admin = new Admin(Admin.TYPE_INTERNALUSER);
		try {
			assertEquals( -4, ejbcaraws.getPublisherQueueLength(PUBLISHER_NAME) );
			final CustomPublisherContainer publisher = new CustomPublisherContainer();
			publisher.setClassPath(DummyCustomPublisher.class.getName());
			publisher.setDescription("Used in Junit Test, Remove this one");
			TestTools.getPublisherSession().addPublisher(admin, PUBLISHER_NAME, publisher);
			assertEquals( 0, ejbcaraws.getPublisherQueueLength(PUBLISHER_NAME) );
			final int publisherID = TestTools.getPublisherSession().getPublisherId(admin, PUBLISHER_NAME);
			TestTools.getPublisherQueueSession().addQueueData(publisherID, PublisherQueueData.PUBLISH_TYPE_CERT, "XX", null, PublisherQueueData.STATUS_PENDING);
			assertEquals( 1, ejbcaraws.getPublisherQueueLength(PUBLISHER_NAME) );
			TestTools.getPublisherQueueSession().addQueueData(publisherID, PublisherQueueData.PUBLISH_TYPE_CERT, "XX", null, PublisherQueueData.STATUS_PENDING);
			assertEquals( 2, ejbcaraws.getPublisherQueueLength(PUBLISHER_NAME) );
			TestTools.getPublisherQueueSession().removeQueueData( ((PublisherQueueData)TestTools.getPublisherQueueSession().getPendingEntriesForPublisher(publisherID).iterator().next()).getPk() );
			assertEquals( 1, ejbcaraws.getPublisherQueueLength(PUBLISHER_NAME) );
			TestTools.getPublisherQueueSession().removeQueueData( ((PublisherQueueData)TestTools.getPublisherQueueSession().getPendingEntriesForPublisher(publisherID).iterator().next()).getPk() );
			assertEquals( 0, ejbcaraws.getPublisherQueueLength(PUBLISHER_NAME) );
		} catch (EjbcaException_Exception e) {
			assertTrue(e.getMessage(),false);
		} finally {
			TestTools.getPublisherSession().removePublisher(admin, PUBLISHER_NAME);
		}
	}
	protected void test99cleanUpAdmins() throws Exception {
		//getHardTokenSession().removeHardToken(intAdmin, "12345678");
		//getUserAdminSession().revokeAndDeleteUser(intAdmin, "WSTESTTOKENUSER1", RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED);
		if (getUserAdminSession().existsUser(intAdmin, wsTestAdminUsername)) {
			// Remove from admin group
			CAInfo cainfo = getCAAdminSession().getCAInfo(intAdmin, getAdminCAName());
			AdminGroup admingroup = getAuthSession().getAdminGroup(intAdmin, AdminGroup.TEMPSUPERADMINGROUP);
			Iterator iter = admingroup.getAdminEntities().iterator();
			while(iter.hasNext()){
				AdminEntity adminEntity = (AdminEntity) iter.next();
				if(adminEntity.getMatchValue().equals(wsTestAdminUsername)){
					ArrayList<AdminEntity> list = new ArrayList<AdminEntity>();
					list.add(new AdminEntity(AdminEntity.WITH_COMMONNAME,AdminEntity.TYPE_EQUALCASE,wsTestAdminUsername,cainfo.getCAId()));
					getAuthSession().removeAdminEntities(intAdmin, AdminGroup.TEMPSUPERADMINGROUP, list);
					getAuthSession().forceRuleUpdate(intAdmin);
				}
			}
			// Remove user
			getUserAdminSession().revokeAndDeleteUser(intAdmin, wsTestAdminUsername, RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED);
		}
		if (getUserAdminSession().existsUser(intAdmin, wsTestNonAdminUsername)) {
			getUserAdminSession().revokeAndDeleteUser(intAdmin, wsTestNonAdminUsername, RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED);
		}
        if (new File("p12/" + wsTestAdminUsername + ".jks").exists()) {
        	new File("p12/" + wsTestAdminUsername + ".jks").delete();
        }
        if (new File("p12/" + wsTestNonAdminUsername + ".jks").exists()) {
        	new File("p12/" + wsTestNonAdminUsername + ".jks").delete();
        }
        
		// Remove test user
        try {
        	getUserAdminSession().revokeAndDeleteUser(intAdmin, "WSTESTUSER1", RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        try {
        	getUserAdminSession().revokeAndDeleteUser(intAdmin, "WSTESTUSERKEYREC1", RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        try {
        	getUserAdminSession().revokeAndDeleteUser(intAdmin, "WSTESTUSER30", RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        try {
        	getUserAdminSession().revokeAndDeleteUser(intAdmin, "WSTESTUSER31", RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED);
        } catch (Exception e) {
        	e.printStackTrace();
        }
        // Remove Key recovery end entity profile
        try {
        	getRAAdmin().removeEndEntityProfile(intAdmin, "KEYRECOVERY");
        } catch (Exception e) {
        	e.printStackTrace();
        }

    } // test99cleanUpAdmins

	//
	// Private methods
	//
    /** Create a CVC CA
     * 
     */
    private void createCVCCA() throws Exception {
        SoftCATokenInfo catokeninfo = new SoftCATokenInfo();
        catokeninfo.setSignKeySpec("1024");
        catokeninfo.setEncKeySpec("1024");
        catokeninfo.setSignKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
        catokeninfo.setEncKeyAlgorithm(SoftCATokenInfo.KEYALGORITHM_RSA);
        catokeninfo.setSignatureAlgorithm(CATokenInfo.SIGALG_SHA256_WITH_RSA_AND_MGF1);
        catokeninfo.setEncryptionAlgorithm(CATokenInfo.SIGALG_SHA256_WITH_RSA_AND_MGF1);
        // No CA Services.
        ArrayList extendedcaservices = new ArrayList();

        String rootcadn = "CN=WSCVCA,C=SE";
    	String rootcaname = "WSTESTCVCA";

    	java.security.cert.Certificate cvcacert = null;
    	int cvcaid = rootcadn.hashCode();
        try {
            getAuthSession().initialize(intAdmin, rootcadn.hashCode());

            CVCCAInfo cvccainfo = new CVCCAInfo(rootcadn, rootcaname, SecConst.CA_ACTIVE, new Date(),
            		SecConst.CERTPROFILE_FIXED_ROOTCA, 3650, 
                    null, // Expiretime 
                    CAInfo.CATYPE_CVC, CAInfo.SELFSIGNED,
                    null, catokeninfo, "JUnit WS CVC CA", 
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
            
            getCAAdminSession().createCA(intAdmin, cvccainfo);

            CAInfo info = getCAAdminSession().getCAInfo(intAdmin, rootcaname);
            cvcaid = info.getCAId();
            assertEquals(CAInfo.CATYPE_CVC, info.getCAType());
            Collection col = info.getCertificateChain();
            assertEquals(1, col.size());
            Iterator iter = col.iterator();
            cvcacert = (java.security.cert.Certificate)iter.next();
        } catch (CAExistsException pee) {
        	pee.printStackTrace();
        }    	

        try {
            String dvcadn = "CN=WSDVCA,C=SE";
        	String dvcaname = "WSTESTDVCA";

        	CVCCAInfo cvcdvinfo = new CVCCAInfo(dvcadn, dvcaname, SecConst.CA_ACTIVE, new Date(),
        			SecConst.CERTPROFILE_FIXED_SUBCA, 3650, 
        			null, // Expiretime 
        			CAInfo.CATYPE_CVC, cvcaid,
        			null, catokeninfo, "JUnit WS CVC DV CA", 
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

        	getCAAdminSession().createCA(intAdmin, cvcdvinfo);

        	CAInfo info = getCAAdminSession().getCAInfo(intAdmin, dvcaname);
        	assertEquals(CAInfo.CATYPE_CVC, info.getCAType());
            Collection col = info.getCertificateChain();
            assertEquals(2, col.size());
            Iterator iter = col.iterator();
            java.security.cert.Certificate dvcacert = (java.security.cert.Certificate)iter.next();
            dvcacert.verify(cvcacert.getPublicKey());
        } catch (CAExistsException pee) {
        	pee.printStackTrace();
        }    	
    }

    /** Create a CVC CA
     * 
     */
    private void deleteCVCCA() throws Exception {
		// Clean up by removing the CVC CA
        try {
        	String dn = CertTools.stringToBCDNString("CN=WSCVCA,C=SE");
            getCAAdminSession().removeCA(intAdmin, dn.hashCode());
        	dn = CertTools.stringToBCDNString("CN=WSDVCA,C=SE");
            getCAAdminSession().removeCA(intAdmin, dn.hashCode());
        } catch (Exception e) {
        	e.printStackTrace();
        	assertTrue(false);
        }
    }
    
	/**
	 * Create a user a generate cert. 
	 */
	private X509Certificate createUserAndCert(String username, int caID) throws Exception {
		UserDataVO userdata = new UserDataVO(username,"CN="+username,caID,null,null,1,SecConst.EMPTY_ENDENTITYPROFILE,
				SecConst.CERTPROFILE_FIXED_ENDUSER,SecConst.TOKEN_SOFT_P12,0,null);
		userdata.setPassword("foo123");
		getUserAdminSession().addUser(intAdmin, userdata , true);
	    BatchMakeP12 makep12 = new BatchMakeP12();
	    File tmpfile = File.createTempFile("ejbca", "p12");
	    makep12.setMainStoreDir(tmpfile.getParent());
	    makep12.createAllNew();
	    Collection userCerts = getCertStore().findCertificatesByUsername(intAdmin, username);
	    assertTrue( userCerts.size() == 1 );
	    return (X509Certificate) userCerts.iterator().next();
	}

	/**
	 * Creates a "hardtoken" with certficates. 
	 */
	private void createHardToken(String username, String caName, String serialNumber) throws Exception {
    	GlobalConfiguration gc = getRAAdmin().loadGlobalConfiguration(intAdmin);
    	boolean originalProfileSetting = gc.getEnableEndEntityProfileLimitations();
    	gc.setEnableEndEntityProfileLimitations(false);
    	getRAAdmin().saveGlobalConfiguration(intAdmin, gc);
    	if(getCertStore().getCertificateProfileId(intAdmin, "WSTESTPROFILE") != 0){
        	getCertStore().removeCertificateProfile(intAdmin, "WSTESTPROFILE");
        }
        CertificateProfile profile = new EndUserCertificateProfile();
        profile.setAllowValidityOverride(true);
        getCertStore().addCertificateProfile(intAdmin, "WSTESTPROFILE", profile);
		UserDataVOWS tokenUser1 = new UserDataVOWS();
		tokenUser1.setUsername(username);
		tokenUser1.setPassword("foo123");
		tokenUser1.setClearPwd(true);
		tokenUser1.setSubjectDN("CN="+username);
		tokenUser1.setCaName(caName);
		tokenUser1.setEmail(null);
		tokenUser1.setSubjectAltName(null);
		tokenUser1.setStatus(UserDataConstants.STATUS_NEW);
		tokenUser1.setTokenType("USERGENERATED");
		tokenUser1.setEndEntityProfileName("EMPTY");
		tokenUser1.setCertificateProfileName("ENDUSER"); 
		KeyPair basickeys = KeyTools.genKeys("1024", CATokenConstants.KEYALGORITHM_RSA);		
		PKCS10CertificationRequest  basicpkcs10 = new PKCS10CertificationRequest("SHA1WithRSA",
                CertTools.stringToBcX509Name("CN=NOTUSED"), basickeys.getPublic(), new DERSet(), basickeys.getPrivate());
		ArrayList<TokenCertificateRequestWS> requests = new ArrayList<TokenCertificateRequestWS>();
		TokenCertificateRequestWS tokenCertReqWS = new TokenCertificateRequestWS();
		tokenCertReqWS.setCAName(caName);
		tokenCertReqWS.setCertificateProfileName("WSTESTPROFILE");
		tokenCertReqWS.setValidityIdDays("1");
		tokenCertReqWS.setPkcs10Data(basicpkcs10.getDEREncoded());
		tokenCertReqWS.setType(HardTokenConstants.REQUESTTYPE_PKCS10_REQUEST);
		requests.add(tokenCertReqWS);
		tokenCertReqWS = new TokenCertificateRequestWS();
		tokenCertReqWS.setCAName(caName);
		tokenCertReqWS.setCertificateProfileName("ENDUSER");
		tokenCertReqWS.setKeyalg("RSA");
		tokenCertReqWS.setKeyspec("1024");
		tokenCertReqWS.setType(HardTokenConstants.REQUESTTYPE_KEYSTORE_REQUEST);
		requests.add(tokenCertReqWS);
		HardTokenDataWS hardTokenDataWS = new HardTokenDataWS();
		hardTokenDataWS.setLabel(HardTokenConstants.LABEL_PROJECTCARD);
		hardTokenDataWS.setTokenType(HardTokenConstants.TOKENTYPE_SWEDISHEID);
		hardTokenDataWS.setHardTokenSN(serialNumber);		
		PinDataWS basicPinDataWS = new PinDataWS();
		basicPinDataWS.setType(HardTokenConstants.PINTYPE_BASIC);
		basicPinDataWS.setInitialPIN("1234");
		basicPinDataWS.setPUK("12345678");
		PinDataWS signaturePinDataWS = new PinDataWS();
		signaturePinDataWS.setType(HardTokenConstants.PINTYPE_SIGNATURE);
		signaturePinDataWS.setInitialPIN("5678");
		signaturePinDataWS.setPUK("23456789");
		hardTokenDataWS.getPinDatas().add(basicPinDataWS);
		hardTokenDataWS.getPinDatas().add(signaturePinDataWS);
		List<TokenCertificateResponseWS> responses = ejbcaraws.genTokenCertificates(tokenUser1, requests, hardTokenDataWS, true, false);
		assertTrue(responses.size() == 2);
		getCertStore().removeCertificateProfile(intAdmin, "WSTESTPROFILE");
		gc.setEnableEndEntityProfileLimitations(originalProfileSetting);
    	getRAAdmin().saveGlobalConfiguration(intAdmin, gc);
	} // createHardToken
}
