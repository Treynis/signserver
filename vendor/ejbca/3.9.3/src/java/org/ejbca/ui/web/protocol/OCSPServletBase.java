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

package org.ejbca.ui.web.protocol;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.net.URLDecoder;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.HashMap;
import java.util.Hashtable;
import java.util.Iterator;
import java.util.Properties;

import javax.servlet.ServletConfig;
import javax.servlet.ServletException;
import javax.servlet.ServletInputStream;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.ocsp.OCSPObjectIdentifiers;
import org.bouncycastle.asn1.ocsp.RevokedInfo;
import org.bouncycastle.asn1.x509.CRLReason;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.OCSPException;
import org.bouncycastle.ocsp.OCSPReq;
import org.bouncycastle.ocsp.OCSPResp;
import org.bouncycastle.ocsp.OCSPRespGenerator;
import org.bouncycastle.ocsp.Req;
import org.bouncycastle.ocsp.RevokedStatus;
import org.bouncycastle.ocsp.SingleResp;
import org.bouncycastle.ocsp.UnknownStatus;
import org.bouncycastle.util.encoders.Hex;
import org.ejbca.config.OcspConfiguration;
import org.ejbca.core.ejb.ca.store.CertificateStatus;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.SecConst;
import org.ejbca.core.model.ca.MalformedRequestException;
import org.ejbca.core.model.ca.SignRequestException;
import org.ejbca.core.model.ca.SignRequestSignatureException;
import org.ejbca.core.model.ca.caadmin.CADoesntExistsException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceNotActiveException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequestException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.IllegalExtendedCAServiceRequestException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.OCSPCAServiceRequest;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.OCSPCAServiceResponse;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.protocol.ocsp.AuditLogger;
import org.ejbca.core.protocol.ocsp.CertificateCache;
import org.ejbca.core.protocol.ocsp.IAuditLogger;
import org.ejbca.core.protocol.ocsp.IOCSPExtension;
import org.ejbca.core.protocol.ocsp.IOCSPLogger;
import org.ejbca.core.protocol.ocsp.ISaferAppenderListener;
import org.ejbca.core.protocol.ocsp.ITransactionLogger;
import org.ejbca.core.protocol.ocsp.OCSPResponseItem;
import org.ejbca.core.protocol.ocsp.OCSPUnidResponse;
import org.ejbca.core.protocol.ocsp.OCSPUtil;
import org.ejbca.core.protocol.ocsp.TransactionLogger;
import org.ejbca.util.CertTools;
import org.ejbca.util.DummyPatternLogger;
import org.ejbca.util.GUIDGenerator;
import org.ejbca.util.IPatternLogger;

/**
 * @author Thomas Meckel (Ophios GmbH), Tomas Gustavsson, Lars Silven
 * @version  $Id: OCSPServletBase.java 8052 2009-10-01 11:42:58Z anatom $
 */
public abstract class OCSPServletBase extends HttpServlet implements ISaferAppenderListener { 

	private static final Logger m_log = Logger.getLogger(OCSPServletBase.class);
	
	/** Max size of an OCSP request is 100000 bytes */
	private static final int MAX_OCSP_REQUEST_SIZE = 100000;
	
	/** Internal localization of logs and errors */
	private static final InternalResources intres = InternalResources.getInstance();
	private  boolean canlog =true;
	protected final Admin m_adm = new Admin(Admin.TYPE_INTERNALUSER);
	private final String m_sigAlg = OcspConfiguration.getSignatureAlgorithm();
	private final boolean m_reqMustBeSigned = OcspConfiguration.getEnforceRequestSigning();
	/** True if requests must be signed by a certificate issued by a list of trusted CA's*/
	private final boolean m_reqRestrictSignatures = OcspConfiguration.getRestrictSignatures();
	private final int m_reqRestrictMethod = OcspConfiguration.getRestrictSignaturesByMethod();
	private final int m_signTrustValidTime = OcspConfiguration.getSignTrustValidTime();
	/** A list of CA's trusted for issuing certificates for signing requests */
	private Hashtable mTrustedReqSigIssuers;
	private Hashtable mTrustedReqSigSigners;
	/** String used to identify default responder id, used to generate responses when a request
	 * for a certificate not signed by a CA on this server is received.
	 */
	private final String m_defaultResponderId = OcspConfiguration.getDefaultResponderId();
	/** Marks if the CAs certificate or the CAs OCSP responder certificate should be used for 
	 * signing the OCSP response.
	 */
	private final boolean m_useCASigningCert = OcspConfiguration.getUseCASigningCert();
	/** Marks if the CAs certificate chain should be included in the OCSP response or not 
	 */
	private final boolean m_includeChain = OcspConfiguration.getIncludeCertChain();
	/** If true a certificate that does not exist in the database, but is issued by a CA the responder handles
	 * will be treated as not revoked. Default (when value is true) is to treat is as "unknown".
	 */
	private final boolean m_nonExistingIsGood = OcspConfiguration.getNonExistingIsGood();
	/** Controls which of the two possible types of responderId should be used. See RFC2560 for details.
	 * Default is to use KeyId, the other possible type is X500name.
	 */
	private final int m_respIdType = OcspConfiguration.getResponderIdType();

	/** The interval on which new OCSP signing certs and keys are loaded in seconds. */
	protected final int m_valid_time = OcspConfiguration.getSigningCertsValidTime();
	/** Cache time counter, set and used by loadPrivateKeys (external responder) */
	protected long mKeysValidTo = 0;

	/** Cache of CA certificates (and chain certs) for CAs handles by this responder */
	protected CertificateCache m_caCertCache = null;
	
	/** Configures OCSP extensions, these init-params are optional
	 */
	private final Collection m_extensionOids = OcspConfiguration.getExtensionOids();
	private final Collection m_extensionClasses = OcspConfiguration.getExtensionClasses();
	private HashMap m_extensionMap = null;
	private final boolean mDoAuditLog = OcspConfiguration.getAuditLog();
	private final boolean mDoTransactionLog = OcspConfiguration.getTransactionLog();

	/**
	 * The interval on which new OCSP signing certs are loaded in seconds.
	 */
	private long m_trustDirValidTo;
	private final String m_signTrustDir = OcspConfiguration.getSignTrustDir();
	private int mTransactionID = 0;
	private final String m_SessionID = GUIDGenerator.generateGUID(this);
	private final boolean mDoSaferLogging = OcspConfiguration.getLogSafer();
	/** Method gotten through reflection, we put it in a variable so we don't have to use
	 * reflection every time we use the audit or transaction log */
	private Method m_errorHandlerMethod = null;
    private TransactionLogger transactionLogger;
    private AuditLogger auditLogger;
	private static final String PROBEABLE_ERRORHANDLER_CLASS = "org.ejbca.appserver.jboss.ProbeableErrorHandler";
	private static final String SAFER_LOG4JAPPENDER_CLASS = "org.ejbca.appserver.jboss.SaferDailyRollingFileAppender";
	
