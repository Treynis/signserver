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

import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.cert.CRLException;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509CRL;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.StringTokenizer;

import org.apache.commons.lang.StringUtils;
import org.apache.log4j.Logger;
import org.bouncycastle.asn1.x509.X509Extensions;
import org.ejbca.core.ejb.ca.store.CertificateDataBean;
import org.ejbca.core.model.InternalResources;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.model.ra.ExtendedInformation;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;
import org.ejbca.util.TCPTool;
import org.ejbca.util.dn.DNFieldExtractor;

import com.novell.ldap.LDAPAttribute;
import com.novell.ldap.LDAPAttributeSet;
import com.novell.ldap.LDAPConnection;
import com.novell.ldap.LDAPConstraints;
import com.novell.ldap.LDAPEntry;
import com.novell.ldap.LDAPException;
import com.novell.ldap.LDAPJSSESecureSocketFactory;
import com.novell.ldap.LDAPModification;
import com.novell.ldap.LDAPSearchConstraints;

/**
 * LdapPublisher is a class handling a publishing to various v3 LDAP catalogs.  
 *
 * @version $Id: LdapPublisher.java 8422 2009-12-10 13:18:39Z primelars $
 */
public class LdapPublisher extends BasePublisher {

	private static final Logger log = Logger.getLogger(LdapPublisher.class);
	/** Internal localization of logs and errors */
	private static final InternalResources intres = InternalResources.getInstance();

	public static final float LATEST_VERSION = 11;

	public static final int TYPE_LDAPPUBLISHER = 2;
	
	// Create some constraints used when connecting, disconnecting, reading and storing in LDAP servers
	/** Use a time limit for generic (non overridden) LDAP operations */
	protected LDAPConstraints ldapConnectionConstraints = new LDAPConstraints();
	/** Use a time limit for LDAP bind operations */
	protected LDAPConstraints ldapBindConstraints = new LDAPConstraints();
	/** Use a time limit for LDAP store operations */
	protected LDAPConstraints ldapStoreConstraints = new LDAPConstraints();
	/** Use a time limit for LDAP disconnect operations */
	protected LDAPConstraints ldapDisconnectConstraints = new LDAPConstraints();
	/** Use a time limit when reading from LDAP */
	protected LDAPSearchConstraints ldapSearchConstraints = new LDAPSearchConstraints();

	/** The normal ldap publisher will modify attributes in LDAP.
	 * If you don't want attributes modified, use the LdapSearchPublisher to 
	 * store certificates in already existing entries. Can be overridden in constructor
	 * of subclasses.
	 */
	protected boolean ADD_MODIFICATION_ATTRIBUTES = true;

	public static final String DEFAULT_USEROBJECTCLASS     = "top;person;organizationalPerson;inetOrgPerson";
	public static final String DEFAULT_CAOBJECTCLASS       = "top;applicationProcess;certificationAuthority-V2";
	public static final String DEFAULT_CACERTATTRIBUTE     = "cACertificate;binary";
	public static final String DEFAULT_USERCERTATTRIBUTE   = "userCertificate;binary";
	public static final String DEFAULT_CRLATTRIBUTE        = "certificateRevocationList;binary";
	public static final String DEFAULT_DELTACRLATTRIBUTE   = "deltaRevocationList;binary";
	public static final String DEFAULT_ARLATTRIBUTE        = "authorityRevocationList;binary";
	public static final String DEFAULT_PORT                = "389";
	public static final String DEFAULT_SSLPORT             = "636";
	public static final String DEFAULT_TIMEOUT             = "5000"; // 5 seconds
	public static final String DEFAULT_READTIMEOUT         = "30000"; // 30 seconds
	public static final String DEFAULT_STORETIMEOUT        = "60000"; // 1 minute


	// Default Values

	protected static final String HOSTNAMES                = "hostname";
	protected static final String USESSL                   = "usessl";
	protected static final String PORT                     = "port";
	protected static final String BASEDN                   = "baswdn";
	protected static final String LOGINDN                  = "logindn";
	protected static final String LOGINPASSWORD            = "loginpassword";
	protected static final String TIMEOUT                  = "timeout";
	protected static final String READTIMEOUT              = "readtimeout";
	protected static final String STORETIMEOUT             = "storetimeout";
	protected static final String CREATENONEXISTING        = "createnonexisting";
	protected static final String MODIFYEXISTING           = "modifyexisting"; 
	protected static final String ADDNONEXISTINGATTR       = "addnonexistingattr"; 
	protected static final String MODIFYEXISTINGATTR       = "modifyexistingattr"; 
	protected static final String USEROBJECTCLASS          = "userobjectclass";
	protected static final String CAOBJECTCLASS            = "caobjectclass";
	protected static final String USERCERTATTRIBUTE        = "usercertattribute";
	protected static final String CACERTATTRIBUTE          = "cacertattribute";
	protected static final String CRLATTRIBUTE             = "crlattribute";
	protected static final String DELTACRLATTRIBUTE        = "deltacrlattribute";
	protected static final String ARLATTRIBUTE             = "arlattribute";
	protected static final String USEFIELDINLDAPDN         = "usefieldsinldapdn";
	protected static final String ADDMULTIPLECERTIFICATES  = "addmultiplecertificates";
	protected static final String REMOVEREVOKED            = "removerevoked";    
	protected static final String REMOVEUSERONCERTREVOKE   = "removeusersoncertrevoke";    
	protected static final String CREATEINTERMEDIATENODES  = "createintermediatenodes";
	protected static final String SETUSERPASSWORD          = "setuserpasssword";
	
	/** Arrays used to extract attributes to store in LDAP */
	protected static final String[] MATCHINGEXTRAATTRIBUTES    = {"CN","L","OU"};
	protected static final String[] MATCHINGPERSONALATTRIBUTES = {"ST","O","uid","initials","title","postalCode","businessCategory","postalAddress","telephoneNumber"};


	public LdapPublisher(){
		super();
		data.put(TYPE, new Integer(TYPE_LDAPPUBLISHER));

		setHostnames("");
		setUseSSL(true);
		setPort(DEFAULT_SSLPORT);
		setBaseDN("");
		setLoginDN("");
		setLoginPassword("");
		int connectiontimeout = getConnectionTimeOut();
		setConnectionTimeOut(connectiontimeout);
		setCreateNonExistingUsers(true);
		setModifyExistingUsers(true);     
		setModifyExistingAttributes(false);
		setAddNonExistingAttributes(true);
		setUserObjectClass(DEFAULT_USEROBJECTCLASS);
		setCAObjectClass(DEFAULT_CAOBJECTCLASS);
		setUserCertAttribute(DEFAULT_USERCERTATTRIBUTE);
		setCACertAttribute(DEFAULT_CACERTATTRIBUTE);
		setCRLAttribute(DEFAULT_CRLATTRIBUTE);
		setDeltaCRLAttribute(DEFAULT_DELTACRLATTRIBUTE);
		setARLAttribute(DEFAULT_ARLATTRIBUTE);     
		setUseFieldInLdapDN(new ArrayList());
		// By default use only one certificate for each user
		setAddMultipleCertificates(false);
		setRemoveRevokedCertificates(true);
		setRemoveUsersWhenCertRevoked(false);
	}

	// Public Methods




