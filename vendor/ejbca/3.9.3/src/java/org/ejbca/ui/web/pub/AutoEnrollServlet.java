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

package org.ejbca.ui.web.pub;

import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateExpiredException;
import java.security.cert.CertificateNotYetValidException;
import java.security.cert.X509Certificate;
import java.util.Collection;
import java.util.Date;
import java.util.Enumeration;
import java.util.Iterator;

import javax.ejb.EJBException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocal;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocalHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocal;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionLocalHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionLocal;
import org.ejbca.core.ejb.ra.IUserAdminSessionLocalHome;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionLocal;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionLocalHome;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.model.ra.raadmin.GlobalConfiguration;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.MSPKCS10RequestMessage;
import org.ejbca.core.protocol.X509ResponseMessage;
import org.ejbca.ui.web.RequestHelper;
import org.ejbca.util.ActiveDirectoryTools;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.MSCertTools;
import org.ejbca.util.StringTools;
import org.ejbca.util.dn.DnComponents;
import org.ejbca.util.passgen.PasswordGeneratorFactory;

/**
 * Parses a posted request and returns a correct certificate depending on the type.
 * 
 * This Servlet assumes that it is protected by an Kerberos verifying proxy and is insecure to use without.
 * 
 * @version $Id: AutoEnrollServlet.java 6976 2009-02-19 09:04:09Z anatom $
 */
public class AutoEnrollServlet extends HttpServlet {

	private final static Logger log = Logger.getLogger(AutoEnrollServlet.class);

	private ISignSessionLocal signSession = null;
	private IRaAdminSessionLocal raAdminSession = null;
	private ICertificateStoreSessionLocal certificateStoreSession = null;
	private IUserAdminSessionLocal userAdminSession = null;

	public void init(ServletConfig config) throws ServletException
	{
		super.init(config);
		try {
			// Install BouncyCastle provider
			CertTools.installBCProvider();
		} catch (Exception e) {
			throw new ServletException(e);
		}
	}

	private ISignSessionLocal getSignSession(){
		if(signSession == null){	
			try {
				ISignSessionLocalHome signhome = (ISignSessionLocalHome)ServiceLocator.getInstance().getLocalHome(ISignSessionLocalHome.COMP_NAME);
				signSession = signhome.create();
			}catch(Exception e){
				throw new EJBException(e);      	  	    	  	
			}
		}
		return signSession;
	}

	private IRaAdminSessionLocal getRaAdminSession(){
		if(raAdminSession == null){	
			try {
				IRaAdminSessionLocalHome raAdminSessionHome = (IRaAdminSessionLocalHome)ServiceLocator.getInstance().getLocalHome(IRaAdminSessionLocalHome.COMP_NAME);
				raAdminSession = raAdminSessionHome.create();
			}catch(Exception e){
				throw new EJBException(e);      	  	    	  	
			}
		}
		return raAdminSession;
	}

	private ICertificateStoreSessionLocal getCertificateStoreSession(){
		if(certificateStoreSession == null){	
			try {
				ICertificateStoreSessionLocalHome certificateStoreSessionHome = (ICertificateStoreSessionLocalHome)ServiceLocator.getInstance().getLocalHome(ICertificateStoreSessionLocalHome.COMP_NAME);
				certificateStoreSession = certificateStoreSessionHome.create();
			}catch(Exception e){
				throw new EJBException(e);      	  	    	  	
			}
		}
		return certificateStoreSession;
	}

	private IUserAdminSessionLocal getUserAdminSession(){
		if(userAdminSession == null){	
			try {
				IUserAdminSessionLocalHome userAdminSessionHome = (IUserAdminSessionLocalHome)ServiceLocator.getInstance().getLocalHome(IUserAdminSessionLocalHome.COMP_NAME);
				userAdminSession = userAdminSessionHome.create();
			}catch(Exception e){
				throw new EJBException(e);      	  	    	  	
			}
		}
		return userAdminSession;
	}

