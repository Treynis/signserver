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
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.security.InvalidKeyException;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.security.NoSuchProviderException;
import java.security.PrivateKey;
import java.security.Signature;
import java.security.SignatureException;
import java.security.cert.CertificateEncodingException;
import java.security.cert.X509Certificate;
import java.util.Date;
import java.util.Random;

import javax.crypto.Mac;
import javax.crypto.SecretKey;
import javax.crypto.spec.SecretKeySpec;

import org.apache.log4j.Logger;
import org.bouncycastle.asn1.ASN1InputStream;
import org.bouncycastle.asn1.DERBitString;
import org.bouncycastle.asn1.DERGeneralizedTime;
import org.bouncycastle.asn1.DERInteger;
import org.bouncycastle.asn1.DERObjectIdentifier;
import org.bouncycastle.asn1.DEROctetString;
import org.bouncycastle.asn1.DEROutputStream;
import org.bouncycastle.asn1.ocsp.BasicOCSPResponse;
import org.bouncycastle.asn1.pkcs.PKCSObjectIdentifiers;
import org.bouncycastle.asn1.x509.AlgorithmIdentifier;
import org.bouncycastle.asn1.x509.GeneralName;
import org.bouncycastle.asn1.x509.X509CertificateStructure;
import org.bouncycastle.asn1.x509.X509Name;
import org.bouncycastle.cms.CMSSignedGenerator;
import org.bouncycastle.ocsp.BasicOCSPResp;
import org.ejbca.core.model.ca.SignRequestException;
import org.ejbca.core.model.ra.NotFoundException;
import org.ejbca.core.protocol.FailInfo;
import org.ejbca.core.protocol.IResponseMessage;
import org.ejbca.core.protocol.ResponseStatus;
import org.ejbca.util.Base64;
import org.ejbca.util.CertTools;

import com.novosec.pkix.asn1.cmp.CMPObjectIdentifiers;
import com.novosec.pkix.asn1.cmp.CertRepMessage;
import com.novosec.pkix.asn1.cmp.CertResponse;
import com.novosec.pkix.asn1.cmp.PKIBody;
import com.novosec.pkix.asn1.cmp.PKIHeader;
import com.novosec.pkix.asn1.cmp.PKIMessage;
import com.novosec.pkix.asn1.cmp.PKIStatusInfo;
import com.novosec.pkix.asn1.crmf.PBMParameter;

/**
 * Helper class to create different standard parts of CMP messages
 * 
 * @author tomas
 * @version $Id: CmpMessageHelper.java 6868 2009-01-22 09:17:49Z anatom $
 */
public class CmpMessageHelper {
	private static Logger log = Logger.getLogger(CmpMessageHelper.class);

	public static PKIHeader createPKIHeader(X509Name sender, X509Name recipient, String senderNonce, String recipientNonce, String transactionId) {
		PKIHeader myPKIHeader =
			new PKIHeader(
					new DERInteger(2),
					new GeneralName(sender),
					new GeneralName(recipient));
		myPKIHeader.setMessageTime(new DERGeneralizedTime(new Date()));
		if (senderNonce != null) {
			myPKIHeader.setSenderNonce(new DEROctetString(Base64.decode(senderNonce.getBytes())));					
		}
		if (recipientNonce != null) {
			myPKIHeader.setRecipNonce(new DEROctetString(Base64.decode(recipientNonce.getBytes())));
		}
		if (transactionId != null) {
			myPKIHeader.setTransactionID(new DEROctetString(Base64.decode(transactionId.getBytes())));
		}
		return myPKIHeader;
	}

    public static byte[] signPKIMessage(PKIMessage myPKIMessage, X509Certificate signCert, PrivateKey signKey, String digestAlg, String provider) throws InvalidKeyException, NoSuchProviderException, NoSuchAlgorithmException, SecurityException, SignatureException, IOException, CertificateEncodingException {
		log.trace(">signPKIMessage()");
		X509CertificateStructure signStruct = X509CertificateStructure.getInstance(new ASN1InputStream(new ByteArrayInputStream(signCert.getEncoded())).readObject());
		CmpMessageHelper.buildCertBasedPKIProtection( myPKIMessage, signStruct, signKey, digestAlg, provider);

		log.trace("<signPKIMessage()");
		// Return response as byte array 
		return CmpMessageHelper.pkiMessageToByteArray(myPKIMessage);
    }
    
