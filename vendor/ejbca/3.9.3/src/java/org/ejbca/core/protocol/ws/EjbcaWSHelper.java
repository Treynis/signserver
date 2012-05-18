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

import java.rmi.RemoteException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.TreeMap;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;
import javax.naming.NamingException;
import javax.servlet.http.HttpServletRequest;
import javax.xml.ws.WebServiceContext;
import javax.xml.ws.handler.MessageContext;

import org.apache.log4j.Logger;
import org.ejbca.core.EjbcaException;
import org.ejbca.core.ErrorCode;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.authorization.AuthorizationDeniedException;
import org.ejbca.core.model.authorization.AvailableAccessRules;
import org.ejbca.core.model.ca.caadmin.CADoesntExistsException;
import org.ejbca.core.model.ca.caadmin.CAInfo;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.hardtoken.HardTokenData;
import org.ejbca.core.model.hardtoken.types.EnhancedEIDHardToken;
import org.ejbca.core.model.hardtoken.types.SwedishEIDHardToken;
import org.ejbca.core.model.hardtoken.types.TurkishEIDHardToken;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.ExtendedInformation;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.model.util.EjbRemoteHelper;
import org.ejbca.core.protocol.ws.common.HardTokenConstants;
import org.ejbca.core.protocol.ws.objects.Certificate;
import org.ejbca.core.protocol.ws.objects.HardTokenDataWS;
import org.ejbca.core.protocol.ws.objects.NameAndId;
import org.ejbca.core.protocol.ws.objects.PINDataWS;
import org.ejbca.core.protocol.ws.objects.UserDataVOWS;
import org.ejbca.core.protocol.ws.objects.UserMatch;
import org.ejbca.util.CertTools;
import org.ejbca.util.query.Query;

/** Helper class for other classes that wants to call remote EJBs.
 * Methods for fetching ejb session bean interfaces.
 * 
 * @version $Id: EjbcaWSHelper.java 8272 2009-11-09 00:35:13Z primelars $
 */
public class EjbcaWSHelper extends EjbRemoteHelper {

	private static final Logger log = Logger.getLogger(EjbcaWSHelper.class);				
	
	//
	// Helper methods for various tasks done from the WS interface
	//
	protected Admin getAdmin(WebServiceContext wsContext) throws AuthorizationDeniedException, EjbcaException{		  
		  return getAdmin(false, wsContext);
	}
	
	protected Admin getAdmin(boolean allowNonAdmins, WebServiceContext wsContext) throws AuthorizationDeniedException, EjbcaException {
		Admin admin = null;
		try {
			MessageContext msgContext = wsContext.getMessageContext();
			HttpServletRequest request = (HttpServletRequest) msgContext.get(MessageContext.SERVLET_REQUEST);
			X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

			if(certificates == null){
				throw new AuthorizationDeniedException("Error no client certificate recieved used for authentication.");
			}

			admin = new Admin(certificates[0]);
			// Check that user have the administrator flag set.
			if(!allowNonAdmins){
				getUserAdminSession().checkIfCertificateBelongToUser(admin, CertTools.getSerialNumber(certificates[0]), CertTools.getIssuerDN(certificates[0]));
				getAuthorizationSession().isAuthorizedNoLog(admin,AvailableAccessRules.ROLE_ADMINISTRATOR);
			}

			RevokedCertInfo revokeResult =  getCertStoreSession().isRevoked(new Admin(Admin.TYPE_INTERNALUSER),CertTools.getIssuerDN(certificates[0]), CertTools.getSerialNumber(certificates[0]));
			if(revokeResult == null || revokeResult.getReason() != RevokedCertInfo.NOT_REVOKED){
				throw new AuthorizationDeniedException("Error administrator certificate doesn't exist or is revoked.");
			}
		} catch (RemoteException e) {
			log.error("EJBCA WebService error: ",e);
			throw new EjbcaException(ErrorCode.INTERNAL_ERROR, e.getMessage());
		} catch (EJBException e) {
			log.error("EJBCA WebService error: ",e);
			throw new EjbcaException(ErrorCode.INTERNAL_ERROR, e.getMessage());
		}

		return admin;
	}
	
