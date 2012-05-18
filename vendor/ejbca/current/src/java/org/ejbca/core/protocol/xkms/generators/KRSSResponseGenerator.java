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

package org.ejbca.core.protocol.xkms.generators;

import gnu.inet.encoding.StringprepException;

import java.io.UnsupportedEncodingException;
import java.math.BigInteger;
import java.security.KeyFactory;
import java.security.KeyPair;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.PublicKey;
import java.security.cert.CertPath;
import java.security.cert.CertPathValidator;
import java.security.cert.CertPathValidatorException;
import java.security.cert.CertStore;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.cert.CollectionCertStoreParameters;
import java.security.cert.PKIXParameters;
import java.security.cert.TrustAnchor;
import java.security.cert.X509Certificate;
import java.security.interfaces.RSAPrivateCrtKey;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.RSAPublicKeySpec;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import javax.crypto.SecretKey;
import javax.xml.bind.JAXBElement;

import org.apache.log4j.Logger;
import org.apache.xml.security.encryption.XMLEncryptionException;
import org.apache.xml.security.exceptions.XMLSecurityException;
import org.apache.xml.security.signature.XMLSignatureException;
import org.bouncycastle.util.encoders.Hex;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.keyrecovery.KeyRecoveryData;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.core.protocol.xkms.common.XKMSConstants;
import org.ejbca.core.protocol.xkms.common.XKMSUtil;
import org.ejbca.util.CertTools;
import org.ejbca.util.keystore.KeyTools;
import org.w3._2000._09.xmldsig_.RSAKeyValueType;
import org.w3._2000._09.xmldsig_.X509DataType;
import org.w3._2002._03.xkms_.NotBoundAuthenticationType;
import org.w3._2002._03.xkms_.RegisterRequestType;
import org.w3._2002._03.xkms_.RequestAbstractType;
import org.w3._2002._03.xkms_.ResultType;
import org.w3._2002._03.xkms_.RevokeRequestType;
import org.w3._2002._03.xkms_.UseKeyWithType;
import org.w3c.dom.Document;
import org.w3c.dom.Element;

/**
 * Class generating a common response for register, reissue and recover calls
 * 
 * 
 * @author Philip Vendil 
 *
 * @version $Id: KRSSResponseGenerator.java 5718 2008-06-10 13:03:58Z anatom $
 */

