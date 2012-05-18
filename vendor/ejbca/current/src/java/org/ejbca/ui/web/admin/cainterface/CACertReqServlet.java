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
 
package org.ejbca.ui.web.admin.cainterface;

import java.io.IOException;
import java.security.cert.Certificate;

import javax.ejb.EJBException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocal;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocalHome;
import org.ejbca.core.model.InternalResources;
import org.ejbca.ui.web.RequestHelper;
import org.ejbca.ui.web.admin.configuration.EjbcaWebBean;
import org.ejbca.ui.web.pub.ServletUtils;

/**
 * Servlet used to distribute  CRLs.<br>
 *
 * The servlet is called with method GET or POST and syntax
 * <code>command=&lt;command&gt;</code>.
 * <p>The follwing commands are supported:<br>
 * <ul>
 * <li>crl - gets the latest CRL.
 *
 * @version $Id: CACertReqServlet.java 7024 2009-02-24 11:27:33Z anatom $
 * 
 * @web.servlet name = "CACertReq"
 *              display-name = "CACertReqServlet"
 *              description="Used to retrive CA certificate request and Processed CA Certificates from AdminWeb GUI"
 *              load-on-startup = "99"
 *
 * @web.servlet-mapping url-pattern = "/ca/editcas/cacertreq"
 *
 */
public class CACertReqServlet extends HttpServlet {

    private static final Logger log = Logger.getLogger(CACertReqServlet.class);
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

    private static final String COMMAND_PROPERTY_NAME = "cmd";
    private static final String COMMAND_CERTREQ = "certreq";
	private static final String COMMAND_CERT           = "cert";    
	private static final String COMMAND_CERTPKCS7 = "certpkcs7";
    private static final String FORMAT_PROPERTY_NAME = "format";
	
	private ISignSessionLocal signsession = null;
   