	/**
	 * Recievies the request.
	 */
	public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		log.trace(">doPost");
		Admin internalAdmin = new Admin(Admin.TYPE_INTERNALUSER);
		GlobalConfiguration globalConfiguration = getRaAdminSession().loadGlobalConfiguration(internalAdmin);
		// Make sure we allow use of this Servlet
		if ( !globalConfiguration.getAutoEnrollUse() ) {
			log.info("Unauthorized access attempt from " + request.getRemoteAddr());
			response.getOutputStream().println("Not allowed.");
			return;
		}
		int caid = globalConfiguration.getAutoEnrollCA();
		if (caid == GlobalConfiguration.AUTOENROLL_DEFAULT_CA) {
			log.info("Configure a proper CA to use with enroll.");
			response.getOutputStream().println("Configure a proper CA to use with enroll.");
			return;
		}
		boolean debugRequest = "true".equalsIgnoreCase(request.getParameter("debug"));
		String debugInfo = "";

		Admin admin = new Admin(Admin.TYPE_RA_USER, request.getRemoteAddr());
		RequestHelper.setDefaultCharacterEncoding(request);

		if (debugRequest) {
			debugInfo += "getAttributeNames:\n";
			Enumeration enumeration = request.getAttributeNames();
			while (enumeration.hasMoreElements()) {
				String temp = enumeration.nextElement().toString();
				debugInfo += temp + " = " + request.getAttribute(temp) + "\n";
			}
			debugInfo += "\ngetParameterNames:\n";
			enumeration = request.getParameterNames();
			while (enumeration.hasMoreElements()) {
				String temp = enumeration.nextElement().toString();
				debugInfo += temp + " = " + request.getParameter(temp) + "\n";
			}
			debugInfo += "\ngetHeaderNames:\n";
			enumeration = request.getHeaderNames();
			while (enumeration.hasMoreElements()) {
				String temp = enumeration.nextElement().toString();
				debugInfo += temp + " = " + request.getHeader(temp) + "\n";
			}
			debugInfo += "Remote address: " + request.getRemoteAddr() + "\n";
			log.info(debugInfo);
		}

		byte[] result = null;	
		String requestData = MSCertTools.extractRequestFromRawData(request.getParameter("request"));
		if (requestData == null) {
			response.getOutputStream().println("No request supplied..");
			return;
		}
		log.info("Got request: "+requestData);
		// The next line expects apache to forward the kerberos-authenticated user as X-Remote-User"
		String remoteUser = request.getHeader("X-Remote-User") ;
		String usernameShort = StringTools.strip(remoteUser.substring(0, remoteUser.indexOf("@"))).replaceAll("/", "");
		if (remoteUser == null || "".equals(remoteUser) || "(null)".equals(remoteUser)) {
			response.getOutputStream().println("X-Remote-User was not supplied..");
			return;
		}
		MSPKCS10RequestMessage req = null;
		String certificateTemplate = null;
		String command = request.getParameter("command");
		if (command != null && "status".equalsIgnoreCase(command)) {
			response.getOutputStream().println(returnStatus(internalAdmin, "Autoenrolled-" + usernameShort + "-" + request.getParameter("template")));
			return; 
		} else {
			// Default command "request"
		}
		req = new MSPKCS10RequestMessage(Base64.decode(requestData.getBytes()));
		certificateTemplate = req.getMSRequestInfoTemplateName();
		int templateIndex = MSCertTools.getTemplateIndex(certificateTemplate);
		/* TODO: Lookup requesting entity in AD here to verify that only Machines request Machine Certificates etc.. Also check permissions
						like who is allowed to enroll for what if possible. */
		// Create or edit a user "Autoenrolled-Username-Templatename"
		String username = "Autoenrolled-" + usernameShort + "-" + certificateTemplate;
		log.info("Got autoenroll request from " + remoteUser + " (" + username + ") for a " + certificateTemplate + "-certificate.");
		String fetchedSubjectDN = null;
		if (MSCertTools.isRequired(templateIndex, MSCertTools.GET_SUBJECTDN_FROM_AD, 0)) {
			fetchedSubjectDN = ActiveDirectoryTools.getUserDNFromActiveDirectory(globalConfiguration, usernameShort);
		}
		int certProfileId = MSCertTools.getOrCreateCertificateProfile(admin, templateIndex, getCertificateStoreSession());
		int endEntityProfileId = MSCertTools.getOrCreateEndEndtityProfile(admin, templateIndex, certProfileId, caid, usernameShort, fetchedSubjectDN, getRaAdminSession());
		if (endEntityProfileId == -1) {
			String msg = "Could not retrieve required information from AD.";
			log.error(msg);
			response.getOutputStream().println(msg);
			return;
		}
		// Create user
		
