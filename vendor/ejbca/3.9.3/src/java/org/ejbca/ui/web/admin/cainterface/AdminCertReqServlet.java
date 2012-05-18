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

import java.beans.Beans;
import java.io.IOException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;

import javax.ejb.EJBException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.HttpSession;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocal;
import org.ejbca.core.ejb.ca.sign.ISignSessionLocalHome;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.AuthLoginException;
import org.ejbca.core.model.ca.AuthStatusException;
import org.ejbca.core.model.ca.IllegalKeyException;
import org.ejbca.core.model.ca.SignRequestException;
import org.ejbca.core.model.ca.SignRequestSignatureException;
import org.ejbca.core.model.ca.caadmin.CADoesntExistsException;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.PKCS10RequestMessage;
import org.ejbca.ui.web.RequestHelper;
import org.ejbca.ui.web.admin.configuration.EjbcaWebBean;
import org.ejbca.ui.web.admin.rainterface.RAInterfaceBean;
import org.ejbca.ui.web.admin.rainterface.UserView;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.FileTools;
import org.ejbca.util.StringTools;


/**
 * This is a servlet that is used for creating a user into EJBCA and retrieving her certificate.
 * This servlet requires authentication of the administrator, specifically it requires that the
 * client certificate has the privilege "/ra_functionallity/create_end_entity", as defined in the
 * admin-GUI.
 * 
 * <p>
 * This implementation handles only the POST method.
 * </p>
 * 
 * <p>
 * The CGI parameters for requests are the following.
 * </p>
 * 
 * <dl>
 * <dt>
 * pkcs10req
 * </dt>
 * <dd>
 * A PKCS#10 request, mandatory.
 * </dd>
 * <dt>
 * username
 * </dt>
 * <dd>
 * The username (for EJBCA use only).  Optional, defaults to the DN in the PKCS#10 request.
 * </dd>
 * <dt>
 * password
 * </dt>
 * <dd>
 * Password for the user (for EJBCA internal use only).  Optional, defaults to an empty string.
 * Used for authorization af certificate request.
 * </dd>
 * <dt>
 * entityprofile
 * </dt>
 * <dd>
 * The name of the EJBCA end entity profile for the user.  Optional, defaults to the built-in EMPTY
 * end entity profile.
 * </dd>
 * <dt>
 * certificateprofile
 * </dt>
 * <dd>
 * The name of the EJBCA certificate profile to use.  Optional, defaults to the built-in ENDUSER
 * certificate profile.
 * </dd>
 * <dt>ca</dt>
 * <dd>
 *   The name of the ca to use.  Required,
 * </dd>
 * </dl>
 * 
 *
 * @author Ville Skytt�
 * @version $Id: AdminCertReqServlet.java 7024 2009-02-24 11:27:33Z anatom $
 * 
 * @web.servlet name = "AdminCertReq"
 *              display-name = "AdminCertReqServlet"
 *              description="Used to retrive CA certificate request and Processed CA Certificates from AdminWeb GUI"
 *              load-on-startup = "99"
 *
 * @web.servlet-mapping url-pattern = "/ca/certreq"
 *
 */
public class AdminCertReqServlet extends HttpServlet {
    private final static Logger log = Logger.getLogger(AdminCertReqServlet.class);
    
    private final static byte[] BEGIN_CERT =
        "-----BEGIN CERTIFICATE-----".getBytes();
    private final static int BEGIN_CERT_LENGTH = BEGIN_CERT.length;
    
    private final static byte[] END_CERT =
        "-----END CERTIFICATE-----".getBytes();
    private final static int END_CERT_LENGTH = END_CERT.length;
    
    private final static byte[] NL = "\n".getBytes();
    private final static int NL_LENGTH = NL.length;
    
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

