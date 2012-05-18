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
 
package org.ejbca.core.model.ca.publisher;

import java.io.Serializable;
import java.security.cert.Certificate;

import org.ejbca.core.model.UpgradeableDataHashMap;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.ExtendedInformation;



/**
 * BasePublisher is a basic class that should be inherited by all types
 * of publishers in the system.
 *  
 *
 * @version $Id: BasePublisher.java 8422 2009-12-10 13:18:39Z primelars $
 */
public abstract class BasePublisher extends UpgradeableDataHashMap implements Serializable, Cloneable {
    // Default Values
    

    public static final String TRUE  = "true";
    public static final String FALSE = "false";

    // Protected Constants.
	public static final String TYPE                           = "type";
	
    protected static final String DESCRIPTION                    = "description";
    protected static final String ONLYUSEQUEUE                   = "onlyUseQueue";
    protected static final String KEEPPUBLISHEDINQUEUE           = "keepPublishedInQueue";
    protected static final String USEQUEUEFORCRLS                = "useQueueForCrls";
    protected static final String USEQUEUEFORCERTIFICATES        = "useQueueForCertificates";

    // Default values
    public static final boolean DEFAULT_ONLYUSEQUEUE 			 = false;
    
    // Public Methods

    /**
     * Creates a new instance of BasePublisher
     */
    public BasePublisher() {
      setDescription("");	       
      setOnlyUseQueue(DEFAULT_ONLYUSEQUEUE);
    }

    // Public Methods mostly used by PrimeCard
    /**
     * Returns the description of publisher
     */
    public String getDescription() { return (String) data.get(DESCRIPTION);}

	/**
	 * Sets the description. 
	 */
	public void setDescription(String description){ data.put(DESCRIPTION, description); }


    /**
     * @return If only the publisher queue should be used instead of publishing directly.
     */
    public boolean getOnlyUseQueue() { return Boolean.TRUE.equals(data.get(ONLYUSEQUEUE));}
    
    /**
     * Sets whether only the publisher queue should be used instead of publishing directly.
     * @param onlyUseQueue true if only the queue should be used.
     */
    public void setOnlyUseQueue(boolean onlyUseQueue) { data.put(ONLYUSEQUEUE, Boolean.valueOf(onlyUseQueue));}

    /**
     * @return true if successfully published items should remain in the queue (with a different status) 
     */
    public boolean getKeepPublishedInQueue() { return Boolean.TRUE.equals(data.get(KEEPPUBLISHEDINQUEUE));}
    
    /**
     * Sets whether a successfully published items should remain in the queue (with a different status)
     * @param keepPublishedInQueue
     */
    public void setKeepPublishedInQueue(boolean keepPublishedInQueue) { data.put(KEEPPUBLISHEDINQUEUE, Boolean.valueOf(keepPublishedInQueue));}

    /**
     * @return true if CRLs should be kept in in the queue if publishing fails 
     */
    public boolean getUseQueueForCRLs() {
    	boolean ret = true;
    	Object o = data.get(USEQUEUEFORCRLS);
    	if (o != null) {
        	ret = Boolean.TRUE.equals(o);    		
    	}
    	return ret;
    }

    /**
     * Sets whether a CRLs should be put in the publish queue if publish failed
     * @param useQueueForCRLs
     */
    public void setUseQueueForCRLs(boolean useQueueForCRLs) { data.put(USEQUEUEFORCRLS, Boolean.valueOf(useQueueForCRLs));}

    /**
     * @return true if Certificates should be kept in in the queue if publishing fails 
     */
    public boolean getUseQueueForCertificates() {
    	boolean ret = true;
    	Object o = data.get(USEQUEUEFORCERTIFICATES);
    	if (o != null) {
        	ret = Boolean.TRUE.equals(o);    		
    	}
    	return ret;
    }