    /* (non-Javadoc)
     * @see org.ejbca.core.model.ca.publisher.BasePublisher#storeCertificate(org.ejbca.core.model.log.Admin, java.security.cert.Certificate, java.lang.String, java.lang.String, java.lang.String, int, int, long, int, java.lang.String, int, long, org.ejbca.core.model.ra.ExtendedInformation)
     */
    protected boolean storeCertificate(Admin admin, Certificate incert, String username, String password, String cafp, int status, int type, long revocationDate, int revocationReason, String tag, int certificateProfileId, long lastUpdate, ExtendedInformation extendedinformation) throws PublisherException{
		throw new Error("This method should never be called since the method with 'userDN' is implemented.");
    }
	/**
	 * Publishes certificate in LDAP, if the certificate is not revoked. If the certificate is revoked, nothing is done
	 * and the publishing is counted as successful (i.e. returns true).
	 * 
	 * @see org.ejbca.core.model.ca.publisher.BasePublisher
	 */    
	public boolean storeCertificate(Admin admin, Certificate incert, String username, String password, String userDN, String cafp, int status, int type, long revocationDate, int revocationReason, String tag, int certificateProfileId, long lastUpdate, ExtendedInformation extendedinformation) throws PublisherException{
		if (log.isTraceEnabled()) {
			log.trace(">storeCertificate(username="+username+")");
		}

		if ( (status == CertificateDataBean.CERT_REVOKED) || (status == CertificateDataBean.CERT_TEMP_REVOKED) ) {
        	// Call separate script for revocation
        	revokeCertificate(admin, incert, username, revocationReason, userDN);
        } else if (status == CertificateDataBean.CERT_ACTIVE) {
            // Don't publish non-active certificates
    		int ldapVersion = LDAPConnection.LDAP_V3;
    		LDAPConnection lc = createLdapConnection();

    		final String dn;
    		final String certdn;
    		try {
    			// Extract the users DN from the cert.
    			certdn = CertTools.getSubjectDN(incert);
    			log.debug( "Constructing DN for: " + username);
    			dn = constructLDAPDN(certdn, userDN);
    			log.debug("LDAP DN for user " +username +" is '" + dn+"'");
    		} catch (Exception e) {
    			String msg = intres.getLocalizedMessage("publisher.errorldapdecode", "certificate");
    			log.error(msg, e);            
    			throw new PublisherException(msg);            
    		}

    		// Extract the users email from the cert.
    		String email = CertTools.getEMailAddress(incert);

    		// Check if the entry is already present, we will update it with the new certificate.
    		// To work well with the LdapSearchPublisher we need to pass the full certificate DN to the 
    		// search function, and not only the LDAP DN. The regular publisher should only use the LDAP DN though, 
    		// but the searchOldEntity function will take care of that.
    		LDAPEntry oldEntry = searchOldEntity(username, ldapVersion, lc, certdn, userDN, email);

    		// PART 2: Create LDAP entry
    		LDAPEntry newEntry = null;
    		ArrayList modSet = new ArrayList();
    		LDAPAttributeSet attributeSet = null;
    		String attribute = null;
    		String objectclass = null;

    		if (type == CertificateDataBean.CERTTYPE_ENDENTITY) {
    			log.debug("Publishing end user certificate to first available server of " + getHostnames());

    			if (oldEntry != null) {
    				modSet = getModificationSet(oldEntry, certdn, email, ADD_MODIFICATION_ATTRIBUTES, true, password);
    			} else {
    				objectclass = getUserObjectClass(); // just used for logging
    				attributeSet = getAttributeSet(incert, getUserObjectClass(), certdn, email, true, true, password, extendedinformation);
    			}

    			try {
    				attribute = getUserCertAttribute();
    				LDAPAttribute certAttr = new LDAPAttribute(getUserCertAttribute(), incert.getEncoded());
    				if (oldEntry != null) {
    					String oldDn = oldEntry.getDN();
    					if (getAddMultipleCertificates()) {
    						modSet.add(new LDAPModification(LDAPModification.ADD, certAttr));                        
    						log.debug("Appended new certificate in user entry; " + username+": "+oldDn);
    					} else {
    						modSet.add(new LDAPModification(LDAPModification.REPLACE, certAttr));                                            
    						log.debug("Replaced certificate in user entry; " + username+": "+oldDn);
    					}
    				} else {
    					attributeSet.add(certAttr);
    					log.debug("Added new certificate to user entry; " + username+": "+dn);
    				}
    			} catch (CertificateEncodingException e) {
    				String msg = intres.getLocalizedMessage("publisher.errorldapencodestore", "certificate");
    				log.error(msg, e);
    				throw new PublisherException(msg);                
    			}
    		} else if ((type == CertificateDataBean.CERTTYPE_SUBCA) || (type == CertificateDataBean.CERTTYPE_ROOTCA)) {
    			log.debug("Publishing CA certificate to first available server of " + getHostnames());

    			if (oldEntry != null) {
    				modSet = getModificationSet(oldEntry, certdn, null, false, false, password);
    			} else {
    				objectclass = getCAObjectClass(); // just used for logging
    				attributeSet = getAttributeSet(incert, getCAObjectClass(), certdn, null, true, false, password, extendedinformation);
    			}
    			try {
    				attribute = getCACertAttribute();
    				LDAPAttribute certAttr = new LDAPAttribute(getCACertAttribute(), incert.getEncoded());
    				if (oldEntry != null) {
    					modSet.add(new LDAPModification(LDAPModification.REPLACE, certAttr));
    				} else {
    					attributeSet.add(certAttr);
    					// Also create using the crlattribute, it may be required
    					LDAPAttribute crlAttr = new LDAPAttribute(getCRLAttribute(), getFakeCRL());
    					attributeSet.add(crlAttr);
    					// Also create using the arlattribute, it may be required
    					LDAPAttribute arlAttr = new LDAPAttribute(getARLAttribute(), getFakeCRL());
    					attributeSet.add(arlAttr);
    					log.debug("Added (fake) attribute for CRL and ARL.");
    				}
    			} catch (CertificateEncodingException e) {
    				String msg = intres.getLocalizedMessage("publisher.errorldapencodestore", "certificate");
    				log.error(msg, e);
    				throw new PublisherException(msg);            
    			}
    		} else {
    			String msg = intres.getLocalizedMessage("publisher.notpubltype", new Integer(type));
    			log.info(msg);
    			throw new PublisherException(msg);                      
    		}

    		// PART 3: MODIFICATION AND ADDITION OF NEW USERS
    		// Try all the listed servers
    		Iterator servers = getHostnameList().iterator();
    		boolean connectionFailed;
    		do {
    			connectionFailed = false;
    			String currentServer = (String) servers.next();
    			try {
    				TCPTool.probeConnectionLDAP(currentServer, Integer.parseInt(getPort()), getConnectionTimeOut());	// Avoid waiting for halfdead-servers
    				lc.connect(currentServer, Integer.parseInt(getPort()));
    				// authenticate to the server
    				lc.bind(ldapVersion, getLoginDN(), getLoginPassword().getBytes("UTF8"), ldapBindConstraints);            
    				// Add or modify the entry
    				if (oldEntry != null && getModifyExistingUsers()) {
    					LDAPModification[] mods = new LDAPModification[modSet.size()]; 
    					mods = (LDAPModification[])modSet.toArray(mods);
    					String oldDn = oldEntry.getDN();
    					log.debug("Writing modification to DN: "+oldDn);
    					lc.modify(oldDn, mods, ldapStoreConstraints);
    					String msg = intres.getLocalizedMessage("publisher.ldapmodify", "CERT", oldDn);
    					log.info(msg);  
    				} else {
    					if(this.getCreateNonExistingUsers()){     
    						if (oldEntry == null) {           
    							// Check if the intermediate parent node is present, and if it is not
    							// we can create it, of allowed to do so by the publisher configuration
    							if(getCreateIntermediateNodes()) {
    								final String parentDN = new String(dn.substring(dn.indexOf(',') + 1));
    								try {
    									lc.read(parentDN, ldapSearchConstraints);
    								} catch(LDAPException e) {
    									if(e.getResultCode() == LDAPException.NO_SUCH_OBJECT) {
    										this.createIntermediateNodes(lc, dn);
    										String msg = intres.getLocalizedMessage("publisher.ldapaddedintermediate", "CERT", parentDN);
    										log.info(msg);
    									}
    								}
    							}
    							newEntry = new LDAPEntry(dn, attributeSet);
    							log.debug("Adding DN: "+dn);
    							lc.add(newEntry, ldapStoreConstraints);
    							String msg = intres.getLocalizedMessage("publisher.ldapadd", "CERT", dn);
    							log.info(msg);
    						}
    					}  
    				}
    			} catch (LDAPException e) {
    				connectionFailed = true;
    				if (servers.hasNext()) {
    					log.warn("Failed to publish to " + currentServer + ". Trying next in list.");
    				} else {
    					String msg = intres.getLocalizedMessage("publisher.errorldapstore", "certificate", attribute, objectclass, dn, e.getMessage());
    					log.error(msg, e);  
    					throw new PublisherException(msg);            
    				}
    			} catch (UnsupportedEncodingException e) {
    				String msg = intres.getLocalizedMessage("publisher.errorpassword", getLoginPassword());
    				log.error(msg, e);
    				throw new PublisherException(msg);            
    			} finally {
    				// disconnect with the server
    				try {
    					lc.disconnect(ldapDisconnectConstraints);
    				} catch (LDAPException e) {
    					String msg = intres.getLocalizedMessage("publisher.errordisconnect", getLoginPassword());
    					log.error(msg, e);
    				}
    			}
    		} while (connectionFailed && servers.hasNext()) ;
        } else {
			String msg = intres.getLocalizedMessage("publisher.notpublwithstatus", new Integer(status));
			log.info(msg);        	
        }
		log.trace("<storeCertificate()");
		return true;

	}