    public void init(ServletConfig config)
    throws ServletException
    {
        super.init(config);
        try {
            // Install BouncyCastle provider
            CertTools.installBCProvider();
        } catch (Exception e) {
            throw new ServletException(e);
        }
    }
    
    
    /**
     * Handles PKCS10 certificate request, these are constructed as:
     * <pre><code>
     * CertificationRequest ::= SEQUENCE {
     * certificationRequestInfo  CertificationRequestInfo,
     * signatureAlgorithm          AlgorithmIdentifier{{ SignatureAlgorithms }},
     * signature                       BIT STRING
     * }
     * CertificationRequestInfo ::= SEQUENCE {
     * version             INTEGER { v1(0) } (v1,...),
     * subject             Name,
     * subjectPKInfo   SubjectPublicKeyInfo{{ PKInfoAlgorithms }},
     * attributes          [0] Attributes{{ CRIAttributes }}
     * }
     * SubjectPublicKeyInfo { ALGORITHM : IOSet} ::= SEQUENCE {
     * algorithm           AlgorithmIdentifier {{IOSet}},
     * subjectPublicKey    BIT STRING
     * }
     * </pre>
     *
     * PublicKey's encoded-format has to be RSA X.509.
     */
    public void doPost(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {        
        // Check if authorized
        EjbcaWebBean ejbcawebbean= getEjbcaWebBean(request);
        try{
            ejbcawebbean.initialize(request, "/ra_functionallity/create_end_entity");
        } catch(Exception e){
            throw new java.io.IOException("Authorization Denied");
        }
        
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null) {
            throw new ServletException("This servlet requires certificate authentication!");
        }
        
        Admin admin = new Admin(certs[0]);
        
        RequestHelper.setDefaultCharacterEncoding(request);

        byte[] buffer = pkcs10Bytes(request.getParameter("pkcs10req"));
        if (buffer == null) {
            response.sendError(HttpServletResponse.SC_BAD_REQUEST, "Invalid request, missing 'pkcs10req'!");
            return;
        }
        
        RAInterfaceBean rabean = getRaBean(request);
        
        // Decompose the PKCS#10 request, and create the user.
        PKCS10RequestMessage p10 = new PKCS10RequestMessage(buffer);
        String dn = p10.getCertificationRequest().getCertificationRequestInfo().getSubject().toString();
        
        String username = request.getParameter("username");
        if (username == null || username.trim().length() == 0) {
            username = dn;
        }
        // Strip dangerous chars
        username = StringTools.strip(username);
        // need null check here?
        // Before doing anything else, check if the user name is unique and ok.
        username = checkUsername(rabean, username);
        
        UserView newuser = new UserView();
        newuser.setUsername(username);
        
        newuser.setSubjectDN(dn);
        newuser.setTokenType(SecConst.TOKEN_SOFT_BROWSERGEN);
        newuser.setKeyRecoverable(false);
        
        String email = CertTools.getPartFromDN(dn, "E"); // BC says VeriSign
        if (email == null) {
        	email = CertTools.getPartFromDN(dn, "EMAILADDRESS");
        } else {
            newuser.setEmail(email);
        }
        
        String tmp = null;
        int eProfileId = SecConst.EMPTY_ENDENTITYPROFILE;
        if ((tmp = request.getParameter("entityprofile")) != null) {
            int reqId = rabean.getEndEntityProfileId(tmp);
            if (reqId == 0) {
                throw new ServletException("No such end entity profile: " + tmp);
            }
            eProfileId = reqId;
        }
        newuser.setEndEntityProfileId(eProfileId);
        
        int cProfileId = SecConst.CERTPROFILE_FIXED_ENDUSER;
        if ((tmp = request.getParameter("certificateprofile")) != null) {
            CAInterfaceBean cabean = getCaBean(request);
            int reqId = cabean.getCertificateProfileId(tmp);
            if (reqId == 0) {
                throw new ServletException("No such certificate profile: " + tmp);
            }
            cProfileId = reqId;
        }
        newuser.setCertificateProfileId(cProfileId);
        
        int caid = 0;
        if ((tmp = request.getParameter("ca")) != null) {
            // TODO: get requested CA to sign with
        }
        newuser.setCAId(caid);
        
        
        String password = request.getParameter("password");
        if (password == null) {
        	password = "";
        }
        newuser.setPassword(password);
        newuser.setClearTextPassword(false);
        
        try {
            rabean.addUser(newuser);
        } catch (Exception e) {
            throw new ServletException("Error adding user: " + e.toString(), e);
        }
        
        byte[] pkcs7;
        try {
            p10.setUsername(username);
            p10.setPassword(password);
            ISignSessionLocal ss = getSignSession();
            IResponseMessage resp = ss.createCertificate(admin, p10, Class.forName(org.ejbca.core.protocol.X509ResponseMessage.class.getName()));
            Certificate cert = CertTools.getCertfromByteArray(resp.getResponseMessage());
            pkcs7 = ss.createPKCS7(admin, cert, true);
        } catch (ClassNotFoundException e) {
            // Class not found
            throw new ServletException(e);
        } catch (CertificateEncodingException e) {
            // Error in cert
            throw new ServletException(e);
        } catch (CertificateException e) {
            // Error in cert
            throw new ServletException(e);
        } catch (NotFoundException e) {
            // User not found
            throw new ServletException(e);
        } catch (AuthStatusException e) {
            // Wrong user status, shouldn't really happen.  The user needs to have
            // status of NEW, FAILED or INPROCESS.
            throw new ServletException(e);
        } catch (AuthLoginException e) {
            // Wrong username or password, hmm... wasn't the wrong username caught
            // in the objectnotfoundexception above... and this shouldn't happen.
            throw new ServletException(e);
        } catch (IllegalKeyException e) {
            // Malformed key (?)
            throw new ServletException(e);
        } catch (SignRequestException e) {
            // Invalid request
            throw new ServletException(e);
        } catch (SignRequestSignatureException e) {
            // Invalid signature in certificate request
            throw new ServletException(e);
        } catch (CADoesntExistsException e) {
            // Reqqested CA does not exist
            throw new ServletException(e);
        }
        
        log.debug("Created certificate (PKCS7) for " + username);
        
        sendNewB64Cert(Base64.encode(pkcs7), response);
        
    }
    
    
    public void doGet(HttpServletRequest request, HttpServletResponse response)
    throws IOException, ServletException
    {
        log.trace(">doGet()");
        response.setHeader("Allow", "POST");
        response.sendError(HttpServletResponse.SC_METHOD_NOT_ALLOWED, "The certificate request servlet only handles the POST method.");
        log.trace("<doGet()");
    } // doGet
    
    
    private void sendNewB64Cert(byte[] b64cert, HttpServletResponse out)
    throws IOException
    {
        out.setContentType("application/octet-stream");
        out.setHeader("Content-Disposition", "filename=cert.pem");
        out.setContentLength(b64cert.length +
                BEGIN_CERT_LENGTH + END_CERT_LENGTH + (3 *NL_LENGTH));
        
        ServletOutputStream os = out.getOutputStream();
        os.write(BEGIN_CERT);
        os.write(NL);
        os.write(b64cert);
        os.write(NL);
        os.write(END_CERT);
        os.write(NL);
        out.flushBuffer();
    }
    
    
    /**
     *
     */
    private final static byte[] pkcs10Bytes(String pkcs10)
    {
    	byte[] bytes = null;
        if (pkcs10 != null) {
            byte[] reqBytes = pkcs10.getBytes();
            try {
                // A real PKCS10 PEM request
                String beginKey = "-----BEGIN CERTIFICATE REQUEST-----";
                String endKey   = "-----END CERTIFICATE REQUEST-----";
                bytes = FileTools.getBytesFromPEM(reqBytes, beginKey, endKey);
            } catch (IOException e) {
                try {
                    // Keytool PKCS10 PEM request
                    String beginKey = "-----BEGIN NEW CERTIFICATE REQUEST-----";
                    String endKey   = "-----END NEW CERTIFICATE REQUEST-----";
                    bytes = FileTools.getBytesFromPEM(reqBytes, beginKey, endKey);
                } catch (IOException e2) {
                    // IE PKCS10 Base64 coded request
                    bytes = Base64.decode(reqBytes);
                }
            }
        }
        return bytes;
    }
    
    
    /**
     *
     */
    private final RAInterfaceBean getRaBean(HttpServletRequest req)
    throws ServletException
    {
        HttpSession session = req.getSession();
        RAInterfaceBean rabean = (RAInterfaceBean) session.getAttribute("rabean");
        if (rabean == null) {
            try {
                rabean = (RAInterfaceBean) Beans.instantiate(this.getClass().getClassLoader(), org.ejbca.ui.web.admin.rainterface.RAInterfaceBean.class.getName());
            } catch (ClassNotFoundException e) {
                throw new ServletException(e);
            } catch (Exception e) {
                throw new ServletException("Unable to instantiate RAInterfaceBean", e);
            }
            try {
                rabean.initialize(req, getEjbcaWebBean(req));
            } catch (Exception e) {
                throw new ServletException("Cannot initialize RAInterfaceBean", e);
            }
            session.setAttribute("rabean", rabean);
        }
        return rabean;
    }
    
    
    /**
     *
     */
    private final EjbcaWebBean getEjbcaWebBean(HttpServletRequest req)
    throws ServletException
    {
        HttpSession session = req.getSession();
        EjbcaWebBean ejbcawebbean= (EjbcaWebBean)session.getAttribute("ejbcawebbean");
        if ( ejbcawebbean == null ){
            try {
                ejbcawebbean = (EjbcaWebBean) java.beans.Beans.instantiate(this.getClass().getClassLoader(), org.ejbca.ui.web.admin.configuration.EjbcaWebBean.class.getName());
            } catch (ClassNotFoundException exc) {
                throw new ServletException(exc.getMessage());
            }catch (Exception exc) {
                throw new ServletException (" Cannot create bean of class "+org.ejbca.ui.web.admin.configuration.EjbcaWebBean.class.getName(), exc);
            }
            session.setAttribute("ejbcawebbean", ejbcawebbean);
        }
        return ejbcawebbean;
    }
    /**
     *
     */
    private final CAInterfaceBean getCaBean(HttpServletRequest req)
    throws ServletException
    {
        HttpSession session = req.getSession();
        CAInterfaceBean cabean = (CAInterfaceBean) session.getAttribute("cabean");
        if (cabean == null) {
            try {
                cabean = (CAInterfaceBean) Beans.instantiate(this.getClass().getClassLoader(), org.ejbca.ui.web.admin.cainterface.CAInterfaceBean.class.getName());
            } catch (ClassNotFoundException e) {
                throw new ServletException(e);
            } catch (Exception e) {
                throw new ServletException("Unable to instantiate CAInterfaceBean", e);
            }
            try {
                cabean.initialize(req, getEjbcaWebBean(req));
            } catch (Exception e) {
                throw new ServletException("Cannot initialize CAInterfaceBean", e);
            }
            session.setAttribute("cabean", cabean);
        }
        return cabean;
    }
    
    
    /**
     *
     */
    private final String checkUsername(RAInterfaceBean rabean, String username)
    throws ServletException
    {
        if (username != null) {
        	username = username.trim();
        }
        if (username == null || username.length() == 0) {
            throw new ServletException("Username must not be empty.");
        }
        
        String msg = null;
        try {
            if (rabean.userExist(username)) {
                msg = "User '" + username + "' already exists.";
            }
        } catch (Exception e) {
            throw new ServletException("Error checking username '" + username +
                    ": " + e.toString(), e);
        }
        if (msg != null) {
            throw new ServletException(msg);
        }
        
        return username;
    }
    
}