	public static void buildCertBasedPKIProtection( PKIMessage pKIMessage, X509CertificateStructure cert, PrivateKey key, String digestAlg, String provider )
	throws NoSuchProviderException, NoSuchAlgorithmException, SecurityException, SignatureException, InvalidKeyException
	{
		// Select which signature algorithm we should use for the response, based on the digest algorithm.
		DERObjectIdentifier oid = PKCSObjectIdentifiers.sha1WithRSAEncryption;
		if (digestAlg.equals(CMSSignedGenerator.DIGEST_SHA256)) {
			oid = PKCSObjectIdentifiers.sha256WithRSAEncryption;			
		}
		if (digestAlg.equals(CMSSignedGenerator.DIGEST_MD5)) {
			oid = PKCSObjectIdentifiers.md5WithRSAEncryption;			
		}
		log.debug("Selected signature alg oid: "+oid.getId());
		pKIMessage.getHeader().setProtectionAlg( new AlgorithmIdentifier(oid) );
		// Most PKCS#11 providers don't like to be fed an OID as signature algorithm, so 
		// we use BC classes to translate it into a signature algorithm name instead
		final String sigAlg = new BasicOCSPResp(new BasicOCSPResponse(null, new AlgorithmIdentifier(oid), null, null)).getSignatureAlgName();
		log.debug("Signing CMP message with signature alg: "+sigAlg);
		Signature sig = Signature.getInstance(sigAlg , provider );
		sig.initSign(key);
		sig.update( pKIMessage.getProtectedBytes() );
		
		pKIMessage.setProtection( new DERBitString(sig.sign()) );
		pKIMessage.addExtraCert( cert );
	}
	
	public static byte[] protectPKIMessageWithPBE(PKIMessage msg, String keyId, String raSecret, String digestAlgId, String macAlgId, int iterationCount) throws NoSuchAlgorithmException, NoSuchProviderException, InvalidKeyException, IOException {
		log.trace(">protectPKIMessageWithPBE()");
		// Create the PasswordBased protection of the message
		PKIHeader head = msg.getHeader();
		byte[] keyIdBytes;
		try {
			keyIdBytes = keyId.getBytes("UTF-8");			
		} catch (UnsupportedEncodingException e) {
			keyIdBytes = keyId.getBytes();
			log.info("UTF-8 not available, using platform default encoding for keyIdBytes.");
		}
		head.setSenderKID(new DEROctetString(keyIdBytes));
		// SHA1
		//AlgorithmIdentifier owfAlg = new AlgorithmIdentifier("1.3.14.3.2.26");
		AlgorithmIdentifier owfAlg = new AlgorithmIdentifier(digestAlgId);
		// iterations, usually something like 1024
		DERInteger iteration = new DERInteger(iterationCount);
		// HMAC/SHA1
		//AlgorithmIdentifier macAlg = new AlgorithmIdentifier("1.2.840.113549.2.7");
		AlgorithmIdentifier macAlg = new AlgorithmIdentifier(macAlgId);
		// We need some random bytes for the nonce
		byte[] saltbytes = createSenderNonce();
		DEROctetString derSalt = new DEROctetString(saltbytes);
		
		// Create the new protected return message
		//String objectId = "1.2.840.113533.7.66.13" = passwordBasedMac;
		String objectId = CMPObjectIdentifiers.passwordBasedMac.getId();
		PBMParameter pp = new PBMParameter(derSalt, owfAlg, iteration, macAlg);
		AlgorithmIdentifier pAlg = new AlgorithmIdentifier(new DERObjectIdentifier(objectId), pp);
		head.setProtectionAlg(pAlg);
		PKIBody body = msg.getBody();
		PKIMessage ret = new PKIMessage(head, body);

		// Calculate the protection bits
		byte[] rasecret = raSecret.getBytes();
		byte[] basekey = new byte[rasecret.length + saltbytes.length];
		for (int i = 0; i < rasecret.length; i++) {
			basekey[i] = rasecret[i];
		}
		for (int i = 0; i < saltbytes.length; i++) {
			basekey[rasecret.length+i] = saltbytes[i];
		}
		// Construct the base key according to rfc4210, section 5.1.3.1
		MessageDigest dig = MessageDigest.getInstance(owfAlg.getObjectId().getId(), "BC");
		for (int i = 0; i < iterationCount; i++) {
			basekey = dig.digest(basekey);
			dig.reset();
		}
		// Do the mac
		String macOid = macAlg.getObjectId().getId();
		byte[] protectedBytes = ret.getProtectedBytes();
		Mac mac = Mac.getInstance(macOid, "BC");
		SecretKey key = new SecretKeySpec(basekey, macOid);
		mac.init(key);
		mac.reset();
		mac.update(protectedBytes, 0, protectedBytes.length);
		byte[] out = mac.doFinal();
		DERBitString bs = new DERBitString(out);

		// Finally store the protection bytes in the msg
		ret.setProtection(bs);
		
		log.trace("<protectPKIMessageWithPBE()");
		// Return response as byte array 
		return CmpMessageHelper.pkiMessageToByteArray(ret);
	}

