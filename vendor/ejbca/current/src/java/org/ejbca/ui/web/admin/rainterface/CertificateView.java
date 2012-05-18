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
 
package org.ejbca.ui.web.admin.rainterface;


import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.CertificateParsingException;
import java.security.cert.X509Certificate;
import java.security.interfaces.DSAPublicKey;
import java.security.interfaces.ECPublicKey;
import java.security.interfaces.RSAPublicKey;
import java.util.Date;
import java.util.Map;

import org.apache.commons.lang.StringUtils;
import org.bouncycastle.util.encoders.Hex;
import org.ejbca.core.model.ca.certificateprofiles.CertificateProfile;
import org.ejbca.core.model.util.AlgorithmTools;
import org.ejbca.cvc.CVCertificateBody;
import org.ejbca.cvc.CardVerifiableCertificate;
import org.ejbca.ui.web.admin.configuration.EjbcaWebBean;
import org.ejbca.util.CertTools;
import org.ejbca.util.cert.QCStatementExtension;
import org.ejbca.util.cert.SubjectDirAttrExtension;
import org.ejbca.util.dn.DNFieldExtractor;
import org.ejbca.util.keystore.KeyTools;



/**
 * A class transforming X509 certificate data into more readable form used
 * by JSP pages.
 *
 * @author  Philip Vendil
 * @version $Id: CertificateView.java 8188 2009-10-26 20:25:35Z anatom $
 */
public class CertificateView implements java.io.Serializable {

    // Private fields
    private Certificate  certificate;
    private DNFieldExtractor subjectdnfieldextractor, issuerdnfieldextractor;
    private RevokedInfoView  revokedinfo;
    private String           username;
    private String           subjectaltnamestring;
    private String           subjectdirattrstring;

   public static final String[] KEYUSAGETEXTS = {"DIGITALSIGNATURE","NONREPUDIATION", "KEYENCIPHERMENT", "DATAENCIPHERMENT", "KEYAGREEMENT", "KEYCERTSIGN", "CRLSIGN", "ENCIPHERONLY", "DECIPHERONLY" };
   

	/** Creates a new instance of CertificateView */
    public CertificateView(Certificate certificate, RevokedInfoView revokedinfo, String username) {
      this.certificate=certificate;
      this.revokedinfo= revokedinfo;
      this.username=username;

      subjectdnfieldextractor = new DNFieldExtractor(CertTools.getSubjectDN(certificate), DNFieldExtractor.TYPE_SUBJECTDN);
      issuerdnfieldextractor  = new DNFieldExtractor(CertTools.getIssuerDN(certificate), DNFieldExtractor.TYPE_SUBJECTDN);
      
    }


    // Public methods
    /** Method that returns the version number of the X509 certificate. */
    public String getVersion() {
        if (certificate instanceof X509Certificate) {
        	X509Certificate x509cert = (X509Certificate)certificate;
            return Integer.toString(x509cert.getVersion());
        } else {
        	return Integer.valueOf(CVCertificateBody.CVC_VERSION).toString();
        }
    }

    public String getType() {
      return certificate.getType();
    }

    public String getSerialNumber() {
      return CertTools.getSerialNumberAsString(certificate);
    }

    public BigInteger getSerialNumberBigInt() {
      return CertTools.getSerialNumber(certificate);
    }

    public String getIssuerDN() {
      return CertTools.getIssuerDN(certificate);
    }

    public String getIssuerDNField(int field, int number) {
      return issuerdnfieldextractor.getField(field, number);
    }

    public String getSubjectDN() {
      return CertTools.getSubjectDN(certificate);
    }

    public String getSubjectDNField(int field, int number) {
      return subjectdnfieldextractor.getField(field, number);
    }

    public Date getValidFrom() {
      return CertTools.getNotBefore(certificate);
    }

    public Date getValidTo() {
      return CertTools.getNotAfter(certificate);
    }

    public boolean checkValidity(){
      boolean valid = true;
      try{
        CertTools.checkValidity(certificate, new Date());
      }
      catch( CertificateExpiredException e){
        valid=false;
      }
      catch(CertificateNotYetValidException e){
         valid=false;
      }

      return valid;
    }

    public boolean checkValidity(Date date)  {
      boolean valid = true;
      try{
        CertTools.checkValidity(certificate, date);
      }
      catch( CertificateExpiredException e){
        valid=false;
      }
      catch(CertificateNotYetValidException e){
         valid=false;
      }

      return valid;
    }

    public String getPublicKeyAlgorithm(){
      return certificate.getPublicKey().getAlgorithm();
    }
    
    public String getKeySpec(EjbcaWebBean ejbcawebbean) {
    	if( certificate.getPublicKey() instanceof ECPublicKey ) {
    		return AlgorithmTools.getKeySpecification(certificate.getPublicKey());
    	} else {
    		return "" + KeyTools.getKeyLength(certificate.getPublicKey()) + " " + ejbcawebbean.getText("BITS");
    	}
    }

    public String getPublicKeyLength(){
      int len = KeyTools.getKeyLength(certificate.getPublicKey());
      return len > 0 ? ""+len : null; 
    }