    /**
     * Sets whether a certificate should be put in the publish queue if publish failed
     * @param useQueueForCertificates
     */
    public void setUseQueueForCertificates(boolean useQueueForCertificates) { data.put(USEQUEUEFORCERTIFICATES, Boolean.valueOf(useQueueForCertificates));}

    // Abstact methods.
    
    /**
     * Publishes a certificate to a certificate store. If status is not active for the certificate, the publisher may choose
     * to not publish the certificate, for instance if revoke removes a certificate from LDAP,
     * re-publishing the certificate should not add it again if the status is revoked.
     *
     * To revoke a certificate (already revoked by the CA) call with status=CertificateDataBean.CERT_ACTIVE, the Publisher decides what to do, if
     * anything.
     * 
     * @param incert The certificate to be stored.
     * @param chainfp Fingerprint (hex) of the CAs certificate.
     * @param username Username of end entity owning the certificate.
     * @param password Password given to the user, may be null if no password exists for the user.
     * @param userDN if an DN object is not found in the certificate use object from user data instead.
     * @param status Status of the certificate (from CertificateData).
     * @param type Type of certificate (from CertificateDataBean.CERTTYPE_ENDENTITY etc).
     * @param revocationDate Date for revocation (of revoked), like System.currentTimeMillis(), or -1 if not revoked.
     * @param revocationReason reason for revocation from RevokedCertInfo, RevokedCertInfo.NOT_REVOKED if not revoked.
     * @param extendedinformation contains extended information about the user, like picture, is null if no extendedinformation exists about the user.
     *
     * @return true if storage was successful.
     *
     * @throws PublisherException if a communication or other error occurs.
     */    
    public boolean storeCertificate(Admin admin, Certificate incert, String username, String password, String userDN, String cafp, int status, int type, long revocationDate, int revocationReason, String tag, int certificateProfileId, long lastUpdate, ExtendedInformation extendedinformation) throws PublisherException {
        return storeCertificate(admin, incert, username, password, cafp, status, type, revocationDate, revocationReason, tag, certificateProfileId, lastUpdate, extendedinformation);
    }
	
    /**
     * Used for backward compatibility, so that old working publishers do not have to be changed.
     * @deprecated since 3.9.3 will be removed in 4.0. Implement {@link #storeCertificate(Admin, Certificate, String, String, String, String, int, int, long, int, String, int, long, ExtendedInformation)} in your publisher.
     */
    protected abstract boolean storeCertificate(Admin admin, Certificate incert, String username, String password, String cafp, int status, int type, long revocationDate, int revocationReason, String tag, int certificateProfileId, long lastUpdate, ExtendedInformation extendedinformation) throws PublisherException;

    /**
     * Published a CRL to a CRL store.
     *
     * @param incrl The DER coded CRL to be stored.
     * @param chainfp Fingerprint (hex) of the CAs certificate.
     * @param number CRL number.
     * @param userDN if an DN object is not found in the certificate use object from user data instead.
     *
     * @return true if storage was successful.
     *
     * @throws PublisherException if a communication or other error occurs.
     */    
    public boolean storeCRL(Admin admin, byte[] incrl, String cafp, int number, String userDN) throws PublisherException {
    	return storeCRL(admin, incrl, cafp, number);
    }
    
    /**
     * Used for backward compatibility, so that old working publishers do not have to be changed.
     * @deprecated since 3.9.3 will be removed in 4.0. Implement {@link #storeCRL(Admin, byte[], String, int, String)} in your publisher.
     */
    protected abstract boolean storeCRL(Admin admin, byte[] incrl, String cafp, int number) throws PublisherException;
    
    /**
     * Method used to test the connection to a publisher.
     * 
     * @param admin the administrator performing the test
     * @throws PublisherConnectionException when couldn't be set up correctly in any way.
     */
    public abstract void testConnection(Admin admin) throws PublisherConnectionException;
    

    public abstract Object clone() throws CloneNotSupportedException;

    
    public abstract float getLatestVersion();

    
    public void upgrade(){
    	// Performing upgrade routines
    }
    
	

}
