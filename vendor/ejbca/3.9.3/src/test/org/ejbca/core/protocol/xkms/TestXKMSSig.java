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

package org.ejbca.core.protocol.xkms;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.FileInputStream;
import java.security.Key;
import java.security.KeyPair;
import java.security.KeyStore;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPublicKey;
import java.util.ArrayList;
import java.util.Random;

import javax.crypto.SecretKey;
import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBElement;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Marshaller;
import javax.xml.bind.PropertyException;
import javax.xml.parsers.DocumentBuilderFactory;

import junit.framework.TestCase;

import org.apache.log4j.Logger;
import org.apache.xml.security.utils.XMLUtils;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.protocol.xkms.client.XKMSInvoker;
import org.ejbca.core.protocol.xkms.common.XKMSConstants;
import org.ejbca.core.protocol.xkms.common.XKMSNamespacePrefixMapper;
import org.ejbca.core.protocol.xkms.common.XKMSUtil;
import org.ejbca.ui.cli.batch.BatchMakeP12;
import org.ejbca.util.CertTools;
import org.ejbca.util.TestTools;
import org.ejbca.util.keystore.KeyTools;
import org.w3._2000._09.xmldsig_.KeyInfoType;
import org.w3._2000._09.xmldsig_.RSAKeyValueType;
import org.w3._2002._03.xkms_.ObjectFactory;
import org.w3._2002._03.xkms_.PrototypeKeyBindingType;
import org.w3._2002._03.xkms_.QueryKeyBindingType;
import org.w3._2002._03.xkms_.RegisterRequestType;
import org.w3._2002._03.xkms_.UseKeyWithType;
import org.w3._2002._03.xkms_.ValidateRequestType;
import org.w3._2002._03.xkms_.ValidateResultType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * To Run this test, there must be a CA with DN "CN=AdminCA1,O=EJBCA Sample,C=SE", and it must have XKMS service enabled.
 * Also you have to enable XKMS in conf/xkms.properties.
 * 
 * @author Philip Vendil 2006 sep 27 
 *
 * @version $Id: TestXKMSSig.java 6668 2008-11-28 16:28:44Z jeklund $
 */

public class TestXKMSSig extends TestCase {
	
	private static Logger log = Logger.getLogger(TestXKMSSig.class);
		
	private ObjectFactory xKMSObjectFactory = new ObjectFactory();	
	private org.w3._2000._09.xmldsig_.ObjectFactory sigFactory = new org.w3._2000._09.xmldsig_.ObjectFactory();
	
	private static String baseUsername;
	
	private static String username;
	private static File tmpfile;
	private static File keystorefile;
	
	private static JAXBContext jAXBContext = null;
	private static Marshaller marshaller = null;
	//private static Unmarshaller unmarshaller = null;
	private static DocumentBuilderFactory dbf = null;

	private static int caid;
	
	static{    	
		try {
			CertTools.installBCProvider();
			org.apache.xml.security.Init.init();

			jAXBContext = JAXBContext.newInstance("org.w3._2002._03.xkms_:org.w3._2001._04.xmlenc_:org.w3._2000._09.xmldsig_");    		
			marshaller = jAXBContext.createMarshaller();
			try {
				marshaller.setProperty("com.sun.xml.bind.namespacePrefixMapper",new XKMSNamespacePrefixMapper());
			} catch( PropertyException e ) {
				log.error("Error registering namespace mapper property",e);
			}
			dbf = DocumentBuilderFactory.newInstance();
			dbf.setNamespaceAware(true);
			//unmarshaller = jAXBContext.createUnmarshaller();

		} catch (JAXBException e) {
			log.error("Error initializing RequestAbstractTypeResponseGenerator",e);
		}

	}

    protected void setUp() throws Exception {
        log.trace(">setUp()"); 
    	caid = CertTools.stringToBCDNString("CN=AdminCA1,O=EJBCA Sample,C=SE").hashCode();
        Random ran = new Random();
        if(baseUsername == null){
          baseUsername = "xkmstestadmin" + (ran.nextInt() % 1000) + "-";
        }
        log.trace("<setUp()");
    }