	/**
	 * Method used to check if the admin is an administrator
	 * i.e have administrator flag set and access to resource
	 * /administrator
	 * @return
	 * @throws AuthorizationDeniedException 
	 */
	protected boolean isAdmin(WebServiceContext wsContext) throws EjbcaException {
		boolean retval = false;
		MessageContext msgContext = wsContext.getMessageContext();
		HttpServletRequest request = (HttpServletRequest) msgContext.get(MessageContext.SERVLET_REQUEST);
		X509Certificate[] certificates = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");

		if(certificates == null){
			throw new EjbcaException(ErrorCode.AUTH_CERT_NOT_RECEIVED, 
                "Error no client certificate recieved used for authentication.");
		}

		Admin admin = new Admin(certificates[0]);
		try{
			getUserAdminSession().checkIfCertificateBelongToUser(admin, CertTools.getSerialNumber(certificates[0]), CertTools.getIssuerDN(certificates[0]));
			getAuthorizationSession().isAuthorizedNoLog(admin,AvailableAccessRules.ROLE_ADMINISTRATOR);
			retval = true;
		}catch(AuthorizationDeniedException e){
		} catch (EJBException e) {			
			log.error("Error checking if isAdmin: ", e);
		} catch (RemoteException e) {
			log.error("EJBCA WebService error, isAdmin : ",e);
			throw new EjbcaException(ErrorCode.INTERNAL_ERROR, e.getMessage());
		} 
		
		return retval;
	}

	protected void isAuthorizedToRepublish(Admin admin, String username, int caid) throws AuthorizationDeniedException, EjbcaException, RemoteException{
		try {
			getAuthorizationSession().isAuthorizedNoLog(admin, AvailableAccessRules.REGULAR_VIEWCERTIFICATE);
			UserDataVO userdata = null;
			try {
				userdata = getUserAdminSession().findUser(admin, username);
			} catch (FinderException e) {
				throw new EjbcaException(ErrorCode.USER_NOT_FOUND, "Error the  user doesn't seem to exist.");
			}
			if(userdata == null){
				throw new EjbcaException(ErrorCode.USER_NOT_FOUND, "Error the  user doesn't seem to exist.");
			}
			getAuthorizationSession().isAuthorizedNoLog(admin, AvailableAccessRules.ENDENTITYPROFILEPREFIX + userdata.getEndEntityProfileId() + AvailableAccessRules.VIEW_RIGHTS);
			getAuthorizationSession().isAuthorizedNoLog(admin, AvailableAccessRules.CAPREFIX + caid );		
		} catch (RemoteException e) {
			throw new EjbcaException(ErrorCode.INTERNAL_ERROR, e);
		} catch (EJBException e) {
			throw new EjbcaException(ErrorCode.INTERNAL_ERROR, e);
		}

	}
	
	
	protected void isAuthorizedToHardTokenData(Admin admin, String username, boolean viewPUKData) throws AuthorizationDeniedException, EjbcaException, RemoteException {
		try {
			getAuthorizationSession().isAuthorizedNoLog(admin, AvailableAccessRules.REGULAR_VIEWHARDTOKENS);
			UserDataVO userdata = null;
			boolean userExists = false;
			try {
				userdata = getUserAdminSession().findUser(admin, username);
				if(userdata != null){
					userExists = true;
				}
			} catch (FinderException e) {
				// Do nothing
			}

			getAuthorizationSession().isAuthorizedNoLog(admin, AvailableAccessRules.REGULAR_VIEWHARDTOKENS);
			if(viewPUKData){
				getAuthorizationSession().isAuthorizedNoLog(admin, AvailableAccessRules.REGULAR_VIEWPUKS);
			}

			if(userExists){		
				getAuthorizationSession().isAuthorizedNoLog(admin, AvailableAccessRules.ENDENTITYPROFILEPREFIX + userdata.getEndEntityProfileId() + AvailableAccessRules.HARDTOKEN_RIGHTS);
				if(viewPUKData){
					getAuthorizationSession().isAuthorizedNoLog(admin, AvailableAccessRules.ENDENTITYPROFILEPREFIX + userdata.getEndEntityProfileId() + AvailableAccessRules.HARDTOKEN_PUKDATA_RIGHTS);			
				}
			}

		} catch (RemoteException e) {
			throw new EjbcaException(ErrorCode.INTERNAL_ERROR, e);
		} catch (EJBException e) {
			throw new EjbcaException(ErrorCode.INTERNAL_ERROR, e);
		}		
	}
	
