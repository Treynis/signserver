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

package org.ejbca.core.ejb.ca.store;

import java.math.BigInteger;
import java.security.cert.Certificate;
import java.security.cert.X509Certificate;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

import javax.ejb.CreateException;
import javax.ejb.EJBException;
import javax.ejb.FinderException;

import org.apache.log4j.Logger;
import org.ejbca.core.ejb.JNDINames;
import org.ejbca.core.ejb.protect.TableProtectSessionLocal;
import org.ejbca.core.ejb.protect.TableProtectSessionLocalHome;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.ca.store.CertificateInfo;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.log.LogConstants;
import org.ejbca.core.model.protect.TableVerifyResult;
import org.ejbca.util.CertTools;
import org.ejbca.util.JDBCUtil;
import org.ejbca.util.StringTools;

/** Common code between CertificateStoreSessionBean and CertificateStoreOnlyDataSessionBean
 * 
 * @author lars
 * @version $Id: CertificateDataUtil.java 7454 2009-05-09 14:57:43Z anatom $
 *
 */
public class CertificateDataUtil {
    /** Internal localization of logs and errors */
    private static final InternalResources intres = InternalResources.getInstance();
    
    public interface Adapter {
        void debug( String s );
        void error( String s );
        void error( String s, Exception e );
        Logger getLogger();
        void log(Admin admin, int caid, int module, Date time, String username,
                 X509Certificate certificate, int event, String comment);
    }
    public static Certificate findCertificateByFingerprint(Admin admin, String fingerprint,
                                                           CertificateDataLocalHome certHome,
                                                           Adapter adapter) {
        adapter.getLogger().trace(">findCertificateByFingerprint()");
        Certificate ret = null;

        try {
            CertificateDataLocal res = certHome.findByPrimaryKey(new CertificateDataPK(fingerprint));
            ret = res.getCertificate();
            adapter.getLogger().trace("<findCertificateByFingerprint()");
        } catch (FinderException fe) {
            // Return null;
        } catch (Exception e) {
            adapter.getLogger().error("Error finding certificate with fp: " + fingerprint);
            throw new EJBException(e);
        }
        return ret;
    } // findCertificateByFingerprint