	/**
	 * Creates intermediate nodes to host an LDAP entry at <code>dn</code>.
	 * @param lc Active LDAP connection
	 * @param dn Distinguished name
	 * @throws PublisherException
	 */
	private void createIntermediateNodes(LDAPConnection lc, String dn) throws PublisherException {
		LDAPAttributeSet attrSet;
		LDAPEntry entry;
		String dnFragment, rdn, field, value;
		int ix = dn.lastIndexOf(getBaseDN()) - 1;

		while((ix = dn.lastIndexOf(',', ix - 1)) >= 0) {
			dnFragment = new String(dn.substring(ix + 1));
			rdn = new String(dnFragment.substring(0, dnFragment.indexOf(',')));
			field = new String(rdn.substring(0, rdn.indexOf('=')));
			value = new String(rdn.substring(rdn.indexOf('=') + 1));
			try {
				lc.read(dnFragment, ldapSearchConstraints);
			} catch(LDAPException e) {
				if(e.getResultCode() == LDAPException.NO_SUCH_OBJECT) {
					attrSet = new LDAPAttributeSet();
					attrSet.add(getObjectClassAttribute(field));
					attrSet.add(new LDAPAttribute(field.toLowerCase(), value));
					entry = new LDAPEntry(dnFragment, attrSet);

					try {
						lc.add(entry, ldapStoreConstraints);
						log.debug("Created node " + dnFragment);
					} catch(LDAPException e1) {
						String msg = intres.getLocalizedMessage("publisher.ldapaddedintermediate", dnFragment);
						log.error(msg, e1);
						throw new PublisherException(msg);            
					}
				}
			}
		}
	}

	/**
	 * Returns an LDAPAttribute initialized with the LDAP object class
	 * definition that corresponds to a DN <code>field</code>.
	 * <p>The only allowed fields are </code>O</code> (organization) and
	 * <code>OU</code> (organizationalUnit).</p>
	 *
	 * @param field A DN field (case-insensitive). Only <code>O</code> and
	 * <code>OU</code> are allowed. 
	 * @return LDAPAttribute initialized with the LDAP object class definition
	 * that corresponds to a DN <code>field</code>.
	 */
	private LDAPAttribute getObjectClassAttribute(String field) {
		final String lowCaseField = field.toLowerCase();
		if(lowCaseField.equals("o")) {
			return new LDAPAttribute("objectclass", new String[] { "top", "organization" });
		} else if(lowCaseField.equals("ou")) {
			return new LDAPAttribute("objectclass", new String[] { "top", "organizationalUnit" });
		} else {
			String msg = intres.getLocalizedMessage("publisher.ldapintermediatenotappropriate", field);
			log.warn(msg);
			return new LDAPAttribute("objectclass");
		}
	}

	/**
	 * @see org.ejbca.core.model.ca.publisher.BasePublisher
	 */    
	public boolean storeCRL(Admin admin, byte[] incrl, String cafp, int number, String userDN) throws PublisherException{
    	log.trace(">storeCRL");
		int ldapVersion = LDAPConnection.LDAP_V3;

		final String dn;
		final String crldn;
		final boolean isDeltaCRL;
		try {
			// Extract the users DN from the crl. Use the least number of encodings...
			final X509CRL crl = CertTools.getCRLfromByteArray(incrl);
			crldn = CertTools.stringToBCDNString(crl.getIssuerDN().toString());
			// Is it a delta CRL?
			if (crl.getExtensionValue(X509Extensions.DeltaCRLIndicator.getId()) != null) {
				isDeltaCRL = true;
			} else {
				isDeltaCRL = false;
			}
			// Construct the DN used for the LDAP object entry
			dn = constructLDAPDN(crldn, userDN);
		} catch (Exception e) {
			String msg = intres.getLocalizedMessage("publisher.errorldapdecode", "CRL");
			log.error(msg, e);        	
			throw new PublisherException(msg);            
		}

		LDAPConnection lc = createLdapConnection();

		// Check if the entry is already present, we will update it with the new CRL.
		LDAPEntry oldEntry = searchOldEntity(null, ldapVersion, lc, crldn, userDN, null);

		LDAPEntry newEntry = null;
		ArrayList modSet = new ArrayList();
		LDAPAttributeSet attributeSet = null;

		if (oldEntry != null) {
			modSet = getModificationSet(oldEntry, crldn, null, false, false, null);
		} else {
			attributeSet = getAttributeSet(null, this.getCAObjectClass(), crldn, null, true, false, null,null);
		}

		if(isDeltaCRL) {
			// It's a delta CRL.
			LDAPAttribute attr = new LDAPAttribute(getDeltaCRLAttribute(), incrl);
			if (oldEntry != null) {
				modSet.add(new LDAPModification(LDAPModification.REPLACE, attr));
			} else {
				attributeSet.add(attr);
			}
		} else {
			// It's a CRL
			LDAPAttribute crlAttr = new LDAPAttribute(getCRLAttribute(), incrl);
			LDAPAttribute arlAttr = new LDAPAttribute(getARLAttribute(), incrl);
			if (oldEntry != null) {
				modSet.add(new LDAPModification(LDAPModification.REPLACE, crlAttr));
				modSet.add(new LDAPModification(LDAPModification.REPLACE, arlAttr));
			} else {
				attributeSet.add(crlAttr);
				attributeSet.add(arlAttr);
			}
		}
		if (oldEntry == null) {
			newEntry = new LDAPEntry(dn, attributeSet);
		}
		// Try all the listed servers
		Iterator servers = getHostnameList().iterator();
		boolean connectionFailed;
		do {
			connectionFailed = false;
			String currentServer = (String) servers.next();
			try {
				TCPTool.probeConnectionLDAP(currentServer, Integer.parseInt(getPort()), getConnectionTimeOut());	// Avoid waiting for halfdead-servers
				// connect to the server
				lc.connect(currentServer, Integer.parseInt(getPort()));
				// authenticate to the server
				lc.bind(ldapVersion, getLoginDN(), getLoginPassword().getBytes("UTF8"), ldapBindConstraints);
				// Add or modify the entry
				if (oldEntry != null) {
					LDAPModification[] mods = new LDAPModification[modSet.size()]; 
					mods = (LDAPModification[])modSet.toArray(mods);
					lc.modify(dn, mods, ldapStoreConstraints);
					String msg = intres.getLocalizedMessage("publisher.ldapmodify", "CRL", dn);
					log.info(msg);  
				} else {
					lc.add(newEntry, ldapStoreConstraints);
					String msg = intres.getLocalizedMessage("publisher.ldapadd", "CRL", dn);
					log.info(msg);  
				}
			} catch (LDAPException e) {
				connectionFailed = true;
				if (servers.hasNext()) {
					log.warn("Failed to publish to " + currentServer + ". Trying next in list.");
				} else {
					String msg = intres.getLocalizedMessage("publisher.errorldapstore", "CRL", getCRLAttribute(), getCAObjectClass(), dn, e.getMessage());
					log.error(msg, e);  
					throw new PublisherException(msg);            
				}
			} catch (UnsupportedEncodingException e) {
				String msg = intres.getLocalizedMessage("publisher.errorpassword", getLoginPassword());
				log.error(msg, e);
				throw new PublisherException(msg);            
			} finally {
				// disconnect with the server
				try {
					lc.disconnect(ldapDisconnectConstraints);
				} catch (LDAPException e) {
					String msg = intres.getLocalizedMessage("publisher.errordisconnect");
					log.error(msg, e);
				}
			}
		} while (connectionFailed && servers.hasNext()) ;
    	log.trace("<storeCRL");
		return true;
	}
	protected boolean storeCRL(Admin admin, byte[] incrl, String cafp, int number) {
		throw new Error("This method should never be called since the method with 'userDN' is implemented.");
	}