public class KRSSResponseGenerator extends
		RequestAbstractTypeResponseGenerator {
	
	 private static Logger log = Logger.getLogger(KRSSResponseGenerator.class);
	
	 private static final InternalResources intres = InternalResources.getInstance();
	 
	 protected Document requestDoc = null;

	public KRSSResponseGenerator(String remoteIP, RequestAbstractType req, Document requestDoc) {
		super(remoteIP, req);
		this.requestDoc = requestDoc;
	}
	
	/**
	 * Method extracting the public key from the message.
	 * @param req the request
	 * @return the public key as and PublicKey or Certificate or null if no public key could be found.
	 */
	protected Object getPublicKeyInfo(RequestAbstractType req, boolean registerRequest){
		Object retval = null;
				
		
		if(GeneralizedKRSSMessageHelper.getKeyBindingAbstractType(req).getKeyInfo() != null && GeneralizedKRSSMessageHelper.getKeyBindingAbstractType(req).getKeyInfo().getContent().get(0) != null){
			try{
				JAXBElement element = (JAXBElement) GeneralizedKRSSMessageHelper.getKeyBindingAbstractType(req).getKeyInfo().getContent().get(0);
				if(element.getValue() instanceof RSAKeyValueType && registerRequest){
					RSAKeyValueType rSAKeyValueType  = (RSAKeyValueType) ((JAXBElement) GeneralizedKRSSMessageHelper.getKeyBindingAbstractType(req).getKeyInfo().getContent().get(0)).getValue();        
					RSAPublicKeySpec rSAPublicKeySpec = new RSAPublicKeySpec(new BigInteger(rSAKeyValueType.getModulus()), new BigInteger(rSAKeyValueType.getExponent()));        
					retval= KeyFactory.getInstance("RSA").generatePublic(rSAPublicKeySpec);
				}
				if(element.getValue() instanceof X509DataType){
					Iterator iter = ((X509DataType) element.getValue()).getX509IssuerSerialOrX509SKIOrX509SubjectName().iterator();
					while(iter.hasNext()){
						JAXBElement next = (JAXBElement) iter.next();					
						if(next.getName().getLocalPart().equals("X509Certificate")){
							byte[] encoded = (byte[]) next.getValue();

							try {
								X509Certificate nextCert = (X509Certificate)CertTools.getCertfromByteArray(encoded);
								if(nextCert.getBasicConstraints() == -1){
									retval = nextCert;
								}
							} catch (CertificateException e) {
								log.error(intres.getLocalizedMessage("xkms.errordecodingcert"),e);								
								resultMajor = XKMSConstants.RESULTMAJOR_RECIEVER;
								resultMinor = XKMSConstants.RESULTMINOR_FAILURE;
							}

						}else{
							resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
							resultMinor = XKMSConstants.RESULTMINOR_MESSAGENOTSUPPORTED;
						}
					}
				}
			
				if(retval == null){
					resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
					resultMinor = XKMSConstants.RESULTMINOR_MESSAGENOTSUPPORTED;
				}
				
			} catch (InvalidKeySpecException e) {
				log.error(e);
				resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
				resultMinor = XKMSConstants.RESULTMINOR_MESSAGENOTSUPPORTED;
			} catch (NoSuchAlgorithmException e) {
				log.error(e);
				resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
				resultMinor = XKMSConstants.RESULTMINOR_MESSAGENOTSUPPORTED;
			}
		}
		
		
		
		return retval;
	}
	
	/**
     * Method performing the actual certificate generation, from the subjectDN and password
     * @param revocationCode The code used later by the user to revoke, it it is allowed by the XKMS Service
     * @return the generated certificate or null if generation failed
     */
    protected X509Certificate registerReissueOrRecover(boolean recover, boolean reissue, ResultType response, UserDataVO userDataVO, String password,  
    		                                  PublicKey publicKey, String revocationCode) {
		X509Certificate retval = null;
    	
		// Check the status of the user
		if((!recover && userDataVO.getStatus() == UserDataConstants.STATUS_NEW) || (recover && userDataVO.getStatus() == UserDataConstants.STATUS_KEYRECOVERY)){
				
			try{		
				boolean usekeyrecovery = !reissue && (getRAAdminSession().loadGlobalConfiguration(pubAdmin)).getEnableKeyRecovery();

				boolean savekeys = userDataVO.getKeyRecoverable() && usekeyrecovery &&  (userDataVO.getStatus() != UserDataConstants.STATUS_KEYRECOVERY);
				boolean loadkeys = (userDataVO.getStatus() == UserDataConstants.STATUS_KEYRECOVERY) && usekeyrecovery;

				// get users Token Type.
				int tokentype = userDataVO.getTokenType();

				PublicKey certKey = null;
				PrivateKey privKey = null;
				KeyPair keyPair = null;
				KeyRecoveryData keyData = null;
				boolean reusecertificate = false;
				if(loadkeys){
					EndEntityProfile endEntityProfile = getRAAdminSession().getEndEntityProfile(pubAdmin, userDataVO.getEndEntityProfileId());
					reusecertificate = endEntityProfile.getReUseKeyRevoceredCertificate();

					// used saved keys.
					keyData = getKeyRecoverySession().keyRecovery(pubAdmin, userDataVO.getUsername(), userDataVO.getEndEntityProfileId());
					keyPair = keyData.getKeyPair();
					certKey = keyPair.getPublic();
					privKey = keyPair.getPrivate();

					if(reusecertificate){
						getKeyRecoverySession().unmarkUser(pubAdmin,userDataVO.getUsername());
					}
				}
				else{
					// generate new keys.
					if(!reissue && (tokentype == SecConst.TOKEN_SOFT_P12 || tokentype == SecConst.TOKEN_SOFT_JKS || tokentype == SecConst.TOKEN_SOFT_PEM)){
						keyPair = KeyTools.genKeys(Integer.toString(XKMSConfig.getServerKeyLength()), "RSA");
						certKey = keyPair.getPublic();
						privKey = keyPair.getPrivate();
					}
					if(reissue || tokentype == SecConst.TOKEN_SOFT_BROWSERGEN){
						certKey = publicKey;
					}
				}

				X509Certificate cert = null;
				if(reusecertificate){
					cert = (X509Certificate) keyData.getCertificate();	             
					boolean finishUser = getCAAdminSession().getCAInfo(pubAdmin,CertTools.getIssuerDN(cert).hashCode()).getFinishUser();
					if(finishUser){	           	  
						getAuthenticationSession().finishUser(pubAdmin, userDataVO.getUsername(), password);
					}

				}else{        	 
					cert = (X509Certificate)getSignSession().createCertificate(pubAdmin, userDataVO.getUsername(), password, certKey);	 
				}

				if (savekeys) {
					// Save generated keys to database.	             
					getKeyRecoverySession().addKeyRecoveryData(pubAdmin, cert, userDataVO.getUsername(), keyPair);
				}

				// Save the revocation code
				if(revocationCode != null && !recover){
					UserDataVO data = getUserAdminSession().findUser(pubAdmin, userDataVO.getUsername());
					data.getExtendedinformation().setRevocationCodeIdentifier(revocationCode);
					getUserAdminSession().changeUser(raAdmin, data, true);

				}

				if(privKey != null){
					GeneralizedKRSSMessageHelper.setPrivateKey(response, XKMSUtil.getEncryptedXMLFromPrivateKey((RSAPrivateCrtKey) privKey, password));
				}

				retval = cert;
			}catch (Exception e) {
				log.error(intres.getLocalizedMessage("xkms.errorregisteringreq"),e);				
			} 

			if(retval == null){
				resultMajor = XKMSConstants.RESULTMAJOR_RECIEVER;
				resultMinor = XKMSConstants.RESULTMINOR_FAILURE;
			}
			
		}else{
			log.error(intres.getLocalizedMessage("xkms.errorinreqwrongstatus",new Integer(userDataVO.getStatus()),userDataVO.getUsername()));			
			resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
			resultMinor = XKMSConstants.RESULTMINOR_REFUSED;
		}
    	

		
		return retval;
	}
	
	protected boolean confirmPOP(PublicKey publicKey) {
    	boolean retval = false;
    	 // Check that POP is required
    	if(XKMSConfig.isPOPRequired() && publicKey != null){
    		// Get the public key 
    		try{
              
    			org.w3c.dom.NodeList pOPElements = requestDoc.getElementsByTagNameNS("http://www.w3.org/2002/03/xkms#", "ProofOfPossession");
    			if(pOPElements.getLength() == 1){
    				Element pOPe = (Element) pOPElements.item(0);
    				org.w3c.dom.NodeList popVerXmlSigs = pOPe.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");
    				org.w3c.dom.Element popVerXmlSigElement = (org.w3c.dom.Element)popVerXmlSigs.item(0);        
    				org.apache.xml.security.signature.XMLSignature popVerXmlSig = new org.apache.xml.security.signature.XMLSignature(popVerXmlSigElement, null);
    				if(popVerXmlSig.checkSignatureValue(publicKey)){
    					retval = true;
    				}
    			}
    			
    			if(!retval){
    				resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
    				resultMinor = XKMSConstants.RESULTMINOR_POPREQUIRED;    				  
    			} 
    		}catch(XMLSignatureException e){
    			log.error(e);
    			resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
    			resultMinor = XKMSConstants.RESULTMINOR_POPREQUIRED;
    		} catch (XMLSecurityException e) {
    			log.error(e);
    			resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
    			resultMinor = XKMSConstants.RESULTMINOR_POPREQUIRED;
    		}
	
    	}else{
    		retval = true;
    	}
    		
		return retval;
	}

	protected boolean isPasswordEncrypted(RequestAbstractType req) {
        if(GeneralizedKRSSMessageHelper.getAuthenticationType(req) == null){
        	return false;
        }
		return GeneralizedKRSSMessageHelper.getAuthenticationType(req).getKeyBindingAuthentication() != null;
	}
	
	protected UserDataVO findUserData(String subjectDN) {
		UserDataVO retval = null;
		
		if(subjectDN != null){
			try {
				retval = getUserAdminSession().findUserBySubjectDN(pubAdmin, subjectDN);
			} catch (AuthorizationDeniedException e) {
				log.error(intres.getLocalizedMessage("xkms.errorinprivs"),e);				
			}		
			if(retval==null){
				resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
				resultMinor = XKMSConstants.RESULTMINOR_NOMATCH;
			}
		}
		return retval;
	}
	
	/**
	 * Method finding the userdata of the specified cert or null
	 * if the user couldn't be foundl
	 */
	protected UserDataVO findUserData(X509Certificate cert) {
		UserDataVO retval = null;
        
		try {
			String username = getCertStoreSession().findUsernameByCertSerno(pubAdmin, cert.getSerialNumber(), CertTools.getIssuerDN(cert));
			retval = getUserAdminSession().findUser(pubAdmin, username);
		} catch (Exception e) {
			log.error(intres.getLocalizedMessage("xkms.errorfindinguserdata",cert.getSubjectDN().toString()));			
		}
		
		if(retval==null){
			resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
			resultMinor = XKMSConstants.RESULTMINOR_NOMATCH;
		}

		return retval;
	}
	
	/**
     * Method that extracts and verifies the password. Then returns the undigested 
     * password from database
     * @param req in Document encoding
     * @param password cleartext version from database
     * @return The password or null if the password doesn't verify
     */
	protected String getEncryptedPassword(Document reqDoc, String password) {
		String retval = null;
		
		try {
			SecretKey sk = XKMSUtil.getSecretKeyFromPassphrase(password, true, 20, XKMSUtil.KEY_AUTHENTICATION);
			org.w3c.dom.NodeList authenticationElements = reqDoc.getElementsByTagNameNS("http://www.w3.org/2002/03/xkms#", "Authentication");        
			Element ae = (Element) authenticationElements.item(0);        
			org.w3c.dom.NodeList xmlSigs = ae.getElementsByTagNameNS("http://www.w3.org/2000/09/xmldsig#", "Signature");

			org.w3c.dom.Element xmlSigElement = (org.w3c.dom.Element)xmlSigs.item(0);        
			org.apache.xml.security.signature.XMLSignature xmlVerifySig = new org.apache.xml.security.signature.XMLSignature(xmlSigElement, null);

			if(xmlVerifySig.checkSignatureValue(sk)){
				retval = password;
			}else{
				resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
				resultMinor = XKMSConstants.RESULTMINOR_NOAUTHENTICATION;	
			}
		} catch (Exception e) {
			log.error(intres.getLocalizedMessage("xkms.errorauthverification"),e);			
			resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
			resultMinor = XKMSConstants.RESULTMINOR_NOAUTHENTICATION;
		} 

		return retval;
	}

	/**
	 * Returns the password when having NotBoundAuthentication instead
	 * of KeyBindingAuthentication. 
	 * 
	 * @param req
	 * @return The password or null if no NotBoundAuthentication were found.
	 */
    protected String getClearPassword(RequestAbstractType req, String dBPassword) {
		String retval = null;
		NotBoundAuthenticationType notBoundAuthenticationType = GeneralizedKRSSMessageHelper.getAuthenticationType(req).getNotBoundAuthentication(); 
		if(notBoundAuthenticationType != null){
			retval = new String(notBoundAuthenticationType.getValue());
		}else{
			resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
			resultMinor = XKMSConstants.RESULTMINOR_MESSAGENOTSUPPORTED;
		}
		
		if(!retval.equals(dBPassword)){
			resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
			resultMinor = XKMSConstants.RESULTMINOR_NOAUTHENTICATION;
			retval = null;
		}
				
		return retval;
	}
	
	/**
	 * Method that returns the subject DN taken from a UseKeyWith PKIX tag
	 * If no such tag exist is null returned and errorcodes set.
	 * @param req
	 * @return the subjectDN of null
	 */
    protected String getSubjectDN(RequestAbstractType req) {
	    String retval = null;
		
	    Iterator<UseKeyWithType> iter = GeneralizedKRSSMessageHelper.getKeyBindingAbstractType(req).getUseKeyWith().iterator();
	    while(iter.hasNext()){
	    	UseKeyWithType next = iter.next();
	    	if(next.getApplication().equals(XKMSConstants.USEKEYWITH_PKIX)){
	    		retval = CertTools.stringToBCDNString(next.getIdentifier());
	    		break;
	    	}
	    }
	    
	    if(retval == null){
	    	resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
	    	resultMinor = XKMSConstants.RESULTMINOR_MESSAGENOTSUPPORTED;
	    }
	    
		return retval;
	}
	
	protected boolean certIsValid(X509Certificate cert) {
		boolean retval = false;
		
		try {
			CAInfo cAInfo = getCAAdminSession().getCAInfo(pubAdmin, CertTools.getIssuerDN(cert).hashCode());
			if(cAInfo != null){		
				Collection caCertChain = cAInfo.getCertificateChain();
				Iterator iter = caCertChain.iterator();
				
				boolean revoked = false;
				
				RevokedCertInfo certInfo = getCertStoreSession().isRevoked(pubAdmin, CertTools.getIssuerDN(cert), cert.getSerialNumber());
				if(certInfo.getReason() != RevokedCertInfo.NOT_REVOKED){
					revoked = true;
				}
				
				while(iter.hasNext()){
					X509Certificate cACert = (X509Certificate) iter.next();
					RevokedCertInfo caCertInfo = getCertStoreSession().isRevoked(pubAdmin, CertTools.getIssuerDN(cACert), cACert.getSerialNumber());
					if(caCertInfo.getReason() != RevokedCertInfo.NOT_REVOKED){
						revoked = true;
					}
					
				}
				
				if(!revoked){
				  retval = verifyCert(caCertChain, null, cert);
				}
			}
		} catch (Exception e) {
			log.error(e);
		}
		
		if(retval == false){
			resultMajor = XKMSConstants.RESULTMAJOR_SENDER;
			resultMinor = XKMSConstants.RESULTMINOR_REFUSED;
		}

		return retval;
	}
	


	   /**
  * method that verifies the certificate and returns an error message
  * @param cACertChain
  * @param trustedCRLs
  * @param cert
  * @return  true if everything is OK
  */
	private boolean verifyCert(Collection cACertChain, Collection trustedCRLs, X509Certificate usercert){
    
     boolean retval = false;
             
     try{                	        	                   	
     	X509Certificate rootCert = null;
     	Iterator iter = cACertChain.iterator();
     	while(iter.hasNext()){
     		X509Certificate cert = (X509Certificate) iter.next();
     		if(cert.getIssuerDN().equals(cert.getSubjectDN())){
     			rootCert = cert;
     			break;
     		}
     	}
     	
     	if(rootCert == null){
     		throw new CertPathValidatorException("Error Root CA cert not found in cACertChain"); 
     	}
     	
     	List list = new ArrayList();
     	list.add(usercert);
     	list.addAll(cACertChain);
     	if(trustedCRLs != null){
     		list.addAll(trustedCRLs);
     	}
     	
     	CollectionCertStoreParameters ccsp = new CollectionCertStoreParameters(list);
     	CertStore store = CertStore.getInstance("Collection", ccsp);
     	
     	//validating path
     	List certchain = new ArrayList();
     	certchain.addAll(cACertChain);
     	certchain.add(usercert);
     	CertPath cp = CertificateFactory.getInstance("X.509","BC").generateCertPath(certchain);
     	
     	Set trust = new HashSet();
     	trust.add(new TrustAnchor(rootCert, null));
     	
     	CertPathValidator cpv = CertPathValidator.getInstance("PKIX","BC");
     	PKIXParameters param = new PKIXParameters(trust);
     	param.addCertStore(store);
     	param.setDate(new Date());
     	if(trustedCRLs == null){
     		param.setRevocationEnabled(false);
     	}else{
     		param.setRevocationEnabled(true);
     	}
     	cpv.validate(cp, param);
     	retval = true;
     }catch(Exception e){
    	 log.error(intres.getLocalizedMessage("xkms.errorverifyingcert"),e);			

     } 

		
		return retval;
	}
    
	/**
	 * Method that checks that the given respondWith specification is valid.
	 * I.e contains one supported RespondWith tag.
	 */
	public boolean checkValidRespondWithRequest(List<String> respondWithList, boolean revokeCall){
		boolean returnval = false;
		if(revokeCall){
			returnval = true;
		}
		
		String[] supportedRespondWith = {XKMSConstants.RESPONDWITH_X509CERT,
				                         XKMSConstants.RESPONDWITH_X509CHAIN,
				                         XKMSConstants.RESPONDWITH_X509CRL,
				                         XKMSConstants.RESPONDWITH_PRIVATEKEY};		
	     
		for(int i=0;i<supportedRespondWith.length;i++){
		  returnval |= respondWithList.contains(supportedRespondWith[i]); 
		  if(returnval){
			  break;
		  }
		}
		  		
		return returnval;
	}
	
	/**
	 * Method returning the revocation code identifier or null
	 * if it doesn't exists.
	 * 
	 * @param req
	 * @return the RevocationCode or null if it doesn't exist.
	 */
    protected String getRevocationCode(RequestAbstractType req) {
    	String retval = null;
    	
    	if(req instanceof RegisterRequestType){
    		if(((RegisterRequestType) req).getPrototypeKeyBinding().getRevocationCodeIdentifier() != null){
    			retval = new String(Hex.encode(((RegisterRequestType) req).getPrototypeKeyBinding().getRevocationCodeIdentifier()));
    		}
    	}
    	if(req instanceof RevokeRequestType){
    		byte[] unMACedCode= ((RevokeRequestType) req).getRevocationCode();
    		if(unMACedCode != null){
    			try{
    				retval = new String(Hex.encode(XKMSUtil.getSecretKeyFromPassphrase(new String(unMACedCode,"ISO8859-1"), false, 20, XKMSUtil.KEY_REVOCATIONCODEIDENTIFIER_PASS2).getEncoded()));
    			}catch (XMLEncryptionException e) {
    				log.error(e);
    			} catch (StringprepException e) {// is never thrown}
    			} catch (UnsupportedEncodingException e) {
    				log.error(e);
				}
    		}
    	}
		
		return retval;
	}
}
