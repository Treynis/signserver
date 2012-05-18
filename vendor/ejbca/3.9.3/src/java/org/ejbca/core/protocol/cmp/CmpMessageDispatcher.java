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

package org.ejbca.core.protocol.cmp;

import java.io.ByteArrayInputStream;
import java.rmi.RemoteException;

import javax.ejb.CreateException;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.ejbca.config.CmpConfiguration;
import org.ejbca.core.model.log.Admin;
import org.ejbca.core.protocol.FailInfo;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.ResponseStatus;
import org.ejbca.util.CertTools;

import com.novosec.pkix.asn1.cmp.PKIBody;
import com.novosec.pkix.asn1.cmp.PKIHeader;
import com.novosec.pkix.asn1.cmp.PKIMessage;

/**
 * Class that receives a CMP message and passes it on to the correct message handler.
 * 
 * ----- 
 * This processes does the following: 
 * 1. receive a CMP message 
 * 2. check which message type it is 
 * 3. dispatch to the correct message handler 
 * 4. send back the response received from the handler 
 * -----
 * 
 * Messages supported:
 * - Initialization Request - will return an Initialization Response
 * - Revocation Request - will return a Revocation Response
 * - PKI Confirmation - same as certificate confirmation accept - will return a PKIConfirm
 * - Certificate Confirmation - accept or reject by client - will return a PKIConfirm
 * 
 * @author tomas
 * @version $Id: CmpMessageDispatcher.java 6650 2008-11-27 09:37:29Z jeklund $
 */
public class CmpMessageDispatcher {
	private static final Logger log = Logger.getLogger(CmpMessageDispatcher.class);
	
	/** This defines if we allows messages that has a POPO setting of raVerify. 
	 * If this variable is true, and raVerify is the POPO defined in the message, no POPO check will be done.
	 */
	private boolean allowRaVerifyPopo = false;
	/** The default CA used for signing requests, if it is not given in the request itself. */
	private String defaultCA = null;
	/** Defines which component from the DN should be used as username in EJBCA. Can be DN, UID or nothing. Nothing means that the DN will be used to look up the user. */
	private String extractUsernameComponent = null;
	private Admin admin;
	
	public CmpMessageDispatcher(Admin adm) {
		this.admin = adm;
		// Install BouncyCastle provider
		CertTools.installBCProvider();
		
		// Read parameters 
		allowRaVerifyPopo = CmpConfiguration.getAllowRAVerifyPOPO();
		defaultCA = CmpConfiguration.getDefaultCA();
		extractUsernameComponent = CmpConfiguration.getExtractUsernameComponent();
	}
	
	/** The message may have been received by any transport protocol, and is passed here in it's binary asn.1 form.
	 * 
	 * @param message der encoded CMP message
	 * @return IResponseMessage containing the CMP response message or null if there is no message to send back
	 */
	public IResponseMessage dispatch(byte[] message) {
		IResponseMessage ret = null;
		try {
			PKIMessage req = null;
			try {
				req = PKIMessage.getInstance(new ASN1InputStream(new ByteArrayInputStream(message)).readObject());				
			} catch (Exception e) {
				log.error("Error parsing CMP message: ", e);
				// If we could not read the message, we should return an error BAD_REQUEST
				ret = CmpMessageHelper.createUnprotectedErrorMessage(null, ResponseStatus.FAILURE, FailInfo.BAD_REQUEST, "Can not parse request message");
				return ret;
			}
			PKIHeader header = req.getHeader();
			PKIBody body = req.getBody();
			
			int tagno = body.getTagNo();
			if (log.isDebugEnabled()) {
				log.debug("Received CMP message with pvno="+header.getPvno()+", sender="+header.getSender().toString()+", recipient="+header.getRecipient().toString());
				log.debug("Body is of type: "+tagno);
				log.debug(req);
				//log.debug(ASN1Dump.dumpAsString(req));				
			}
			BaseCmpMessage cmpMessage = null;
			ICmpMessageHandler handler = null;
			int unknownMessageType = -1;
			switch (tagno) {
			case 0:
				// 0 and 2 are both certificate requests
				handler = new CrmfMessageHandler(admin);
				cmpMessage = new CrmfRequestMessage(req, defaultCA, allowRaVerifyPopo, extractUsernameComponent);
				break;
			case 2:
				handler = new CrmfMessageHandler(admin);
				cmpMessage = new CrmfRequestMessage(req, defaultCA, allowRaVerifyPopo, extractUsernameComponent);
				break;
			case 19:
				// PKI confirm
				handler = new ConfirmationMessageHandler();
				cmpMessage = new GeneralCmpMessage(req);
				break;
			case 24:
				// Certificate confirmation
				handler = new ConfirmationMessageHandler();
				cmpMessage = new GeneralCmpMessage(req);
				break;
			case 11:
				// Revocation request
				handler = new RevocationMessageHandler(admin);
				cmpMessage = new GeneralCmpMessage(req);
				break;
			default:
				unknownMessageType = tagno;
				log.info("Received an unknown message type, tagno="+tagno);
				break;
			}
			if ( (handler != null) && (cmpMessage != null) ) {
				ret  = handler.handleMessage(cmpMessage);
				if (ret != null) {
					log.debug("Received a response message from CmpMessageHandler.");
				} else {
					log.error("CmpMessageHandler returned a null message");
				}
			} else {
				log.error("Something is null! Handler="+handler+", cmpMessage="+cmpMessage);
				if (unknownMessageType > -1) {
					log.error("Unknown message type "+unknownMessageType+" received, creating error message");
					ret = CmpMessageHelper.createUnprotectedErrorMessage(null, ResponseStatus.FAILURE, FailInfo.BAD_REQUEST, "Can not handle message type");					
				}

			}
		} catch (CreateException e) {
			log.error("Exception during CMP processing: ", e);
		} catch (RemoteException e) {
			log.error("Exception during CMP processing: ", e);
		}

		return ret;
	}
	
}