	protected UserDataVO convertUserDataVOWS(Admin admin, UserDataVOWS userdata) throws CADoesntExistsException, EjbcaException, ClassCastException, RemoteException {
		CAInfo cainfo = getCAAdminSession().getCAInfoOrThrowException(admin,userdata.getCaName());
		int caid = cainfo.getCAId();
		if (caid == 0) {
			throw new CADoesntExistsException("Error CA " + userdata.getCaName() + " have caid 0, which is impossible.");
		}
		
		int endentityprofileid = getRAAdminSession().getEndEntityProfileId(admin,userdata.getEndEntityProfileName());
		if(endentityprofileid == 0){
			throw new EjbcaException(ErrorCode.EE_PROFILE_NOT_EXISTS, 
                "Error End Entity profile " + userdata.getEndEntityProfileName() + " doesn't exists.");
		}

		int certificateprofileid = getCertStoreSession().getCertificateProfileId(admin,userdata.getCertificateProfileName());
		if(certificateprofileid == 0){
			throw new EjbcaException(ErrorCode.CERT_PROFILE_NOT_EXISTS,
                "Error Certificate profile " + userdata.getCertificateProfileName() + " doesn't exists.");
		}
		
		int hardtokenissuerid = 0;
		if(userdata.getHardTokenIssuerName() != null){
         hardtokenissuerid = getHardTokenSession().getHardTokenIssuerId(admin,userdata.getHardTokenIssuerName());
		   if(hardtokenissuerid == 0){
			  throw new EjbcaException(ErrorCode.HARD_TOKEN_ISSUER_NOT_EXISTS,
                  "Error Hard Token Issuer " + userdata.getHardTokenIssuerName() + " doesn't exists.");
		   }
		}
		
		int tokenid = getTokenId(admin,userdata.getTokenType());
		if(tokenid == 0){
			throw new EjbcaException(ErrorCode.UNKOWN_TOKEN_TYPE,
                "Error Token Type  " + userdata.getTokenType() + " doesn't exists.");
		}

		ExtendedInformation ei = new ExtendedInformation();
		boolean useEI = false;

		if(userdata.getStartTime() != null) {
		    ei.setCustomData(ExtendedInformation.CUSTOM_STARTTIME, userdata.getStartTime());
		    useEI = true;
		}
        if(userdata.getEndTime() != null) {
            ei.setCustomData(ExtendedInformation.CUSTOM_ENDTIME, userdata.getEndTime());
            useEI = true;
        }

		UserDataVO userdatavo = new UserDataVO(userdata.getUsername(),
				userdata.getSubjectDN(),
				caid,
				userdata.getSubjectAltName(),
				userdata.getEmail(),
				userdata.getStatus(),
				userdata.getType(),
				endentityprofileid,
				certificateprofileid,
				null,
				null,
				tokenid,
				hardtokenissuerid,
				useEI ? ei : null);
		
		userdatavo.setPassword(userdata.getPassword());
		
		return userdatavo;
	}
	
	
	