	/**
	 * Revokes a certificate, which means for LDAP that we may remove the certificate or the whole user entry.
	 */    
	public void revokeCertificate(Admin admin, Certificate cert, String username, int reason, String userDN) throws PublisherException{
		log.trace(">revokeCertificate()");

		// Check first if we should do anything then revoking
		boolean removecert = getRemoveRevokedCertificates();
		boolean removeuser = getRemoveUsersWhenCertRevoked();
		if ( (!removecert) && (!removeuser) ) {
			log.debug("The configuration for the publisher '" + getDescription() + "' does not allow removing of certificates or users.");
			return;
		}
		if (removecert) {
			log.debug("Removing user certificate from ldap");
		}
		if (removeuser) {
			log.debug("Removing user entry from ldap");
		}

		int ldapVersion = LDAPConnection.LDAP_V3;
		LDAPConnection lc = createLdapConnection();

		final String dn;
		final String certdn;
		try {
			// Extract the users DN from the cert.
			certdn = CertTools.getSubjectDN(cert);
			dn = constructLDAPDN(certdn, userDN);
		} catch (Exception e) {
			String msg = intres.getLocalizedMessage("publisher.errorldapdecode", "certificate");
			log.error(msg, e);            
			throw new PublisherException(msg);            
		}

		// Extract the users email from the cert.
		String email = CertTools.getEMailAddress(cert);

		// Check if the entry is already present, we will update it with the new certificate.
		LDAPEntry oldEntry = searchOldEntity(username, ldapVersion, lc, certdn, userDN, email);

		ArrayList modSet = null;

		if (!CertTools.isCA(cert)) {
			log.debug("Removing end user certificate from first available server of " + getHostnames());

			if (oldEntry != null) {          
				if (removecert) {
					// Don't try to remove the cert if there does not exist any
					LDAPAttribute oldAttr = oldEntry.getAttribute(getUserCertAttribute());
					if (oldAttr != null) {
						modSet = getModificationSet(oldEntry, certdn, null, false, true, null);
						LDAPAttribute attr = new LDAPAttribute(getUserCertAttribute());
						modSet.add(new LDAPModification(LDAPModification.DELETE, attr));                    
					} else {
						String msg = intres.getLocalizedMessage("publisher.inforevokenocert");
						log.info(msg);
					}            		
				}
			} else {
				String msg = intres.getLocalizedMessage("publisher.errorrevokenoentry");
				log.error(msg);            
				throw new PublisherException(msg);            
			}
		} else  {
			// Removal of CA certificate isn't support because of object class restrictions
			log.debug("Not removing CA certificate from first available server of " + getHostnames() + ", because of object class restrictions.");
		}

		// Try all the listed servers
		Iterator servers = getHostnameList().iterator();
		boolean connectionFailed;
		do {
			connectionFailed = false;
			String currentServer =(String) servers.next(); 
			log.debug("currentServer: "+currentServer);
			try {
				TCPTool.probeConnectionLDAP(currentServer, Integer.parseInt(getPort()), getConnectionTimeOut());	// Avoid waiting for halfdead-servers
				lc.connect(currentServer, Integer.parseInt(getPort()));
				// authenticate to the server
				lc.bind(ldapVersion, getLoginDN(), getLoginPassword().getBytes("UTF8"), ldapBindConstraints);            
				// Add or modify the entry
				if (oldEntry != null && modSet != null && getModifyExistingUsers()) {
					if (removecert) {
						LDAPModification[] mods = new LDAPModification[modSet.size()]; 
						mods = (LDAPModification[])modSet.toArray(mods);
						lc.modify(oldEntry.getDN(), mods, ldapStoreConstraints);            		
					}
					if (removeuser) {
						lc.delete(oldEntry.getDN(), ldapStoreConstraints);            		
					}
					String msg = intres.getLocalizedMessage("publisher.ldapremove", dn);
					log.info(msg);  
				} else {
					if (log.isDebugEnabled()) {
						if (oldEntry == null) {
							log.debug("Not modifying LDAP entry because there is no existing entry.");						
						}
						if (modSet == null) {
							log.debug("Not modifying LDAP entry because we don't have anything to modify.");						
						}
						if (!getModifyExistingUsers()) {
							log.debug("Not modifying LDAP entry because we're not configured to do so.");						
						}
					}
				}
			} catch (LDAPException e) {
				connectionFailed = true;
				if (servers.hasNext()) {
					log.warn("Failed to publish to " + currentServer + ". Trying next in list.");
				} else {
					String msg = intres.getLocalizedMessage("publisher.errorldapremove", dn);
					log.error(msg, e);  
					throw new PublisherException(msg);            
				}
			} catch (UnsupportedEncodingException e) {
				String msg = intres.getLocalizedMessage("publisher.errorpassword", getLoginPassword());
				log.error(msg, e);
				throw new PublisherException(msg);            
			} finally {
				// disconnect with the server
				try {
					lc.disconnect(ldapDisconnectConstraints);
				} catch (LDAPException e) {
					String msg = intres.getLocalizedMessage("publisher.errordisconnect");
					log.error(msg, e);
				}
			}
		} while (connectionFailed && servers.hasNext()) ;
		log.trace("<revokeCertificate()");
	}

	/** SearchOldEntity is the only method differing between regular ldap and ldap search publishers.
	 *  Apart from how they find existing users, the publishing works the same.
	 *  
	 *  @param dn the DN from the certificate, can be used to extract search information or a LDAP DN
	 */
	protected LDAPEntry searchOldEntity(String username, int ldapVersion, LDAPConnection lc, String certDN, String userDN, String email) throws PublisherException {
		LDAPEntry oldEntry = null; // return value
		// Try all the listed servers
		final Iterator servers = getHostnameList().iterator();
		boolean connectionFailed;
		do {
			connectionFailed = false;
			final String currentServer = (String) servers.next();
			log.debug("Current server is: "+currentServer);
			final String ldapdn = constructLDAPDN(certDN, userDN);
			try {
				TCPTool.probeConnectionLDAP(currentServer, Integer.parseInt(getPort()), getConnectionTimeOut());	// Avoid waiting for halfdead-servers
				// connect to the server
				lc.connect(currentServer, Integer.parseInt(getPort()));
				// authenticate to the server
				lc.bind(ldapVersion, getLoginDN(), getLoginPassword().getBytes("UTF8"), ldapBindConstraints);
				// try to read the old object
				log.debug("Searching for old entry with DN '" + ldapdn+"'");
				oldEntry = lc.read(ldapdn, ldapSearchConstraints);
				if (log.isDebugEnabled()) {
					if (oldEntry != null) {
						log.debug("Found an old entry with DN '" + ldapdn+"'");
					} else {
						log.debug("Did not find an old entry with DN '" + ldapdn+"'");
					}					
				}
			} catch (LDAPException e) {
				if (e.getResultCode() == LDAPException.NO_SUCH_OBJECT) {
					log.debug("No old entry exist for '" + ldapdn + "'.");
				} else {
					connectionFailed = true;
					if (servers.hasNext()) {
						log.warn("Failed to publish to " + currentServer + ". Trying next in list.");
					} else {
						String msg = intres.getLocalizedMessage("publisher.errorldapbind", e.getMessage());
						log.error(msg, e);
						throw new PublisherException(msg);                                
					}
				}
			} catch (UnsupportedEncodingException e) {
				String msg = intres.getLocalizedMessage("publisher.errorpassword", getLoginPassword());
				throw new PublisherException(msg);            
			} finally {
				// disconnect with the server
				try {
					lc.disconnect(ldapDisconnectConstraints);
				} catch (LDAPException e) {
					String msg = intres.getLocalizedMessage("publisher.errordisconnect");
					log.error(msg, e);
				}
			}
		} while (connectionFailed && servers.hasNext()) ;
		return oldEntry;
	}