   private synchronized ISignSessionLocal getSignSession(){
   	  if(signsession == null){	
		try {
		    ISignSessionLocalHome signhome = (ISignSessionLocalHome)ServiceLocator.getInstance().getLocalHome(ISignSessionLocalHome.COMP_NAME);
		    signsession = signhome.create();
		}catch(Exception e){
			throw new EJBException(e);      	  	    	  	
		}
   	  }
   	  return signsession;
   }
   
   
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
    }

    public void doPost(HttpServletRequest req, HttpServletResponse res)
        throws IOException, ServletException {
        log.trace(">doPost()");
        doGet(req, res);
        log.trace("<doPost()");
    } //doPost

    public void doGet(HttpServletRequest req,  HttpServletResponse res) throws java.io.IOException, ServletException {
        log.trace(">doGet()");

        // Check if authorized
        EjbcaWebBean ejbcawebbean= (org.ejbca.ui.web.admin.configuration.EjbcaWebBean)
                                   req.getSession().getAttribute("ejbcawebbean");
        if ( ejbcawebbean == null ){
          try {
            ejbcawebbean = (org.ejbca.ui.web.admin.configuration.EjbcaWebBean) java.beans.Beans.instantiate(this.getClass().getClassLoader(), org.ejbca.ui.web.admin.configuration.EjbcaWebBean.class.getName());
           } catch (ClassNotFoundException exc) {
               throw new ServletException(exc.getMessage());
           }catch (Exception exc) {
               throw new ServletException (" Cannot create bean of class "+org.ejbca.ui.web.admin.configuration.EjbcaWebBean.class.getName(), exc);
           }
           req.getSession().setAttribute("ejbcawebbean", ejbcawebbean);
        }

		// Check if authorized
		CAInterfaceBean cabean= (org.ejbca.ui.web.admin.cainterface.CAInterfaceBean)
								   req.getSession().getAttribute("cabean");
		if ( cabean == null ){
		  try {
			cabean = (org.ejbca.ui.web.admin.cainterface.CAInterfaceBean) java.beans.Beans.instantiate(this.getClass().getClassLoader(), org.ejbca.ui.web.admin.cainterface.CAInterfaceBean.class.getName());
		   } catch (ClassNotFoundException exc) {
			   throw new ServletException(exc.getMessage());
		   }catch (Exception exc) {
			   throw new ServletException (" Cannot create bean of class "+org.ejbca.ui.web.admin.cainterface.CAInterfaceBean.class.getName(), exc);
		   }
		   req.getSession().setAttribute("cabean", cabean);
		}


        try{
          ejbcawebbean.initialize(req, "/super_administrator");          
        } catch(Exception e){
           throw new java.io.IOException("Authorization Denied");
        }

		try{
		  cabean.initialize(req, ejbcawebbean);
		} catch(Exception e){
		   throw new java.io.IOException("Error initializing CACertReqServlet");
		}        
                
        
        // Keep this for logging.
        String remoteAddr = req.getRemoteAddr();
        RequestHelper.setDefaultCharacterEncoding(req);
        String command = req.getParameter(COMMAND_PROPERTY_NAME);
        String format = req.getParameter(FORMAT_PROPERTY_NAME);
        if (command == null) {
            command = "";
        }
        if (command.equalsIgnoreCase(COMMAND_CERTREQ)) {
            try {
                
            	byte[] request = cabean.getRequestData();
                String filename = "certificaterequest.req";
                int length = request.length;
                byte[] outbytes = request;
            	if (!StringUtils.equals(format, "binary")) {
    				byte[] b64certreq = org.ejbca.util.Base64.encode(request);
    				String out = "-----BEGIN CERTIFICATE REQUEST-----\n";
    				out += new String(b64certreq);
    				out += "\n-----END CERTIFICATE REQUEST-----\n";
    				length = out.length();
                    filename = "certificaterequest.pem";
                    outbytes = out.getBytes();
            	}
                // We must remove cache headers for IE
                ServletUtils.removeCacheHeaders(res);
                res.setHeader("Content-disposition", "attachment; filename=" +  filename);
                res.setContentType("application/octet-stream");
                res.setContentLength(length);
                res.getOutputStream().write(outbytes);
        		String iMsg = intres.getLocalizedMessage("certreq.sentlatestcertreq", remoteAddr);
                log.info(iMsg);
            } catch (Exception e) {
        		String errMsg = intres.getLocalizedMessage("certreq.errorsendlatestcertreq", remoteAddr);
                log.error(errMsg, e);
                res.sendError(HttpServletResponse.SC_NOT_FOUND, errMsg);
                return;
            }
        }
		if (command.equalsIgnoreCase(COMMAND_CERT)) {
			 try {
			 	Certificate cert = cabean.getProcessedCertificate();
            	if (!StringUtils.equals(format, "binary")) {
    				byte[] b64cert = org.ejbca.util.Base64.encode(cert.getEncoded());	
    				RequestHelper.sendNewB64Cert(b64cert, res, RequestHelper.BEGIN_CERTIFICATE_WITH_NL, RequestHelper.END_CERTIFICATE_WITH_NL);							
            	} else {
            		RequestHelper.sendBinaryBytes(cert.getEncoded(), res, "application/octet-stream", "cert.crt");
            	}
			 } catch (Exception e) {
				 String errMsg = intres.getLocalizedMessage("certreq.errorsendcert", remoteAddr, e.getMessage());
                 log.error(errMsg, e);
				 res.sendError(HttpServletResponse.SC_NOT_FOUND, errMsg);
				 return;
			 }
		 }
		if (command.equalsIgnoreCase(COMMAND_CERTPKCS7)) {
			 try {
				Certificate cert = cabean.getProcessedCertificate();		
		        byte[] pkcs7 =  getSignSession().createPKCS7(ejbcawebbean.getAdminObject(), cert, true);							 	
			    byte[] b64cert = org.ejbca.util.Base64.encode(pkcs7);	
			    RequestHelper.sendNewB64Cert(b64cert, res, RequestHelper.BEGIN_PKCS7_WITH_NL, RequestHelper.END_PKCS7_WITH_NL);																		 					
			 } catch (Exception e) {
				 String errMsg = intres.getLocalizedMessage("certreq.errorsendcert", remoteAddr, e.getMessage());
                 log.error(errMsg, e);
				 res.sendError(HttpServletResponse.SC_NOT_FOUND, errMsg);
				 return;
			 }
		 }




    } // doGet

}