	protected UserDataVOWS convertUserDataVO(Admin admin, UserDataVO userdata) throws EjbcaException, ClassCastException, RemoteException {
	    UserDataVOWS dataWS = new UserDataVOWS();
		String username = userdata.getUsername();
		String caname = getCAAdminSession().getCAInfo(admin,userdata.getCAId()).getName();
		ExtendedInformation ei = userdata.getExtendedinformation();

		dataWS.setUsername(username);

		if(caname == null){
			String message = "Error CA id " + userdata.getCAId() + " doesn't exists. User: "+username;
			log.error(message);
			throw new EjbcaException(ErrorCode.CA_NOT_EXISTS, message);
		}
		dataWS.setCaName(caname);
		
		String endentityprofilename = getRAAdminSession().getEndEntityProfileName(admin,userdata.getEndEntityProfileId());
		if(endentityprofilename == null){
			String message = "Error End Entity profile id " + userdata.getEndEntityProfileId() + " doesn't exists. User: "+username;
			log.error(message);
			throw new EjbcaException(ErrorCode.EE_PROFILE_NOT_EXISTS, message);
		}
        dataWS.setEndEntityProfileName(endentityprofilename);

		String certificateprofilename = getCertStoreSession().getCertificateProfileName(admin,userdata.getCertificateProfileId());
		if(certificateprofilename == null){
			String message = "Error Certificate profile id " + userdata.getCertificateProfileId() + " doesn't exists. User: "+username;
			log.error(message);
			throw new EjbcaException(ErrorCode.CERT_PROFILE_NOT_EXISTS, message);
		}
	    dataWS.setCertificateProfileName(certificateprofilename);
		
		String hardtokenissuername = null;
		if(userdata.getHardTokenIssuerId() != 0){
		   hardtokenissuername = getHardTokenSession().getHardTokenIssuerAlias(admin,userdata.getHardTokenIssuerId());
		   if(hardtokenissuername == null){
			   String message = "Error Hard Token Issuer id " + userdata.getHardTokenIssuerId() + " doesn't exists. User: "+username;
			   log.error(message);
			   throw new EjbcaException(ErrorCode.HARD_TOKEN_ISSUER_NOT_EXISTS, message);
		   }
		   dataWS.setHardTokenIssuerName(hardtokenissuername);
		}
		
		String tokenname = getTokenName(admin,userdata.getTokenType());
		if(tokenname == null){
			String message = "Error Token Type id " + userdata.getTokenType() + " doesn't exists. User: "+username;
			log.error(message);
			throw new EjbcaException(ErrorCode.UNKOWN_TOKEN_TYPE, message);
		}
		dataWS.setTokenType(tokenname);

		dataWS.setPassword(null);
		dataWS.setClearPwd(false);
		dataWS.setSubjectDN(userdata.getDN());
		dataWS.setSubjectAltName(userdata.getSubjectAltName());
		dataWS.setEmail(userdata.getEmail());
		dataWS.setStatus(userdata.getStatus());

		if(ei != null) {
		    dataWS.setStartTime(ei.getCustomData(ExtendedInformation.CUSTOM_STARTTIME));
            dataWS.setEndTime(ei.getCustomData(ExtendedInformation.CUSTOM_ENDTIME));
		}

		return dataWS;
	}
	
	/**
	 * Method used to convert a HardToken data to a WS version
	 * @param data
	 * @throws EjbcaException 
	 */
	protected HardTokenDataWS convertHardTokenToWS(HardTokenData data, Collection certificates, boolean includePUK) throws EjbcaException {
		HardTokenDataWS retval = new HardTokenDataWS();
		retval.setHardTokenSN(data.getTokenSN());
		retval.setLabel(data.getHardToken().getLabel());
		retval.setCopyOfSN(data.getCopyOf());
		ArrayList<String> copies = new ArrayList<String>();
		if(data.getCopies() != null){
			Iterator iter = data.getCopies().iterator();
			while(iter.hasNext()){
				copies.add((String) iter.next());

			}
		}
		retval.setCopies(copies);
		retval.setModifyTime(data.getModifyTime());
		retval.setCreateTime(data.getCreateTime());
		retval.setEncKeyKeyRecoverable(false);

		try{
			Iterator iter = certificates.iterator();
			while(iter.hasNext()){
				retval.getCertificates().add(new Certificate((java.security.cert.Certificate) iter.next()));
			}
		}catch(CertificateEncodingException e){
			log.error("EJBCA WebService error, getHardToken: ",e);
			throw new EjbcaException(ErrorCode.INTERNAL_ERROR, e.getMessage());
		}


		if(data.getHardToken() instanceof SwedishEIDHardToken){
			SwedishEIDHardToken ht = (SwedishEIDHardToken) data.getHardToken();
			if(includePUK){
			  retval.getPinDatas().add(new PINDataWS(HardTokenConstants.PINTYPE_SIGNATURE,ht.getInitialSignaturePIN(),ht.getSignaturePUK()));
			  retval.getPinDatas().add(new PINDataWS(HardTokenConstants.PINTYPE_BASIC,ht.getInitialAuthEncPIN(),ht.getAuthEncPUK()));
			}
			retval.setTokenType(HardTokenConstants.TOKENTYPE_SWEDISHEID);
			return retval;
		}
		if(data.getHardToken() instanceof EnhancedEIDHardToken){
			EnhancedEIDHardToken ht = (EnhancedEIDHardToken) data.getHardToken();
			retval.setEncKeyKeyRecoverable(ht.getEncKeyRecoverable());
			if(includePUK){
				retval.getPinDatas().add(new PINDataWS(HardTokenConstants.PINTYPE_SIGNATURE,ht.getInitialSignaturePIN(),ht.getSignaturePUK()));
				retval.getPinDatas().add(new PINDataWS(HardTokenConstants.PINTYPE_BASIC,ht.getInitialAuthPIN(),ht.getAuthPUK()));
			}
			retval.setTokenType(HardTokenConstants.TOKENTYPE_ENHANCEDEID);
			return retval;
		}
		if(data.getHardToken() instanceof TurkishEIDHardToken){
			TurkishEIDHardToken ht = (TurkishEIDHardToken) data.getHardToken();
			if(includePUK){
			  retval.getPinDatas().add(new PINDataWS(HardTokenConstants.PINTYPE_BASIC,ht.getInitialPIN(),ht.getPUK()));
			}
			retval.setTokenType(HardTokenConstants.TOKENTYPE_TURKISHEID);
			return retval;
		}
		throw new EjbcaException(ErrorCode.INTERNAL_ERROR,
		                         "Error: only SwedishEIDHardToken, EnhancedEIDHardToken, TurkishEIDHardToken supported.");
	}
	