	/**
	 * @see org.ejbca.core.model.ca.publisher.BasePublisher
	 */    
	public void testConnection(Admin admin) throws PublisherConnectionException {
		int ldapVersion = LDAPConnection.LDAP_V3;
		LDAPConnection lc = createLdapConnection();
		// Try all the listed servers
		Iterator servers = getHostnameList().iterator();
		boolean connectionFailed;
		do {
			connectionFailed = false;
			String currentServer = (String) servers.next();
			LDAPEntry entry = null;
			try {
				TCPTool.probeConnectionLDAP(currentServer, Integer.parseInt(getPort()), getConnectionTimeOut());	// Avoid waiting for halfdead-servers
				// connect to the server
				lc.connect(currentServer, Integer.parseInt(getPort()));
				// authenticate to the server
				lc.bind(ldapVersion, getLoginDN(), getLoginPassword().getBytes("UTF8"), ldapBindConstraints);
				// try to read the base object
				String baseDN = getBaseDN();
				log.debug("Trying to read top node '"+baseDN+"'");
				entry = lc.read(baseDN, ldapSearchConstraints);			
				if(entry == null) {
					String msg = intres.getLocalizedMessage("publisher.errornobinddn");
					throw new PublisherConnectionException(msg);
				}
				log.debug("Entry" + entry.toString());
			} catch (LDAPException e) {
				connectionFailed = true;
				if (servers.hasNext()) {
					log.warn("Failed to connect to " + currentServer + ". Trying next in list.", e);
				} else {
					String msg = intres.getLocalizedMessage("publisher.errorldapbind", e.getMessage());
					log.error(msg, e);
					throw new PublisherConnectionException(msg);                            
				}
			} catch (UnsupportedEncodingException e) {
				String msg = intres.getLocalizedMessage("publisher.errorpassword", getLoginPassword());
				log.error(msg, e);
				throw new PublisherConnectionException(msg);            
			} finally {
				// disconnect with the server
				try {
					lc.disconnect(ldapDisconnectConstraints);
				} catch (LDAPException e) {
					String msg = intres.getLocalizedMessage("publisher.errordisconnect");
					log.error(msg, e);
				}
			}
		} while (connectionFailed && servers.hasNext()) ;
	} 

	protected LDAPConnection createLdapConnection() {
		// Set timeouts
		int connectiontimeout = getConnectionTimeOut();
		ldapBindConstraints.setTimeLimit(connectiontimeout); 
		ldapDisconnectConstraints.setTimeLimit(connectiontimeout);
		ldapConnectionConstraints.setTimeLimit(connectiontimeout);
		ldapSearchConstraints.setTimeLimit(getReadTimeOut());
		ldapStoreConstraints.setTimeLimit(getStoreTimeOut());
		if (log.isDebugEnabled()) {
			log.debug("connecttimeout: "+ldapConnectionConstraints.getTimeLimit());
			log.debug("bindtimeout: "+ldapBindConstraints.getTimeLimit());
			log.debug("disconnecttimeout: "+ldapDisconnectConstraints.getTimeLimit());
			log.debug("readtimeout: "+ldapSearchConstraints.getTimeLimit());
			log.debug("storetimeout: "+ldapStoreConstraints.getTimeLimit());
		}
		LDAPConnection lc;
		if (getUseSSL()) {
			lc = new LDAPConnection(new LDAPJSSESecureSocketFactory());
		} else {
			lc = new LDAPConnection();
		}
		lc.setConstraints(ldapConnectionConstraints);
		return lc;
	}

	/**
	 *  Returns the hostnames of ldap server.
	 */    
	public ArrayList getHostnameList(){
		ArrayList ret = new ArrayList();	// <String>
		String[] hostnames = getHostnames().split(";");
		for (int i=0; i<hostnames.length; i++) {
			ret.add(hostnames[i]);
		}
		return ret;
	}

	/**
	 *  Returns the hostnames of ldap server.
	 */    
	public String getHostnames(){
		return (String) data.get(HOSTNAMES);
	}

	/**
	 *  Sets the hostname of ldap server.
	 */        
	public void setHostnames(String hostnames){
		data.put(HOSTNAMES, hostnames);	
	}

	/**
	 *  Returns true if SSL connetion should be used.
	 */    
	public boolean getUseSSL (){
		return ((Boolean) data.get(USESSL)).booleanValue();
	}

	/**
	 *  Sets if SSL connetion should be used.
	 */        
	public void setUseSSL (boolean usessl){
		data.put(USESSL, Boolean.valueOf(usessl));	
	}

	/**
	 *  Returns the port of ldap server.
	 */    
	public String getPort (){
		return (String) data.get(PORT);
	}

	/**
	 *  Sets the port of ldap server.
	 */        
	public void setPort(String port){
		data.put(PORT, port);	
	}

	/**
	 *  Returns the basedn of ldap server.
	 */    
	public String getBaseDN(){
		return (String) data.get(BASEDN);
	}

	/**
	 *  Sets the basedn of ldap server.
	 */        
	public void setBaseDN(String basedn){
		data.put(BASEDN, basedn);	
	}

	/**
	 *  Returns the logindn to the ldap server.
	 */    
	public String getLoginDN(){
		return (String) data.get(LOGINDN);
	}

	/**
	 *  Sets the logindn to the ldap server.
	 */        
	public void setLoginDN(String logindn){
		data.put(LOGINDN, logindn);	
	}

	/**
	 *  Returns the loginpwd to the ldap server.
	 */    
	public String getLoginPassword(){
		return (String) data.get(LOGINPASSWORD);
	}

	/**
	 *  Sets the loginpwd to the ldap server.
	 */        
	public void setLoginPassword(String loginpwd){
		data.put(LOGINPASSWORD, loginpwd);	
	}

	/**
	 *  Returns true if nonexisting users should be created
	 */    
	public boolean getCreateNonExistingUsers (){
		return ((Boolean) data.get(CREATENONEXISTING)).booleanValue();
	}

	/**
	 *  Sets if nonexisting users should be created.
	 */        
	public void setCreateNonExistingUsers (boolean createnonexistingusers){
		data.put(CREATENONEXISTING, Boolean.valueOf(createnonexistingusers));	
	}

	/**
	 *  Returns true if existing users should be modified.
	 */    
	public boolean getModifyExistingUsers (){
		return ((Boolean) data.get(MODIFYEXISTING)).booleanValue();
	}

	/**
	 *  Sets if existing users should be modified.
	 */        
	public void setModifyExistingUsers (boolean modifyexistingusers){
		data.put(MODIFYEXISTING, Boolean.valueOf(modifyexistingusers));	
	}

	/**
	 *  Returns true if existing user attributes should be modified.
	 */    
	public boolean getModifyExistingAttributes (){
		return ((Boolean) data.get(MODIFYEXISTINGATTR)).booleanValue();
		
	}

	/**
	 *  Sets if existing user attributes should be modified.
	 */        
	public void setModifyExistingAttributes (boolean modifyexistingattributes){
		data.put(MODIFYEXISTINGATTR, Boolean.valueOf(modifyexistingattributes));	
	}

	/**
	 *  Returns true if existing user attributes should be added.
	 */    
	public boolean getAddNonExistingAttributes (){
		return ((Boolean) data.get(ADDNONEXISTINGATTR)).booleanValue();
	}

	/**
	 *  Sets if existing user attributes should be added.
	 */        
	public void setAddNonExistingAttributes (boolean modifyexistingusers){
		data.put(ADDNONEXISTINGATTR, Boolean.valueOf(modifyexistingusers));	
	}


	/**
	 *  Returns the user object class in the ldap instance
	 */    
	public String getUserObjectClass(){
		return (String) data.get(USEROBJECTCLASS);
	}

	/**
	 *  Sets the user object class in the ldap instance
	 */        
	public void setUserObjectClass(String userobjectclass){
		data.put(USEROBJECTCLASS, userobjectclass);	
	}

	/**
	 *  Returns the CA object class in the ldap instance
	 */    
	public String getCAObjectClass(){
		return (String) data.get(CAOBJECTCLASS);
	}

	/**
	 *  Sets the CA object class in the ldap instance
	 */        
	public void setCAObjectClass(String caobjectclass){
		data.put(CAOBJECTCLASS, caobjectclass);	
	}

	/**
	 *  Returns the user cert attribute in the ldap instance
	 */    
	public String getUserCertAttribute(){
		return (String) data.get(USERCERTATTRIBUTE);
	}

	/**
	 *  Sets the user cert attribute in the ldap instance
	 */        
	public void setUserCertAttribute(String usercertattribute){
		data.put(USERCERTATTRIBUTE, usercertattribute);	
	}

	/**
	 *  Returns the ca cert attribute in the ldap instance
	 */    
	public String getCACertAttribute(){
		return (String) data.get(CACERTATTRIBUTE);
	}

	/**
	 *  Sets the ca cert attribute in the ldap instance
	 */        
	public void setCACertAttribute(String cacertattribute){
		data.put(CACERTATTRIBUTE, cacertattribute);	
	}

	/**
	 *  Returns the CRL attribute in the ldap instance
	 */    
	public String getCRLAttribute(){
		return (String) data.get(CRLATTRIBUTE);
	}

	/**
	 *  Sets the CRL attribute in the ldap instance
	 */        
	public void setCRLAttribute(String crlattribute){
		data.put(CRLATTRIBUTE, crlattribute);	
	}