    public String getPublicKeyModulus(){
    	String mod = null;
    	if( certificate.getPublicKey() instanceof RSAPublicKey){
    		mod = "" + ((RSAPublicKey)certificate.getPublicKey()).getModulus().toString(16);
    		mod = mod.toUpperCase();
    		mod = StringUtils.abbreviate(mod, 50);
    	} else if( certificate.getPublicKey() instanceof DSAPublicKey){
    		mod = "" + ((DSAPublicKey)certificate.getPublicKey()).getY().toString(16);
    		mod = mod.toUpperCase();
    		mod = StringUtils.abbreviate(mod, 50);
    	} else if( certificate.getPublicKey() instanceof ECPublicKey){
    		mod = "" + ((ECPublicKey)certificate.getPublicKey()).getW().getAffineX().toString(16);
    		mod = mod + ((ECPublicKey)certificate.getPublicKey()).getW().getAffineY().toString(16);
    		mod = mod.toUpperCase();
    		mod = StringUtils.abbreviate(mod, 50);
    	}
    	return mod;
    }

    public String getSignatureAlgoritm() {
    	// Only used for displaying to user so we can use this value that always works
    	return CertTools.getCertSignatureAlgorithmAsString(certificate);
    }

    /** Method that returns if key is allowed for given usage. Usage must be one of this class key usage constants. */
    public boolean getKeyUsage(int usage) {
    	boolean returnval = false;
    	if (certificate instanceof X509Certificate) {
    		X509Certificate x509cert = (X509Certificate)certificate;
    		if(x509cert.getKeyUsage() != null)
    			returnval= x509cert.getKeyUsage()[usage];
    	} else {
    		returnval = false;
    	}
    	return returnval;
    }

    public String[] getExtendedKeyUsageAsTexts(){
        java.util.List extendedkeyusage = null;  
        if (certificate instanceof X509Certificate) {
      	  X509Certificate x509cert = (X509Certificate)certificate;
            try {  
                extendedkeyusage = x509cert.getExtendedKeyUsage();  
              } catch (java.security.cert.CertificateParsingException e) {}  
        }
        if(extendedkeyusage == null) {
          extendedkeyusage = new java.util.ArrayList();
        }
        String[] returnval = new String[extendedkeyusage.size()]; 
        Map map = CertificateProfile.getAllExtendedKeyUsageTexts();
        for(int i=0; i < extendedkeyusage.size(); i++){
          returnval[i] = (String)map.get(extendedkeyusage.get(i));    
        }
          
        return returnval; 
      }

    public String getBasicConstraints(EjbcaWebBean ejbcawebbean) {
    	String retval = ejbcawebbean.getText("NONE");
    	if (certificate instanceof X509Certificate) {
    		X509Certificate x509cert = (X509Certificate)certificate;
    		int bc = x509cert.getBasicConstraints();
    		if (bc == Integer.MAX_VALUE) {
    			retval = ejbcawebbean.getText("CANOLIMIT");
    		} else if (bc == -1) {
    			retval = ejbcawebbean.getText("ENDENTITY");
    		} else {
    			retval = ejbcawebbean.getText("CAPATHLENGTH") + " : " + x509cert.getBasicConstraints();                    	     			
    		}
    	} else if (certificate.getType().equals("CVC")) {
    		CardVerifiableCertificate cvccert = (CardVerifiableCertificate)certificate;
    		try {
    			retval = cvccert.getCVCertificate().getCertificateBody().getAuthorizationTemplate().getAuthorizationField().getRole().name();
    		} catch (NoSuchFieldException e) {
    	    	retval = ejbcawebbean.getText("NONE");
    		}
    	}
    	return retval;
    }

    public String getSignature() {
      return (new java.math.BigInteger(CertTools.getSignature(certificate))).toString(16);
    }

    public String getSHA1Fingerprint(){
      String returnval = "";
      try {
         byte[] res = CertTools.generateSHA1Fingerprint(certificate.getEncoded());
         String ret = new String(Hex.encode(res));
         returnval = ret.toUpperCase();
      } catch (CertificateEncodingException cee) {
      }
      return  returnval;
    }

    public String getMD5Fingerprint(){
      String returnval = "";
      try {
         byte[] res = CertTools.generateMD5Fingerprint(certificate.getEncoded());
         String ret = new String(Hex.encode(res));
         returnval = ret.toUpperCase();
      } catch (CertificateEncodingException cee) {
      }
      return  returnval;
    }
     
     

    public boolean isRevoked(){
      return revokedinfo != null  && revokedinfo.isRevoked();     
    }

    public String[] getRevokationReasons(){
      String[] returnval = null;
      if(revokedinfo != null)
        returnval = revokedinfo.getRevokationReasons();
      return returnval;
    }

    public Date getRevokationDate(){
      Date returnval = null;
      if(revokedinfo != null)
        returnval = revokedinfo.getRevocationDate();
      return returnval;
    }

    public String getUsername(){
      return this.username;
    }

    public Certificate getCertificate(){
      return certificate;
    }
    
    public String getSubjectDirAttr() {
    	if(subjectdirattrstring == null) {
    		try {
    			subjectdirattrstring = SubjectDirAttrExtension.getSubjectDirectoryAttributes(certificate);
    		} catch (Exception e) {
    			subjectdirattrstring = e.getMessage();		
    		}
    	}
    	return subjectdirattrstring;
    }
    
    public String getSubjectAltName() {
    	if(subjectaltnamestring == null){  
    		try {
    			subjectaltnamestring = CertTools.getSubjectAlternativeName(certificate);
    		} catch (CertificateParsingException e) {
    			subjectaltnamestring = e.getMessage();		
    		} catch (IOException e) {
    			subjectaltnamestring = e.getMessage();		
			}                  
    	}        

      return subjectaltnamestring; 	
    }

    public boolean hasQcStatement() {
    	boolean ret = false; 
    	try {
			ret = QCStatementExtension.hasQcStatement(certificate);
		} catch (IOException e) {
			ret = false;
		}
		return ret;
    }
}
