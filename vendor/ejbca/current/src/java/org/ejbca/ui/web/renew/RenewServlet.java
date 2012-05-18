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
 
package org.ejbca.ui.web.renew;

import java.io.IOException;
import java.security.cert.X509Certificate;

import javax.ejb.ObjectNotFoundException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionHome;
import org.ejbca.core.ejb.ca.store.ICertificateStoreSessionRemote;
import org.ejbca.core.ejb.ra.IUserAdminSessionHome;
import org.ejbca.core.ejb.ra.IUserAdminSessionRemote;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionHome;
import org.ejbca.core.ejb.ra.raadmin.IRaAdminSessionRemote;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.approval.ApprovalException;
import org.ejbca.core.model.approval.WaitingForApprovalException;
import org.ejbca.core.model.ca.crl.RevokedCertInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.UserDataConstants;
import org.ejbca.core.model.ra.UserDataVO;
import org.ejbca.core.model.ra.raadmin.EndEntityProfile;
import org.ejbca.util.CertTools;

/**
 * Servlet used for requesting browser certificate renewals.
 * 
 * @author Markus Kilås
 * @version $Id: RenewServlet.java 7815 2009-07-16 11:54:18Z netmackan $
 */
public class RenewServlet extends HttpServlet {
    
	private static final Logger log = Logger.getLogger(RenewServlet.class);
    
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    /** Submit button on the web page */
	public static final String BUTTONRENEW = "buttonrenew";

	private ICertificateStoreSessionRemote certificateStoreSession;
	private IRaAdminSessionRemote raadminsession;
	private IUserAdminSessionRemote useradminhome;
    
    /**
     * Servlet init
     *
     * @param config servlet configuration
     * @throws ServletException on error
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        try {
            // Install BouncyCastle provider
            CertTools.installBCProvider();

            // Get EJB context and home interfaces
            ServiceLocator locator = ServiceLocator.getInstance();
            useradminhome = ((IUserAdminSessionHome) locator.getRemoteHome(IUserAdminSessionHome.JNDI_NAME, IUserAdminSessionHome.class)).create();
            certificateStoreSession = ((ICertificateStoreSessionHome) locator.getRemoteHome(ICertificateStoreSessionHome.JNDI_NAME, ICertificateStoreSessionHome.class)).create();
            raadminsession = ((IRaAdminSessionHome) locator.getRemoteHome(IRaAdminSessionHome.JNDI_NAME, IRaAdminSessionHome.class)).create();
        } catch(Exception e) {
        	throw new ServletException(e);
        }
    }

    
    public void doRequest(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    	
    	Admin admin = new Admin(Admin.TYPE_RA_USER);
    	
    	// SSL client authentication
    	Object o = request.getAttribute("javax.servlet.request.X509Certificate");
    	if (o == null || !(o instanceof X509Certificate[])) {
    	    throw new ServletException("This servlet requires certificate authentication!");
    	}
    	X509Certificate certificate = ((X509Certificate[]) o)[0];
    	request.setAttribute("certificate", certificate);
    	RevokedCertInfo rci = certificateStoreSession.isRevoked(admin, certificate.getIssuerDN().getName(), certificate.getSerialNumber());
    	if (rci == null || rci.getReason() != RevokedCertInfo.NOT_REVOKED) {
    		request.setAttribute("errorMessage", "User certificate with serial number "+certificate.getSerialNumber() + " from issuer \'"+certificate.getIssuerX500Principal()+"\' is revoked.");
    	} else {
	    	String username = certificateStoreSession.findUsernameByCertSerno(admin, certificate.getSerialNumber(), certificate.getIssuerX500Principal().toString());
	    	if (username==null || username.length()==0) {
	    		throw new ServletException(new ObjectNotFoundException("Not possible to retrieve user name"));
	    	}
	    	request.setAttribute("username", username);
	    	if(log.isDebugEnabled()) {
	    		log.debug("User authenticated as " + username + ".");
	    	}
	    	
	    	// Request certificate renewal
	    	if(request.getParameter(BUTTONRENEW) != null) {
	    		if(log.isDebugEnabled()) {
	    			log.debug("Got renewal request for " + username + ".");
	    		}
	    		String statusMessage;
	    		try {
		    		UserDataVO userdata = useradminhome.findUser(admin, username);
		    		EndEntityProfile profile = raadminsession.getEndEntityProfile(admin, userdata.getEndEntityProfileId());
		    		userdata.setPassword(profile.getAutoGeneratedPasswd());
		    		userdata.setStatus(UserDataConstants.STATUS_NEW);
	    			useradminhome.changeUser(admin, userdata, false);
	    			statusMessage = "Your request for certificate renewal has been submitted.";
	    		} catch(WaitingForApprovalException ex) {
	    			statusMessage = "Your request for certificate renewal has been submitted and is now waiting for approval.";
	    		} catch(ApprovalException ex) {
	    			statusMessage = "Your request for certificate renewal has been submitted before and is already waiting for approval.";
	    		} catch(Exception ex) {
	    			throw new ServletException(ex);
	    		}
	    		request.setAttribute("statusMessage", statusMessage);
	    	}
    	}
    	request.setAttribute("buttonRenew", BUTTONRENEW);
    	getServletContext().getRequestDispatcher("/renewpage.jsp").include(request, response); 
    }

    /**
     * Handles HTTP POST
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws IOException input/output error
     * @throws ServletException on error
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
        doRequest(request, response);
    }

    /**
     * Handles HTTP GET
     *
     * @param request servlet request
     * @param response servlet response
     *
     * @throws IOException input/output error
     * @throws ServletException on error
     */
    public void doGet(HttpServletRequest request, HttpServletResponse response) throws IOException, ServletException {
    	doRequest(request, response);
    }
}