	/**  Returns the delta CRL attribute in the ldap instance
	 */
	public String getDeltaCRLAttribute(){
		if(data.get(DELTACRLATTRIBUTE) == null) {
			this.setDeltaCRLAttribute(DEFAULT_DELTACRLATTRIBUTE);
			return DEFAULT_DELTACRLATTRIBUTE;
		} else {
			return (String) data.get(DELTACRLATTRIBUTE);
		}
	}

	/**
	 *  Sets the delta CRL attribute in the ldap instance
	 */
	public void setDeltaCRLAttribute(String deltacrlattribute){
		data.put(DELTACRLATTRIBUTE, deltacrlattribute);   
	}

	/**
	 *  Returns the ARL attribute in the ldap instance
	 */    
	public String getARLAttribute(){
		return (String) data.get(ARLATTRIBUTE);
	}

	/**
	 *  Sets the ARL attribute in the ldap instance
	 */        
	public void setARLAttribute(String arlattribute){
		data.put(ARLATTRIBUTE, arlattribute);	
	}

	/**
	 * Method getting a collection of DNFieldExtractor constants indicating which
	 * fields of the x509 certificate DN that should be used in the LDAP DN.
	 * 
	 * Valid values are  DNFieldExtractor.E, .UID, .CN, .SN, .GIVENNAME, .SURNAME, .T, .OU, .L 
	 * Other values should be defined in baseDN instead.
	 * If there exists multiple fields of the same type, then will all fields be mappen to LDAP dn.
	 * 
	 * @return Collection of (Integer) containing DNFieldExtractor constants.
	 */
	public Collection getUseFieldInLdapDN(){
		return (Collection) data.get(USEFIELDINLDAPDN);
	}

	/**
	 * Method setting a collection of DNFieldExtractor constants indicating which
	 * fields of the x509 certificate DN that should be used in the LDAP DN.
	 * 
	 * Valid values are  DNFieldExtractor.E, .UID, .CN, .SN, .GIVENNAME, .SURNAME, .T, .OU, .L 
	 * Other values should be defined in baseDN instead.
	 * If there exists multiple fields of the same type, then will all fields be mappen to LDAP dn.
	 * 
	 * @return Collection of (Integer) containing DNFieldExtractor constants.
	 */
	public void setUseFieldInLdapDN(Collection usefieldinldapdn){
		data.put(USEFIELDINLDAPDN, usefieldinldapdn);
	}    

	/**
	 *  Returns true if multiple certificates should be appended to existing user entries, instead of replacing.
	 */    
	public boolean getAddMultipleCertificates (){
		return ((Boolean) data.get(ADDMULTIPLECERTIFICATES)).booleanValue();
	}
	/**
	 *  Sets if multiple certificates should be appended to existing user entries, instead of replacing.
	 */        
	public void setAddMultipleCertificates (boolean appendcerts){
		data.put(ADDMULTIPLECERTIFICATES, Boolean.valueOf(appendcerts)); 
	}

	public void setRemoveRevokedCertificates( boolean removerevoked ){
		data.put(REMOVEREVOKED, Boolean.valueOf(removerevoked));  
	}

	public boolean getRemoveRevokedCertificates(){
		boolean removerevoked = true; //-- default value
		if ( data.get(REMOVEREVOKED) != null ) {
			removerevoked = ((Boolean)data.get(REMOVEREVOKED)).booleanValue();
		}
		return removerevoked;
	}

	public void setRemoveUsersWhenCertRevoked( boolean removeuser ){
		data.put(REMOVEUSERONCERTREVOKE, Boolean.valueOf(removeuser));  
	}

	public boolean getRemoveUsersWhenCertRevoked(){
		boolean removeuser = false; //-- default value
		if ( data.get(REMOVEUSERONCERTREVOKE) != null ) {
			removeuser = ((Boolean)data.get(REMOVEUSERONCERTREVOKE)).booleanValue();
		}
		return removeuser;
	}

	public void setCreateIntermediateNodes( boolean createnodes ){
		data.put(CREATEINTERMEDIATENODES, Boolean.valueOf(createnodes));  
	}

	public boolean getCreateIntermediateNodes(){
		boolean createnodes = false; //-- default value
		if ( data.get(CREATEINTERMEDIATENODES) != null ) {
			createnodes = ((Boolean)data.get(CREATEINTERMEDIATENODES)).booleanValue();
		}
		return createnodes;
	}

	public void setUserPassword( boolean userpassword ){
		data.put(SETUSERPASSWORD, Boolean.valueOf(userpassword));  
	}

	public boolean getSetUserPassword(){
		boolean userpassword = false; //-- default value
		if ( data.get(SETUSERPASSWORD) != null ) {
			userpassword = ((Boolean)data.get(SETUSERPASSWORD)).booleanValue();
		}
		return userpassword;
	}

	/** Return timout in milliseconds */
	public int getConnectionTimeOut() {
		int timeout = Integer.parseInt(DEFAULT_TIMEOUT);
		if ( data.get(TIMEOUT) != null ) {
			timeout = Integer.parseInt((String) data.get(TIMEOUT));
		}
		return timeout;
	}
	/** Return timout in milliseconds */
	public int getReadTimeOut() {
		int timeout = Integer.parseInt(DEFAULT_READTIMEOUT);
		if ( data.get(READTIMEOUT) != null ) {
			timeout = Integer.parseInt((String) data.get(READTIMEOUT));
		}
		return timeout;
	}
	/** Return timout in milliseconds */
	public int getStoreTimeOut() {
		int timeout = Integer.parseInt(DEFAULT_STORETIMEOUT);
		if ( data.get(STORETIMEOUT) != null ) {
			timeout = Integer.parseInt((String) data.get(STORETIMEOUT));
		}
		return timeout;
	}
	
	/** Set timout in milliseconds */
	public void setConnectionTimeOut(int timeout) {
		data.put(TIMEOUT, Integer.toString(timeout));  
		ldapBindConstraints.setTimeLimit(timeout);
		ldapConnectionConstraints.setTimeLimit(timeout);
		ldapDisconnectConstraints.setTimeLimit(timeout);
	}
	/** Set timout in milliseconds */
	public void setReadTimeOut(int timeout) {
		data.put(READTIMEOUT, Integer.toString(timeout));  
		ldapSearchConstraints.setTimeLimit(timeout);
	}
	/** Set timout in milliseconds */
	public void setStoreTimeOut(int timeout) {
		data.put(STORETIMEOUT, Integer.toString(timeout)); 
		ldapStoreConstraints.setTimeLimit(timeout);
	}

	// Private methods   
	/**
	 * Returns a list of attributes found in DN
	 * Can only be used when the same attribute string is used in EJBCA and LDAP
	 *  
	 * @param dn The DN to search
	 * @param attributes Strings to search for in the DN
	 * @return An LDAPAttributeSet containing all attributes found int the DN
	 */
	protected Collection getAttributesFromDN(String dn, String[] attributes) {
		Collection attributeList= new LinkedList();
		for (int i =0; i<attributes.length;i++){
			String attribute = CertTools.getPartFromDN(dn, attributes[i]);
			if (attribute != null) {
				attributeList.add(new LDAPAttribute(attributes[i], attribute));
			}
		}
		return attributeList;
	}

	/**
	 * Returns a list containing LDAPModification's
	 * Can only be used when the same attribute string is used in EJBCA and LDAP
	 * 
	 * @param dn The DN to search
	 * @param oldDn the old DN
	 * @param attributes Strings to search for in the DN
	 * @return An ArrayList containing all attributes found int the DN
	 */
	protected ArrayList getModificationSetFromDN(String dn, LDAPEntry oldEntry, String[] attributes){
		ArrayList modset = new ArrayList();
		boolean modifyExisting = getModifyExistingAttributes();
		boolean addNonExisting = getAddNonExistingAttributes();
		for (int i =0; i<attributes.length;i++){
			String attribute = CertTools.getPartFromDN(dn, attributes[i]);
			LDAPAttribute oldattribute = oldEntry.getAttribute(attributes[i]);
			if(oldattribute!=null) {log.debug("removeme, oldattribute="+oldattribute.toString());}
			if(dn!=null) {log.debug("removeme, dn="+dn);}
			if ( ((attribute != null) && (oldattribute == null) && addNonExisting) || ( ((attribute != null) && (oldattribute != null )) && modifyExisting) ) {
				LDAPAttribute attr = new LDAPAttribute(attributes[i], attribute);
				modset.add(new LDAPModification(LDAPModification.REPLACE, attr));
			}
		}
		return modset;
	}