	public static byte[] pkiMessageToByteArray(PKIMessage msg) throws IOException {
		// Return response as byte array 
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		DEROutputStream mout = new DEROutputStream( baos );
		mout.writeObject( msg );
		mout.close();
		return baos.toByteArray();
		
	}

	/** Creates a 16 bytes random sender nonce
	 * 
	 * @return byte array of length 16
	 */
	public static byte[] createSenderNonce() {
    	// Sendernonce is a random number
    	byte[] senderNonce = new byte[16];
        Random randomSource;
        randomSource = new Random();
        randomSource.nextBytes(senderNonce);
    	return senderNonce;
	}
	/**
	 * creates a very simple error message in response to msg (that's why we switch sender and recipient)
	 * @param msg
	 * @param status
	 * @param failInfo
	 * @param failText
	 * @return IResponseMessage that can be sent to user
	 */
	public static IResponseMessage createUnprotectedErrorMessage(BaseCmpMessage msg, ResponseStatus status, FailInfo failInfo, String failText) {
		// Create a failure message
		if (log.isDebugEnabled()) {
			log.debug("Creating an unprotected error message with status="+status.getValue()+", failInfo="+failInfo+", failText="+failText);
		}
		CmpErrorResponseMessage resp = new CmpErrorResponseMessage();
		resp.setSenderNonce(new String(Base64.encode(CmpMessageHelper.createSenderNonce())));
		if (msg != null) {
			resp.setRecipientNonce(msg.getSenderNonce());
			resp.setSender(msg.getRecipient());
			resp.setRecipient(msg.getSender());
			resp.setTransactionId(msg.getTransactionId());			
		} else {
			// We didn't even have a request the get these from, so send back some dummy values
			resp.setSender(new GeneralName(CertTools.stringToBcX509Name("CN=Failure Sender")));
			resp.setRecipient(new GeneralName(CertTools.stringToBcX509Name("CN=Failure Recipient")));
		}
		resp.setFailInfo(failInfo);
		resp.setStatus( status);
		resp.setFailText(failText);
		try {
			resp.create();
		} catch (InvalidKeyException e) {
			log.error("Exception during CMP processing: ", e);			
		} catch (NoSuchAlgorithmException e) {
			log.error("Exception during CMP processing: ", e);			
		} catch (NoSuchProviderException e) {
			log.error("Exception during CMP processing: ", e);			
		} catch (SignRequestException e) {
			log.error("Exception during CMP processing: ", e);			
		} catch (NotFoundException e) {
			log.error("Exception during CMP processing: ", e);			
		} catch (IOException e) {
			log.error("Exception during CMP processing: ", e);			
		}
		return resp;
	}
	
	/**
	 * creates a very simple error message in response to msg (that's why we switch sender and recipient)
	 * @param msg
	 * @param status
	 * @param failInfo
	 * @param failText
	 * @return IResponseMessage that can be sent to user
	 */
	public static PKIBody createCertRequestRejectBody(PKIHeader header, PKIStatusInfo info, int requestId, int requestType) {
		// Create a failure message
		if (log.isDebugEnabled()) {
			log.debug("Creating a cert request rejection message");
		}

		log.debug("Creating a CertRepMessage 'rejected'");

		/*
		String senderNonce = new String(Base64.encode(CmpMessageHelper.createSenderNonce()));
		String rcptNonce = null;
		X509Name sender = CertTools.stringToBcX509Name("CN=Failure Sender");
		X509Name rcpt = CertTools.stringToBcX509Name("CN=Failure Recipient");
		String transactionId = msg.getTransactionId();
		PKIHeader myPKIHeader = CmpMessageHelper.createPKIHeader(sender, rcpt, senderNonce, rcptNonce, transactionId);
		*/
		
		CertResponse myCertResponse = new CertResponse(new DERInteger(requestId), info);
		CertRepMessage myCertRepMessage = new CertRepMessage(myCertResponse);


		int respType = requestType + 1; // 1 = intitialization response, 3 = certification response etc
		log.debug("Creating response body of type respType.");
		PKIBody myPKIBody = new PKIBody(myCertRepMessage, respType); 
		
		return myPKIBody;
	}
}