    protected void tearDown() throws Exception {
    }

    
    public void test00SetupAccessRights() throws Exception {
    	Admin administrator = new Admin(Admin.TYPE_RA_USER);
        Object o = null;
        username = baseUsername + "1";
        try {
        	TestTools.getUserAdminSession().addUser(administrator, username, "foo123", "CN=superadmin", null,null, false,
                    SecConst.EMPTY_ENDENTITYPROFILE, SecConst.CERTPROFILE_FIXED_ENDUSER,
                    SecConst.USER_ADMINISTRATOR, SecConst.TOKEN_SOFT_JKS, 0, caid);
        	TestTools.getUserAdminSession().setClearTextPassword(administrator, username, "foo123");
            o = new String("");
        } catch (Exception e) {
            assertNotNull("Failed to create user " + username, o);
        }
        
        BatchMakeP12 makep12 = new BatchMakeP12();
        tmpfile = new File("p12");

        //System.out.println("tempdir="+tmpfile.getParent());
        makep12.setMainStoreDir(tmpfile.getAbsolutePath());
        makep12.createAllNew();
    	
    } 
    
    public void test01ClientSignature() throws Exception {    	    	
    	KeyStore clientKeyStore = Constants.getUserKeyStore();
    	
    	// Test simple validate
    	ValidateRequestType validateRequestType = xKMSObjectFactory.createValidateRequestType();
    	validateRequestType.setId("200");       	
        	
        UseKeyWithType useKeyWithType = xKMSObjectFactory.createUseKeyWithType();
        useKeyWithType.setApplication(XKMSConstants.USEKEYWITH_TLSHTTP);
        useKeyWithType.setIdentifier("Test");
        
        validateRequestType.getRespondWith().add(XKMSConstants.RESPONDWITH_X509CHAIN);
    	
        QueryKeyBindingType queryKeyBindingType = xKMSObjectFactory.createQueryKeyBindingType();
        queryKeyBindingType.getUseKeyWith().add(useKeyWithType);
        validateRequestType.setQueryKeyBinding(queryKeyBindingType);

        JAXBElement<ValidateRequestType> validateRequest = xKMSObjectFactory.createValidateRequest(validateRequestType);
        
        
        String alias = "TEST";
        java.security.cert.X509Certificate pkCert = (java.security.cert.X509Certificate)clientKeyStore.getCertificate(alias);
            
        Key key = clientKeyStore.getKey(alias,"foo123".toCharArray());
        
		Document doc = dbf.newDocumentBuilder().newDocument();
		marshaller.marshal( validateRequest, doc );

		org.apache.xml.security.signature.XMLSignature xmlSig = new org.apache.xml.security.signature.XMLSignature(doc, "", org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, org.apache.xml.security.c14n.Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		org.apache.xml.security.transforms.Transforms transforms = new org.apache.xml.security.transforms.Transforms(doc);
		transforms.addTransform(org.apache.xml.security.transforms.Transforms.TRANSFORM_ENVELOPED_SIGNATURE);
		transforms.addTransform(org.apache.xml.security.transforms.Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
		xmlSig.addDocument("#" + validateRequest.getValue().getId(), transforms, org.apache.xml.security.utils.Constants.ALGO_ID_DIGEST_SHA1);        			
		xmlSig.addKeyInfo(pkCert);
		doc.getDocumentElement().insertBefore( xmlSig.getElement() ,doc.getDocumentElement().getFirstChild());
		xmlSig.sign(key);   
		
		//DOMSource dOMSource = new DOMSource(doc);
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
        
	    XMLUtils.outputDOMc14nWithComments(doc, System.out);
	    
	    XMLUtils.outputDOMc14nWithComments(doc, baos);
	    
	    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
	    
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        
        Document doc2 = db.parse(bais);
        
        XMLUtils.outputDOMc14nWithComments(doc2, System.out);
        
        org.w3c.dom.NodeList xmlSigs = doc2.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");					
		org.w3c.dom.Element xmlSigElement = (org.w3c.dom.Element)xmlSigs.item(0);        
		org.apache.xml.security.signature.XMLSignature xmlVerifySig = new org.apache.xml.security.signature.XMLSignature(xmlSigElement, null);
        
		org.apache.xml.security.keys.KeyInfo keyInfo = xmlVerifySig.getKeyInfo();
		java.security.cert.X509Certificate verCert = keyInfo.getX509Certificate();
		
		assertTrue(xmlVerifySig.checkSignatureValue(verCert));		

    }
    public void test02SendSignedRequest() throws Exception {    	    	
    	
    	KeyStore clientKeyStore = KeyStore.getInstance("JKS");
    	keystorefile = new File(tmpfile.getAbsolutePath() + "/" + username + ".jks");
    	clientKeyStore.load(new FileInputStream(keystorefile), "foo123".toCharArray());
    	
        String alias = "superadmin";       
        java.security.cert.X509Certificate pkCert = (java.security.cert.X509Certificate)clientKeyStore.getCertificate(alias);            
        Key key = clientKeyStore.getKey(alias,"foo123".toCharArray());
    	Certificate[] trustedcerts = clientKeyStore.getCertificateChain(alias);
    	ArrayList trustcol = new ArrayList();
    	for(int i=0;i<trustedcerts.length;i++ ){
    		if(((X509Certificate)trustedcerts[i]).getBasicConstraints() != -1){
    		  trustcol.add(trustedcerts[i]);
    		}
    	}
    	
    	XKMSInvoker xKMSInvoker = new XKMSInvoker("http://localhost:8080/ejbca/xkms/xkms",trustcol);
    	
    	// Test simple validate   	
    	ValidateRequestType validateRequestType = xKMSObjectFactory.createValidateRequestType();
    	validateRequestType.setId("200");       	
        	
    	
        UseKeyWithType useKeyWithType = xKMSObjectFactory.createUseKeyWithType();
        useKeyWithType.setApplication(XKMSConstants.USEKEYWITH_TLSHTTP);
        useKeyWithType.setIdentifier("Test");
        
        validateRequestType.getRespondWith().add(XKMSConstants.RESPONDWITH_X509CHAIN);
    	
        QueryKeyBindingType queryKeyBindingType = xKMSObjectFactory.createQueryKeyBindingType();
        queryKeyBindingType.getUseKeyWith().add(useKeyWithType);
        validateRequestType.setQueryKeyBinding(queryKeyBindingType);

        JAXBElement<ValidateRequestType> validateRequest = xKMSObjectFactory.createValidateRequest(validateRequestType);
        

        
		Document doc = dbf.newDocumentBuilder().newDocument();
		marshaller.marshal( validateRequest, doc );
	    
        ValidateResultType validateResultType = xKMSInvoker.validate(validateRequestType, pkCert, key);
        
        
        assertTrue(validateResultType.getRequestId().equals("200"));
        assertTrue(validateResultType.getResultMajor().equals(XKMSConstants.RESULTMAJOR_SUCCESS));
 
    }	    
	    
    public void test03SendUntrustedRequest() throws Exception {    	    	
    	KeyStore clientKeyStore = Constants.getUserKeyStore();
    	KeyStore trustKeyStore = KeyStore.getInstance("JKS");
    	keystorefile = new File(tmpfile.getAbsolutePath() + "/" + username + ".jks");
    	trustKeyStore.load(new FileInputStream(keystorefile), "foo123".toCharArray());
    	
        String alias = "TEST";       
        java.security.cert.X509Certificate pkCert = (java.security.cert.X509Certificate)clientKeyStore.getCertificate(alias);            
        Key key = clientKeyStore.getKey(alias,"foo123".toCharArray());
    	Certificate[] trustedcerts = trustKeyStore.getCertificateChain("superadmin");
    	ArrayList trustcol = new ArrayList();
    	for(int i=0;i<trustedcerts.length;i++ ){
    		if(((X509Certificate)trustedcerts[i]).getBasicConstraints() != -1){
    		  trustcol.add(trustedcerts[i]);
    		}
    	}
    	
    	XKMSInvoker xKMSInvoker = new XKMSInvoker("http://localhost:8080/ejbca/xkms/xkms",trustcol);
    	
    	// Test simple validate   	
    	ValidateRequestType validateRequestType = xKMSObjectFactory.createValidateRequestType();
    	validateRequestType.setId("201");       	
        	
    	
        UseKeyWithType useKeyWithType = xKMSObjectFactory.createUseKeyWithType();
        useKeyWithType.setApplication(XKMSConstants.USEKEYWITH_TLSHTTP);
        useKeyWithType.setIdentifier("Test");
        
        validateRequestType.getRespondWith().add(XKMSConstants.RESPONDWITH_X509CHAIN);
    	
        QueryKeyBindingType queryKeyBindingType = xKMSObjectFactory.createQueryKeyBindingType();
        queryKeyBindingType.getUseKeyWith().add(useKeyWithType);
        validateRequestType.setQueryKeyBinding(queryKeyBindingType);

        JAXBElement<ValidateRequestType> validateRequest = xKMSObjectFactory.createValidateRequest(validateRequestType);
        

        
		Document doc = dbf.newDocumentBuilder().newDocument();
		marshaller.marshal( validateRequest, doc );
	    
        ValidateResultType validateResultType = xKMSInvoker.validate(validateRequestType, pkCert, key);
        
        
        assertTrue(validateResultType.getRequestId().equals("201"));
        assertTrue(validateResultType.getResultMajor().equals(XKMSConstants.RESULTMAJOR_SENDER));
        assertTrue(validateResultType.getResultMinor().equals(XKMSConstants.RESULTMINOR_NOAUTHENTICATION));
    }		
    
    public void test04SendRevokedRequest() throws Exception {    	    	
    	
    	TestTools.getUserAdminSession().revokeUser(new Admin(Admin.TYPE_RA_USER), username, RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE);
    	
    	KeyStore clientKeyStore = KeyStore.getInstance("JKS");
    	keystorefile = new File(tmpfile.getAbsolutePath() + "/" + username + ".jks");
    	clientKeyStore.load(new FileInputStream(keystorefile), "foo123".toCharArray());
    	
        String alias = "superadmin";       
        java.security.cert.X509Certificate pkCert = (java.security.cert.X509Certificate)clientKeyStore.getCertificate(alias);            
        Key key = clientKeyStore.getKey(alias,"foo123".toCharArray());
    	Certificate[] trustedcerts = clientKeyStore.getCertificateChain(alias);
    	ArrayList trustcol = new ArrayList();
    	for(int i=0;i<trustedcerts.length;i++ ){
    		if(((X509Certificate)trustedcerts[i]).getBasicConstraints() != -1){
    		  trustcol.add(trustedcerts[i]);
    		}
    	}
    	
    	XKMSInvoker xKMSInvoker = new XKMSInvoker("http://localhost:8080/ejbca/xkms/xkms",trustcol);
    	
    	// Test simple validate   	
    	ValidateRequestType validateRequestType = xKMSObjectFactory.createValidateRequestType();
    	validateRequestType.setId("200");       	
        	
    	
        UseKeyWithType useKeyWithType = xKMSObjectFactory.createUseKeyWithType();
        useKeyWithType.setApplication(XKMSConstants.USEKEYWITH_TLSHTTP);
        useKeyWithType.setIdentifier("Test");
        
        validateRequestType.getRespondWith().add(XKMSConstants.RESPONDWITH_X509CHAIN);
    	
        QueryKeyBindingType queryKeyBindingType = xKMSObjectFactory.createQueryKeyBindingType();
        queryKeyBindingType.getUseKeyWith().add(useKeyWithType);
        validateRequestType.setQueryKeyBinding(queryKeyBindingType);

        JAXBElement<ValidateRequestType> validateRequest = xKMSObjectFactory.createValidateRequest(validateRequestType);
        

        
		Document doc = dbf.newDocumentBuilder().newDocument();
		marshaller.marshal( validateRequest, doc );
	    
        ValidateResultType validateResultType = xKMSInvoker.validate(validateRequestType, pkCert, key);
        
        
        assertTrue(validateResultType.getRequestId().equals("200"));
        assertTrue(validateResultType.getResultMajor().equals(XKMSConstants.RESULTMAJOR_SENDER));
        assertTrue(validateResultType.getResultMinor().equals(XKMSConstants.RESULTMINOR_NOAUTHENTICATION));

    }
    public void test05POPSignature() throws Exception {    	    	

    	KeyStore clientKeyStore = Constants.getUserKeyStore();
    	
        String alias = "TEST";
        java.security.cert.X509Certificate pkCert = (java.security.cert.X509Certificate)clientKeyStore.getCertificate(alias);
            
        Key key = clientKeyStore.getKey(alias,"foo123".toCharArray());
    	
    	RegisterRequestType registerRequestType = xKMSObjectFactory.createRegisterRequestType();
    	registerRequestType.setId("500");       	
        	
        UseKeyWithType useKeyWithType = xKMSObjectFactory.createUseKeyWithType();
        useKeyWithType.setApplication(XKMSConstants.USEKEYWITH_PKIX);
        useKeyWithType.setIdentifier("CN=Test Testarsson");
        
        registerRequestType.getRespondWith().add(XKMSConstants.RESPONDWITH_X509CHAIN);
    	
        KeyInfoType keyInfoType = sigFactory.createKeyInfoType();
        RSAKeyValueType rsaKeyValueType = sigFactory.createRSAKeyValueType();
        rsaKeyValueType.setExponent(((RSAPublicKey) pkCert.getPublicKey()).getPublicExponent().toByteArray());
        rsaKeyValueType.setModulus(((RSAPublicKey) pkCert.getPublicKey()).getModulus().toByteArray());
        JAXBElement<RSAKeyValueType> rsaKeyValue = sigFactory.createRSAKeyValue(rsaKeyValueType);
        keyInfoType.getContent().add(rsaKeyValue);
        PrototypeKeyBindingType prototypeKeyBindingType = xKMSObjectFactory.createPrototypeKeyBindingType();
        prototypeKeyBindingType.getUseKeyWith().add(useKeyWithType);
        prototypeKeyBindingType.setKeyInfo(keyInfoType);
        prototypeKeyBindingType.setId("100231");
        registerRequestType.setPrototypeKeyBinding(prototypeKeyBindingType);                
        JAXBElement<RegisterRequestType> registerRequest = xKMSObjectFactory.createRegisterRequest(registerRequestType);
                        
		Document registerRequestDoc = dbf.newDocumentBuilder().newDocument();
		marshaller.marshal( registerRequest, registerRequestDoc );

		Element prototypeKeyBindingTag = (Element) registerRequestDoc.getDocumentElement().getElementsByTagNameNS("http://www.w3.org/2002/03/xkms#", "PrototypeKeyBinding").item(0);
        assertTrue(prototypeKeyBindingTag != null);
        
		org.apache.xml.security.signature.XMLSignature xmlSig = new org.apache.xml.security.signature.XMLSignature(registerRequestDoc, "", org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, org.apache.xml.security.c14n.Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		org.apache.xml.security.transforms.Transforms transforms = new org.apache.xml.security.transforms.Transforms(registerRequestDoc);		
		transforms.addTransform(org.apache.xml.security.transforms.Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
		xmlSig.addDocument("#" + prototypeKeyBindingType.getId(), transforms, org.apache.xml.security.utils.Constants.ALGO_ID_DIGEST_SHA1);        			
	
		xmlSig.sign(key);   
		
        Element pOPElement = registerRequestDoc.createElementNS("http://www.w3.org/2002/03/xkms#", "ProofOfPossession");
        pOPElement.appendChild(xmlSig.getElement().cloneNode(true));
        registerRequestDoc.getDocumentElement().appendChild(pOPElement);
        
        XMLUtils.outputDOMc14nWithComments(registerRequestDoc, System.out);
		
        
                        
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

	    XMLUtils.outputDOMc14nWithComments(registerRequestDoc, baos);
	    
	    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
	    
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        
        Document doc2 = db.parse(bais);
        
        XMLUtils.outputDOMc14nWithComments(doc2, System.out);
        
        org.w3c.dom.NodeList xmlSigs = doc2.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");					
		org.w3c.dom.Element xmlSigElement = (org.w3c.dom.Element)xmlSigs.item(0);        
		org.apache.xml.security.signature.XMLSignature xmlVerifySig = new org.apache.xml.security.signature.XMLSignature(xmlSigElement, null);
        
		
		assertTrue(xmlVerifySig.checkSignatureValue(pkCert.getPublicKey()));		

		KeyPair keyPair= KeyTools.genKeys("1024", "RSA");
		assertFalse(xmlVerifySig.checkSignatureValue(keyPair.getPublic()));
    }       
        
	
    public void test06AuthenticationKeyBindingSignature() throws Exception {    	    	    	
    	KeyStore clientKeyStore = Constants.getUserKeyStore();
    	KeyPair keyPair= KeyTools.genKeys("1024", "RSA");    	
    	
        String alias = "TEST";
        java.security.cert.X509Certificate pkCert = (java.security.cert.X509Certificate)clientKeyStore.getCertificate(alias);                    
    	
    	RegisterRequestType registerRequestType = xKMSObjectFactory.createRegisterRequestType();
    	registerRequestType.setId("500");       	
        	
        UseKeyWithType useKeyWithType = xKMSObjectFactory.createUseKeyWithType();
        useKeyWithType.setApplication(XKMSConstants.USEKEYWITH_PKIX);
        useKeyWithType.setIdentifier("CN=Test Testarsson");
        
        registerRequestType.getRespondWith().add(XKMSConstants.RESPONDWITH_X509CHAIN);
    	
        KeyInfoType keyInfoType = sigFactory.createKeyInfoType();
        RSAKeyValueType rsaKeyValueType = sigFactory.createRSAKeyValueType();
        rsaKeyValueType.setExponent(((RSAPublicKey) keyPair.getPublic()).getPublicExponent().toByteArray());
        rsaKeyValueType.setModulus(((RSAPublicKey) keyPair.getPublic()).getModulus().toByteArray());
        JAXBElement<RSAKeyValueType> rsaKeyValue = sigFactory.createRSAKeyValue(rsaKeyValueType);
        keyInfoType.getContent().add(rsaKeyValue);
        PrototypeKeyBindingType prototypeKeyBindingType = xKMSObjectFactory.createPrototypeKeyBindingType();
        prototypeKeyBindingType.getUseKeyWith().add(useKeyWithType);
        prototypeKeyBindingType.setKeyInfo(keyInfoType);
        prototypeKeyBindingType.setId("100231");
        registerRequestType.setPrototypeKeyBinding(prototypeKeyBindingType);                
        JAXBElement<RegisterRequestType> registerRequest = xKMSObjectFactory.createRegisterRequest(registerRequestType);
                        
		Document registerRequestDoc = dbf.newDocumentBuilder().newDocument();
		marshaller.marshal( registerRequest, registerRequestDoc );
        
        String authenticationData= "024837";
		
        SecretKey sk = XKMSUtil.getSecretKeyFromPassphrase(authenticationData, true, 20, XKMSUtil.KEY_AUTHENTICATION);
		
		org.apache.xml.security.signature.XMLSignature authXMLSig = new org.apache.xml.security.signature.XMLSignature(registerRequestDoc, "", org.apache.xml.security.signature.XMLSignature.ALGO_ID_MAC_HMAC_SHA1, org.apache.xml.security.c14n.Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		org.apache.xml.security.transforms.Transforms transforms = new org.apache.xml.security.transforms.Transforms(registerRequestDoc);		
		transforms.addTransform(org.apache.xml.security.transforms.Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
		authXMLSig.addDocument("#" + prototypeKeyBindingType.getId(), transforms, org.apache.xml.security.utils.Constants.ALGO_ID_DIGEST_SHA1);        			
	
		authXMLSig.sign(sk); 
		
		Element authenticationElement = registerRequestDoc.createElementNS("http://www.w3.org/2002/03/xkms#", "Authentication");
		Element keyBindingAuthenticationElement = registerRequestDoc.createElementNS("http://www.w3.org/2002/03/xkms#", "KeyBindingAuthentication");
		keyBindingAuthenticationElement.appendChild(authXMLSig.getElement().cloneNode(true));
		authenticationElement.appendChild(keyBindingAuthenticationElement);
        registerRequestDoc.getDocumentElement().appendChild(authenticationElement);
								
		org.apache.xml.security.signature.XMLSignature xmlSig = new org.apache.xml.security.signature.XMLSignature(registerRequestDoc, "", org.apache.xml.security.signature.XMLSignature.ALGO_ID_SIGNATURE_RSA_SHA1, org.apache.xml.security.c14n.Canonicalizer.ALGO_ID_C14N_EXCL_OMIT_COMMENTS);
		transforms = new org.apache.xml.security.transforms.Transforms(registerRequestDoc);		
		transforms.addTransform(org.apache.xml.security.transforms.Transforms.TRANSFORM_C14N_EXCL_OMIT_COMMENTS);
		
		xmlSig.addDocument("#" + prototypeKeyBindingType.getId(), transforms, org.apache.xml.security.utils.Constants.ALGO_ID_DIGEST_SHA1);        			
	
		xmlSig.sign(keyPair.getPrivate());   
		
        Element pOPElement = registerRequestDoc.createElementNS("http://www.w3.org/2002/03/xkms#", "ProofOfPossession");
        pOPElement.appendChild(xmlSig.getElement().cloneNode(true));
        registerRequestDoc.getDocumentElement().appendChild(pOPElement);
        
        XMLUtils.outputDOMc14nWithComments(registerRequestDoc, System.out);		        
                        
		ByteArrayOutputStream baos = new ByteArrayOutputStream();

	    XMLUtils.outputDOMc14nWithComments(registerRequestDoc, baos);
	    
	    ByteArrayInputStream bais = new ByteArrayInputStream(baos.toByteArray());
	    
        javax.xml.parsers.DocumentBuilder db = dbf.newDocumentBuilder();
        
        Document doc2 = db.parse(bais);
        
        XMLUtils.outputDOMc14nWithComments(doc2, System.out);                
        
		// Verify the authentication
        org.w3c.dom.NodeList authenticationElements = doc2.getElementsByTagNameNS("http://www.w3.org/2002/03/xkms#", "Authentication");
        assertTrue(authenticationElements.getLength() == 1);
        Element ae = (Element) authenticationElements.item(0);
        
        org.w3c.dom.NodeList xmlSigs = ae.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
        
		org.w3c.dom.Element xmlSigElement = (org.w3c.dom.Element)xmlSigs.item(0);        
		org.apache.xml.security.signature.XMLSignature xmlVerifySig = new org.apache.xml.security.signature.XMLSignature(xmlSigElement, null);
			
		assertTrue(xmlVerifySig.checkSignatureValue(sk));		
		
		// Verify the pop
        org.w3c.dom.NodeList pOPElements = doc2.getElementsByTagNameNS("http://www.w3.org/2002/03/xkms#", "ProofOfPossession");
        assertTrue(pOPElements.getLength() == 1);
        Element pOPe = (Element) pOPElements.item(0);
        org.w3c.dom.NodeList popVerXmlSigs = pOPe.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
        assertTrue(popVerXmlSigs.getLength() == 1);
		org.w3c.dom.Element popVerXmlSigElement = (org.w3c.dom.Element)popVerXmlSigs.item(0);        
		org.apache.xml.security.signature.XMLSignature popVerXmlSig = new org.apache.xml.security.signature.XMLSignature(popVerXmlSigElement, null);
		assertTrue(popVerXmlSig.checkSignatureValue(keyPair.getPublic()));
		assertFalse(popVerXmlSig.checkSignatureValue(pkCert.getPublicKey()));		
    }
    
    public void test99RemoveUser() throws Exception {
    	Admin administrator = new Admin(Admin.TYPE_RA_USER);
    	TestTools.getUserAdminSession().deleteUser(administrator, username);
    	keystorefile.deleteOnExit();
    } 
}