	/**
	 * Creates an LDAPAttributeSet.
	 *
	 * @param cert the certificate to use or null if no cert involved.
	 * @param objectclass the objectclass the attribute set should be of.
	 * @param dn dn of the LDAP entry.
	 * @param email email address for entry, or null
	 * @param extra if we should add extra attributes except the objectclass to the attributeset.
	 * @param person true if this is a person-entry, false if it is a CA.
	 * @param password, users password, to be added into SecurityObjects, and AD
	 * @param extendedinformation, for future use...
	 *
	 * @return LDAPAtributeSet created...
	 */
	protected LDAPAttributeSet getAttributeSet(Certificate cert, String objectclass, String dn, String email, boolean extra, boolean person,
			String password, ExtendedInformation extendedinformation) {
		if (log.isTraceEnabled()) {
			log.trace(">getAttributeSet(dn="+dn+", email="+email+")");			
		}
		LDAPAttributeSet attributeSet = new LDAPAttributeSet();
		LDAPAttribute attr = new LDAPAttribute("objectclass");
		// The full LDAP object tree is divided with ; in the objectclass
		StringTokenizer token = new StringTokenizer(objectclass,";");
		while (token.hasMoreTokens()) {
			String value = token.nextToken();
			log.debug("Adding objectclass value: "+value);
			attr.addValue(value);
		}
		attributeSet.add(attr);

		/* To Add an entry to the directory,
		 *   -- Create the attributes of the entry and add them to an attribute set
		 *   -- Specify the DN of the entry to be created
		 *   -- Create an LDAPEntry object with the DN and the attribute set
		 *   -- Call the LDAPConnection add method to add it to the directory
		 */
		if (extra) {
			attributeSet.addAll(getAttributesFromDN(dn, MATCHINGEXTRAATTRIBUTES));

			// Only persons have (normally) all these extra attributes. 
			// A CA might have them if you don't use the default objectClass, but we don't
			// handle that case.
			if (person) {
				// First get the easy ones where LDAP and EJBCA spelling is the same
				attributeSet.addAll(getAttributesFromDN(dn, MATCHINGPERSONALATTRIBUTES));
				// sn means surname in LDAP, and is required for persons
				String cn = CertTools.getPartFromDN(dn, "CN");
				String sn = CertTools.getPartFromDN(dn, "SURNAME");
				if ( (sn == null) && (cn != null) ) {
					// Only construct this if we are the standard object class
					if (getUserObjectClass().endsWith("inetOrgPerson")) {
						// Take surname to be the last part of the cn
						int index = cn.lastIndexOf(' ');
						if (index <=0) {
							// If there is no natural sn, use cn since sn is required
							sn = cn;
						} else {
							if (index < cn.length()) {
								sn = new String(cn.substring(index+1));
							}
						}
					}
				}
				if (sn != null) {
					attributeSet.add(new LDAPAttribute("sn", sn));
				}
				// gn means givenname in LDAP, and is required for persons
				String gn = CertTools.getPartFromDN(dn, "GIVENNAME");
				if ( (gn == null) && (cn != null) ) {
					// Only construct this if we are the standard object class
					if (getUserObjectClass().endsWith("inetOrgPerson")) {
						// Take givenname to be the first part of the cn
						int index = cn.indexOf(' ');
						if (index <=0) {
							// If there is no natural gn/sn, ignore gn if we are using sn
							if (sn == null) {
								gn = cn;
							}
						} else {
							gn = new String(cn.substring(0, index));
						}
					}
				}
				if (gn != null) {
					attributeSet.add(new LDAPAttribute("givenName", gn));
				}
				String title = CertTools.getPartFromDN(dn, "T");
				if (title != null) {
					attributeSet.add(new LDAPAttribute("title", title));
				}
				if (email != null) {
					attributeSet.add(new LDAPAttribute("mail", email));											
				}
				
				// If we have selected to use the SN (serialNUmber DN field, we will also add it as an attribute
				// This is not present in the normal objectClass (inetOrgPerson)
				// Modifying the schema is as simple as adding serialNumber as MAY in the inetOrgPerson object class in inetorgperson.schema.
				Collection usefields = getUseFieldInLdapDN();
				if (usefields.contains(new Integer(DNFieldExtractor.SN))) {
					String serno = CertTools.getPartFromDN(dn, "SN");
					if (serno != null) {
						attributeSet.add(new LDAPAttribute("serialNumber", serno));
					}            		
				}
				
				// If this is an objectClass which is a SecurityObject, such as simpleSecurityObject, we will add the password as well, if not null.
				if (getSetUserPassword() && (password != null)) {
					log.debug("Adding userPassword attribute");
					attributeSet.add(new LDAPAttribute("userPassword", password));
				}
				
			}
		}
		log.trace("<getAttributeSet()");
		return attributeSet;
	} // getAttributeSet


	/**
	 * Creates an LDAPModificationSet.
	 *
	 * @param oldEntry the objectclass the attribute set should be of.
	 * @param dn dn of the LDAP entry.
	 * @param email email address for entry, or null
	 * @param extra if we should add extra attributes except the objectclass to the
	 *        modificationset.
	 * @param pserson true if this is a person-entry, false if it is a CA.
	 * @param password, users password, to be added into SecurityObjects, and AD
	 * @param overwrite if true then old attributes in LDAP will be overwritten, otherwise not.
	 *
	 * @return LDAPModificationSet created...
	 */
	protected ArrayList getModificationSet(LDAPEntry oldEntry, String dn, String email, boolean extra, boolean person, String password) {
		if (log.isTraceEnabled()) {
			log.trace(">getModificationSet(dn="+dn+", email="+email+")");			
		}
		boolean modifyExisting = getModifyExistingAttributes();
		boolean addNonExisting = getAddNonExistingAttributes();
		ArrayList modSet = new ArrayList();
		// We get this, because we can not modify attributes that are present in the original DN
		// i.e. if the ldap entry have a DN, we are not allowed to modify that
		if (extra) {
			log.debug("Adding extra attributes to modificationSet");
			modSet.addAll(getModificationSetFromDN(dn, oldEntry, MATCHINGEXTRAATTRIBUTES));
			// Only persons have (normally) all these extra attributes. 
			// A CA might have them if you don't use the default objectClass, but we don't
			// handle that case.
			if (person) {
				// sn means surname in LDAP, and is required for inetOrgPerson
				String cn = CertTools.getPartFromDN(dn, "CN");
				String sn = CertTools.getPartFromDN(dn, "SURNAME");
				if ( (sn == null) && (cn != null) ) {
					// Only construct this if we are the standard object class
					if (getUserObjectClass().endsWith("inetOrgPerson")) {
						// Take surname to be the last part of the cn
						int index = cn.lastIndexOf(' ');
						if (index <=0) {
							// If there is no natural sn, use cn since sn is required
							sn = cn;
						} else {
							if (index < cn.length()) {
								sn = new String(cn.substring(index+1));
							}
						}
					}
				}
				LDAPAttribute oldsn = oldEntry.getAttribute("sn");
				if (((sn != null) && (oldsn == null) && addNonExisting) || ( (sn != null) && (oldsn != null ) && modifyExisting)) {
					LDAPAttribute attr = new LDAPAttribute("sn", sn);
					modSet.add(new LDAPModification(LDAPModification.REPLACE, attr));
				}
				// gn means givenname in LDAP, and is required for inetOrgPerson
				String gn = CertTools.getPartFromDN(dn, "GIVENNAME");
				LDAPAttribute oldgn = oldEntry.getAttribute("GIVENNAME");
				if ( (gn == null) && (cn != null) ) {
					// Only construct this if we are the standard object class
					if (getUserObjectClass().endsWith("inetOrgPerson")) {
						// Take givenname to be the first part of the cn
						int index = cn.indexOf(' ');
						if (index <=0) {
							// If there is no natural gn/sn, ignore gn if we are using sn
							if (sn == null) {
								gn = cn;
							}
						} else {
							gn = new String(cn.substring(0, index));
						}
					}
					if ( ( ((gn != null) && (oldgn == null)) && addNonExisting) || ( ((gn != null) && (oldgn != null )) && modifyExisting) ) {
						LDAPAttribute attr = new LDAPAttribute("givenName", gn);
						modSet.add(new LDAPModification(LDAPModification.REPLACE, attr));
					}
				}
				String title = CertTools.getPartFromDN(dn, "T");
				LDAPAttribute oldTitle = oldEntry.getAttribute("Title");
				if ( ( (title != null) && (oldTitle == null) && addNonExisting) || ( (title != null) && (oldTitle != null ) && modifyExisting) ) {
					LDAPAttribute attr = new LDAPAttribute("givenName", title);
					modSet.add(new LDAPModification(LDAPModification.REPLACE, attr));
				}
				LDAPAttribute oldEmail = oldEntry.getAttribute("mail");
				if ( ( (email != null) && (oldEmail == null) && addNonExisting) || ( (email != null) && (oldEmail != null ) && modifyExisting) ) {
					LDAPAttribute mailAttr = new LDAPAttribute("mail", email);
					modSet.add(new LDAPModification(LDAPModification.REPLACE, mailAttr));											
				}

				// All generic personal attributes
				modSet.addAll(getModificationSetFromDN(dn, oldEntry, MATCHINGPERSONALATTRIBUTES));
				// If we have selected to use the SN (serialNUmber DN field, we will also add it as an attribute
				// This is not present in the normal objectClass (inetOrgPerson)
				Collection usefields = getUseFieldInLdapDN();
				if (usefields.contains(new Integer(DNFieldExtractor.SN))) {
					String serno = CertTools.getPartFromDN(dn, "SN");
					LDAPAttribute oldserno = oldEntry.getAttribute("SN");
					if (((serno != null) && (oldserno == null) && addNonExisting) || ( (serno != null) && (oldserno != null ) && modifyExisting)) {
						LDAPAttribute attr = new LDAPAttribute("serialNumber", serno);
						modSet.add(new LDAPModification(LDAPModification.REPLACE, attr));
					}            		
				}
				
				// If this is an objectClass which is a SecurityObject, such as simpleSecurityObject, we will add the password as well, if not null
				if ( (getSetUserPassword() && (password != null)) && (addNonExisting || modifyExisting) ) {
					log.debug("Modifying userPassword attribute");
					LDAPAttribute attr = new LDAPAttribute("userPassword", password);
					modSet.add(new LDAPModification(LDAPModification.REPLACE, attr));
				}
			}
		}
		log.trace("<getModificationSet()");
		return modSet;
	} // getModificationSet

