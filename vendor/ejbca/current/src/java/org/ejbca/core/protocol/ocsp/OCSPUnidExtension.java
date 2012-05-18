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

package org.ejbca.core.protocol.ocsp;

import java.io.File;
import java.io.IOException;
import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.HashSet;
import java.util.Hashtable;
import java.util.Set;

import javax.ejb.EJBException;
import javax.servlet.ServletConfig;
import javax.servlet.http.HttpServletRequest;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.x509.X509Extension;
import org.bouncycastle.ocsp.CertificateStatus;
import org.ejbca.config.OcspConfiguration;
import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.model.InternalResources;
import org.ejbca.util.CertTools;
import org.ejbca.util.FileTools;
import org.ejbca.util.JDBCUtil;

/** ASN.1 OCSP extension used to map a UNID to a Fnr, OID for this extension is 2.16.578.1.16.3.2
 * 
 * @author tomas
 * @version $Id: OCSPUnidExtension.java 8039 2009-09-29 16:49:54Z primelars $
 *
 */
public class OCSPUnidExtension implements IOCSPExtension {

	private static final Logger m_log = Logger.getLogger(OCSPUnidExtension.class);
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();

	/** Constants capturing the possible error returned by the Unid-Fnr OCSP Extension 
	 * 
	 */
	public static final int ERROR_NO_ERROR = 0;
	public static final int ERROR_UNKNOWN = 1;
	public static final int ERROR_UNAUTHORIZED = 2;
	public static final int ERROR_NO_FNR_MAPPING = 3;
	public static final int ERROR_NO_SERIAL_IN_DN = 4;
	public static final int ERROR_SERVICE_UNAVAILABLE = 5;
    public static final int ERROR_CERT_REVOKED = 6;
    
    private String dataSourceJndi;
    private Set<BigInteger> trustedCerts = new HashSet<BigInteger>();
    private Certificate cacert = null;
    private int errCode = OCSPUnidExtension.ERROR_NO_ERROR;
    
