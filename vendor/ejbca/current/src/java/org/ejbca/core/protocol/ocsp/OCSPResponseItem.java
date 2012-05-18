package org.ejbca.core.protocol.ocsp;

import java.util.Date;

import org.bouncycastle.ocsp.CertificateID;
import org.bouncycastle.ocsp.CertificateStatus;

public class OCSPResponseItem {
	private CertificateID       certID;
	private CertificateStatus   certStatus;
	/* RFC 2560 2.4: The time at which the status being indicated is known to be correct. */
	private Date                thisUpdate;
	/* RFC 2560 2.4: The time at or before which newer information will be available about
	 * the status of the certificate. If nextUpdate is not set, the responder is indicating
	 * that newer revocation information is available all the time. */
	private Date                nextUpdate = null;

	public OCSPResponseItem(CertificateID certID, CertificateStatus certStatus, long untilNextUpdate) {
		this.certID = certID;
		this.certStatus = certStatus;
		this.thisUpdate = new Date();
		if (untilNextUpdate > 0) {
			this.nextUpdate = new Date(this.thisUpdate.getTime() + untilNextUpdate);
		}
	}

	public CertificateID getCertID() {
		return certID;
	}

	public CertificateStatus getCertStatus() {
		return certStatus;
	}

	public Date getThisUpdate() {
		return thisUpdate;
	}

	public Date getNextUpdate() {
		return nextUpdate;
	}
}