	/**
	 * Constructs the LDAP DN for a certificate to be published. Only DN objects defined by the publisher is used.
	 * For each DN object to be published:
	 *  First the certificate DN is search for this object.
	 *  If no such certificate object then the userdata DN is searched.
	 *  If no such userdata object either the object will not be a part of the LDAP DN.
	 * @param certDN certificate DN
	 * @param userDataDN user data DN
	 * @return LDAP DN to be used.
	 */
	protected String constructLDAPDN(String certDN, String userDataDN){
		log.debug("DN in certificate '"+certDN+"'. DN in user data '"+userDataDN+"'.");
		String retval = "";
		final DNFieldExtractor certExtractor = new DNFieldExtractor(certDN, DNFieldExtractor.TYPE_SUBJECTDN);
		final DNFieldExtractor userDataExtractor = userDataDN!=null ? new DNFieldExtractor(userDataDN, DNFieldExtractor.TYPE_SUBJECTDN) : null;

		Collection usefields = getUseFieldInLdapDN();
		if(usefields instanceof List){
			Collections.sort((List) usefields);
		}
		Iterator iter = usefields.iterator(); 
		while(iter.hasNext()){
			Integer next = (Integer) iter.next();
			String dnField = certExtractor.getFieldString(next.intValue());
			if ( StringUtils.isEmpty(dnField) && userDataExtractor!=null ) {
				dnField = userDataExtractor.getFieldString(next.intValue());
			}
			if (StringUtils.isNotEmpty(dnField)) {
				if (dnField.startsWith("SN")) {
					// This is SN in Bouncycastle, but it should be serialNumber in LDAP
					dnField = "serialNumber"+new String(dnField.substring(2));
				}
				if (dnField.startsWith("E")) {
					// This is E in Bouncycastle, but it should be mail in LDAP
					dnField = "mail"+new String(dnField.substring(1));
				}
				if(retval.length() == 0) {
					retval += dnField; // first item, don't start with a comma
				} else {
					retval += "," + dnField;
				}
			}
		}
		retval = retval + "," + this.getBaseDN();
		log.debug("LdapPublisher: constructed DN: " + retval );
		return retval;	
	}

	protected static byte[] fakecrlbytes = Base64.decode(
			("MIIBKDCBkgIBATANBgkqhkiG9w0BAQUFADAvMQ8wDQYDVQQDEwZUZXN0Q0ExDzAN"+
					"BgNVBAoTBkFuYVRvbTELMAkGA1UEBhMCU0UXDTA0MDExMjE0MTQyMloXDTA0MDEx"+
					"MzE0MTQyMlqgLzAtMB8GA1UdIwQYMBaAFK1tyidIzx1qpuj5OjHl/0Ro8xTDMAoG"+
					"A1UdFAQDAgEBMA0GCSqGSIb3DQEBBQUAA4GBABBSCWRAX8xyWQSuZYqR9MC8t4/V"+
					"Tp4xTGJeT1OPlCfuyeHyjUdvdjB/TjTgc4EOJ7eIF7aQU8Mp6AcUAKil/qBlrTYa"+
					"EFVr0WDeh2Aglgm4klAFnoJjDWfjTP1NVFdN4GMizqAz/vdXOY3DaDmkwx24eaRw"+
			"7SzqXca4gE7f1GTO").getBytes());

	/**
	 * Method to lazy create the fake CRL.
	 */
	protected byte[] getFakeCRL(){
		byte[] fakecrl = null;
		try {
			X509CRL crl = CertTools.getCRLfromByteArray(fakecrlbytes);
			fakecrl = crl.getEncoded();
		} catch (CRLException e) {}
		catch (IOException e) {}
		return fakecrl;
	}

	/** 
	 * @see org.ejbca.core.model.ca.publisher.BasePublisher#clone()
	 */
	public Object clone() throws CloneNotSupportedException {
		LdapPublisher clone = new LdapPublisher();
		HashMap clonedata = (HashMap) clone.saveData();

		Iterator i = (data.keySet()).iterator();
		while(i.hasNext()){
			Object key = i.next();
			clonedata.put(key, data.get(key));
		}
		clone.loadData(clonedata);
		return clone;	
	}

	/* *
	 * @see org.ejbca.core.model.ca.publisher.BasePublisher#getLatestVersion()
	 */
	public float getLatestVersion() {		
		return LATEST_VERSION;
	}

	/** 
	 * Implemtation of UpgradableDataHashMap function upgrade. 
	 */
	public void upgrade() {
		log.trace(">upgrade");
		if(Float.compare(LATEST_VERSION, getVersion()) != 0) {
			// New version of the class, upgrade
			String msg = intres.getLocalizedMessage("publisher.upgrade", new Float(getVersion()));
			log.info(msg);
			if(data.get(ADDMULTIPLECERTIFICATES) == null) {
				setAddMultipleCertificates(false);                
			}
			if(data.get(REMOVEREVOKED) == null) {
				setRemoveRevokedCertificates(true);                
			}
			if(data.get(REMOVEUSERONCERTREVOKE) == null) {
				setRemoveUsersWhenCertRevoked(false);                
			}
			if(data.get(CREATEINTERMEDIATENODES) == null) {
				setCreateIntermediateNodes(false); // v6
			}
			if(data.get(DELTACRLATTRIBUTE) == null) {
				setDeltaCRLAttribute(DEFAULT_DELTACRLATTRIBUTE); // v7
			}
			if(data.get(ADDNONEXISTINGATTR) == null) {
				setModifyExistingAttributes(false); // v8
				setAddNonExistingAttributes(true);
			}
			if (getVersion() < 9) {
				setConnectionTimeOut(getConnectionTimeOut());	// v9
			}
			if(data.get(SETUSERPASSWORD) == null) {
				setUserPassword(false);	// v10
			}
			if (data.get(READTIMEOUT) == null) {
				setStoreTimeOut(getStoreTimeOut());	// v11
				setReadTimeOut(getReadTimeOut());
			}

			data.put(VERSION, new Float(LATEST_VERSION));
		}
		log.trace("<upgrade");
	}
}
