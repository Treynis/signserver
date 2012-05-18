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
package org.ejbca.core.model.ca.store;

import java.io.Serializable;
import java.util.Date;

import org.ejbca.core.model.ra.UserDataVO;




/**
 * Value object class containing the data stored in the 
 * CertReqHistory Entity Bean. See constructor for details of its fields.
 * 
 * @author Philip Vendil
 * @version $Id: CertReqHistory.java 5585 2008-05-01 20:55:00Z anatom $
 * @see org.ejbca.core.ejb.ca.store.CertReqHistoryDataBean  
 */

public class CertReqHistory implements Serializable{
    private String fingerprint;
    private String serialNumber;
    private String issuerDN;
    private String username;
    private Date timestamp;
    private UserDataVO userDataVO;
    
    /**
     * @param fingerprint the PK of the certificate in the CertificateDataBean
     * @param serialNumber of the certificate 
     * @param issuerDN DN of the CA issuing the certificate
     * @param username of the user used in the certificate request.
     * @param timestamp when the certicate was created.
     * @param userDataVO the userdata used to create the certificate.
     */
    public CertReqHistory(String fingerprint, String serialNumber,
            String issuerDN, String username, Date timestamp,
            UserDataVO userDataVO) {
        super();
        this.fingerprint = fingerprint;
        this.serialNumber = serialNumber;
        this.issuerDN = issuerDN;
        this.username = username;
        this.timestamp = timestamp;
        this.userDataVO = userDataVO;
    }
    /**
     * @return Returns the issuerDN.
     */
    public String getFingerprint() {
        return fingerprint;
    }
    /**
     * @return Returns the issuerDN.
     */
    public String getIssuerDN() {
        return issuerDN;
    }
    /**
     * @return Returns the serialNumber.
     */
    public String getSerialNumber() {
        return serialNumber;
    }
    /**
     * @return Returns the timestamp.
     */
    public Date getTimestamp() {
        return timestamp;
    }
    /**
     * @return Returns the userAdminData.
     */
    public UserDataVO getUserDataVO() {
        return userDataVO;
    }
    /**
     * @return Returns the username.
     */
    public String getUsername() {
        return username;
    }
    

}
