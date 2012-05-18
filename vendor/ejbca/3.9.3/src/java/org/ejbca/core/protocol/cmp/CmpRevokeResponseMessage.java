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

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.cert.CRL;
import java.security.cert.Certificate;
import java.security.cert.CertificateEncodingException;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.DERUTF8String;
import org.bouncycastle.asn1.x509.X509Name;
import org.ejbca.core.model.ca.SignRequestException;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.protocol.FailInfo;
import org.ejbca.core.protocol.IRequestMessage;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.ResponseStatus;

import com.novosec.pkix.asn1.cmp.PKIBody;
import com.novosec.pkix.asn1.cmp.PKIFreeText;
import com.novosec.pkix.asn1.cmp.PKIHeader;
import com.novosec.pkix.asn1.cmp.PKIMessage;
import com.novosec.pkix.asn1.cmp.PKIStatusInfo;
import com.novosec.pkix.asn1.cmp.RevRepContent;


/**
 * A very simple confirmation message, no protection and a nullbody
 * @author tomas
 * @version $Id: CmpRevokeResponseMessage.java 5681 2008-06-02 13:36:44Z anatom $
 */
public class CmpRevokeResponseMessage extends BaseCmpMessage implements IResponseMessage {

	/**
	 * Determines if a de-serialized file is compatible with this class.
	 *
	 * Maintainers must change this value if and only if the new version
	 * of this class is not compatible with old versions. See Sun docs
	 * for <a href=http://java.sun.com/products/jdk/1.1/docs/guide
	 * /serialization/spec/version.doc.html> details. </a>
	 *
	 */
	static final long serialVersionUID = 10003L;

	private static final Logger log = Logger.getLogger(CmpRevokeResponseMessage .class);

	/** The encoded response message */
    private byte[] responseMessage = null;
    private String failText = null;
    private FailInfo failInfo = FailInfo.BAD_REQUEST;
    private ResponseStatus status = ResponseStatus.FAILURE;

    public void setCertificate(Certificate cert) {
	}

	public void setCrl(CRL crl) {
	}

	public void setIncludeCACert(boolean incCACert) {
	}
	public void setCACert(Certificate cACert) {
	}

	public byte[] getResponseMessage() throws IOException,
			CertificateEncodingException {
        return responseMessage;
	}

	public void setStatus(ResponseStatus status) {
		this.status = status;
	}

	public ResponseStatus getStatus() {
		return status;
	}

	public void setFailInfo(FailInfo failInfo) {
		this.failInfo = failInfo;
	}

	public FailInfo getFailInfo() {
		return failInfo;
	}

	public void setFailText(String failText) {
		this.failText = failText;
	}

	public String getFailText() {
		return failText;
	}

	public boolean create() throws IOException, InvalidKeyException,
			NoSuchAlgorithmException, NoSuchProviderException,
			SignRequestException, NotFoundException {

		X509Name sender = X509Name.getInstance(getSender().getName());
		X509Name recipient = X509Name.getInstance(getRecipient().getName());
		PKIHeader myPKIHeader = CmpMessageHelper.createPKIHeader(sender, recipient, getSenderNonce(), getRecipientNonce(), getTransactionId());

		PKIStatusInfo myPKIStatusInfo = new PKIStatusInfo(new DERInteger(0)); // 0 = accepted
		if (status != ResponseStatus.SUCCESS && status != ResponseStatus.GRANTED_WITH_MODS) {
			log.debug("Creating a rejection message");
			myPKIStatusInfo = new PKIStatusInfo(new DERInteger(2)); // 2 = rejection			
			myPKIStatusInfo.setFailInfo(failInfo.getAsBitString());
			if (failText != null) {
				myPKIStatusInfo.setStatusString(new PKIFreeText(new DERUTF8String(failText)));					
			}
		}
		RevRepContent myRevrepMessage = new RevRepContent(myPKIStatusInfo);

		PKIBody myPKIBody = new PKIBody(myRevrepMessage, CmpPKIBodyConstants.REVOCATIONRESPONSE);
		PKIMessage myPKIMessage = new PKIMessage(myPKIHeader, myPKIBody);

		if ((getPbeDigestAlg() != null) && (getPbeMacAlg() != null) && (getPbeKeyId() != null) && (getPbeKey() != null) ) {
			responseMessage = CmpMessageHelper.protectPKIMessageWithPBE(myPKIMessage, getPbeKeyId(), getPbeKey(), getPbeDigestAlg(), getPbeMacAlg(), getPbeIterationCount());
		} else {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			DEROutputStream mout = new DEROutputStream( baos );
			mout.writeObject( myPKIMessage );
			mout.close();
			responseMessage = baos.toByteArray();			
		}
		return true;
	}

	public boolean requireSignKeyInfo() {
		return false;
	}

	public boolean requireEncKeyInfo() {
		return false;
	}

	public void setSignKeyInfo(Certificate cert, PrivateKey key,
			String provider) {
	}

	public void setEncKeyInfo(Certificate cert, PrivateKey key,
			String provider) {
	}

	public void setSenderNonce(String senderNonce) {
		super.setSenderNonce(senderNonce);
	}

	public void setRecipientNonce(String recipientNonce) {
		super.setRecipientNonce(recipientNonce);
	}

	public void setTransactionId(String transactionId) {
		super.setTransactionId(transactionId);
	}

	public void setRecipientKeyInfo(byte[] recipientKeyInfo) {
	}

	public void setPreferredDigestAlg(String digest) {
	}

	public void setRequestType(int reqtype) {
	}

	public void setRequestId(int reqid) {
	}

    /** @see org.ejca.core.protocol.IResponseMessage
     */
    public void setProtectionParamsFromRequest(IRequestMessage reqMsg) {
    }
}