	/**
	 * Method that converts profilenames etc to corresponding Id's
	 * @param admin
	 * @param usermatch a usermatch containing names of profiles
	 * @return a query containg id's of profiles.
	 * @throws NumberFormatException
	 * @throws ClassCastException
	 * @throws CreateException
	 * @throws NamingException
	 */
	protected Query convertUserMatch(Admin admin, UserMatch usermatch) throws NumberFormatException, ClassCastException, RemoteException{
		Query retval = new Query(Query.TYPE_USERQUERY);		  		
		switch(usermatch.getMatchwith()){
		  case UserMatch.MATCH_WITH_ENDENTITYPROFILE:
			  String endentityprofilename = Integer.toString(getRAAdminSession().getEndEntityProfileId(admin,usermatch.getMatchvalue()));
			  retval.add(usermatch.getMatchwith(),usermatch.getMatchtype(),endentityprofilename);
			  break;
		  case UserMatch.MATCH_WITH_CERTIFICATEPROFILE:
			  String certificateprofilename = Integer.toString(getCertStoreSession().getCertificateProfileId(admin,usermatch.getMatchvalue()));
			  retval.add(usermatch.getMatchwith(),usermatch.getMatchtype(),certificateprofilename);
			  break;			  
		  case UserMatch.MATCH_WITH_CA:
			  String caname = Integer.toString(getCAAdminSession().getCAInfo(admin,usermatch.getMatchvalue()).getCAId());
			  retval.add(usermatch.getMatchwith(),usermatch.getMatchtype(),caname);
			  break;	
		  case UserMatch.MATCH_WITH_TOKEN:
			  String tokenname = Integer.toString(getTokenId(admin,usermatch.getMatchvalue()));
			  retval.add(usermatch.getMatchwith(),usermatch.getMatchtype(),tokenname);
			  break;
		  default:		
			  retval.add(usermatch.getMatchwith(),usermatch.getMatchtype(),usermatch.getMatchvalue());
			  break;
		}
		
		
		return retval;
	}
	
	/**
	 * Help metod returning a subset of certificates containing only valid certificates
	 * expiredate and revokation status is checked.
	 * @throws NamingException 
	 * @throws CreateException 
	 * @throws ClassCastException 
	 */
	protected Collection<java.security.cert.Certificate> returnOnlyValidCertificates(Admin admin, Collection<java.security.cert.Certificate> certs) throws RemoteException {
     ArrayList<java.security.cert.Certificate> retval = new ArrayList<java.security.cert.Certificate>();
     Iterator<java.security.cert.Certificate> iter = certs.iterator();
     while(iter.hasNext()){
    	 java.security.cert.Certificate next = iter.next();
  	   
  	   RevokedCertInfo info = getCertStoreSession().isRevoked(admin,CertTools.getIssuerDN(next),CertTools.getSerialNumber(next));
  	   if ( (info != null) && (info.getReason() == RevokedCertInfo.NOT_REVOKED) ) {
  		   try{
  			   CertTools.checkValidity(next, new Date());
  			   retval.add(next);
  		   }catch(CertificateExpiredException e){    			   
  		   }catch (CertificateNotYetValidException e) {    			   
  		   }
  	   }
     }
	
     return retval;
	}
	