	/** Called after construction
	 * 
	 * @param config ServletConfig that can be used to read init-params from web-xml
	 */
	public void init(ServletConfig config) {
		// DataSource
		dataSourceJndi = OcspConfiguration.getUnidDataSource();
        if (StringUtils.isEmpty(dataSourceJndi)) {
    		String errMsg = intres.getLocalizedMessage("ocsp.errornoinitparam", "unidDataSource");
            m_log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        String trustDir = OcspConfiguration.getUnidTrustDir();
        if (StringUtils.isEmpty(trustDir)) {
    		String errMsg = intres.getLocalizedMessage("ocsp.errornoinitparam", "unidTrustDir");
            m_log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        // read all files from trustDir, expect that they are PEM formatted certificates
        CertTools.installBCProvider();
        File dir = new File(trustDir);
        try {
            if (dir == null || dir.isDirectory() == false) {
                m_log.error(dir.getCanonicalPath()+ " is not a directory.");
                throw new IllegalArgumentException(dir.getCanonicalPath()+ " is not a directory.");                
            }
            File files[] = dir.listFiles();
            if (files == null || files.length == 0) {
        		String errMsg = intres.getLocalizedMessage("ocsp.errornotrustfiles", dir.getCanonicalPath());
                m_log.error(errMsg);                
            }
            for ( int i=0; i<files.length; i++ ) {
                final String fileName = files[i].getCanonicalPath();
                // Read the file, don't stop completely if one file has errors in it
                try {
                    final byte bFromFile[] = FileTools.readFiletoBuffer(fileName);
                    byte[] bytes;
                    try {
                        bytes = FileTools.getBytesFromPEM(bFromFile, CertTools.BEGIN_CERTIFICATE, CertTools.END_CERTIFICATE);
                    } catch( Throwable t ) {
                        bytes = bFromFile; // assume binary data (.der)
                    }
                    final X509Certificate  cert = (X509Certificate) CertTools.getCertfromByteArray(bytes);
                    this.trustedCerts.add(cert.getSerialNumber());
                } catch (CertificateException e) {
            		String errMsg = intres.getLocalizedMessage("ocsp.errorreadingfile", fileName, "trustDir", e.getMessage());
                    m_log.error(errMsg, e);
                } catch (IOException e) {
            		String errMsg = intres.getLocalizedMessage("ocsp.errorreadingfile", fileName, "trustDir", e.getMessage());
                    m_log.error(errMsg, e);
                }
            }
        } catch (IOException e) {
    		String errMsg = intres.getLocalizedMessage("ocsp.errorreadingtrustfiles", e.getMessage());
            m_log.error(errMsg, e);
            throw new IllegalArgumentException(errMsg);
        }
        String cacertfile = OcspConfiguration.getUnidCaCert();
        if (StringUtils.isEmpty(cacertfile)) {
    		String errMsg = intres.getLocalizedMessage("ocsp.errornoinitparam", "unidCACert");
            m_log.error(errMsg);
            throw new IllegalArgumentException(errMsg);
        }
        try {
            byte[] bytes = FileTools.getBytesFromPEM(FileTools
                    .readFiletoBuffer(cacertfile),
                    CertTools.BEGIN_CERTIFICATE, CertTools.END_CERTIFICATE);
            cacert = CertTools.getCertfromByteArray(bytes);
        } catch (Exception e) {
    		String errMsg = intres.getLocalizedMessage("ocsp.errorreadingfile", "file", "cacertfile", e.getMessage());
            m_log.error(errMsg, e);
            throw new IllegalArgumentException(errMsg);
        }

	}
	
	/** Called by OCSP responder when the configured extension is found in the request.
	 * 
	 * @param request HttpServletRequest that can be used to find out information about caller, TLS certificate etc.
	 * @param cert X509Certificate the caller asked for in the OCSP request
     * @param status CertificateStatus the status the certificate has according to the OCSP responder, null means the cert is good
	 * @return X509Extension that will be added to responseExtensions by OCSP responder, or null if an error occurs
	 */
	public Hashtable process(HttpServletRequest request, X509Certificate cert, CertificateStatus status) {
        if (m_log.isTraceEnabled()) {
            m_log.trace(">process()");            
        }
        // Check authorization first
        if (!checkAuthorization(request)) {
        	errCode = OCSPUnidExtension.ERROR_UNAUTHORIZED;
        	return null;
        }
        // If the certificate is revoked, we must not return an FNR
        if (status != null) {
            errCode = OCSPUnidExtension.ERROR_CERT_REVOKED;
            return null;
        }
		Connection con = null;
		PreparedStatement ps = null;
		ResultSet result = null;
    	String fnr = null;
        String sn = null;
        try {
        	// The Unis is in the DN component serialNumber
        	sn = CertTools.getPartFromDN(cert.getSubjectDN().getName(), "SN");
        	if (sn != null) {
                if (m_log.isDebugEnabled()) {
                    m_log.debug("Found serialNumber: "+sn);                    
                }
				String iMsg = intres.getLocalizedMessage("ocsp.receivedunidreq", request.getRemoteAddr(), request.getRemoteHost(), sn);
                m_log.info(iMsg);
        		try {
        			con = ServiceLocator.getInstance().getDataSource(dataSourceJndi).getConnection();
        		} catch (SQLException e) {
    				String errMsg = intres.getLocalizedMessage("ocsp.errordatabaseunid");
        			m_log.error(errMsg, e);
        			errCode = OCSPUnidExtension.ERROR_SERVICE_UNAVAILABLE;
        			return null;
        		}
                ps = con.prepareStatement("select fnr from UnidFnrMapping where unid=?");
                ps.setString(1, sn);
                result = ps.executeQuery();
                if (result.next()) {
                    fnr = result.getString(1);
                }
        	} else {
				String errMsg = intres.getLocalizedMessage("ocsp.errorunidnosnindn", cert.getSubjectDN().getName());
        		m_log.error(errMsg);
        		errCode = OCSPUnidExtension.ERROR_NO_SERIAL_IN_DN;
        		return null;
        	}
            m_log.trace("<process()");
        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, result);
        }
        
        // Construct the response extentsion if we found a mapping
        if (fnr == null) {
			String errMsg = intres.getLocalizedMessage("ocsp.errorunidnosnmapping", sn);
            m_log.error(errMsg);
        	errCode = OCSPUnidExtension.ERROR_NO_FNR_MAPPING;
        	return null;
        	
        }
		String errMsg = intres.getLocalizedMessage("ocsp.returnedunidresponse", request.getRemoteAddr(), request.getRemoteHost(), fnr, sn);
        m_log.info(errMsg);
        FnrFromUnidExtension ext = new FnrFromUnidExtension(fnr);
        Hashtable ret = new Hashtable();
        ret.put(FnrFromUnidExtension.FnrFromUnidOid, new X509Extension(false, new DEROctetString(ext)));
		return ret;
	}
	
	/** Returns the last error that occured during process(), when process returns null
	 * 
	 * @return error code as defined by implementing class
	 */
	public int getLastErrorCode() {
		return errCode;
	}
	
	// 
	// Private methods
	//
	boolean checkAuthorization(HttpServletRequest request) {
        X509Certificate[] certs = (X509Certificate[]) request.getAttribute("javax.servlet.request.X509Certificate");
        if (certs == null) {
    		String errMsg = intres.getLocalizedMessage("ocsp.errornoclientauth", request.getRemoteAddr(), request.getRemoteHost());
            m_log.error(errMsg);
            return false;
        }
        // The certificate of the entity is nr 0
        X509Certificate cert = certs[0];
        if (cert == null) {
    		String errMsg = intres.getLocalizedMessage("ocsp.errornoclientauth", request.getRemoteAddr(), request.getRemoteHost());
            m_log.error(errMsg);
            return false;
        }
        // Check if the certificate is authorised to access the Fnr
        if ( this.trustedCerts.contains(cert.getSerialNumber()) ) {
            // If we found in the hashmap the same key with issuer and serialnumber, we know we got it. 
            // Just verify it as well to be damn sure
            try {
                cert.verify(this.cacert.getPublicKey());
            } catch (Exception e) {
        		String errMsg = intres.getLocalizedMessage("ocsp.errorverifycert");
                m_log.error(errMsg, e);
                return false;
            }
            // If verify was successful we know if was good!
            return true;
        }
		String errMsg = intres.getLocalizedMessage("ocsp.erroruntrustedclientauth", request.getRemoteAddr(), request.getRemoteHost());
        m_log.error(errMsg);
		return false;
	}
}
