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
package org.ejbca.core.protocol.ws.objects;

import java.util.Date;

import org.ejbca.core.model.ca.crl.RevokedCertInfo;

/**
 * Class used when checking the revokation status of a certificate.
 * 
 * Contains the following data:
 *   IssuerDN
 *   CertificateSN (hex)
 *   RevokationDate 
 *   Reason (One of the REVOKATION_REASON constants)
 *
 * @author Philip Vendil
 * @version $Id: RevokeStatus.java 5585 2008-05-01 20:55:00Z anatom $
 */

public class RevokeStatus {
	
    /** Constants defining different revokation reasons. */
    public static final int NOT_REVOKED                            = RevokedCertInfo.NOT_REVOKED;
    public static final int REVOKATION_REASON_UNSPECIFIED          = RevokedCertInfo.REVOKATION_REASON_UNSPECIFIED;
    public static final int REVOKATION_REASON_KEYCOMPROMISE        = RevokedCertInfo.REVOKATION_REASON_KEYCOMPROMISE;
    public static final int REVOKATION_REASON_CACOMPROMISE         = RevokedCertInfo.REVOKATION_REASON_CACOMPROMISE;
    public static final int REVOKATION_REASON_AFFILIATIONCHANGED   = RevokedCertInfo.REVOKATION_REASON_AFFILIATIONCHANGED;
    public static final int REVOKATION_REASON_SUPERSEDED           = RevokedCertInfo.REVOKATION_REASON_SUPERSEDED;
    public static final int REVOKATION_REASON_CESSATIONOFOPERATION = RevokedCertInfo.REVOKATION_REASON_CESSATIONOFOPERATION;
    public static final int REVOKATION_REASON_CERTIFICATEHOLD      = RevokedCertInfo.REVOKATION_REASON_CERTIFICATEHOLD;
    public static final int REVOKATION_REASON_REMOVEFROMCRL        = RevokedCertInfo.REVOKATION_REASON_REMOVEFROMCRL;
    public static final int REVOKATION_REASON_PRIVILEGESWITHDRAWN  = RevokedCertInfo.REVOKATION_REASON_PRIVILEGESWITHDRAWN;
    public static final int REVOKATION_REASON_AACOMPROMISE         = RevokedCertInfo.REVOKATION_REASON_AACOMPROMISE;
    
	private String      issuerDN;
    private String      certificateSN;
    private Date        revocationDate;
    private int         reason;
	
    
    /** Default Web Service Constuctor */
	public RevokeStatus(){}
	
	public RevokeStatus(RevokedCertInfo info, String issuerDN){
		certificateSN = info.getUserCertificate().toString(16);
		this.issuerDN = issuerDN;
		revocationDate = info.getRevocationDate();
		reason = info.getReason();		
	}

	/**
	 * @return Returns the reason.
	 */
	public int getReason() {
		return reason;
	}

	/**
	 * @param reason The reason to set.
	 */
	public void setReason(int reason) {
		this.reason = reason;
	}

	/**
	 * @return Returns the revocationDate.
	 */
	public Date getRevocationDate() {
		return revocationDate;
	}

	/**
	 * @param revocationDate The revocationDate to set.
	 */
	public void setRevocationDate(Date revocationDate) {
		this.revocationDate = revocationDate;
	}

	/**
	 * @return Returns the certificateSN in hex format.
	 */
	public String getCertificateSN() {
		return certificateSN;
	}

	/**
	 * @param certificateSN The certificateSN to set in hex format
	 */
	public void setCertificateSN(String certificateSN) {
		this.certificateSN = certificateSN;
	}

	/**
	 * @return Returns the issuerDN.
	 */
	public String getIssuerDN() {
		return issuerDN;
	}

	/**
	 * @param issuerDN The issuerDN to set.
	 */
	public void setIssuerDN(String issuerDN) {
		this.issuerDN = issuerDN;
	}
	


}