	protected synchronized void loadTrustDir() throws Exception {
		// Check if we have a cached collection that is not too old
		if(m_reqRestrictMethod == OcspConfiguration.RESTRICTONISSUER) {
			if (mTrustedReqSigIssuers != null && m_trustDirValidTo > new Date().getTime()) {
				return;
			}
			mTrustedReqSigIssuers = OCSPUtil.getCertificatesFromDirectory(m_signTrustDir);
			if (m_log.isDebugEnabled()) {
				m_log.debug("Loaded "+mTrustedReqSigIssuers == null ? "0":mTrustedReqSigIssuers.size()+" CA-certificates as trusted for OCSP-request signing");        	
			}
			m_trustDirValidTo = m_signTrustValidTime>0 ? new Date().getTime()+m_signTrustValidTime : Long.MAX_VALUE;;
		}
		if(m_reqRestrictMethod == OcspConfiguration.RESTRICTONSIGNER) {
			if (mTrustedReqSigSigners != null && m_trustDirValidTo > new Date().getTime()) {
				return;
			}
			mTrustedReqSigSigners = OCSPUtil.getCertificatesFromDirectory(m_signTrustDir);
			if (m_log.isDebugEnabled()) {
				m_log.debug("Loaded "+mTrustedReqSigSigners == null ? "0":mTrustedReqSigSigners.size()+" Signer-certificates as trusted for OCSP-request signing");        	
			}
			m_trustDirValidTo = m_signTrustValidTime>0 ? new Date().getTime()+m_signTrustValidTime : Long.MAX_VALUE;;
		}
	}

	abstract void loadPrivateKeys(Admin adm, String password) throws Exception;

	abstract Certificate findCertificateByIssuerAndSerno(Admin adm, String issuerDN, BigInteger serno);

	abstract OCSPCAServiceResponse extendedService(Admin m_adm2, int caid, OCSPCAServiceRequest request) throws CADoesntExistsException, ExtendedCAServiceRequestException, IllegalExtendedCAServiceRequestException, ExtendedCAServiceNotActiveException;

    abstract CertificateStatus getStatus(Admin adm, String name, BigInteger serialNumber);

	/** returns a CertificateCache of appropriate type */
	abstract CertificateCache createCertificateCache(Properties prop);

	/** Generates an EJBCA caid from a CA certificate, or looks up the default responder certificate.
	 * 
	 * @param cacert the CA certificate to get the CAid from. If this is null, the default responder CA cert  is looked up and used
	 * @return int 
	 */
	 int getCaid( X509Certificate cacert ) {
		X509Certificate cert = cacert;
		if (cacert == null) {
			m_log.debug("No correct CA-certificate available to sign response, signing with default CA: "+m_defaultResponderId);
			cert = m_caCertCache.findLatestBySubjectDN(m_defaultResponderId);    		
		}

		int result = CertTools.stringToBCDNString(cert.getSubjectDN().toString()).hashCode();
		if (m_log.isDebugEnabled()) {
			m_log.debug( cert.getSubjectDN() + " has caid: " + result );
		}
		return result;
	}


	private BasicOCSPResp signOCSPResponse(OCSPReq req, ArrayList responseList, X509Extensions exts, X509Certificate cacert)
	throws CADoesntExistsException, ExtendedCAServiceRequestException, ExtendedCAServiceNotActiveException, IllegalExtendedCAServiceRequestException {

	    // Call extended CA services to get our OCSP stuff
	    OCSPCAServiceRequest ocspservicerequest = new OCSPCAServiceRequest(req, responseList, exts, m_sigAlg, m_useCASigningCert, m_includeChain);
	    ocspservicerequest.setRespIdType(m_respIdType);
	    OCSPCAServiceResponse caserviceresp = extendedService(m_adm, getCaid(cacert), ocspservicerequest);
	    // Now we can use the returned OCSPServiceResponse to get private key and cetificate chain to sign the ocsp response
	    if (m_log.isDebugEnabled()) {
	        Collection coll = caserviceresp.getOCSPSigningCertificateChain();
	        m_log.debug("Cert chain for OCSP signing is of size " + coll.size());            	
	    }
	    return caserviceresp.getBasicOCSPResp();
	}