	/**
	 * Checks authorization for each certificate and optionally check that it's valid. Does not check revocation status. 
	 * @param admin is the admin used for authorization
	 * @param certs is the collection of certs to verify
	 * @param validate set to true to perform validation of each certificate
	 * @return a List of valid and authorized certificates
	 */
	protected List<Certificate> returnAuthorizedCertificates(Admin admin, Collection<java.security.cert.Certificate> certs, boolean validate) {
		List<Certificate> retval = new ArrayList<Certificate>();
		Iterator<java.security.cert.Certificate> iter = certs.iterator();
		Map<Integer, Boolean> authorizationCache = new HashMap<Integer, Boolean>(); 
		while(iter.hasNext()){
			java.security.cert.Certificate next = iter.next();
			try {
				if (validate) {
					// Check validity
					CertTools.checkValidity(next, new Date());
					getCertStoreSession().verifyProtection(admin, CertTools.getIssuerDN(next), CertTools.getSerialNumber(next));
				}
				// Check authorization
				int caid = CertTools.getIssuerDN(next).hashCode();
				Boolean authorized = authorizationCache.get(caid);
				if (authorized == null) {
					authorized = getAuthorizationSession().isAuthorizedNoLog(admin,AvailableAccessRules.CAPREFIX +caid);
					authorizationCache.put(caid, authorized);
				}
				if (authorized) {
					retval.add(new Certificate((java.security.cert.Certificate) next));
				}
			} catch (CertificateExpiredException e) {		// Drop invalid cert
			} catch (CertificateNotYetValidException e) {   // Drop invalid cert
			} catch (CertificateEncodingException e) {		// Drop invalid cert
				log.error("A defect certificate was detected.");
			} catch (AuthorizationDeniedException e) {		// Drop unauthorized cert
			} catch (RemoteException e) {
				throw new EJBException(e);	// No reason to use checked Exception here
			}
		}
		return retval;
	}
	
	
	private final String[] softtokennames = {UserDataVOWS.TOKEN_TYPE_USERGENERATED,UserDataVOWS.TOKEN_TYPE_P12,
			                                 UserDataVOWS.TOKEN_TYPE_JKS,UserDataVOWS.TOKEN_TYPE_PEM};
	private final int[] softtokenids = {SecConst.TOKEN_SOFT_BROWSERGEN,
			SecConst.TOKEN_SOFT_P12, SecConst.TOKEN_SOFT_JKS, SecConst.TOKEN_SOFT_PEM};
	
	private int getTokenId(Admin admin, String tokenname) throws RemoteException {
      int returnval = 0;
      
      // First check for soft token type
      for(int i=0;i< softtokennames.length;i++){
      	if(softtokennames[i].equals(tokenname)){
      		returnval = softtokenids[i];
      		break;
      	}        	
      }
      if (returnval == 0) {
           returnval = getHardTokenSession().getHardTokenProfileId(admin , tokenname);
      }

      return returnval;
	}
	
	private String getTokenName(Admin admin, int tokenid) throws RemoteException {
      String returnval = null;
      
      // First check for soft token type
      for(int i=0;i< softtokenids.length;i++){
      	if(softtokenids[i] == tokenid){
      		returnval = softtokennames[i];
      		break;
      	}        	
      }
      if (returnval == null) {
           returnval = getHardTokenSession().getHardTokenProfileName(admin , tokenid);
      }

      return returnval;
	}

  /**
	 * Web services does not support Collection type so convert it to array.
	 * 
	 * @param mytree TreeMap of name and id pairs to convert to an array
	 * @return array of NameAndId objects
	 */
  protected NameAndId[] convertTreeMapToArray(TreeMap<String, Integer> mytree) {
  	NameAndId[] ret = null;

		if ((mytree == null) || (mytree.size() == 0) ) {
			ret = new NameAndId[0];
		} else {
			ret = new NameAndId[mytree.size()];
			int i = 0;
			for (String name : mytree.keySet()) {
				ret[i++] = new NameAndId(name, mytree.get(name));
			}
		}
		return ret;
	}



}