		// The CA needs to use non-LDAP order and we need to have the SAN like "CN=Users, CN=Username, DC=com, DC=company".. why??
		// TODO: fix this here.. or is this an general order issue?
		String subjectDN = fetchedSubjectDN;
		if (subjectDN == null) {
			if (MSCertTools.isRequired(templateIndex, DnComponents.COMMONNAME, 0)) {
				subjectDN = "CN="+usernameShort;
			}
		}
		String subjectAN = "";
		if (MSCertTools.isRequired(templateIndex, DnComponents.UPN, 0)) {
			subjectAN += (subjectAN.length() == 0 ? "" : ",") + "UPN=" +remoteUser;
		}
		if (MSCertTools.isRequired(templateIndex, DnComponents.GUID, 0)) {
			String reqGUID = req.getMSRequestInfoSubjectAltnames()[0];
			subjectAN += (subjectAN.length() == 0 ? "" : ",") + "GUID=" +reqGUID;
		}
		if (MSCertTools.isRequired(templateIndex, DnComponents.DNSNAME, 0)) {
			String reqDNS = req.getMSRequestInfoSubjectAltnames()[1];
			subjectAN += (subjectAN.length() == 0 ? "" : ",") + "DNSNAME=" +reqDNS;
		}
		log.info("sdn=" + subjectDN + ", san=" + subjectAN);
		debugInfo += "\nsdn=" + subjectDN + ", san=" + subjectAN + "\n";
		UserDataVO userData = new UserDataVO(username, subjectDN, caid, subjectAN, null, UserDataConstants.STATUS_NEW, 1,endEntityProfileId, certProfileId,
				new Date(), new Date(), SecConst.TOKEN_SOFT_BROWSERGEN, 0, null);
		String password = PasswordGeneratorFactory.getInstance(PasswordGeneratorFactory.PASSWORDTYPE_LETTERSANDDIGITS).getNewPassword(8,8);
		userData.setPassword(password);
		try {
			if (getUserAdminSession().existsUser(admin, username)) {
				getUserAdminSession().changeUser(admin, userData, true);
			} else {
				getUserAdminSession().addUser(admin, userData, true);
			}
		} catch (Exception e) {
			log.error("Could not add user "+username, e);
		}
		Certificate cert=null;
		debugInfo += "Request: " + requestData + "\n";
		req.setUsername(username);
		req.setPassword(password);
		IResponseMessage resp;
		try {
			resp = getSignSession().createCertificate(admin, req,Class.forName(X509ResponseMessage.class.getName()));
			cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
			result = getSignSession().createPKCS7(admin, cert, true);
			debugInfo += "Resulting cert: " + new String(Base64.encode(result, true)) + "\n"; 
		} catch (Exception e) {
			log.error("Noooo!!! ", e);
			response.getOutputStream().println("An error has occurred.");
			return;
		}
		if (debugRequest) {
			response.getOutputStream().println(debugInfo);
		} else {
			// Output the certificate
			ServletOutputStream os = response.getOutputStream();
			os.print(RequestHelper.BEGIN_PKCS7_WITH_NL);
			os.print(new String(Base64.encode(result, true)));
			os.print(RequestHelper.END_PKCS7_WITH_NL);
			response.flushBuffer();
			log.info("Sent cert to client");
		}
		log.trace("<doPost");
	}

	public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
		log.trace(">doGet");
		doPost(request, response);
		log.trace("<doGet");
	}

	/**
	 * Return "OK" if renewal isn't needed.
	 */
	private String returnStatus(Admin admin, String username) {
		if (!getUserAdminSession().existsUser(admin, username)) {
			return "NO_SUCH_USER";
		}
		Collection certificates = getCertificateStoreSession().findCertificatesByUsername(admin, username);
		Iterator iter = certificates.iterator();
		if (!iter.hasNext()) {
			return "NO_CERTIFICATES";
		}
		while (iter.hasNext()) {
			X509Certificate cert = (X509Certificate) iter.next();
			try {
				cert.checkValidity(new Date(System.currentTimeMillis() + 14 * 24 * 3600 * 1000));
				return "OK";
			} catch (CertificateExpiredException e) {
				try {
					cert.checkValidity(new Date(System.currentTimeMillis()));
					return "EXPIRING";
				} catch (CertificateExpiredException e1) {
				} catch (CertificateNotYetValidException e1) {
					return "ERROR";
				}
			} catch (CertificateNotYetValidException e) {
				return "ERROR";
			}
		}
		return "EXPIRED";
		
	}

}