	/* (non-Javadoc)
	 * @see javax.servlet.GenericServlet#init(javax.servlet.ServletConfig)
	 */
	public void init(ServletConfig config) throws ServletException {
		super.init(config);
		CertTools.installBCProvider();
		if (m_log.isDebugEnabled()) {
			m_log.debug("signTrustValidTime is: " + m_signTrustValidTime);
			m_log.debug("SignatureAlgorithm is: " + m_sigAlg);
			m_log.debug("defaultResponderID is: " + m_defaultResponderId);
		}
		if (m_reqRestrictSignatures) {
			if (m_log.isDebugEnabled()) {
				m_log.debug("Directory containing trusted CA's for request Signing: '"
						+ (StringUtils.isEmpty(m_signTrustDir) ? "<not set>" : m_signTrustDir) + "'");
			}
			try {
				if ( m_reqRestrictMethod == OcspConfiguration.RESTRICTONISSUER ) {
					mTrustedReqSigIssuers = OCSPUtil.getCertificatesFromDirectory(m_signTrustDir);
				} else {
					mTrustedReqSigSigners = OCSPUtil.getCertificatesFromDirectory(m_signTrustDir);
				}
			} catch (IOException e1) {
				m_log.error("OCSP request signatures are restricted but allowed signatures could not be read from file, check ocsp.properties." +e1);
			} 
		}
		if (m_log.isDebugEnabled()) {
			m_log.debug("Use CA signing cert: '" + m_useCASigningCert + "'");
			m_log.debug("Responder Id type: '" + m_respIdType + "'");
			m_log.debug("Include certificate chain: '" + m_includeChain + "'");
			m_log.debug("Non existing certificates are good: '" + m_nonExistingIsGood + "'");
			m_log.debug("Are we doing auditLogging?: '" + mDoAuditLog + "'");
		}
		// Set up Audit and Transaction Logging
		String timezone = OcspConfiguration.getLogTimeZone();
		m_log.debug("Time zone setting: '" + timezone + "'");
		String logDateFormat = OcspConfiguration.getLogDateFormat();
		m_log.debug("Date format setting: '" + logDateFormat + "'");
		if (mDoAuditLog==true) { // If we are not going to do any logging we wont bother setting it up
			final String auditLogPattern = OcspConfiguration.getAuditLogPattern();
			m_log.debug("Pattern used for auditLogPattern: '" + auditLogPattern + "'");
			final String auditLogOrder = OcspConfiguration.getAuditLogOrder();
			m_log.debug("Pattern used for auditLogOrder: '" + auditLogOrder + "'");
			this.auditLogger = new AuditLogger(auditLogPattern, auditLogOrder,logDateFormat, timezone);
		}
		m_log.debug("Are we doing auditLogging?: '" + mDoTransactionLog + "'");
		if (mDoTransactionLog==true) { // If we are not going to do any logging we wont bother setting it up
			final String transactionLogPattern = OcspConfiguration.getTransactionLogPattern();
			m_log.debug("Pattern used for transactionLogPattern: '" + transactionLogPattern + "'");
			final String transactionLogOrder = OcspConfiguration.getTransactionLogOrder();
			m_log.debug("Pattern used for transactionLogOrder: '" + transactionLogOrder + "'");
            this.transactionLogger = new TransactionLogger(transactionLogPattern, transactionLogOrder, logDateFormat, timezone);
		}
		// Are we supposed to abort the response if logging is failing?
		m_log.debug("Are we doing safer logging?: '" + mDoSaferLogging + "'");
        if (mDoSaferLogging==true) {
            try {
                final Class implClass = Class.forName(SAFER_LOG4JAPPENDER_CLASS);
                final Method method = implClass.getMethod("addSubscriber", ISaferAppenderListener.class);
                method.invoke(null, this); // first object parameter can be null because this is a static method
                m_log.info("added us as subscriber to org.ejbca.appserver.jboss.SaferDailyRollingFileAppender");
                // create the method object of the static probeable error handler, so we don't have to do this every time we log
    			final Class errHandlerClass = Class.forName(PROBEABLE_ERRORHANDLER_CLASS);
    			m_errorHandlerMethod = errHandlerClass.getMethod("hasFailedSince", Date.class);
            } catch (Exception e) {
                m_log.error("Was configured to do safer logging but could not instantiate needed classes", e);
            }
        }
		// Setup extensions
        if (m_extensionOids.size() == 0) {
			m_log.info("ExtensionOids not defined.");
        }
        if (m_extensionClasses.size() == 0) {
			m_log.info("ExtensionClass not defined.");
        }
		if (m_extensionClasses.size() != m_extensionOids.size()) {
			throw new ServletException("Number of extension classes does not match no of extension oids.");        	
		}
		Iterator iter = m_extensionClasses.iterator();
		Iterator iter2 = m_extensionOids.iterator();
		m_extensionMap = new HashMap();
		while (iter.hasNext()) {
			String clazz = (String)iter.next();
			String oid = (String)iter2.next();
			IOCSPExtension ext = null;
			try {
				ext = (IOCSPExtension)Class.forName(clazz).newInstance();
				ext.init(config);
			} catch (Exception e) {
				m_log.error("Can not create extension with class "+clazz, e);
				continue;
			}
			m_extensionMap.put(oid,ext);
		}
		// Cache-friendly parameters
		if (m_log.isDebugEnabled()) {
			m_log.debug("untilNextUpdate: " + OcspConfiguration.getUntilNextUpdate(SecConst.CERTPROFILE_NO_PROFILE));
			m_log.debug("maxAge: " + OcspConfiguration.getMaxAge(SecConst.CERTPROFILE_NO_PROFILE));
			m_log.debug("ocspSigningCertsValidTime is: " + m_valid_time);
		}
		// Finally we load the CA certificates and private keys of this OCSP responder
		// Create properties used to set up the CertificateCache
		Properties cacheProperties = new Properties();
		cacheProperties.put("ocspSigningCertsValidTime", Integer.valueOf(m_valid_time));
		// Create and load the certificate cache if this is an internal or external OCSP responder
		m_caCertCache = createCertificateCache(cacheProperties);
	} // init
	
	/* (non-Javadoc)
	 * @see org.ejbca.ui.web.protocol.SaferAppenderInterface#canlog(boolean)
	 */
	public void setCanlog(boolean pCanlog) {
		canlog = pCanlog;
	}

	/** Method that checks with ProbeableErrorHandler if an error has happended since a certain time.
	 * Uses reflection to call ProbeableErrorHandler because it is dependent on JBoss log4j logging, 
	 * which is not available on other application servers.
	 * 
	 * @param startTime
	 * @return true if an error has occurred since startTime 
	 */
    private boolean hasErrorHandlerFailedSince(Date startTime) {
    	boolean result = true; // Default true. If something goes wrong we will fail
        if ( m_errorHandlerMethod == null ) {
        	result = false;
        } else {
            try {
                result = ((Boolean)m_errorHandlerMethod.invoke(null, startTime)).booleanValue(); // first object parameter can be null because this is a static method
                if (result) {
                    m_log.error("Audit and/or account logging failed since "+startTime);
                }
            } catch (Exception e) {
                m_log.error(e);
            }
        }
        return result;
    }

