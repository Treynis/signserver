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

import java.math.BigInteger;
import java.security.cert.Certificate;
import java.util.Properties;

import javax.ejb.EJBException;
import javax.servlet.ServletConfig;
import javax.servlet.ServletException;

import org.ejbca.core.ejb.ServiceLocator;
import org.ejbca.core.ejb.ca.store.CertificateStatus;
import org.ejbca.core.ejb.ca.store.ICertificateStoreOnlyDataSessionLocal;
import org.ejbca.core.ejb.ca.store.ICertificateStoreOnlyDataSessionLocalHome;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceNotActiveException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.ExtendedCAServiceRequestException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.IllegalExtendedCAServiceRequestException;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.OCSPCAServiceRequest;
import org.ejbca.core.model.ca.caadmin.extendedcaservices.OCSPCAServiceResponse;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.protocol.ocsp.CertificateCache;
import org.ejbca.core.protocol.ocsp.CertificateCacheStandalone;
import org.ejbca.ui.web.pub.cluster.ExtOCSPHealthCheck;

/** 
 * Servlet implementing server side of the Online Certificate Status Protocol (OCSP)
 * For a detailed description of OCSP refer to RFC2560.
 * 
 * @web.servlet name = "OCSP"
 *              display-name = "OCSPServletStandAlone"
 *              description="Answers OCSP requests"
 *              load-on-startup = "1"
 *
 * @web.servlet-mapping url-pattern = "/ocsp"
 * @web.servlet-mapping url-pattern = "/ocsp/*"
 *
 * @web.resource-ref
 *  name="${datasource.jndi-name-prefix}${datasource.jndi-name}"
 *  type="javax.sql.DataSource"
 *  auth="Container"
 *  
 * @web.ejb-local-ref
 *  name="ejb/CertificateStoreOnlyDataSessionLocal"
 *  type="Session"
 *  link="CertificateStoreOnlyDataSession"
 *  home="org.ejbca.core.ejb.ca.store.ICertificateStoreOnlyDataSessionLocalHome"
 *  local="org.ejbca.core.ejb.ca.store.ICertificateStoreOnlyDataSessionLocal"
 *
 * @author Lars Silven PrimeKey
 * @version  $Id: OCSPServletStandAlone.java 7645 2009-06-04 07:28:01Z anatom $
 */
public class OCSPServletStandAlone extends OCSPServletBase implements IHealtChecker {

    private static final long serialVersionUID = -7093480682721604160L;

    private ICertificateStoreOnlyDataSessionLocal m_certStore = null;
    private OCSPServletStandAloneSession session;

    public OCSPServletStandAlone() {
        super();
    }
    /* (non-Javadoc)
     * @see org.ejbca.ui.web.protocol.OCSPServletBase#init(javax.servlet.ServletConfig)
     */
    public void init(ServletConfig config) throws ServletException {
        super.init(config);
        this.session = new OCSPServletStandAloneSession(this);
        // session must be crated before health check could be done
        ExtOCSPHealthCheck.setHealtChecker(this);
    }
    
    /**
     * Returns the certificate data only session bean
     */
    synchronized ICertificateStoreOnlyDataSessionLocal getStoreSessionOnlyData(){
    	if(this.m_certStore == null){	
    		try {
                ServiceLocator locator = ServiceLocator.getInstance();
                ICertificateStoreOnlyDataSessionLocalHome castorehome =
                    (ICertificateStoreOnlyDataSessionLocalHome)locator.getLocalHome(ICertificateStoreOnlyDataSessionLocalHome.COMP_NAME);
                this.m_certStore = castorehome.create();
    		}catch(Exception e){
    			throw new EJBException(e);      	  	    	  	
    		}
    	}
    	return this.m_certStore;
    }

    /* (non-Javadoc)
     * @see org.ejbca.ui.web.protocol.IHealtChecker#healthCheck()
     */
    public String healthCheck() {
        return this.session.healthCheck();
    }
    /* (non-Javadoc)
     * @see org.ejbca.ui.web.protocol.OCSPServletBase#loadPrivateKeys(org.ejbca.core.model.log.Admin, java.lang.String)
     */
    void loadPrivateKeys(Admin adm, String password) throws Exception {
        this.session.loadPrivateKeys(adm, password);
    }
    /* (non-Javadoc)
     * @see org.ejbca.ui.web.protocol.OCSPServletBase#findCertificateByIssuerAndSerno(org.ejbca.core.model.log.Admin, java.lang.String, java.math.BigInteger)
     */
    Certificate findCertificateByIssuerAndSerno(Admin adm, String issuer, BigInteger serno) {
        return getStoreSessionOnlyData().findCertificateByIssuerAndSerno(adm, issuer, serno);
    }
    /* (non-Javadoc)
     * @see org.ejbca.ui.web.protocol.OCSPServletBase#extendedService(org.ejbca.core.model.log.Admin, int, org.ejbca.core.model.ca.caadmin.extendedcaservices.OCSPCAServiceRequest)
     */
    OCSPCAServiceResponse extendedService(Admin adm, int caid, OCSPCAServiceRequest request) throws ExtendedCAServiceRequestException,
                                                                                                    ExtendedCAServiceNotActiveException, IllegalExtendedCAServiceRequestException {
        return this.session.extendedService(caid, request);
    }
    /* (non-Javadoc)
     * @see org.ejbca.ui.web.protocol.OCSPServletBase#getStatus(org.ejbca.core.model.log.Admin, java.lang.String, java.math.BigInteger)
     */
    CertificateStatus getStatus(Admin adm, String name, BigInteger serialNumber) {
        return getStoreSessionOnlyData().getStatus(adm, name, serialNumber);
    }
    /* (non-Javadoc)
     * @see org.ejbca.ui.web.protocol.OCSPServletBase#createCertificateCache(java.util.Properties)
     */
    CertificateCache createCertificateCache(Properties prop) {
		return new CertificateCacheStandalone(prop);
	}
}