    public static Certificate findCertificateByIssuerAndSerno(Admin admin, String issuerDN, BigInteger serno, CertificateDataLocalHome certHome, Adapter adapter) {
        if (adapter.getLogger().isTraceEnabled()) {
        	adapter.getLogger().trace(">findCertificateByIssuerAndSerno(), dn:" + issuerDN + ", serno=" + serno.toString(16));
        }
        // First make a DN in our well-known format
        String dn = CertTools.stringToBCDNString(issuerDN);
        dn = StringTools.strip(dn);
        if (adapter.getLogger().isDebugEnabled()) {
        	adapter.debug("Looking for cert with (transformed)DN: " + dn);
        }
        try {
            Collection coll = certHome.findByIssuerDNSerialNumber(dn, serno.toString());
            Certificate ret = null;
            if (coll != null) {
                if (coll.size() > 1) {
                	String msg = intres.getLocalizedMessage("store.errorseveralissuerserno", issuerDN, serno.toString(16));            	
                    adapter.log(admin, issuerDN.hashCode(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_INFO_DATABASE, msg);	
                }
                Iterator iter = coll.iterator();
                Certificate cert = null;
                // There are several certs, we will try to find the latest issued one
                if (iter.hasNext()) {
                    cert = ((CertificateDataLocal) iter.next()).getCertificate();
                    if (ret != null) {
                    	if (CertTools.getNotBefore(cert).after(CertTools.getNotBefore(ret))) {
                    		// cert is never than ret
                    		ret = cert;
                    	}
                    } else {
                    	ret = cert;
                    }
                }
            }
            if (adapter.getLogger().isTraceEnabled()) {
            	adapter.getLogger().trace("<findCertificateByIssuerAndSerno(), dn:" + issuerDN + ", serno=" + serno.toString(16));
            }
            return ret;
        } catch (Exception fe) {
            throw new EJBException(fe);
        }
    } //findCertificateByIssuerAndSerno

    public static Collection findCertificatesByType(Admin admin, int type, String issuerDN,
                                                    CertificateDataLocalHome certHome,
                                                    Adapter adapter) {
        adapter.getLogger().trace(">findCertificatesByType()");
        if (null == admin
                || type <= 0
                || type > CertificateDataBean.CERTTYPE_SUBCA + CertificateDataBean.CERTTYPE_ENDENTITY + CertificateDataBean.CERTTYPE_ROOTCA) {
            throw new IllegalArgumentException();
        }
        StringBuffer ctypes = new StringBuffer();
        if ((type & CertificateDataBean.CERTTYPE_SUBCA) > 0) {
            ctypes.append(CertificateDataBean.CERTTYPE_SUBCA);
        }
        if ((type & CertificateDataBean.CERTTYPE_ENDENTITY) > 0) {
            if (ctypes.length() > 0) {
                ctypes.append(", ");
            }
            ctypes.append(CertificateDataBean.CERTTYPE_ENDENTITY);
        }
        if ((type & CertificateDataBean.CERTTYPE_ROOTCA) > 0) {
            if (ctypes.length() > 0) {
                ctypes.append(", ");
            }
            ctypes.append(CertificateDataBean.CERTTYPE_ROOTCA);
        }

        Connection con = null;
        PreparedStatement ps = null;
        ResultSet result = null;
        try {
            ArrayList vect;
            // Status 20 = CertificateDataBean.CERT_ACTIVE, 21 = CertificateDataBean.CERT_NOTIFIEDABOUTEXPIRATION 
            StringBuffer stmt = new StringBuffer("SELECT DISTINCT fingerprint FROM CertificateData WHERE (status="+CertificateDataBean.CERT_ACTIVE+" or status="+CertificateDataBean.CERT_NOTIFIEDABOUTEXPIRATION+") AND ");
            stmt.append(" type IN (");
            stmt.append(ctypes.toString());
            stmt.append(')');
            if (null != issuerDN && issuerDN.length() > 0) {
                String dn = CertTools.stringToBCDNString(issuerDN);
                dn = StringTools.strip(dn);
                if (adapter.getLogger().isDebugEnabled()) {
                    adapter.debug("findCertificatesByType() : Looking for cert with (transformed)DN: " + dn);
                }
                stmt.append(" AND issuerDN = '");
                stmt.append(dn);
                stmt.append('\'');
            }
            if (adapter.getLogger().isDebugEnabled()) {
                adapter.debug("findCertificatesByType() : executing SQL statement\n"
                        + stmt.toString());
            }
            con = JDBCUtil.getDBConnection(JNDINames.DATASOURCE);
            ps = con.prepareStatement(stmt.toString());
            result = ps.executeQuery();

            vect = new ArrayList();
            while (result.next()) {
                Certificate cert = findCertificateByFingerprint(admin, result.getString(1),
                                                                certHome, adapter);
                if (cert != null) {
                    vect.add(cert);
                }
            }

            adapter.getLogger().trace("<findCertificatesByType()");
            return vect;
        } catch (Exception e) {
            throw new EJBException(e);
        } finally {
            JDBCUtil.close(con, ps, result);
        }
    } // findCertificatesByType

    static public CertificateStatus getStatus(Admin admin, String issuerDN, BigInteger serno,
                                              CertificateDataLocalHome certHome, TableProtectSessionLocalHome protectHome, Adapter adapter) {
        if (adapter.getLogger().isTraceEnabled()) {
            adapter.getLogger().trace(">getStatus(), dn:" + issuerDN + ", serno=" + serno.toString(16));
        }
        // First make a DN in our well-known format
        final String dn = CertTools.stringToBCDNString(issuerDN);

        try {
            Collection coll = certHome.findByIssuerDNSerialNumber(dn, serno.toString());
            if (coll != null) {
                if (coll.size() > 1) {
                	String msg = intres.getLocalizedMessage("store.errorseveralissuerserno", issuerDN, serno.toString(16));            	
                    //adapter.log(admin, issuerDN.hashCode(), LogConstants.MODULE_CA, new java.util.Date(), null, null, LogConstants.EVENT_ERROR_DATABASE, msg);
                	adapter.error(msg);
                }
                Iterator iter = coll.iterator();
                if (iter.hasNext()) {
                	final CertificateDataLocal data = (CertificateDataLocal) iter.next();
                	if (protectHome != null) {
                		verifyProtection(data, protectHome, adapter);
                	}
                    final CertificateStatus result = CertificateStatus.getIt(data);
                	if (adapter.getLogger().isTraceEnabled()) {
                		adapter.getLogger().trace("<getStatus() returned " + result + " for cert number "+serno);
                	}
                	return result;
                }
            }
            if (adapter.getLogger().isTraceEnabled()) {
            	adapter.getLogger().trace("<getStatus() did not find certificate with dn "+dn+" and serno "+serno.toString(16));
            }
        } catch (Exception e) {
            throw new EJBException(e);
        }
        return CertificateStatus.NOT_AVAILABLE;
    } //isRevoked
    

    static public void verifyProtection(Admin admin, String issuerDN, BigInteger serno,
    		CertificateDataLocalHome certHome, TableProtectSessionLocalHome protectHome, Adapter adapter) {
    	if (adapter.getLogger().isTraceEnabled()) {
    		adapter.getLogger().trace(">verifyProtection, dn:" + issuerDN + ", serno=" + serno.toString(16));
    	}
		try {
			if (protectHome != null) {
				// First make a DN in our well-known format
				Collection coll = certHome.findByIssuerDNSerialNumber(CertTools.stringToBCDNString(issuerDN), serno.toString());
				if (coll != null) {
					if (coll.size() > 1) {
						String msg = intres.getLocalizedMessage("store.errorseveralissuerserno", issuerDN, serno.toString(16));            	
						adapter.error(msg);
					}
					Iterator iter = coll.iterator();
					if (iter.hasNext()) {
						CertificateDataLocal data = (CertificateDataLocal) iter.next();
						verifyProtection(data, protectHome, adapter);
					}
				}
			}
		} catch (FinderException e) {
			throw new EJBException(e);	// This should exist here
		}
    }


    static void verifyProtection(CertificateDataLocal data, TableProtectSessionLocalHome protectHome, Adapter adapter) {
		CertificateInfo entry = new CertificateInfo(data.getFingerprint(), data.getCaFingerprint(), data.getSerialNumber(), data.getIssuerDN(), data.getSubjectDN(), data.getStatus(), data.getType(), data.getExpireDate(), data.getRevocationDate(), data.getRevocationReason(), data.getUsername(), data.getTag(), data.getCertificateProfileId(), data.getUpdateTime());
		TableProtectSessionLocal protect;
		try {
			protect = protectHome.create();
			// The verify method will log failed verifies itself
			TableVerifyResult res = protect.verify(entry);
			if (res.getResultCode() != TableVerifyResult.VERIFY_SUCCESS) {
				//adapter.error("Verify failed, but we go on anyway.");
			}
		} catch (CreateException e) {
        	String msg = intres.getLocalizedMessage("protect.errorcreatesession");            	
			adapter.error(msg, e);
		}
    }
}