    public void doPost(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
        m_log.trace(">doPost()");
        try {
            final String contentType = request.getHeader("Content-Type");
            if ( contentType!=null && contentType.equalsIgnoreCase("application/ocsp-request")) {
                serviceOCSP(request, response);
                return;
            }
            if ( contentType!=null ) {
                final String sError = "Content-type is not application/ocsp-request. It is \'"+contentType+"\'.";
                m_log.debug(sError);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, sError);
                return;
            }
            final String password=request.getHeader("activate");
            if ( password==null ) {
                final String sError = "No \'Content-Type\' or \'activate\' property in request.";
                m_log.debug(sError);
                response.sendError(HttpServletResponse.SC_BAD_REQUEST, sError);
                return;
            }
            final String remoteAddr = request.getRemoteAddr();
            if ( !remoteAddr.equals("127.0.0.1") ) {
                final String sError = "You have connected from \'"+remoteAddr+"\'. You may only connect from 127.0.0.1";
                m_log.debug(sError);
                response.sendError(HttpServletResponse.SC_UNAUTHORIZED, sError);
                return;
            }
            // Also reload signing keys
            this.mKeysValidTo = 0;
            try {
                loadPrivateKeys(this.m_adm, password);
            } catch (Exception e) {
                m_log.error("Problem loading keys.", e);
                response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Problem. See ocsp responder server log.");
            }
        } finally {
            m_log.trace("<doPost()");
        }
	} //doPost

	public void doGet(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
		m_log.trace(">doGet()");
		// We have a command to force reloading of keys that can only be run from localhost
		final boolean doReload = StringUtils.equals(request.getParameter("reloadkeys"), "true");
		if ( doReload ) {
			final String remote = request.getRemoteAddr();
			if (StringUtils.equals(remote, "127.0.0.1")) {
				String iMsg = intres.getLocalizedMessage("ocsp.reloadkeys", remote);
				m_log.info(iMsg);
				// Reload CA certificates
				this.m_caCertCache.forceReload();
				try {
					// Also reload signing keys
					this.mKeysValidTo = 0;
					loadPrivateKeys(this.m_adm, null);
				} catch (Exception e) {
                    m_log.error("Problem loading keys.", e);
                    response.sendError(HttpServletResponse.SC_INTERNAL_SERVER_ERROR, "Problem. See ocsp responder server log.");
				}
			} else {
				m_log.info("Got reloadKeys command from unauthorized ip: "+remote);
				response.sendError(HttpServletResponse.SC_UNAUTHORIZED);
			}
		} else {
			serviceOCSP(request, response);
		}
		m_log.trace("<doGet()");
	} // doGet

	/**
	 * Reads the request bytes and verifies min and max size of the request. If an error occurs it throws a MalformedRequestException. 
	 * Can get request bytes both from a HTTP GET and POST request
	 * 
	 * @param request
	 * @param response
	 * @return the request bytes or null if an error occured.
	 * @throws IOException In case there is no stream to read
	 * @throws MalformedRequestException 
	 */
	private byte[] checkAndGetRequestBytes(HttpServletRequest request, HttpServletResponse response) throws IOException, MalformedRequestException {
		byte[] ret = null;
		// Get the request data
		String method = request.getMethod();
		String remoteAddress = request.getRemoteAddr();
		if (m_log.isDebugEnabled()) {
			m_log.debug("Received "+method+" request with content length: "+request.getContentLength()+" from "+remoteAddress);		
		}
		if (request.getContentLength() > MAX_OCSP_REQUEST_SIZE) {
			String msg = intres.getLocalizedMessage("ocsp.toolarge", MAX_OCSP_REQUEST_SIZE, request.getContentLength());
			m_log.info(msg);
			throw new MalformedRequestException(msg);
		} else {
			// So we passed basic tests, now we can read the bytes, but still keep an eye on the size
			// we can not fully trust the sent content length.
			if (StringUtils.equals(method, "POST")) {
		        ServletInputStream in = request.getInputStream();
				ByteArrayOutputStream baos = new ByteArrayOutputStream();
				try {
					// This works for small requests, and OCSP requests are small
					int b = in.read();
					while ( (b != -1) && (baos.size() <= MAX_OCSP_REQUEST_SIZE) ) {
						baos.write(b);
						b = in.read();
					}
					// Double-check so the actual data also is smaller than the allowed length, not just the Content-Length header.
					if (baos.size() > MAX_OCSP_REQUEST_SIZE) {
						String msg = intres.getLocalizedMessage("ocsp.toolarge", MAX_OCSP_REQUEST_SIZE, baos.size());
						m_log.info(msg);
						throw new MalformedRequestException(msg);
					} else {
						// All seems good, we got the request bytes
						baos.flush();
						in.close();
						ret = baos.toByteArray();				
					}
				} finally {
					in.close();
					baos.close();
				}				
			} else if (StringUtils.equals(method, "GET")) {
				// GET request
				final StringBuffer url = request.getRequestURL();
				// RFC2560 A.1.1 says that request longer than 255 bytes SHOULD be sent by POST, we support GET for longer requests anyway.
				if (url.length() <= MAX_OCSP_REQUEST_SIZE) {
					final String decodedRequest;
					try {
						// We have to extract the pathInfo manually, to avoid multiple slashes being converted to a single
						// According to RFC 2396 2.2 chars only have to encoded if they conflict with the purpose, so
						// we can for example expect both '/' and "%2F" in the request.
						final String fullServletpath = request.getContextPath() + request.getServletPath();
						final String requestString = url.substring(Math.max(url.indexOf(fullServletpath), 0) + fullServletpath.length() + 1);
						decodedRequest = URLDecoder.decode(requestString, "UTF-8").replaceAll(" ", "+");
//						if (m_log.isDebugEnabled()) {
//							m_log.debug("URL: "+url.toString());
//						}
					} catch (Exception e) {
						String msg = intres.getLocalizedMessage("ocsp.badurlenc");
						m_log.info(msg);
						throw new MalformedRequestException(e);
					}
					if (decodedRequest != null && decodedRequest.length() > 0) {
						if (m_log.isDebugEnabled()) {
							// Don't log the request if it's too long, we don't want to cause denial of service by filling log files or buffers.
							if (decodedRequest.length() < 2048) {
								m_log.debug("decodedRequest: "+decodedRequest);
							} else {
								m_log.debug("decodedRequest too long to log: "+decodedRequest.length());
							}
						}
						try {
							ret = org.ejbca.util.Base64.decode(decodedRequest.getBytes());
						} catch (Exception e) {
							String msg = intres.getLocalizedMessage("ocsp.badurlenc");
							m_log.info(msg);
							throw new MalformedRequestException(e);
						}
					} else {
						String msg = intres.getLocalizedMessage("ocsp.missingreq");
						m_log.info(msg);
						throw new MalformedRequestException(msg);
					}
				} else {
					String msg = intres.getLocalizedMessage("ocsp.toolarge", MAX_OCSP_REQUEST_SIZE, url.length());
					m_log.info(msg);
					throw new MalformedRequestException(msg);
				}
			} else {
				// Strange, an unknown method
				String msg = intres.getLocalizedMessage("ocsp.unknownmethod", method);
				m_log.info(msg);
				throw new MalformedRequestException(msg);
			}
		}
		// Make a final check that we actually received something
		if ((ret == null) || (ret.length == 0)) {
			String msg = intres.getLocalizedMessage("ocsp.emptyreq", remoteAddress);
			m_log.info(msg);
			throw new MalformedRequestException(msg);
		}
		return ret;
	}
	
	/** Performs service of the actual OCSP request, which is contained in reqBytes. 
	 *  
	 *  @param reqBytes the binary OCSP request bytes. This parameter must already have been checked for max or min size. 
	 */
	public void serviceOCSP(HttpServletRequest request, HttpServletResponse response)
	throws IOException, ServletException {
		if (m_log.isTraceEnabled()) {
			m_log.trace(">service()");
		}
        mTransactionID += 1;
		final IPatternLogger transactionLogger;
		final IPatternLogger auditLogger;
		final Date startTime = new Date();
		if (this.mDoTransactionLog) {
			transactionLogger = this.transactionLogger.getPatternLogger();
		} else {
			transactionLogger = new DummyPatternLogger();	// Ignores everything
		}
		if (this.mDoAuditLog) {
			auditLogger = this.auditLogger.getPatternLogger();
		} else {
			auditLogger = new DummyPatternLogger();	// Ignores everything
		}
		final String remoteAddress = request.getRemoteAddr();
		auditLogger.paramPut(IAuditLogger.OCSPREQUEST, ""); // No request bytes yet
		auditLogger.paramPut(IPatternLogger.LOG_ID, new Integer(this.mTransactionID));
		auditLogger.paramPut(IPatternLogger.SESSION_ID, this.m_SessionID);
		auditLogger.paramPut(IOCSPLogger.CLIENT_IP, remoteAddress);
		transactionLogger.paramPut(IPatternLogger.LOG_ID, new Integer(this.mTransactionID));
		transactionLogger.paramPut(IPatternLogger.SESSION_ID, this.m_SessionID);
		transactionLogger.paramPut(IOCSPLogger.CLIENT_IP, remoteAddress);

		try {
			// Read configuration values affecting the response, these can be dynamically updated from properties files in file system
			// Read default values here for each request since may take a millisecond to read the value
			// These values can be changed depending on if there are different configurations for different certificate profiles
			// In that case it is updated once we have read the certificate status of the certificate searched for.
			long maxAge = OcspConfiguration.getMaxAge(SecConst.CERTPROFILE_NO_PROFILE); 
			long nextUpdate = OcspConfiguration.getUntilNextUpdate(SecConst.CERTPROFILE_NO_PROFILE);

			OCSPResp ocspresp = null;
			OCSPRespGenerator res = new OCSPRespGenerator();
			X509Certificate cacert = null; // CA-certificate used to sign response
			try {
				byte[] reqBytes = checkAndGetRequestBytes(request, response);
				auditLogger.paramPut(IAuditLogger.OCSPREQUEST, new String (Hex.encode(reqBytes)));
				OCSPReq req = null;
				try {
					req = new OCSPReq(reqBytes);					
				} catch (Exception e) {
					// When not beeing able to parse the request, we want to send a MalformedRequest back
					throw new MalformedRequestException(e);
				}
				if (req.getRequestorName() == null) {
					m_log.debug("Requestorname is null"); 
				} else {
					if (m_log.isDebugEnabled()) {
						m_log.debug("Requestorname is: "+req.getRequestorName().toString());						
					}
					transactionLogger.paramPut(ITransactionLogger.REQ_NAME, req.getRequestorName().toString());
				}
				// Make sure our signature keys are updated
				loadPrivateKeys(m_adm, null);

				/**
				 * check the signature if contained in request.
				 * if the request does not contain a signature
				 * and the servlet is configured in the way 
				 * the a signature is required we send back
				 * 'sigRequired' response.
				 */
				if (m_log.isDebugEnabled()) {
					m_log.debug("Incoming OCSP request is signed : " + req.isSigned());
				}
				if (req.isSigned()) {
					X509Certificate signercert = OCSPUtil.checkRequestSignature(request.getRemoteAddr(), req, m_caCertCache);
					String signercertIssuerName = CertTools.getIssuerDN(signercert);
					BigInteger signercertSerNo = CertTools.getSerialNumber(signercert);
					String signercertSubjectName = CertTools.getSubjectDN(signercert);
					transactionLogger.paramPut(ITransactionLogger.SIGN_ISSUER_NAME_DN, signercertIssuerName);
					transactionLogger.paramPut(ITransactionLogger.SIGN_SERIAL_NO, signercert.getSerialNumber().toByteArray());
					transactionLogger.paramPut(ITransactionLogger.SIGN_SUBJECT_NAME, signercertSubjectName);
					transactionLogger.paramPut(IPatternLogger.REPLY_TIME, ITransactionLogger.REPLY_TIME);
					if (m_reqMustBeSigned) {
						// If it verifies OK, check if it is revoked
						final CertificateStatus status = getStatus(m_adm, CertTools.getIssuerDN(signercert), CertTools.getSerialNumber(signercert));
						// If rci == null it means the certificate does not exist in database, we then treat it as ok,
						// because it may be so that only revoked certificates is in the (external) OCSP database.
						if ( status.equals(CertificateStatus.REVOKED) ) {
							String serno = signercertSerNo.toString(16);
							String infoMsg = intres.getLocalizedMessage("ocsp.infosigner.revoked", signercertSubjectName, signercertIssuerName, serno);
							m_log.info(infoMsg);
							throw new SignRequestSignatureException(infoMsg);
						}

						if (m_reqRestrictSignatures) {
							loadTrustDir();
							if ( m_reqRestrictMethod == OcspConfiguration.RESTRICTONSIGNER) {
								if (!OCSPUtil.checkCertInList(signercert, mTrustedReqSigSigners)) {
									String infoMsg = intres.getLocalizedMessage("ocsp.infosigner.notallowed", signercertSubjectName, signercertIssuerName, signercertSerNo.toString(16));
									m_log.info(infoMsg);
									throw new SignRequestSignatureException(infoMsg);
								}
							} else if (m_reqRestrictMethod == OcspConfiguration.RESTRICTONISSUER) {
								X509Certificate signerca = m_caCertCache.findLatestBySubjectDN(signercertIssuerName);
								if ((signerca == null) || (!OCSPUtil.checkCertInList(signerca, mTrustedReqSigIssuers)) ) {
									String infoMsg = intres.getLocalizedMessage("ocsp.infosigner.notallowed", signercertSubjectName, signercertIssuerName, signercertSerNo.toString(16));
									m_log.info(infoMsg);
									throw new SignRequestSignatureException(infoMsg);
								}
							} else {
								throw new Exception("m_reqRestrictMethod="+m_reqRestrictMethod); // there must be an internal error. We do not want to send a response, just to be safe.
							}
						}
					}
				} else {
					if (m_reqMustBeSigned) {
						// Signature required
						throw new SignRequestException("Signature required");
					}
				}
				
				// Get the certificate status requests that are inside this OCSP req
				Req[] requests = req.getRequestList();
				transactionLogger.paramPut(ITransactionLogger.NUM_CERT_ID, requests.length);
				if (requests.length <= 0) {
					String infoMsg = intres.getLocalizedMessage("ocsp.errornoreqentities");
					m_log.info(infoMsg);
					{
						// All this just so we can create an error response
						cacert = m_caCertCache.findLatestBySubjectDN(m_defaultResponderId);
					}
					throw new MalformedRequestException(infoMsg);
				}
				int maxRequests = 100;
				if (requests.length > maxRequests) {
					String infoMsg = intres.getLocalizedMessage("ocsp.errortoomanyreqentities", maxRequests);
					m_log.info(infoMsg);
					{
						// All this just so we can create an error response
						cacert = m_caCertCache.findLatestBySubjectDN(m_defaultResponderId);
					}
					throw new MalformedRequestException(infoMsg);
				}

				if (m_log.isDebugEnabled()) {
					m_log.debug("The OCSP request contains " + requests.length + " simpleRequests.");
				}

				// Add standard response extensions
				Hashtable responseExtensions = OCSPUtil.getStandardResponseExtensions(req);
            	transactionLogger.paramPut(ITransactionLogger.STATUS, OCSPRespGenerator.SUCCESSFUL);
            	auditLogger.paramPut(IAuditLogger.STATUS, OCSPRespGenerator.SUCCESSFUL);
				// Look over the status requests
				ArrayList responseList = new ArrayList();
				for (int i = 0; i < requests.length; i++) {
					CertificateID certId = requests[i].getCertID();
					// now some Logging
					transactionLogger.paramPut(ITransactionLogger.SERIAL_NOHEX, certId.getSerialNumber().toByteArray());
					transactionLogger.paramPut(ITransactionLogger.DIGEST_ALGOR, certId.getHashAlgOID()); //todo, find text version of this or find out if it should be something else                    
					transactionLogger.paramPut(ITransactionLogger.ISSUER_NAME_HASH, certId.getIssuerNameHash());
					transactionLogger.paramPut(ITransactionLogger.ISSUER_KEY, certId.getIssuerKeyHash());
					auditLogger.paramPut(IAuditLogger.ISSUER_KEY, certId.getIssuerKeyHash());
					auditLogger.paramPut(IAuditLogger.SERIAL_NOHEX, certId.getSerialNumber().toByteArray());
					auditLogger.paramPut(IAuditLogger.ISSUER_NAME_HASH, certId.getIssuerNameHash());
 					byte[] hashbytes = certId.getIssuerNameHash();
					String hash = null;
					if (hashbytes != null) {
						hash = new String(Hex.encode(hashbytes));                    	
					}
					String infoMsg = intres.getLocalizedMessage("ocsp.inforeceivedrequest", certId.getSerialNumber().toString(16), hash, request.getRemoteAddr());
					m_log.info(infoMsg);
					boolean unknownCA = false; 
					// if the certId was issued by an unknown CA
					// The algorithm here:
					// We will sign the response with the CA that issued the first 
					// certificate(certId) in the request. If the issuing CA is not available
					// on this server, we sign the response with the default responderId (from params in web.xml).
					// We have to look up the ca-certificate for each certId in the request though, as we will check
					// for revocation on the ca-cert as well when checking for revocation on the certId. 
					cacert = m_caCertCache.findByHash(certId);	// Get the issuer of certId
					if (cacert == null) {
						// We could not find certificate for this request so get certificate for default responder
						cacert = m_caCertCache.findLatestBySubjectDN(m_defaultResponderId);
						unknownCA = true;
					}
					if (cacert == null) {
						String errMsg = intres.getLocalizedMessage("ocsp.errorfindcacert", new String(Hex.encode(certId.getIssuerNameHash())), m_defaultResponderId);
						m_log.error(errMsg);
						continue;
					}
					if (unknownCA == true) {
						String errMsg = intres.getLocalizedMessage("ocsp.errorfindcacertusedefault", new String(Hex.encode(certId.getIssuerNameHash())));
						m_log.info(errMsg);
						// If we can not find the CA, answer UnknowStatus
						responseList.add(new OCSPResponseItem(certId, new UnknownStatus(), nextUpdate));
						transactionLogger.paramPut(ITransactionLogger.CERT_STATUS, OCSPUnidResponse.OCSP_UNKNOWN); 
						transactionLogger.writeln();
						continue;
					} else {
						transactionLogger.paramPut(ITransactionLogger.ISSUER_NAME_DN, cacert.getSubjectDN().getName());
					}
					/*
					 * Implement logic according to
					 * chapter 2.7 in RFC2560
					 * 
					 * 2.7  CA Key Compromise
					 *    If an OCSP responder knows that a particular CA's private key has
					 *    been compromised, it MAY return the revoked state for all
					 *    certificates issued by that CA.
					 */
					final org.bouncycastle.ocsp.CertificateStatus certStatus;
					transactionLogger.paramPut(ITransactionLogger.CERT_STATUS, OCSPUnidResponse.OCSP_GOOD); // it seems to be correct
                    // Check if the cacert (or the default responderid) is revoked
                    final CertificateStatus cacertStatus = getStatus(m_adm, CertTools.getIssuerDN(cacert), CertTools.getSerialNumber(cacert));
					if ( !cacertStatus.equals(CertificateStatus.REVOKED) ) {
						// Check if cert is revoked
						final CertificateStatus status = getStatus(m_adm, cacert.getSubjectDN().getName(), certId.getSerialNumber());
						// If we have different maxAge and untilNextUpdate for different certificate profiles, we have to fetch these
						// values now that we have fetched the certificate status, that includes certificate profile.
                        nextUpdate = OcspConfiguration.getUntilNextUpdate(status.certificateProfileId);
                        maxAge = OcspConfiguration.getMaxAge(status.certificateProfileId);
                        if (m_log.isDebugEnabled()) {
                        	m_log.debug("Set nextUpdate="+nextUpdate+", and maxAge="+maxAge+" for certificateProfileId="+status.certificateProfileId);
                        }

                        final String sStatus;
						if (status.equals(CertificateStatus.NOT_AVAILABLE)) {
							// No revocation info available for this cert, handle it
							if (m_log.isDebugEnabled()) {
								m_log.debug("Unable to find revocation information for certificate with serial '"
										+ certId.getSerialNumber().toString(16) + "'"
										+ " from issuer '" + cacert.getSubjectDN().getName() + "'");                                
							}
							// If we do not treat non existing certificates as good 
							// OR
							// we don't actually handle requests for the CA issuing the certificate asked about
							// then we return unknown
							if ( (!m_nonExistingIsGood) || (m_caCertCache.findByHash(certId) == null) ) {
								sStatus = "unknown";
								certStatus = new UnknownStatus();
								transactionLogger.paramPut(ITransactionLogger.CERT_STATUS, OCSPUnidResponse.OCSP_UNKNOWN);
							} else {
                                sStatus = "good";
                                certStatus = null; // null means "good" in OCSP
                                transactionLogger.paramPut(ITransactionLogger.CERT_STATUS, OCSPUnidResponse.OCSP_GOOD); 
                            }
						} else if ( status.equals(CertificateStatus.REVOKED) ) {
						    // Revocation info available for this cert, handle it
						    sStatus ="revoked";
						    certStatus = new RevokedStatus(new RevokedInfo(new DERGeneralizedTime(status.revocationDate),
						                                                   new CRLReason(status.revocationReason)));
						    transactionLogger.paramPut(ITransactionLogger.CERT_STATUS, OCSPUnidResponse.OCSP_REVOKED); //1 = revoked
						} else {
						    sStatus = "good";
						    certStatus = null;
						    transactionLogger.paramPut(ITransactionLogger.CERT_STATUS, OCSPUnidResponse.OCSP_GOOD); 
						}
                        infoMsg = intres.getLocalizedMessage("ocsp.infoaddedstatusinfo", sStatus, certId.getSerialNumber().toString(16), cacert.getSubjectDN().getName());
                        m_log.info(infoMsg);
                        responseList.add(new OCSPResponseItem(certId, certStatus, nextUpdate));
                        transactionLogger.writeln();
					} else {
						certStatus = new RevokedStatus(new RevokedInfo(new DERGeneralizedTime(cacertStatus.revocationDate),
								new CRLReason(cacertStatus.revocationReason)));
						infoMsg = intres.getLocalizedMessage("ocsp.infoaddedstatusinfo", "revoked", certId.getSerialNumber().toString(16), cacert.getSubjectDN().getName());
						m_log.info(infoMsg);
						responseList.add(new OCSPResponseItem(certId, certStatus, nextUpdate));
						transactionLogger.paramPut(ITransactionLogger.CERT_STATUS, OCSPUnidResponse.OCSP_REVOKED);
						transactionLogger.writeln();
					}
					// Look for extension OIDs
					Iterator iter = m_extensionOids.iterator();
					while (iter.hasNext()) {
						String oidstr = (String)iter.next();
						DERObjectIdentifier oid = new DERObjectIdentifier(oidstr);
						X509Extensions reqexts = req.getRequestExtensions();
						if (reqexts != null) {
							X509Extension ext = reqexts.getExtension(oid);
							if (null != ext) {
								// We found an extension, call the extenstion class
								if (m_log.isDebugEnabled()) {
									m_log.debug("Found OCSP extension oid: "+oidstr);
								}
								IOCSPExtension extObj = (IOCSPExtension)m_extensionMap.get(oidstr);
								if (extObj != null) {
									// Find the certificate from the certId
									X509Certificate cert = null;
									cert = (X509Certificate)findCertificateByIssuerAndSerno(m_adm, cacert.getSubjectDN().getName(), certId.getSerialNumber());
									if (cert != null) {
										// Call the OCSP extension
										Hashtable retext = extObj.process(request, cert, certStatus);
										if (retext != null) {
											// Add the returned X509Extensions to the responseExtension we will add to the basic OCSP response
											responseExtensions.putAll(retext);
										} else {
											String errMsg = intres.getLocalizedMessage("ocsp.errorprocessextension", extObj.getClass().getName(),  new Integer(extObj.getLastErrorCode()));
											m_log.error(errMsg);
										}
									}
								}
							}
						}
					}
				} // end of huge for loop
				if (cacert != null) {
					// Add responseExtensions
					X509Extensions exts = new X509Extensions(responseExtensions);
					// generate the signed response object
					BasicOCSPResp basicresp = signOCSPResponse(req, responseList, exts, cacert);
					ocspresp = res.generate(OCSPRespGenerator.SUCCESSFUL, basicresp);
					auditLogger.paramPut(IAuditLogger.STATUS, OCSPRespGenerator.SUCCESSFUL);
					transactionLogger.paramPut(ITransactionLogger.STATUS, OCSPRespGenerator.SUCCESSFUL);
				} else {
					// Only unknown CAs in requests and no default reponders cert 
					String errMsg = intres.getLocalizedMessage("ocsp.errornocacreateresp");
					m_log.error(errMsg);
					throw new ServletException(errMsg);
				}
			} catch (MalformedRequestException e) {
				String errMsg = intres.getLocalizedMessage("ocsp.errorprocessreq", e.getMessage());
				m_log.info(errMsg);
				if (m_log.isDebugEnabled()) {
					m_log.debug(errMsg, e);
				}
				ocspresp = res.generate(OCSPRespGenerator.MALFORMED_REQUEST, null);	// RFC 2560: responseBytes are not set on error.
				transactionLogger.paramPut(ITransactionLogger.STATUS, OCSPRespGenerator.MALFORMED_REQUEST);
				transactionLogger.writeln();
				auditLogger.paramPut(IAuditLogger.STATUS, OCSPRespGenerator.MALFORMED_REQUEST);
			} catch (SignRequestException e) {
				String errMsg = intres.getLocalizedMessage("ocsp.errorprocessreq", e.getMessage());
				m_log.info(errMsg); // No need to log the full exception here
				ocspresp = res.generate(OCSPRespGenerator.SIG_REQUIRED, null);	// RFC 2560: responseBytes are not set on error.
				transactionLogger.paramPut(ITransactionLogger.STATUS, OCSPRespGenerator.SIG_REQUIRED);
				transactionLogger.writeln();
				auditLogger.paramPut(IAuditLogger.STATUS, OCSPRespGenerator.SIG_REQUIRED);
			} catch (SignRequestSignatureException e) {
				String errMsg = intres.getLocalizedMessage("ocsp.errorprocessreq", e.getMessage());
				m_log.info(errMsg); // No need to log the full exception here
				ocspresp = res.generate(OCSPRespGenerator.UNAUTHORIZED, null);	// RFC 2560: responseBytes are not set on error.
				transactionLogger.paramPut(ITransactionLogger.STATUS, OCSPRespGenerator.UNAUTHORIZED);
				transactionLogger.writeln();
				auditLogger.paramPut(IAuditLogger.STATUS, OCSPRespGenerator.UNAUTHORIZED);
			} catch (InvalidKeyException e) {
				String errMsg = intres.getLocalizedMessage("ocsp.errorprocessreq", e.getMessage());
				m_log.info(errMsg, e);
				ocspresp = res.generate(OCSPRespGenerator.UNAUTHORIZED, null);	// RFC 2560: responseBytes are not set on error.
				transactionLogger.paramPut(ITransactionLogger.STATUS, OCSPRespGenerator.UNAUTHORIZED);
				transactionLogger.writeln();
				auditLogger.paramPut(IAuditLogger.STATUS, OCSPRespGenerator.UNAUTHORIZED);
			} catch (Throwable e) {
				String errMsg = intres.getLocalizedMessage("ocsp.errorprocessreq", e.getMessage());
				m_log.error(errMsg, e);
				ocspresp = res.generate(OCSPRespGenerator.INTERNAL_ERROR, null);	// RFC 2560: responseBytes are not set on error.
				transactionLogger.paramPut(ITransactionLogger.STATUS, OCSPRespGenerator.INTERNAL_ERROR);
				transactionLogger.writeln();
				auditLogger.paramPut(IAuditLogger.STATUS, OCSPRespGenerator.INTERNAL_ERROR);
			}
			byte[] respBytes = ocspresp.getEncoded();
			auditLogger.paramPut(IAuditLogger.OCSPRESPONSE, new String (Hex.encode(respBytes)));
            auditLogger.writeln();
			auditLogger.flush();
			transactionLogger.flush();
			if (mDoSaferLogging){
				// See if the Errorhandler has found any problems
				if (hasErrorHandlerFailedSince(startTime)) {
					m_log.info("ProbableErrorhandler reported error, cannot answer request");
					ocspresp = res.generate(OCSPRespGenerator.INTERNAL_ERROR, null);	// RFC 2560: responseBytes are not set on error.
					respBytes = ocspresp.getEncoded();
				}
				// See if the Appender has reported any problems
				if (!canlog) {
					m_log.info("SaferDailyRollingFileAppender reported error, cannot answer request");
					ocspresp = res.generate(OCSPRespGenerator.INTERNAL_ERROR, null);	// RFC 2560: responseBytes are not set on error.
					respBytes = ocspresp.getEncoded();
				}
			}
			response.setContentType("application/ocsp-response");
			//response.setHeader("Content-transfer-encoding", "binary");
			response.setContentLength(respBytes.length);
			addRfc5019CacheHeaders(request, response, ocspresp, maxAge);
			response.getOutputStream().write(respBytes);
			response.getOutputStream().flush();
		} catch (OCSPException e) {
			String errMsg = intres.getLocalizedMessage("ocsp.errorprocessreq", e.getMessage());
			m_log.error(errMsg, e);
			throw new ServletException(e);
		} catch (Exception e ) {
			m_log.error("", e);
			transactionLogger.flush();
			auditLogger.flush();
		}
		if (m_log.isTraceEnabled()) {
			m_log.trace("<service()");
		}
	}

	/**
	 * RFC 2560 does not specify how cache headers should be used, but RFC 5019 does. Therefore we will only
	 * add the headers if the requirements of RFC 5019 is fulfilled: A GET-request, a single embedded reponse,
	 * the response contains a nextUpdate and no nonce is present.
	 * @param maxAge is the margin to Expire when using max-age in milliseconds 
	 */
	private void addRfc5019CacheHeaders(HttpServletRequest request, HttpServletResponse response, OCSPResp ocspresp, long maxAge) throws IOException, NoSuchAlgorithmException, NoSuchProviderException, OCSPException {
		if (maxAge <= 0) {
			m_log.debug("Will not add RFC 5019 caches headers: RFC 5019 6.2: max-age should be 'later than thisUpdate but earlier than nextUpdate'.");
			return;
		}
		if (!"GET".equalsIgnoreCase(request.getMethod())) {
			m_log.debug("Will not add RFC 5019 caches headers: \"clients MUST use the GET method (to enable OCSP response caching)\"");
			return;
		}
		if (ocspresp.getResponseObject() == null) {
			m_log.debug("Will not add cache headers for response to bad request.");
			return;
		}
		SingleResp[] singleRespones = ((BasicOCSPResp) ocspresp.getResponseObject()).getResponses();
		if (singleRespones.length != 1) {
			m_log.debug("Will not add RFC 5019 caches headers: reponse contains multiple embedded responses.");
			return;
		}
		if (singleRespones[0].getNextUpdate() == null) {
			m_log.debug("Will not add RFC 5019 caches headers: nextUpdate isn't set.");
			return;
		}
		if (singleRespones[0].getSingleExtensions() != null && singleRespones[0].getSingleExtensions().getExtension(OCSPObjectIdentifiers.id_pkix_ocsp_nonce) == null) {
			m_log.debug("Will not add RFC 5019 caches headers: response contains a nonce.");
			return;
		}
		long now = new Date().getTime();
		//long producedAt = ((BasicOCSPResp) ocspresp.getResponseObject()).getProducedAt().getTime();
		long nextUpdate = singleRespones[0].getNextUpdate().getTime();
		long thisUpdate = singleRespones[0].getThisUpdate().getTime();
		if (maxAge >= (nextUpdate - thisUpdate)) {
			maxAge = nextUpdate - thisUpdate - 1;
			m_log.warn(intres.getLocalizedMessage("ocsp.shrinkmaxage", maxAge));
		}
		// RFC 5019 6.2: Date: The date and time at which the OCSP server generated the HTTP response.
		// On JBoss AS the "Date"-header is cached for 1 second, so this value will be overwritten and off by up to a second 
		response.setDateHeader("Date", now);
		// RFC 5019 6.2: Last-Modified: date and time at which the OCSP responder last modified the response. == thisUpdate
		response.setDateHeader("Last-Modified", thisUpdate);
		// RFC 5019 6.2: Expires: This date and time will be the same as the nextUpdate timestamp in the OCSP response itself.
		response.setDateHeader("Expires", nextUpdate);	// This is overridden by max-age on HTTP/1.1 compatible components
		// RFC 5019 6.2: This profile RECOMMENDS that the ETag value be the ASCII HEX representation of the SHA1 hash of the OCSPResponse structure.
		response.setHeader("ETag", "\"" + new String(Hex.encode(MessageDigest.getInstance("SHA-1", "BC").digest(ocspresp.getEncoded()))) + "\"");
		response.setHeader("Cache-Control", "max-age=" + (maxAge/1000) + ",public,no-transform,must-revalidate");
	}
} // OCSPServlet
